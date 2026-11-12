/**
 * AgileLab Notifications Management
 */
const NotificationsPage = {
    init: function() {
        this.bindEvents();
    },

    bindEvents: function() {
        const markAllBtn = document.getElementById('btn-mark-all-read');
        if (markAllBtn) {
            markAllBtn.onclick = () => this.markAllAsRead();
        }

        window.addEventListener('newNotification', (e) => {
            this.prependNotification(e.detail);
        });
    },

    handleItemClick: function(el) {
        const id = el.getAttribute('data-notif-id');
        const linkUrl = el.getAttribute('data-link-url');
        this.markAsRead(id, linkUrl);
    },

    markAsRead: async function(id, linkUrl) {
        try {
            await fetch(`/api/notifications/${id}/read`, { method: 'POST' });
            const item = document.getElementById(`notif-item-${id}`);
            if (item) {
                item.classList.remove('unread');
                item.classList.add('read');
            }
            if (linkUrl) {
                window.location.href = linkUrl;
            }
        } catch (e) {
            console.error('Failed to mark notification read:', e);
            if (linkUrl) window.location.href = linkUrl;
        }
    },

    markAllAsRead: async function() {
        try {
            const resp = await fetch('/api/notifications/read-all', { method: 'POST' });
            if (resp.ok) {
                document.querySelectorAll('.notification').forEach(n => {
                    n.classList.remove('unread');
                    n.classList.add('read');
                });
                const badge = document.getElementById('navbar-notif-badge');
                if (badge) badge.style.display = 'none';
            }
        } catch (e) {
            console.error('Failed to mark all notifications read:', e);
        }
    },

    prependNotification: function(n) {
        const list = document.querySelector('.notifications-list');
        if (!list) return;

        const li = document.createElement('li');
        li.id = `notif-item-${n.id}`;
        li.className = 'notification unread';
        li.onclick = () => this.markAsRead(n.id, n.linkUrl);

        li.innerHTML = `
            <div style="display:flex; justify-content:space-between; align-items:center;">
                <h4 style="margin:0 0 5px 0; font-size:1.05rem;">${n.title}</h4>
                <span style="font-size:0.75rem; color:#888;">${n.createdAt || 'Just now'}</span>
            </div>
            <p style="margin:0; color:#555;">${n.message}</p>
        `;

        list.prepend(li);
    }
};

document.addEventListener('DOMContentLoaded', () => {
    NotificationsPage.init();
});
