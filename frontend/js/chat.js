/**
 * AgileLab Chat Application (Vercel Frontend)
 * STOMP WebSockets, Dual-panel UI, Typing Indicators, and In-Chat AI Assistant.
 */

const ChatApp = {
    currentRoomId: null,
    currentRoom: null,
    stompClient: null,
    activeRoomSubscription: null,
    typingSubscription: null,
    typingTimeout: null,
    displayedMessageIds: new Set(),

    init: async function () {
        this.bindEvents();

        if (window.App && window.App.stompClient && window.App.connected) {
            this.stompClient = window.App.stompClient;
            this.setupGlobalSubscriptions();
        } else {
            window.addEventListener('stompConnected', (e) => {
                this.stompClient = e.detail.client;
                this.setupGlobalSubscriptions();
                if (this.currentRoomId) this.setupActiveRoom();
            });
        }

        await this.loadChatRooms();

        const params = new URLSearchParams(window.location.search);
        const requestedRoom = params.get('roomId') || params.get('room');
        if (requestedRoom) {
            this.selectRoom(requestedRoom);
        } else {
            const firstItem = document.querySelector('.chat-item');
            if (firstItem) {
                this.selectRoom(firstItem.getAttribute('data-room-id'));
            }
        }
    },

    bindEvents: function () {
        const searchInput = document.getElementById('search-chats');
        if (searchInput) {
            searchInput.addEventListener('input', (e) => {
                const query = e.target.value.toLowerCase();
                document.querySelectorAll('.chat-item').forEach(item => {
                    const name = item.querySelector('.chat-name').innerText.toLowerCase();
                    item.style.display = name.includes(query) ? 'flex' : 'none';
                });
            });
        }

        const msgInput = document.getElementById('chat-message-input');
        if (msgInput) {
            msgInput.addEventListener('keydown', (e) => {
                if (e.key === 'Enter') {
                    this.sendMessage();
                } else {
                    this.notifyTyping(true);
                }
            });
        }

        const sendBtn = document.getElementById('btn-send-message');
        if (sendBtn) sendBtn.addEventListener('click', () => this.sendMessage());

        const voiceBtn = document.getElementById('btn-voice-call');
        if (voiceBtn) voiceBtn.addEventListener('click', () => this.startCall('VOICE'));

        const videoBtn = document.getElementById('btn-video-call');
        if (videoBtn) videoBtn.addEventListener('click', () => this.startCall('VIDEO'));
    },

    loadChatRooms: async function () {
        const container = document.getElementById('chat-list-container');
        if (!container) return;

        try {
            const resp = await AppConfig.fetch('/api/chats');
            if (resp.ok) {
                const rooms = await resp.json();
                container.innerHTML = '';
                if (rooms.length === 0) {
                    container.innerHTML = '<div style="text-align:center; padding:2rem; color:#94a3b8;">No conversations yet. Click "+" to start!</div>';
                    return;
                }

                rooms.forEach(r => {
                    const item = document.createElement('div');
                    item.className = 'chat-item';
                    item.setAttribute('data-room-id', r.id);
                    item.onclick = () => ChatApp.selectRoom(r.id);
                    item.innerHTML = `
                        <div class="chat-avatar-wrapper">
                            <img src="${r.avatarUrl || '/images/default-avatar.svg'}" class="chat-avatar" alt="Avatar">
                            <span class="presence-dot ${r.online ? 'online' : 'offline'}" data-user-presence="${r.partnerUsername || ''}"></span>
                        </div>
                        <div class="chat-info">
                            <div class="chat-top-row">
                                <h3 class="chat-name">${escapeHtml(r.displayName)}</h3>
                                <span class="chat-time">${escapeHtml(r.lastMessageTime || '')}</span>
                            </div>
                            <p class="chat-preview">${escapeHtml(r.lastMessagePreview || 'No messages yet')}</p>
                        </div>
                    `;
                    container.appendChild(item);
                });
            }
        } catch (e) {
            console.error('Error loading rooms:', e);
        }
    },

    selectRoom: async function (roomId) {
        if (!roomId) return;
        this.currentRoomId = roomId;
        this.displayedMessageIds.clear();

        document.querySelectorAll('.chat-item').forEach(item => {
            item.classList.toggle('active', item.getAttribute('data-room-id') == roomId);
        });

        try {
            const resp = await AppConfig.fetch(`/api/chats/${roomId}`);
            if (resp.ok) {
                this.currentRoom = await resp.json();
                this.renderRoomHeader(this.currentRoom);
            }
        } catch (err) {
            console.error('Error loading room details', err);
        }

        await this.loadMessages(roomId);
        this.setupActiveRoom();
    },

    setupGlobalSubscriptions: function () {
        if (!this.stompClient) return;

        this.stompClient.subscribe('/topic/messages', (frame) => {
            const msg = JSON.parse(frame.body);
            this.updateChatItemPreview(msg);
        });
    },

    setupActiveRoom: function () {
        if (!this.stompClient || !this.currentRoomId) return;

        if (this.activeRoomSubscription) this.activeRoomSubscription.unsubscribe();
        if (this.typingSubscription) this.typingSubscription.unsubscribe();

        this.activeRoomSubscription = this.stompClient.subscribe(`/topic/room.${this.currentRoomId}`, (frame) => {
            const msg = JSON.parse(frame.body);
            this.appendMessage(msg);
            this.updateChatItemPreview(msg);
        });

        this.typingSubscription = this.stompClient.subscribe(`/topic/room.${this.currentRoomId}.typing`, (frame) => {
            const data = JSON.parse(frame.body);
            this.renderTypingIndicator(data);
        });
    },

    renderRoomHeader: function (room) {
        const partnerEl = document.getElementById('chat-partner-name');
        const statusEl = document.getElementById('chat-partner-status');
        const avatarEl = document.getElementById('chat-partner-avatar');
        const addMembersBtn = document.getElementById('btn-add-members');

        if (partnerEl) partnerEl.innerText = room.displayName;
        if (avatarEl) avatarEl.src = room.avatarUrl || '/images/default-avatar.svg';

        if (statusEl) {
            if (room.group) {
                statusEl.innerText = `${room.participantCount || 'Multiple'} team members`;
                if (addMembersBtn) addMembersBtn.style.display = 'inline-block';
            } else {
                statusEl.innerText = room.online ? 'Online' : 'Offline';
                if (addMembersBtn) addMembersBtn.style.display = 'none';
            }
        }
    },

    loadMessages: async function (roomId) {
        const historyContainer = document.getElementById('chat-history');
        if (!historyContainer) return;

        historyContainer.innerHTML = '<div style="text-align:center; padding:1.5rem; color:#94a3b8;">Loading conversation... ⏳</div>';

        try {
            const resp = await AppConfig.fetch(`/api/chats/${roomId}/messages`);
            if (resp.ok) {
                const messages = await resp.json();
                historyContainer.innerHTML = '';
                messages.forEach(msg => this.appendMessage(msg, false));
                this.scrollToBottom();
            }
        } catch (err) {
            console.error('Error loading messages', err);
            historyContainer.innerHTML = '<div style="text-align:center; padding:1.5rem; color:#ef4444;">Failed to load messages.</div>';
        }
    },

    appendMessage: function (msg, autoScroll = true) {
        const historyContainer = document.getElementById('chat-history');
        if (!historyContainer) return;

        if (msg.id && this.displayedMessageIds.has(msg.id)) return;
        if (msg.id) this.displayedMessageIds.add(msg.id);

        const isMe = window.App.currentUser && msg.senderUsername === window.App.currentUser.username;
        const msgDiv = document.createElement('div');
        msgDiv.className = `message ${isMe ? 'sent' : 'received'}`;

        let senderInfo = '';
        if (!isMe && this.currentRoom && this.currentRoom.group) {
            senderInfo = `<div class="message-sender" style="font-size:0.75rem; color:#0284c7; font-weight:700; margin-bottom:2px;">${escapeHtml(msg.senderName)}</div>`;
        }

        msgDiv.innerHTML = `
            ${senderInfo}
            <div class="message-text">${escapeHtml(msg.content)}</div>
            <div class="message-time">${escapeHtml(msg.formattedTime || '')}</div>
        `;

        historyContainer.appendChild(msgDiv);
        if (autoScroll) this.scrollToBottom();
    },

    sendMessage: function () {
        const input = document.getElementById('chat-message-input');
        if (!input) return;
        const text = input.value.trim();
        if (!text || !this.currentRoomId) return;

        const payload = {
            roomId: this.currentRoomId,
            content: text,
            type: 'CHAT'
        };

        if (this.stompClient && window.App.connected) {
            this.stompClient.send('/app/chat.sendMessage', {}, JSON.stringify(payload));
        } else {
            // REST Fallback
            AppConfig.fetch(`/api/chats/${this.currentRoomId}/message`, {
                method: 'POST',
                body: JSON.stringify({ content: text })
            });
        }

        input.value = '';
        this.notifyTyping(false);
    },

    notifyTyping: function (isTyping) {
        if (!this.stompClient || !window.App.connected || !this.currentRoomId) return;

        if (this.typingTimeout) clearTimeout(this.typingTimeout);

        this.stompClient.send('/app/chat.typing', {}, JSON.stringify({
            roomId: this.currentRoomId,
            typing: isTyping
        }));

        if (isTyping) {
            this.typingTimeout = setTimeout(() => this.notifyTyping(false), 2500);
        }
    },

    renderTypingIndicator: function (data) {
        if (window.App.currentUser && data.username === window.App.currentUser.username) return;

        let typingEl = document.getElementById('chat-typing-indicator');
        if (!typingEl) {
            typingEl = document.createElement('div');
            typingEl.id = 'chat-typing-indicator';
            typingEl.className = 'typing-indicator-box';
            const history = document.getElementById('chat-history');
            if (history) history.parentNode.insertBefore(typingEl, history.nextSibling);
        }

        if (data.typing) {
            typingEl.innerText = `${data.fullName || data.username} is typing...`;
            typingEl.style.display = 'block';
        } else {
            typingEl.style.display = 'none';
        }
    },

    updateChatItemPreview: function (msg) {
        const item = document.querySelector(`.chat-item[data-room-id="${msg.roomId}"]`);
        if (item) {
            const preview = item.querySelector('.chat-preview');
            const time = item.querySelector('.chat-time');
            if (preview) preview.innerText = msg.content;
            if (time && msg.formattedTime) time.innerText = msg.formattedTime;
        }
    },

    startCall: function (callType) {
        if (!this.currentRoom) return;

        const roomId = 'call-' + this.currentRoom.id + '-' + Date.now();
        const target = this.currentRoom.group ? this.currentRoom.displayName : this.currentRoom.partnerUsername;

        if (!this.currentRoom.group && this.stompClient && window.App.connected) {
            this.stompClient.send('/app/call.signal', {}, JSON.stringify({
                type: 'INVITE',
                roomId: roomId,
                toUser: this.currentRoom.partnerUsername,
                callType: callType
            }));
        }

        window.location.href = `/call.html?roomId=${roomId}&target=${encodeURIComponent(target)}&type=${callType}`;
    },

    scrollToBottom: function () {
        const historyContainer = document.getElementById('chat-history');
        if (historyContainer) historyContainer.scrollTop = historyContainer.scrollHeight;
    },

    // ----------------------------------------------------
    // In-Chat AI Actions
    // ----------------------------------------------------
    toggleAiMenu: function (e) {
        if (e) e.stopPropagation();
        const menu = document.getElementById('ai-chat-menu');
        if (menu) menu.style.display = menu.style.display === 'none' ? 'block' : 'none';
    },

    triggerSummarize: async function () {
        this.toggleAiMenu();
        if (!this.currentRoomId) return;

        const modal = document.getElementById('modal-ai-summary');
        const loading = document.getElementById('ai-summary-loading');
        const content = document.getElementById('ai-summary-content');
        const label = document.getElementById('ai-summary-room-label');

        modal.classList.add('active');
        loading.style.display = 'block';
        content.style.display = 'none';
        label.innerText = (this.currentRoom ? this.currentRoom.displayName : 'Room #' + this.currentRoomId);

        try {
            const res = await AppConfig.fetch(`/api/ai/summarize/${this.currentRoomId}`);
            const data = await res.json();

            loading.style.display = 'none';
            content.style.display = 'block';

            document.getElementById('ai-summary-sentiment').innerText = data.sentiment || 'Balanced';
            document.getElementById('ai-summary-msg-count').innerText = `${data.messageCount} messages analyzed`;
            document.getElementById('ai-summary-text').innerText = data.executiveSummary || 'No discussion summary available.';

            const decList = document.getElementById('ai-summary-decisions');
            decList.innerHTML = '';
            (data.decisions || []).forEach(d => {
                const li = document.createElement('li');
                li.innerText = d;
                decList.appendChild(li);
            });

            const taskList = document.getElementById('ai-summary-tasks');
            taskList.innerHTML = '';
            (data.actionItems || []).forEach(t => {
                const li = document.createElement('li');
                li.innerText = t;
                taskList.appendChild(li);
            });
        } catch (err) {
            loading.innerHTML = '<p style="color:#ef4444;">Failed to generate AI summary.</p>';
        }
    },

    closeAiSummaryModal: function () {
        const modal = document.getElementById('modal-ai-summary');
        if (modal) modal.classList.remove('active');
    },

    copySummaryToClipboard: function () {
        const text = document.getElementById('ai-summary-text').innerText;
        navigator.clipboard.writeText(text).then(() => alert('Summary copied to clipboard!'));
    },

    triggerExtractTasks: async function () {
        this.toggleAiMenu();
        if (!this.currentRoomId) return;

        const modal = document.getElementById('modal-ai-tasks');
        const loading = document.getElementById('ai-tasks-loading');
        const list = document.getElementById('ai-tasks-list');

        modal.classList.add('active');
        loading.style.display = 'block';
        list.innerHTML = '';

        try {
            const res = await AppConfig.fetch(`/api/ai/action-items/extract/${this.currentRoomId}`, { method: 'POST' });
            const tasks = await res.json();

            loading.style.display = 'none';
            tasks.forEach(t => {
                const row = document.createElement('div');
                row.style.cssText = 'background:#f8fafc; padding:10px 14px; border-radius:8px; border:1px solid #e2e8f0; display:flex; justify-content:space-between; align-items:center;';
                row.innerHTML = `
                    <div>
                        <strong style="font-size:0.92rem; color:#0f172a;">${escapeHtml(t.title)}</strong>
                        <div style="font-size:0.78rem; color:#64748b;">Assigned to: ${escapeHtml(t.assignedToName || 'Team')} • Due: ${escapeHtml(t.formattedDueDate || 'Soon')}</div>
                    </div>
                    <span style="font-size:0.72rem; padding:2px 8px; border-radius:4px; font-weight:700; background:#e0f2fe; color:#0369a1;">${escapeHtml(t.priority || 'MEDIUM')}</span>
                `;
                list.appendChild(row);
            });
        } catch (err) {
            loading.innerHTML = '<p style="color:#ef4444;">Failed to extract tasks.</p>';
        }
    },

    closeAiTasksModal: function () {
        const modal = document.getElementById('modal-ai-tasks');
        if (modal) modal.classList.remove('active');
    },

    openTranslateModal: function () {
        this.toggleAiMenu();
        const modal = document.getElementById('modal-ai-translate');
        if (modal) modal.classList.add('active');
    },

    closeAiTranslateModal: function () {
        const modal = document.getElementById('modal-ai-translate');
        if (modal) modal.classList.remove('active');
    },

    translateCurrentRoom: async function () {
        const targetLanguage = document.getElementById('chat-translate-lang').value;
        const results = document.getElementById('chat-translate-results');
        results.innerHTML = '<p style="color:#64748b; text-align:center;">Translating recent room messages... ⏳</p>';

        if (!this.currentRoomId) return;

        try {
            const res = await AppConfig.fetch(`/api/chats/${this.currentRoomId}/messages`);
            const msgs = await res.json();
            const slice = msgs.slice(-5);

            results.innerHTML = '';
            for (const m of slice) {
                const tr = await AppConfig.fetch('/api/ai/translate', {
                    method: 'POST',
                    body: JSON.stringify({ text: m.content, targetLanguage })
                });
                const tData = await tr.json();

                const box = document.createElement('div');
                box.style.cssText = 'background:#f8fafc; padding:8px 12px; border-radius:8px; border:1px solid #e2e8f0; font-size:0.86rem;';
                box.innerHTML = `
                    <div style="font-weight:600; color:#0284c7; font-size:0.78rem;">${escapeHtml(m.senderName)}:</div>
                    <div style="color:#1e293b; margin:2px 0;">${escapeHtml(tData.translatedText)}</div>
                    <div style="font-size:0.72rem; color:#94a3b8; font-style:italic;">Orig: "${escapeHtml(m.content)}"</div>
                `;
                results.appendChild(box);
            }
        } catch (err) {
            results.innerHTML = '<p style="color:#ef4444; text-align:center;">Failed to translate.</p>';
        }
    }
};

