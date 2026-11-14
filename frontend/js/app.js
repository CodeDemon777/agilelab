/**
 * AgileLab Global Application Client (Vercel Frontend)
 * Manages user authentication, STOMP WebSocket connection, notifications, and presence.
 */

window.App = {
    currentUser: null,
    stompClient: null,
    connected: false,

    init: async function () {
        await this.checkAuth();
        if (this.currentUser) {
            this.initWebSocket();
            this.loadUnreadNotificationsCount();
        }
    },

    checkAuth: async function () {
        try {
            const resp = await AppConfig.fetch('/api/auth/current');
            if (resp.ok) {
                this.currentUser = await resp.json();
                this.renderUserNavbar();
            } else {
                // If on a protected page, redirect to login
                const path = window.location.pathname;
                const isPublic = path.endsWith('login.html') || path.endsWith('signup.html') || path === '/' || path.endsWith('index.html');
                if (!isPublic) {
                    window.location.href = '/login.html';
                }
            }
        } catch (e) {
            console.warn('Auth check error (backend might be starting up):', e);
        }
    },

    renderUserNavbar: function () {
        const profileEl = document.getElementById('navbar-user-profile');
        if (profileEl && this.currentUser) {
            profileEl.style.display = 'flex';
            const nameEl = document.getElementById('navbar-user-name');
            const avatarEl = document.getElementById('navbar-user-avatar');
            if (nameEl) nameEl.innerText = this.currentUser.fullName || this.currentUser.username;
            if (avatarEl) avatarEl.src = this.currentUser.avatarUrl || '/images/default-avatar.svg';
        }

        const loginBtn = document.getElementById('navbar-login-btn');
        if (loginBtn && this.currentUser) {
            loginBtn.style.display = 'none';
        }
    },

    initWebSocket: function () {
        if (!this.currentUser) return;

        const wsEndpoint = AppConfig.getWsUrl();
        const socket = new SockJS(wsEndpoint);
        this.stompClient = Stomp.over(socket);
        this.stompClient.debug = () => {}; // silence debug in prod

        this.stompClient.connect({}, (frame) => {
            this.connected = true;
            console.log('STOMP WebSocket Connected to Render Backend');

            // 1. Subscribe to personal incoming calls
            this.stompClient.subscribe('/user/queue/call', (msg) => {
                const signal = JSON.parse(msg.body);
                this.handleIncomingCall(signal);
            });

            // 2. Subscribe to personal notifications
            this.stompClient.subscribe('/user/queue/notifications', (msg) => {
                const notif = JSON.parse(msg.body);
                this.handleNotification(notif);
            });

            // 3. User Presence updates
            this.stompClient.subscribe('/topic/presence', (msg) => {
                const presence = JSON.parse(msg.body);
                this.updatePresenceBadge(presence);
            });

            // Notify other components
            window.dispatchEvent(new CustomEvent('stompConnected', { detail: { client: this.stompClient } }));
        }, (err) => {
            this.connected = false;
            console.warn('STOMP Connection error:', err);
            setTimeout(() => this.initWebSocket(), 5000);
        });
    },

    handleIncomingCall: function (signal) {
        if (signal.type === 'INVITE' && !window.location.pathname.includes('call.html')) {
            const accept = confirm(`Incoming ${signal.callType || 'Video'} call from ${signal.fromUser}. Do you want to answer?`);
            if (accept) {
                window.location.href = `/call.html?roomId=${encodeURIComponent(signal.roomId)}&target=${encodeURIComponent(signal.fromUser)}&type=${signal.callType || 'VIDEO'}`;
            }
        }
    },

    handleNotification: function (notif) {
        const badge = document.getElementById('navbar-notif-badge');
        if (badge) {
            const cur = parseInt(badge.innerText || '0') + 1;
            badge.innerText = cur;
            badge.style.display = 'inline-block';
        }
    },

    loadUnreadNotificationsCount: async function () {
        try {
            const resp = await AppConfig.fetch('/api/notifications/unread-count');
            if (resp.ok) {
                const data = await resp.json();
                const badge = document.getElementById('navbar-notif-badge');
                if (badge && data.count > 0) {
                    badge.innerText = data.count;
                    badge.style.display = 'inline-block';
                }
            }
        } catch (e) {}
    },

    updatePresenceBadge: function (presence) {
        document.querySelectorAll(`[data-user-presence="${presence.username}"]`).forEach(dot => {
            dot.classList.toggle('online', presence.online);
            dot.classList.toggle('offline', !presence.online);
        });
    },

    logout: async function () {
        try {
            await AppConfig.fetch('/api/auth/logout', { method: 'POST' });
        } catch (e) {}
        window.location.href = '/login.html';
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
    document.querySelectorAll('#copyright-year').forEach(el => {
        el.textContent = new Date().getFullYear();
    });
});

