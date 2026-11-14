/**
 * AgileLab Vercel Frontend Configuration & API Client
 * Dynamically bridges the Vercel static frontend to the Render Spring Boot backend.
 */

const AppConfig = {
    DEFAULT_RENDER_BACKEND: 'https://agilelab-backend.onrender.com',

    getBackendUrl: function () {
        // 1. Explicit user override in localStorage
        const stored = localStorage.getItem('agilelab_backend_url');
        if (stored) return stored.replace(/\/+$/, '');

        // 2. Window global configuration
        if (window.AGILELAB_BACKEND_URL) {
            return window.AGILELAB_BACKEND_URL.replace(/\/+$/, '');
        }

        // 3. Same-origin fallback (when hosted together or proxied)
        if (window.location.port === '8088' || window.location.hostname === 'localhost' && !window.location.port) {
            return '';
        }

        // 4. Default Render deployment domain
        return this.DEFAULT_RENDER_BACKEND;
    },

    setBackendUrl: function (url) {
        if (!url) {
            localStorage.removeItem('agilelab_backend_url');
        } else {
            localStorage.setItem('agilelab_backend_url', url.trim().replace(/\/+$/, ''));
        }
        window.location.reload();
    },

    apiUrl: function (path) {
        const base = this.getBackendUrl();
        const cleanPath = path.startsWith('/') ? path : '/' + path;
        return base ? `${base}${cleanPath}` : cleanPath;
    },

    getWsUrl: function () {
        const base = this.getBackendUrl();
        if (!base) {
            const proto = window.location.protocol === 'https:' ? 'https:' : 'http:';
            return `${proto}//${window.location.host}/ws`;
        }
        return `${base}/ws`;
    },

    fetch: function (path, options = {}) {
        const url = this.apiUrl(path);
        const defaults = {
            credentials: 'include', // Crucial for cross-origin Spring Security session cookies
            headers: {
                'Content-Type': 'application/json'
            }
        };

        const merged = {
            ...defaults,
            ...options,
            headers: {
                ...defaults.headers,
                ...(options.headers || {})
            }
        };

        return window.fetch(url, merged);
    },

    renderConnectionIndicator: function () {
        if (document.getElementById('render-conn-indicator')) return;

        const currentBackend = this.getBackendUrl() || 'Same Origin (Local)';
        const indicator = document.createElement('div');
        indicator.id = 'render-conn-indicator';
        indicator.style.cssText = `
            position: fixed;
            bottom: 12px;
            right: 14px;
            z-index: 9999;
            background: rgba(15, 23, 42, 0.88);
            backdrop-filter: blur(8px);
            color: #e2e8f0;
            padding: 6px 12px;
            border-radius: 20px;
            font-size: 0.76rem;
            font-family: -apple-system, sans-serif;
            display: flex;
            align-items: center;
            gap: 8px;
            box-shadow: 0 4px 14px rgba(0,0,0,0.25);
            border: 1px solid rgba(255, 255, 255, 0.12);
            cursor: pointer;
            transition: all 0.2s ease;
        `;

        indicator.innerHTML = `
            <span style="width: 8px; height: 8px; border-radius: 50%; background: #10b981; display: inline-block;"></span>
            <span style="max-width: 180px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;">
                Backend: <strong style="color: #818cf8;">${this.getBackendUrl() ? new URL(this.getBackendUrl()).hostname : 'localhost'}</strong>
            </span>
            <span style="opacity: 0.7; font-size: 0.7rem;">⚙️</span>
        `;

        indicator.onclick = () => AppConfig.openConfigModal();
        document.body.appendChild(indicator);
    },

    openConfigModal: function () {
        let modal = document.getElementById('config-backend-modal');
        if (!modal) {
            modal = document.createElement('div');
            modal.id = 'config-backend-modal';
            modal.style.cssText = `
                position: fixed; top: 0; left: 0; right: 0; bottom: 0;
                background: rgba(0,0,0,0.65); backdrop-filter: blur(4px);
                z-index: 10000; display: flex; align-items: center; justify-content: center;
                padding: 1rem;
            `;
            modal.innerHTML = `
                <div style="background: white; border-radius: 14px; max-width: 460px; width: 100%; padding: 1.5rem; box-shadow: 0 20px 40px rgba(0,0,0,0.3); font-family: -apple-system, sans-serif;">
                    <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom: 1rem; border-bottom: 1px solid #e2e8f0; padding-bottom: 0.5rem;">
                        <h3 style="margin:0; font-size:1.15rem; color:#0f172a; display:flex; align-items:center; gap:0.4rem;">
                            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                                <path d="M10 13a5 5 0 0 0 7.54.54l3-3a5 5 0 0 0-7.07-7.07l-1.72 1.71"></path>
                                <path d="M14 11a5 5 0 0 0-7.54-.54l-3 3a5 5 0 0 0 7.07 7.07l1.71-1.71"></path>
                            </svg>
                            Configure Render Backend URL
                        </h3>
                        <button onclick="document.getElementById('config-backend-modal').style.display='none'" style="background:none; border:none; font-size:1.4rem; cursor:pointer;" aria-label="Close Modal">&times;</button>
                    </div>
                    <p style="font-size:0.86rem; color:#64748b; line-height:1.45; margin:0 0 1rem;">
                        Enter the public URL of your Spring Boot backend hosted on Render (or local dev server):
                    </p>
                    <input type="text" id="cfg-backend-input" value="${this.getBackendUrl()}" placeholder="https://agilelab-backend.onrender.com"
                           style="width:100%; box-sizing:border-box; padding:8px 12px; border:1px solid #cbd5e1; border-radius:8px; font-size:0.9rem; margin-bottom:1rem;">
                    
                    <div id="cfg-ping-result" style="font-size:0.82rem; margin-bottom:1rem; display:none;"></div>

                    <div style="display:flex; justify-content:space-between; align-items:center;">
                        <button onclick="AppConfig.testBackendConnection()" style="background:#f1f5f9; border:1px solid #cbd5e1; padding:6px 14px; border-radius:6px; cursor:pointer; font-size:0.85rem; font-weight:600;">
                            Test Connection
                        </button>
                        <div style="display:flex; gap:0.5rem;">
                            <button onclick="document.getElementById('config-backend-modal').style.display='none'" style="background:none; border:none; color:#64748b; cursor:pointer; font-weight:600;">Cancel</button>
                            <button onclick="AppConfig.saveModalUrl()" style="background:#4f46e5; color:white; border:none; padding:6px 16px; border-radius:6px; cursor:pointer; font-weight:700;">Save &amp; Reload</button>
                        </div>
                    </div>
                </div>
            `;
            document.body.appendChild(modal);
        } else {
            modal.style.display = 'flex';
        }
    },

    saveModalUrl: function () {
        const val = document.getElementById('cfg-backend-input').value;
        this.setBackendUrl(val);
    },

    testBackendConnection: async function () {
        const val = document.getElementById('cfg-backend-input').value.trim().replace(/\/+$/, '');
        const resBox = document.getElementById('cfg-ping-result');
        resBox.style.display = 'block';
        resBox.style.color = '#64748b';
        resBox.innerText = 'Pinging backend...';

        try {
            let res;
            try {
                res = await window.fetch(`${val}/api/health`, { method: 'GET', credentials: 'omit' });
            } catch (err) {
                res = await window.fetch(`${val}/api/auth/current`, { credentials: 'include' });
            }
            if (res.ok || res.status === 401) {
                resBox.style.color = '#10b981';
                resBox.innerHTML = 'Backend is online and reachable.';
            } else {
                resBox.style.color = '#f59e0b';
                resBox.innerText = `Backend replied with HTTP ${res.status}`;
            }
        } catch (e) {
            resBox.style.color = '#ef4444';
            resBox.innerText = 'Failed to reach backend. Check URL and CORS configuration.';
        }
    }
};

document.addEventListener('DOMContentLoaded', () => {
    AppConfig.renderConnectionIndicator();
});