window.addEventListener('click', function (e) {
    const menu = document.getElementById('ai-chat-menu');
    const btn = document.getElementById('btn-ai-menu');
    if (menu && menu.style.display === 'block' && btn && !btn.contains(e.target) && !menu.contains(e.target)) {
        menu.style.display = 'none';
    }
});

function openNewChatModal() {
    const modal = document.getElementById('new-chat-modal');
    if (modal) {
        modal.classList.add('active');
        searchUsersForNewChat('');
    }
}

function closeNewChatModal() {
    const modal = document.getElementById('new-chat-modal');
    if (modal) modal.classList.remove('active');
}

async function searchUsersForNewChat(query) {
    const container = document.getElementById('user-search-results');
    if (!container) return;

    try {
        const resp = await AppConfig.fetch(`/api/users/search?q=${encodeURIComponent(query)}`);
        if (resp.ok) {
            const users = await resp.json();
            container.innerHTML = '';
            users.forEach(u => {
                const row = document.createElement('div');
                row.className = 'user-pick-item';
                row.innerHTML = `
                    <div style="display:flex; align-items:center; gap:8px;">
                        <img src="${u.avatarUrl || '/images/default-avatar.svg'}" style="width:32px; height:32px; border-radius:50%;">
                        <div>
                            <strong style="font-size:0.9rem;">${escapeHtml(u.fullName)}</strong>
                            <div style="font-size:0.78rem; color:#64748b;">@${escapeHtml(u.username)}</div>
                        </div>
                    </div>
                    <button class="btn-chat-start" onclick="startDirectChat('${u.username}')">Message</button>
                `;
                container.appendChild(row);
            });
        }
    } catch (e) {}
}

