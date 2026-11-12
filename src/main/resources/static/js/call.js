/**
 * AgileLab WebRTC Voice & Video Call Client
 */
const CallManager = {
    localStream: null,
    remoteStream: null,
    peerConnection: null,
    roomId: null,
    targetUser: null,
    callType: 'VIDEO',
    isAudioMuted: false,
    isVideoMuted: false,
    isScreenSharing: false,
    callTimer: null,
    secondsElapsed: 0,

    servers: {
        iceServers: [
            { urls: 'stun:stun.l.google.com:19302' },
            { urls: 'stun:stun1.l.google.com:19302' }
        ]
    },

    init: async function() {
        const pathParts = window.location.pathname.split('/');
        this.roomId = pathParts[pathParts.length - 1];

        const params = new URLSearchParams(window.location.search);
        this.targetUser = params.get('target');
        this.callType = (params.get('type') || 'VIDEO').toUpperCase();

        this.bindUiControls();
        await this.startMedia();
        this.setupSignaling();
    },

    bindUiControls: function() {
        const btnMuteMic = document.getElementById('btn-toggle-mic');
        const btnMuteCam = document.getElementById('btn-toggle-cam');
        const btnEnd = document.getElementById('btn-end-call');
        const btnShare = document.getElementById('btn-screen-share');

        if (btnMuteMic) btnMuteMic.onclick = () => this.toggleMic();
        if (btnMuteCam) btnMuteCam.onclick = () => this.toggleCam();
        if (btnEnd) btnEnd.onclick = () => this.endCall();
        if (btnShare) btnShare.onclick = () => this.toggleScreenShare();

        const titleEl = document.getElementById('call-target-title');
        if (titleEl && this.targetUser) {
            titleEl.innerText = `${this.callType === 'VIDEO' ? 'Video' : 'Voice'} Call with ${this.targetUser}`;
        }
    },

    startMedia: async function() {
        const constraints = {
            audio: true,
            video: this.callType === 'VIDEO'
        };

        try {
            if (navigator.mediaDevices && navigator.mediaDevices.getUserMedia) {
                this.localStream = await navigator.mediaDevices.getUserMedia(constraints);
                const localVideo = document.getElementById('local-video');
                if (localVideo) {
                    localVideo.srcObject = this.localStream;
                }
                this.updateStatus('Media access granted. Connecting to peer...');
            } else {
                this.updateStatus('Media devices not supported in this browser.');
            }
        } catch (err) {
            console.warn('Could not access camera/mic (running in mock/restricted environment):', err);
            this.updateStatus('Microphone or Camera not detected or permission denied. Simulated call mode.');
            // Create a fake audio track if needed or proceed gracefully
        }
    },

    setupSignaling: function() {
        // Subscribe to call room topic for group/peer signals
        const checkStomp = () => {
            if (window.App && window.App.stompClient && window.App.connected) {
                const stomp = window.App.stompClient;

                stomp.subscribe(`/topic/call.${this.roomId}`, (msg) => {
                    const signal = JSON.parse(msg.body);
                    if (signal.fromUser !== window.App.currentUser.username) {
                        this.handleSignal(signal);
                    }
                });

                window.addEventListener('callSignal', (e) => {
                    this.handleSignal(e.detail);
                });

                // If caller, send initial OFFER
                this.createPeerConnection();
                this.initiateOffer();
            } else {
                setTimeout(checkStomp, 400);
            }
        };
        checkStomp();
    },

    createPeerConnection: function() {
        try {
            this.peerConnection = new RTCPeerConnection(this.servers);

            if (this.localStream) {
                this.localStream.getTracks().forEach(track => {
                    this.peerConnection.addTrack(track, this.localStream);
                });
            }

            this.peerConnection.ontrack = (event) => {
                const remoteVideo = document.getElementById('remote-video');
                if (remoteVideo) {
                    remoteVideo.srcObject = event.streams[0];
                    this.updateStatus('Connected');
                    this.startTimer();
                }
            };

            this.peerConnection.onicecandidate = (event) => {
                if (event.candidate) {
                    this.sendSignal({
                        type: 'CANDIDATE',
                        roomId: this.roomId,
                        candidate: event.candidate,
                        toUser: this.targetUser
                    });
                }
            };

            this.peerConnection.onconnectionstatechange = () => {
                if (this.peerConnection.connectionState === 'connected') {
                    this.updateStatus('Call in progress');
                    this.startTimer();
                } else if (this.peerConnection.connectionState === 'disconnected' ||
                           this.peerConnection.connectionState === 'failed') {
                    this.updateStatus('Peer disconnected');
                }
            };
        } catch (e) {
            console.warn('WebRTC peer connection setup warning:', e);
        }
    },

    initiateOffer: async function() {
        if (!this.peerConnection) return;
        try {
            const offer = await this.peerConnection.createOffer();
            await this.peerConnection.setLocalDescription(offer);

            this.sendSignal({
                type: 'OFFER',
                roomId: this.roomId,
                sdp: offer,
                toUser: this.targetUser
            });
        } catch (e) {
            console.warn('Could not create offer:', e);
        }
    },

    handleSignal: async function(signal) {
        if (!signal) return;

        if (signal.type === 'OFFER' && signal.sdp) {
            if (!this.peerConnection) this.createPeerConnection();
            await this.peerConnection.setRemoteDescription(new RTCSessionDescription(signal.sdp));
            const answer = await this.peerConnection.createAnswer();
            await this.peerConnection.setLocalDescription(answer);

            this.sendSignal({
                type: 'ANSWER',
                roomId: this.roomId,
                sdp: answer,
                toUser: signal.fromUser
            });
            this.updateStatus('Connected with peer');
            this.startTimer();
        } else if (signal.type === 'ANSWER' && signal.sdp) {
            if (this.peerConnection) {
                await this.peerConnection.setRemoteDescription(new RTCSessionDescription(signal.sdp));
                this.updateStatus('Connected with peer');
                this.startTimer();
            }
        } else if (signal.type === 'CANDIDATE' && signal.candidate) {
            if (this.peerConnection) {
                try {
                    await this.peerConnection.addIceCandidate(new RTCIceCandidate(signal.candidate));
                } catch (e) {
                    console.warn('Error adding ICE candidate', e);
                }
            }
        } else if (signal.type === 'LEAVE' || signal.type === 'DECLINE') {
            this.updateStatus('Call ended by peer');
            setTimeout(() => {
                window.location.href = '/chats';
            }, 1500);
        }
    },

    sendSignal: function(signal) {
        if (window.App && window.App.stompClient && window.App.connected) {
            window.App.stompClient.send('/app/call.signal', {}, JSON.stringify(signal));
        }
    },

    toggleMic: function() {
        if (!this.localStream) return;
        const audioTrack = this.localStream.getAudioTracks()[0];
        if (audioTrack) {
            audioTrack.enabled = !audioTrack.enabled;
            this.isAudioMuted = !audioTrack.enabled;
            const btn = document.getElementById('btn-toggle-mic');
            if (btn) {
                btn.style.backgroundColor = this.isAudioMuted ? '#bdc3c7' : '#00bfff';
                btn.innerText = this.isAudioMuted ? 'Unmute Mic' : 'Mute Mic';
            }
        }
    },

    toggleCam: function() {
        if (!this.localStream) return;
        const videoTrack = this.localStream.getVideoTracks()[0];
        if (videoTrack) {
            videoTrack.enabled = !videoTrack.enabled;
            this.isVideoMuted = !videoTrack.enabled;
            const btn = document.getElementById('btn-toggle-cam');
            if (btn) {
                btn.style.backgroundColor = this.isVideoMuted ? '#bdc3c7' : '#00bfff';
                btn.innerText = this.isVideoMuted ? 'Turn Cam On' : 'Turn Cam Off';
            }
        }
    },

    toggleScreenShare: async function() {
        if (!navigator.mediaDevices || !navigator.mediaDevices.getDisplayMedia) {
            alert('Screen sharing is not supported by your browser.');
            return;
        }

        try {
            if (!this.isScreenSharing) {
                const screenStream = await navigator.mediaDevices.getDisplayMedia({ video: true });
                const screenTrack = screenStream.getVideoTracks()[0];

                if (this.peerConnection) {
                    const sender = this.peerConnection.getSenders().find(s => s.track && s.track.kind === 'video');
                    if (sender) sender.replaceTrack(screenTrack);
                }

                const localVideo = document.getElementById('local-video');
                if (localVideo) localVideo.srcObject = screenStream;

                screenTrack.onended = () => this.stopScreenShare();
                this.isScreenSharing = true;

                const btn = document.getElementById('btn-screen-share');
                if (btn) btn.innerText = 'Stop Sharing';
            } else {
                this.stopScreenShare();
            }
        } catch (e) {
            console.warn('Error starting screen share:', e);
        }
    },

    stopScreenShare: function() {
        if (!this.isScreenSharing) return;
        if (this.localStream) {
            const videoTrack = this.localStream.getVideoTracks()[0];
            if (this.peerConnection && videoTrack) {
                const sender = this.peerConnection.getSenders().find(s => s.track && s.track.kind === 'video');
                if (sender) sender.replaceTrack(videoTrack);
            }
            const localVideo = document.getElementById('local-video');
            if (localVideo) localVideo.srcObject = this.localStream;
        }
        this.isScreenSharing = false;
        const btn = document.getElementById('btn-screen-share');
        if (btn) btn.innerText = 'Screen Share';
    },

    endCall: function() {
        this.sendSignal({
            type: 'LEAVE',
            roomId: this.roomId,
            toUser: this.targetUser
        });

        if (this.localStream) {
            this.localStream.getTracks().forEach(t => t.stop());
        }
        if (this.peerConnection) {
            this.peerConnection.close();
        }
        if (this.callTimer) {
            clearInterval(this.callTimer);
        }

        window.location.href = '/chats';
    },

    startTimer: function() {
        if (this.callTimer) return;
        const timerEl = document.getElementById('call-duration-timer');
        this.secondsElapsed = 0;
        this.callTimer = setInterval(() => {
            this.secondsElapsed++;
            const mins = String(Math.floor(this.secondsElapsed / 60)).padStart(2, '0');
            const secs = String(this.secondsElapsed % 60).padStart(2, '0');
            if (timerEl) timerEl.innerText = `${mins}:${secs}`;
        }, 1000);
    },

    updateStatus: function(text) {
        const el = document.getElementById('call-status-text');
        if (el) el.innerText = text;
    }
};

document.addEventListener('DOMContentLoaded', () => {
    CallManager.init();
});
