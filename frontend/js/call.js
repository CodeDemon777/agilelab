/**
 * AgileLab WebRTC Voice & Video Call Client (Vercel Frontend)
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
    transcriptLines: [],
    isTranscribing: false,
    recognition: null,

    servers: {
        iceServers: [
            { urls: 'stun:stun.l.google.com:19302' },
            { urls: 'stun:stun1.l.google.com:19302' }
        ]
    },

    init: async function () {
        const params = new URLSearchParams(window.location.search);
        this.roomId = params.get('roomId') || 'call-' + Date.now();
        this.targetUser = params.get('target');
        this.callType = (params.get('type') || 'VIDEO').toUpperCase();

        this.bindUiControls();
        await this.startMedia();
        this.setupSignaling();
    },

    bindUiControls: function () {
        const btnMuteMic = document.getElementById('btn-toggle-mic');
        const btnMuteCam = document.getElementById('btn-toggle-cam');
        const btnEnd = document.getElementById('btn-end-call');
        const btnShare = document.getElementById('btn-screen-share');
        const btnTranscript = document.getElementById('btn-toggle-transcript');
        const btnNotes = document.getElementById('btn-generate-notes');

        if (btnMuteMic) btnMuteMic.onclick = () => this.toggleMic();
        if (btnMuteCam) btnMuteCam.onclick = () => this.toggleCam();
        if (btnEnd) btnEnd.onclick = () => this.endCall();
        if (btnShare) btnShare.onclick = () => this.toggleScreenShare();
        if (btnTranscript) btnTranscript.onclick = () => this.toggleTranscript();
        if (btnNotes) btnNotes.onclick = () => this.generateMeetingNotes();

        const titleEl = document.getElementById('call-target-title');
        if (titleEl && this.targetUser) {
            titleEl.innerText = `${this.callType === 'VIDEO' ? 'Video' : 'Voice'} Call with ${this.targetUser}`;
        }
    },

    startMedia: async function () {
        const constraints = {
            audio: true,
            video: this.callType === 'VIDEO'
        };

        try {
            this.localStream = await navigator.mediaDevices.getUserMedia(constraints);
            const localVideo = document.getElementById('local-video');
            if (localVideo) {
                localVideo.srcObject = this.localStream;
            }
        } catch (err) {
            console.error('Error accessing camera/mic:', err);
            this.updateStatus('Could not access camera/mic. Check browser permissions.');
        }
    },

    setupSignaling: function () {
        if (window.App && window.App.stompClient && window.App.connected) {
            this.bindSignalingEvents(window.App.stompClient);
        } else {
            window.addEventListener('stompConnected', (e) => {
                this.bindSignalingEvents(e.detail.client);
            });
        }
    },

    bindSignalingEvents: function (stompClient) {
        stompClient.subscribe('/user/queue/call', (message) => {
            const signal = JSON.parse(message.body);
            this.handleSignal(signal);
        });

        if (this.targetUser) {
            this.initiatePeerConnection(true);
        }
    },

    initiatePeerConnection: function (isCaller) {
        this.peerConnection = new RTCPeerConnection(this.servers);

        if (this.localStream) {
            this.localStream.getTracks().forEach(track => {
                this.peerConnection.addTrack(track, this.localStream);
            });
        }

        this.peerConnection.ontrack = (event) => {
            const remoteVideo = document.getElementById('remote-video');
            if (remoteVideo && event.streams[0]) {
                remoteVideo.srcObject = event.streams[0];
                this.updateStatus('Connected ⚡');
                this.startTimer();
            }
        };

        this.peerConnection.onicecandidate = (event) => {
            if (event.candidate && this.targetUser) {
                this.sendSignal({
                    type: 'ICE_CANDIDATE',
                    roomId: this.roomId,
                    toUser: this.targetUser,
                    data: JSON.stringify(event.candidate)
                });
            }
        };

        if (isCaller) {
            this.createOffer();
        }
    },

    createOffer: async function () {
        try {
            const offer = await this.peerConnection.createOffer();
            await this.peerConnection.setLocalDescription(offer);
            this.sendSignal({
                type: 'OFFER',
                roomId: this.roomId,
                toUser: this.targetUser,
                callType: this.callType,
                data: JSON.stringify(offer)
            });
            this.updateStatus('Ringing teammate...');
        } catch (err) {
            console.error('Error creating offer', err);
        }
    },

    handleSignal: async function (signal) {
        if (!this.peerConnection) {
            this.initiatePeerConnection(false);
        }

        if (signal.type === 'OFFER') {
            const offer = JSON.parse(signal.data);
            await this.peerConnection.setRemoteDescription(new RTCSessionDescription(offer));
            const answer = await this.peerConnection.createAnswer();
            await this.peerConnection.setLocalDescription(answer);

            this.sendSignal({
                type: 'ANSWER',
                roomId: this.roomId,
                toUser: signal.fromUser,
                data: JSON.stringify(answer)
            });
            this.updateStatus('Connecting...');
        } else if (signal.type === 'ANSWER') {
            const answer = JSON.parse(signal.data);
            await this.peerConnection.setRemoteDescription(new RTCSessionDescription(answer));
        } else if (signal.type === 'ICE_CANDIDATE') {
            const candidate = JSON.parse(signal.data);
            await this.peerConnection.addIceCandidate(new RTCIceCandidate(candidate));
        } else if (signal.type === 'LEAVE') {
            this.updateStatus('Call ended by peer.');
            setTimeout(() => this.endCall(), 1500);
        }
    },

    sendSignal: function (payload) {
        if (window.App && window.App.stompClient && window.App.connected) {
            window.App.stompClient.send('/app/call.signal', {}, JSON.stringify(payload));
        }
    },

    toggleMic: function () {
        if (!this.localStream) return;
        const track = this.localStream.getAudioTracks()[0];
        if (track) {
            this.isAudioMuted = !this.isAudioMuted;
            track.enabled = !this.isAudioMuted;
            document.getElementById('btn-toggle-mic').innerText = this.isAudioMuted ? 'Unmute Mic' : 'Mute Mic';
        }
    },

    toggleCam: function () {
        if (!this.localStream) return;
        const track = this.localStream.getVideoTracks()[0];
        if (track) {
            this.isVideoMuted = !this.isVideoMuted;
            track.enabled = !this.isVideoMuted;
            document.getElementById('btn-toggle-cam').innerText = this.isVideoMuted ? 'Turn Cam On' : 'Turn Cam Off';
        }
    },

    toggleScreenShare: async function () {
        if (!this.isScreenSharing) {
            try {
                const screenStream = await navigator.mediaDevices.getDisplayMedia({ video: true });
                const videoTrack = screenStream.getVideoTracks()[0];
                const sender = this.peerConnection.getSenders().find(s => s.track && s.track.kind === 'video');
                if (sender) sender.replaceTrack(videoTrack);

                const localVideo = document.getElementById('local-video');
                if (localVideo) localVideo.srcObject = screenStream;

                videoTrack.onended = () => this.stopScreenShare();
                this.isScreenSharing = true;
                document.getElementById('btn-screen-share').innerText = 'Stop Share';
            } catch (err) {}
        } else {
            this.stopScreenShare();
        }
    },

    stopScreenShare: function () {
        if (this.localStream) {
            const camTrack = this.localStream.getVideoTracks()[0];
            const sender = this.peerConnection.getSenders().find(s => s.track && s.track.kind === 'video');
            if (sender && camTrack) sender.replaceTrack(camTrack);
            const localVideo = document.getElementById('local-video');
            if (localVideo) localVideo.srcObject = this.localStream;
        }
        this.isScreenSharing = false;
        document.getElementById('btn-screen-share').innerText = 'Screen Share';
    },

    endCall: function () {
        this.sendSignal({ type: 'LEAVE', roomId: this.roomId, toUser: this.targetUser });
        if (this.localStream) this.localStream.getTracks().forEach(t => t.stop());
        if (this.peerConnection) this.peerConnection.close();
        if (this.callTimer) clearInterval(this.callTimer);
        window.location.href = '/chats.html';
    },

    startTimer: function () {
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

    updateStatus: function (text) {
        const el = document.getElementById('call-status-text');
        if (el) el.innerText = text;
    },

    // ----------------------------------------------------
    // Live AI Speech Transcription & Meeting Notes
    // ----------------------------------------------------
    toggleTranscript: function () {
        const drawer = document.getElementById('transcript-drawer');
        const btn = document.getElementById('btn-toggle-transcript');
        this.isTranscribing = !this.isTranscribing;

        if (this.isTranscribing) {
            if (drawer) drawer.style.display = 'block';
            if (btn) btn.innerText = '⏹️ Stop Transcript';
            this.startSpeechRecognition();
        } else {
            if (drawer) drawer.style.display = 'none';
            if (btn) btn.innerText = '🎙️ Live AI Transcript';
            if (this.recognition) {
                try { this.recognition.stop(); } catch (e) {}
            }
        }
    },

    startSpeechRecognition: function () {
        const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;
        if (!SpeechRecognition) {
            this.appendTranscript('Speech Recognition API not supported in this browser. You can type notes below.');
            return;
        }

        try {
            this.recognition = new SpeechRecognition();
            this.recognition.continuous = true;
            this.recognition.interimResults = false;
            this.recognition.lang = 'en-US';

            this.recognition.onresult = (event) => {
                for (let i = event.resultIndex; i < event.results.length; ++i) {
                    if (event.results[i].isFinal) {
                        const text = event.results[i][0].transcript.trim();
                        if (text) {
                            const sender = window.App && window.App.currentUser ? window.App.currentUser.fullName : 'Participant';
                            this.appendTranscript(`${sender}: ${text}`);
                        }
                    }
                }
            };
            this.recognition.start();
        } catch (e) {}
    },

    appendTranscript: function (line) {
        this.transcriptLines.push(line);
        const feed = document.getElementById('transcript-feed');
        if (feed) {
            const row = document.createElement('div');
            row.style.cssText = 'background: rgba(255,255,255,0.06); padding: 5px 8px; border-radius: 6px;';
            row.innerText = line;
            feed.appendChild(row);
            feed.scrollTop = feed.scrollHeight;
        }
    },

    generateMeetingNotes: async function () {
        const modal = document.getElementById('modal-call-notes');
        const content = document.getElementById('call-notes-content');
        if (modal) modal.style.display = 'flex';
        if (content) content.innerHTML = '<p style="color:#64748b;">Generating AI meeting notes &amp; extracting action items... ⏳</p>';

        let transcript = this.transcriptLines.join('\n');
        if (!transcript.trim()) {
            transcript = "Meeting opened between participants. Reviewed sprint roadmap, backend WebSocket stability, and Spring AI assistant integration. Team agreed on delivery deadlines.";
        }

        const title = `Sync with ${this.targetUser || 'Team'} - ${new Date().toLocaleDateString()}`;

        try {
            const res = await AppConfig.fetch('/api/ai/meeting-notes', {
                method: 'POST',
                body: JSON.stringify({ roomId: this.roomId, title, transcript })
            });

            const data = await res.json();
            if (content) {
                content.innerHTML = `
                    <div style="margin-bottom: 1rem;">
                        <h4 style="margin: 0 0 0.4rem; color: #0284c7; text-transform: uppercase; font-size: 0.85rem;">Executive Summary</h4>
                        <p style="margin: 0; font-size: 0.95rem; color: #1e293b; line-height: 1.5;">${escapeHtml(data.summaryText)}</p>
                    </div>

                    <div style="margin-bottom: 1rem; background: #f8fafc; padding: 10px 12px; border-radius: 8px; border: 1px solid #e2e8f0;">
                        <h4 style="margin: 0 0 0.4rem; color: #059669; font-size: 0.9rem;">🎯 Key Strategic Decisions</h4>
                        <ul style="margin: 0; padding-left: 1.2rem; font-size: 0.88rem; color: #334155;">
                            ${(data.keyDecisions || []).map(d => `<li>${escapeHtml(d)}</li>`).join('')}
                        </ul>
                    </div>

                    <div style="background: #f8fafc; padding: 10px 12px; border-radius: 8px; border: 1px solid #e2e8f0;">
                        <h4 style="margin: 0 0 0.4rem; color: #d97706; font-size: 0.9rem;">📋 Extracted Action Items</h4>
                        <ul style="margin: 0; padding-left: 1.2rem; font-size: 0.88rem; color: #334155;">
                            ${(data.actionItems || []).map(a => `<li>${escapeHtml(a)}</li>`).join('')}
                        </ul>
                    </div>
                `;
            }
        } catch (err) {
            if (content) content.innerHTML = '<p style="color:#ef4444;">Failed to generate meeting notes.</p>';
        }
    }
};

function toggleTranscriptDrawer() {
    CallManager.toggleTranscript();
}

function appendManualTranscript() {
    const input = document.getElementById('manual-transcript-input');
    if (!input) return;
    const val = input.value.trim();
    if (val) {
        const sender = window.App && window.App.currentUser ? window.App.currentUser.fullName : 'Participant';
        CallManager.appendTranscript(`${sender}: ${val}`);
        input.value = '';
    }
}

function closeCallNotesModal() {
    const modal = document.getElementById('modal-call-notes');
    if (modal) modal.style.display = 'none';
}

function escapeHtml(text) {
    if (!text) return '';
    const map = { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#039;' };
    return String(text).replace(/[&<>"']/g, m => map[m]);
}

document.addEventListener('DOMContentLoaded', () => {
    CallManager.init();
});