async function startDirectChat(username) {
    try {
        const resp = await AppConfig.fetch('/api/chats/direct', {
            method: 'POST',
            body: JSON.stringify({ username })
        });
        if (resp.ok) {
            const room = await resp.json();
            closeNewChatModal();
            await ChatApp.loadChatRooms();
            ChatApp.selectRoom(room.id);
        }
    } catch (e) {}
}

async function createGroupChat() {
    const nameInput = document.getElementById('group-name-input');
    const name = nameInput ? nameInput.value.trim() : '';
    if (!name) return alert('Please enter a group name');

    const checkboxes = document.querySelectorAll('input[name="selectedGroupMembers"]:checked');
    const memberUsernames = Array.from(checkboxes).map(cb => cb.value);

    try {
        const resp = await AppConfig.fetch('/api/chats/group', {
            method: 'POST',
            body: JSON.stringify({ name, memberUsernames })
        });
        if (resp.ok) {
            const room = await resp.json();
            closeNewChatModal();
            await ChatApp.loadChatRooms();
            ChatApp.selectRoom(room.id);
        }
    } catch (e) {}
}

function escapeHtml(text) {
    if (!text) return '';
    const map = { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#039;' };
    return String(text).replace(/[&<>"']/g, m => map[m]);
}

document.addEventListener('DOMContentLoaded', () => {
    ChatApp.init();
});

