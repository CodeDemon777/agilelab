/**
 * AgileLab Global Application Coordinator
 */
const App = {
    stompClient: null,
    currentUser: null,
    connected: false,
    activeIncomingCall: null,

    init: async function() {
        await this.fetchCurrentUser();
        this.initWebSocket();
        this.bindGlobalEvents();
    },

    fetchCurrentUser: async function() {
        try {
            const resp = await fetch('/api/auth/current');
            if (resp.ok) {
                this.currentUser = await resp.json();
            }
        } catch (e) {
            console.warn('User not authenticated or error fetching current user', e);
        }
    },

    initWebSocket: function() {
        if (!this.currentUser) return;

        const socket = new SockJS('/ws');
        this.stompClient = Stomp.over(socket);
        this.stompClient.debug = null; // disable console spam

        this.stompClient.connect({}, (frame) => {
            this.connected = true;

            // 1. Listen for personal notifications
            this.stompClient.subscribe('/user/queue/notifications', (msg) => {
                const notif = JSON.parse(msg.body);
                this.handleIncomingNotification(notif);
            });

            // 2. Listen for incoming WebRTC call signals
            this.stompClient.subscribe('/user/queue/call', (msg) => {
                const signal = JSON.parse(msg.body);
                this.handleIncomingCallSignal(signal);
            });

            // 3. Listen for global presence updates
            this.stompClient.subscribe('/topic/presence', (msg) => {
                const presence = JSON.parse(msg.body);
                this.handlePresenceUpdate(presence);
            });

            // 4. Listen for real-time messages across all rooms (for instant sidebar preview & notifications)
            this.stompClient.subscribe('/topic/messages', (msg) => {
                const chatMsg = JSON.parse(msg.body);
                window.dispatchEvent(new CustomEvent('newChatMessage', { detail: chatMsg }));
            });

            this.updateWsBadge(true);

            // Trigger event for page-specific listeners (e.g. Chat page)
            window.dispatchEvent(new CustomEvent('stompConnected', { detail: { stompClient: this.stompClient } }));
        }, (err) => {
            console.error('STOMP connection error:', err);
            this.connected = false;
            this.updateWsBadge(false);
            window.dispatchEvent(new CustomEvent('stompDisconnected'));
            // Retry after 3 seconds
            setTimeout(() => this.initWebSocket(), 3000);
        });
    },

    updateWsBadge: function(isConnected) {
        const badge = document.getElementById('ws-status-badge');
        if (badge) {
            if (isConnected) {
                badge.style.display = 'inline-flex';
                badge.style.background = 'rgba(46, 204, 113, 0.2)';
                badge.style.color = '#2ecc71';
                badge.style.borderColor = '#2ecc71';
                badge.innerHTML = '<span style="width:7px; height:7px; border-radius:50%; background:#2ecc71; display:inline-block; animation: pulseRing 1.8s infinite;"></span> Real-time Live';
            } else {
                badge.style.display = 'inline-flex';
                badge.style.background = 'rgba(231, 76, 60, 0.2)';
                badge.style.color = '#e74c3c';
                badge.style.borderColor = '#e74c3c';
                badge.innerHTML = '<span style="width:7px; height:7px; border-radius:50%; background:#e74c3c; display:inline-block;"></span> Reconnecting...';
            }
        }
    },

    handleIncomingNotification: function(notif) {
        this.showToast(`${notif.title}: ${notif.message}`);

        // Update badge
        const badge = document.getElementById('navbar-notif-badge');
        if (badge) {
            let current = parseInt(badge.innerText || '0', 10);
            badge.innerText = current + 1;
            badge.style.display = 'inline-block';
        }

        // If on notifications page, prepend to list
        window.dispatchEvent(new CustomEvent('newNotification', { detail: notif }));
    },

    handleIncomingCallSignal: function(signal) {
        if (signal.type === 'INVITE') {
            this.showIncomingCallBanner(signal);
        } else if (signal.type === 'DECLINE' || signal.type === 'LEAVE') {
            this.hideIncomingCallBanner();
        }
        // Dispatch to call page if open
        window.dispatchEvent(new CustomEvent('callSignal', { detail: signal }));
    },

    handlePresenceUpdate: function(presence) {
        const dots = document.querySelectorAll(`[data-user-presence="${presence.username}"]`);
        dots.forEach(dot => {
            if (presence.status === 'ONLINE') {
                dot.classList.add('online');
                dot.classList.remove('offline');
            } else {
                dot.classList.remove('online');
                dot.classList.add('offline');
            }
        });
    },

    showIncomingCallBanner: function(signal) {
        this.activeIncomingCall = signal;
        let banner = document.getElementById('incoming-call-banner');
        if (!banner) {
            banner = document.createElement('div');
            banner.id = 'incoming-call-banner';
            banner.className = 'call-banner-overlay';
            document.body.appendChild(banner);
        }

        const callerName = signal.fromFullName || signal.fromUser;
        const callType = signal.callType || 'VIDEO';
        const avatar = signal.fromAvatar || '/images/default-avatar.svg';

        banner.innerHTML = `
            <div class="call-banner-header">
                <img src="${avatar}" class="call-caller-avatar calling-pulse" alt="${callerName}">
                <div>
                    <h4 style="margin:0 0 4px 0; font-size:1.05rem;">${callerName}</h4>
                    <p style="margin:0; font-size:0.85rem; color:#666;">Incoming ${callType.toLowerCase()} call...</p>
                </div>
            </div>
            <div class="call-banner-actions">
                <button class="btn-accept-call" onclick="App.acceptCall('${signal.roomId}', '${signal.fromUser}', '${callType}')">Accept</button>
                <button class="btn-decline-call" onclick="App.declineCall('${signal.roomId}', '${signal.fromUser}')">Decline</button>
            </div>
        `;
        banner.classList.add('show');
    },

    hideIncomingCallBanner: function() {
        const banner = document.getElementById('incoming-call-banner');
        if (banner) {
            banner.classList.remove('show');
        }
        this.activeIncomingCall = null;
    },

    acceptCall: function(roomId, caller, callType) {
        this.hideIncomingCallBanner();
        // Redirect to call room
        window.location.href = `/call/${roomId}?target=${encodeURIComponent(caller)}&type=${callType}`;
    },

    declineCall: function(roomId, caller) {
        if (this.stompClient && this.connected) {
            this.stompClient.send('/app/call.signal', {}, JSON.stringify({
                type: 'DECLINE',
                roomId: roomId,
                toUser: caller
            }));
        }
        this.hideIncomingCallBanner();
    },

    showToast: function(message) {
        let container = document.getElementById('toast-container');
        if (!container) {
            container = document.createElement('div');
            container.id = 'toast-container';
            container.className = 'toast-container';
            document.body.appendChild(container);
        }

        const toast = document.createElement('div');
        toast.className = 'toast';
        toast.innerText = message;
        container.appendChild(toast);

        setTimeout(() => {
            toast.style.opacity = '0';
            toast.style.transition = 'opacity 0.5s ease';
            setTimeout(() => toast.remove(), 500);
        }, 4000);
    },

    bindGlobalEvents: function() {
        // Modal dismissals on overlay click
        document.querySelectorAll('.modal-overlay').forEach(overlay => {
            overlay.addEventListener('click', (e) => {
                if (e.target === overlay) {
                    overlay.classList.remove('active');
                }
            });
        });

        // Dynamic Copyright Year
        document.querySelectorAll('#copyright-year').forEach(el => {
            el.textContent = new Date().getFullYear();
        });
    }
};

function toggleMobileNav() {
    const navLinks = document.getElementById('main-nav-links');
    const toggleBtn = document.getElementById('mobile-nav-toggle');
    if (navLinks) {
        navLinks.classList.toggle('mobile-open');
        const isOpen = navLinks.classList.contains('mobile-open');
        if (toggleBtn) {
            toggleBtn.setAttribute('aria-expanded', isOpen ? 'true' : 'false');
        }
    }
}

document.addEventListener('DOMContentLoaded', () => {
    App.init();
});

