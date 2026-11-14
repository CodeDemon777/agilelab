/**
 * AgileLab Chat Management & Real-time Messaging
 */
const ChatApp = {
    currentRoomId: null,
    currentRoom: null,
    stompClient: null,
    roomSubscription: null,
    typingSubscription: null,
    typingTimeout: null,
    displayedMessageIds: new Set(),

    init: function () {
        this.bindEvents();

        // Listen for STOMP connection from App
        if (window.App && window.App.connected) {
            this.stompClient = window.App.stompClient;
            this.setupActiveRoom();
        } else {
            window.addEventListener('stompConnected', (e) => {
                this.stompClient = e.detail.stompClient;
                this.setupActiveRoom();
            });
        }

        // Listen for global real-time chat messages
        window.addEventListener('newChatMessage', (e) => {
            const message = e.detail;
            this.updateChatItemPreview(message);
            if (this.currentRoomId && message.roomId == this.currentRoomId) {
                this.appendMessage(message);
            }
        });

        // Check URL parameter for room
        const urlParams = new URLSearchParams(window.location.search);
        const roomIdParam = urlParams.get('room');
        if (roomIdParam) {
            this.selectRoom(parseInt(roomIdParam, 10));
        } else {
            const firstItem = document.querySelector('.chat-item');
            if (firstItem) {
                const id = firstItem.getAttribute('data-room-id');
                if (id) this.selectRoom(parseInt(id, 10));
            }
        }
    },

    bindEvents: function () {
        // Search bar filtering
        const searchInput = document.getElementById('search-chats');
        if (searchInput) {
            searchInput.addEventListener('input', (e) => {
                const term = e.target.value.toLowerCase().trim();
                document.querySelectorAll('.chat-item').forEach(item => {
                    const name = item.querySelector('.chat-name').innerText.toLowerCase();
                    const preview = item.querySelector('.chat-preview').innerText.toLowerCase();
                    if (name.includes(term) || preview.includes(term)) {
                        item.style.display = 'block';
                    } else {
                        item.style.display = 'none';
                    }
                });
            });
        }

        // Message input
        const msgInput = document.getElementById('chat-message-input');
        const sendBtn = document.getElementById('btn-send-message');

        if (msgInput) {
            msgInput.addEventListener('keydown', (e) => {
                if (e.key === 'Enter' && !e.shiftKey) {
                    e.preventDefault();
                    this.sendMessage();
                } else {
                    this.notifyTyping(true);
                }
            });
        }

        if (sendBtn) {
            sendBtn.addEventListener('click', () => this.sendMessage());
        }
    },

    selectRoom: async function (roomId) {
        if (!roomId) return;
        this.currentRoomId = roomId;
        this.displayedMessageIds.clear();

        // Highlight active item
        document.querySelectorAll('.chat-item').forEach(el => {
            if (el.getAttribute('data-room-id') == roomId) {
                el.classList.add('active');
            } else {
                el.classList.remove('active');
            }
        });

        // Update URL without reload
        const newUrl = new URL(window.location);
        newUrl.searchParams.set('room', roomId);
        window.history.pushState({}, '', newUrl);

        try {
            const resp = await fetch(`/api/chats/${roomId}`);
            if (resp.ok) {
                this.currentRoom = await resp.json();
                this.renderRoomHeader(this.currentRoom);
                this.setupActiveRoom();
                await this.loadMessages(roomId);
                // Mark room read
                fetch(`/api/chats/${roomId}/read`, { method: 'POST' });
            }
        } catch (e) {
            console.error('Failed to load room details:', e);
        }
    },

    setupActiveRoom: function () {
        if (!this.stompClient || !this.currentRoomId) return;

        // Unsubscribe prior room
        if (this.roomSubscription) this.roomSubscription.unsubscribe();
        if (this.typingSubscription) this.typingSubscription.unsubscribe();

        // Subscribe to current room messages
        this.roomSubscription = this.stompClient.subscribe(`/topic/room.${this.currentRoomId}`, (msg) => {
            const message = JSON.parse(msg.body);
            this.appendMessage(message);
            this.updateChatItemPreview(message);
        });

        // Subscribe to current room typing indicator
        this.typingSubscription = this.stompClient.subscribe(`/topic/room.${this.currentRoomId}.typing`, (msg) => {
            const data = JSON.parse(msg.body);
            this.renderTypingIndicator(data);
        });
    },

    renderRoomHeader: function (room) {
        const partnerEl = document.getElementById('chat-partner-name');
        const statusEl = document.getElementById('chat-partner-status');
        const avatarEl = document.getElementById('chat-partner-avatar');
        const addMembersBtn = document.getElementById('btn-add-members');
        const videoCallBtn = document.getElementById('btn-video-call');
        const voiceCallBtn = document.getElementById('btn-voice-call');

        if (partnerEl) partnerEl.innerText = room.displayName;
        if (avatarEl) avatarEl.src = room.avatarUrl || '/images/default-avatar.svg';

        if (statusEl) {
            if (room.group) {
                const count = room.members ? room.members.length : 0;
                statusEl.innerText = `${count} members`;
            } else {
                statusEl.innerText = room.online ? 'Online' : 'Offline';
            }
        }

        if (addMembersBtn) {
            addMembersBtn.style.display = room.group ? 'inline-block' : 'none';
        }

        if (videoCallBtn) {
            videoCallBtn.onclick = () => this.startCall('VIDEO');
        }
        if (voiceCallBtn) {
            voiceCallBtn.onclick = () => this.startCall('VOICE');
        }
    },

    loadMessages: async function (roomId) {
        const historyContainer = document.getElementById('chat-history');
        if (!historyContainer) return;

        historyContainer.innerHTML = '<div style="text-align:center; padding: 2rem; color:#888;">Loading messages...</div>';

        try {
            const resp = await fetch(`/api/chats/${roomId}/messages`);
            if (resp.ok) {
                const messages = await resp.json();
                historyContainer.innerHTML = '';
                if (messages.length === 0) {
                    historyContainer.innerHTML = '<div style="text-align:center; padding: 3rem; color:#999;">No messages yet. Start the conversation!</div>';
                } else {
                    messages.forEach(m => this.appendMessage(m, false));
                    this.scrollToBottom();
                }
            }
        } catch (e) {
            console.error('Error fetching messages:', e);
            historyContainer.innerHTML = '<div style="text-align:center; color:red;">Failed to load messages.</div>';
        }
    },

    appendMessage: function (msg, autoScroll = true) {
        const historyContainer = document.getElementById('chat-history');
        if (!historyContainer) return;

        // Prevent duplicate rendering
        if (msg.id && this.displayedMessageIds.has(msg.id)) {
            return;
        }
        if (msg.id) {
            this.displayedMessageIds.add(msg.id);
        }

        // Remove empty state message if present
        if (historyContainer.children.length === 1 && historyContainer.children[0].innerText.includes('No messages yet')) {
            historyContainer.innerHTML = '';
        }

        const myUsername = (window.App && window.App.currentUser && window.App.currentUser.username)
            ? window.App.currentUser.username.toLowerCase()
            : '';
        const sender = (msg.senderUsername || '').toLowerCase();
        const isMe = myUsername && (sender === myUsername);
        const msgDiv = document.createElement('div');

        if (msg.type === 'JOIN' || msg.type === 'LEAVE' || msg.type === 'SYSTEM') {
            msgDiv.className = 'message-system';
            msgDiv.innerHTML = `<span>${this.escapeHtml(msg.content)}</span>`;
        } else {
            msgDiv.className = `message ${isMe ? 'sent' : 'received'}`;
            let senderNameHtml = (!isMe && this.currentRoom && this.currentRoom.group)
                ? `<div class="message-sender-title">${this.escapeHtml(msg.senderFullName || msg.senderUsername)}</div>`
                : '';

            msgDiv.innerHTML = `
                ${senderNameHtml}
                <p class="message-text">${this.escapeHtml(msg.content)}</p>
                <span class="timestamp">${msg.timestamp || 'Now'}</span>
            `;
        }

        historyContainer.appendChild(msgDiv);
        if (autoScroll) this.scrollToBottom();
    },

    sendMessage: function () {
        const input = document.getElementById('chat-message-input');
        if (!input) return;
        const text = input.value.trim();
        if (!text || !this.currentRoomId) return;

        input.value = '';
        this.notifyTyping(false);

        const currentUsername = (window.App && window.App.currentUser) ? window.App.currentUser.username : null;

        if (this.stompClient && window.App.connected) {
            this.stompClient.send('/app/chat.sendMessage', {}, JSON.stringify({
                roomId: this.currentRoomId,
                content: text,
                senderUsername: currentUsername,
                type: 'CHAT'
            }));
        } else {
            // REST Fallback
            fetch(`/api/chats/${this.currentRoomId}/message`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    content: text,
                    senderUsername: currentUsername
                })
            }).then(r => r.json()).then(msg => {
                this.appendMessage(msg);
                this.updateChatItemPreview(msg);
            });
        }
    },

    notifyTyping: function (isTyping) {
        if (!this.stompClient || !window.App.connected || !this.currentRoomId) return;

        if (this.typingTimeout) clearTimeout(this.typingTimeout);

        const currentUsername = (window.App && window.App.currentUser) ? window.App.currentUser.username : null;

        this.stompClient.send('/app/chat.typing', {}, JSON.stringify({
            roomId: this.currentRoomId,
            username: currentUsername,
            isTyping: isTyping
        }));

        if (isTyping) {
            this.typingTimeout = setTimeout(() => {
                this.notifyTyping(false);
            }, 2500);
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

        if (data.isTyping) {
            typingEl.innerText = `${data.username} is typing...`;
            typingEl.style.display = 'block';
        } else {
            typingEl.style.display = 'none';
        }
    },

    updateChatItemPreview: function (msg) {
        const item = document.querySelector(`.chat-item[data-room-id="${msg.roomId}"]`);
        if (item) {
            const preview = item.querySelector('.chat-preview');
            if (preview) preview.innerText = msg.content;
            // Move item to top
            const list = document.querySelector('.chat-list');
            if (list) list.prepend(item);
        }
    },

    startCall: function (callType) {
        if (!this.currentRoom) return;

        const roomId = 'call-' + this.currentRoom.id + '-' + Date.now();
        const target = this.currentRoom.group ? this.currentRoom.displayName : this.currentRoom.partnerUsername;

        // Send invite through STOMP if not a group
        if (!this.currentRoom.group && this.stompClient && window.App.connected) {
            this.stompClient.send('/app/call.signal', {}, JSON.stringify({
                type: 'INVITE',
                roomId: roomId,
                toUser: this.currentRoom.partnerUsername,
                callType: callType
            }));
        }

        window.location.href = `/call/${roomId}?target=${encodeURIComponent(target)}&type=${callType}`;
    },

    scrollToBottom: function () {
        const historyContainer = document.getElementById('chat-history');
        if (historyContainer) {
            historyContainer.scrollTop = historyContainer.scrollHeight;
        }
    },

    escapeHtml: function (text) {
        if (!text) return '';
        const map = { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#039;' };
        return text.replace(/[&<>"']/g, m => map[m]);
    },

    toggleAiMenu: function (e) {
        if (e) e.stopPropagation();
        const menu = document.getElementById('ai-chat-menu');
        if (menu) {
            menu.style.display = menu.style.display === 'none' ? 'block' : 'none';
        }
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
            const res = await fetch(`/api/ai/summarize/${this.currentRoomId}`);
            const data = await res.json();

            loading.style.display = 'none';
            content.style.display = 'block';

            document.getElementById('ai-summary-sentiment').innerText = data.sentiment || 'Balanced';
            document.getElementById('ai-summary-msg-count').innerText = `${data.messageCount} messages analyzed`;
            document.getElementById('ai-summary-text').innerText = data.executiveSummary || 'No discussion summary available.';

            const decList = document.getElementById('ai-summary-decisions');
            decList.innerHTML = '';
            if (data.decisions && data.decisions.length > 0) {
                data.decisions.forEach(d => {
                    const li = document.createElement('li');
                    li.innerText = d;
                    decList.appendChild(li);
                });
            } else {
                decList.innerHTML = '<li style="color:#94a3b8;">No explicit consensus or decisions recorded yet.</li>';
            }

            const taskList = document.getElementById('ai-summary-tasks');
            taskList.innerHTML = '';
            if (data.actionItems && data.actionItems.length > 0) {
                data.actionItems.forEach(t => {
                    const li = document.createElement('li');
                    li.innerText = t;
                    taskList.appendChild(li);
                });
            } else {
                taskList.innerHTML = '<li style="color:#94a3b8;">No action items detected in this chat.</li>';
            }
        } catch (err) {
            console.error('Error summarizing chat:', err);
            loading.innerHTML = '<p style="color:#ef4444;">Failed to generate AI summary. Please try again.</p>';
        }
    },

    closeAiSummaryModal: function () {
        const modal = document.getElementById('modal-ai-summary');
        if (modal) modal.classList.remove('active');
    },

    copySummaryToClipboard: function () {
        const text = document.getElementById('ai-summary-text').innerText;
        navigator.clipboard.writeText(text).then(() => {
            alert('Summary copied to clipboard!');
        });
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
            const res = await fetch(`/api/ai/action-items/extract/${this.currentRoomId}`, { method: 'POST' });
            const tasks = await res.json();

            loading.style.display = 'none';
            if (tasks.length === 0) {
                list.innerHTML = '<p style="color:#94a3b8; text-align:center; padding:1rem;">No new tasks detected in current messages.</p>';
            } else {
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
            }
        } catch (err) {
            console.error('Error extracting tasks:', err);
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
        results.innerHTML = '<p style="color:#64748b; text-align:center;">Translating recent room messages...</p>';

        if (!this.currentRoomId) return;

        try {
            const res = await fetch(`/api/chat/rooms/${this.currentRoomId}/messages`);
            const msgs = await res.json();
            const slice = msgs.slice(-5);

            results.innerHTML = '';
            for (const m of slice) {
                const tr = await fetch('/api/ai/translate', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
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
            console.error('Error in translateCurrentRoom:', err);
            results.innerHTML = '<p style="color:#ef4444; text-align:center;">Failed to translate conversation.</p>';
        }
    }
};

window.addEventListener('click', function(e) {
    const menu = document.getElementById('ai-chat-menu');
    const btn = document.getElementById('btn-ai-menu');
    if (menu && menu.style.display === 'block' && btn && !btn.contains(e.target) && !menu.contains(e.target)) {
        menu.style.display = 'none';
    }
});

// Modal handlers for New Chat & Add Members
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
    const list = document.getElementById('user-search-results');
    if (!list) return;

    try {
        const resp = await fetch(`/api/users/search?q=${encodeURIComponent(query)}`);
        if (resp.ok) {
            const users = await resp.json();
            list.innerHTML = '';
            if (users.length === 0) {
                list.innerHTML = '<div style="padding:1rem; color:#888; text-align:center;">No users found</div>';
                return;
            }

            users.forEach(u => {
                const item = document.createElement('div');
                item.className = 'user-pick-item';
                item.innerHTML = `
                    <div style="display:flex; align-items:center; gap:0.75rem;">
                        <img src="${u.avatarUrl || '/images/default-avatar.svg'}" class="nav-avatar" style="width:36px; height:36px;">
                        <div>
                            <div style="font-weight:600; font-size:0.95rem;">${u.fullName}</div>
                            <div style="font-size:0.8rem; color:#666;">@${u.username} • <i>${u.statusQuote}</i></div>
                        </div>
                    </div>
                    <button class="btn-chat-start" onclick="startDirectChat('${u.username}')">Message</button>
                `;
                list.appendChild(item);
            });
        }
    } catch (e) {
        console.error('Failed to search users:', e);
    }
}

async function startDirectChat(targetUsername) {
    try {
        const resp = await fetch('/api/chats/direct', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username: targetUsername })
        });
        if (resp.ok) {
            const room = await resp.json();
            closeNewChatModal();
            window.location.href = `/chats?room=${room.id}`;
        }
    } catch (e) {
        console.error('Failed to start chat:', e);
    }
}

async function createGroupChat() {
    const nameInput = document.getElementById('group-name-input');
    const groupName = nameInput ? nameInput.value.trim() : '';
    if (!groupName) {
        alert('Please enter a group name');
        return;
    }

    const selectedCheckboxes = document.querySelectorAll('input[name="selectedGroupMembers"]:checked');
    const members = Array.from(selectedCheckboxes).map(cb => cb.value);

    try {
        const resp = await fetch('/api/chats/group', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ name: groupName, memberUsernames: members })
        });
        if (resp.ok) {
            const room = await resp.json();
            closeNewChatModal();
            window.location.href = `/chats?room=${room.id}`;
        }
    } catch (e) {
        console.error('Failed to create group:', e);
    }
}

// Open / Close Add Members Modal
function openAddMembersModal() {
    const modal = document.getElementById('add-members-modal');
    if (modal) {
        modal.classList.add('active');
        loadEligibleMembersForGroup();
    }
}

function closeAddMembersModal() {
    const modal = document.getElementById('add-members-modal');
    if (modal) modal.classList.remove('active');
}

async function loadEligibleMembersForGroup() {
    const container = document.getElementById('add-members-list');
    if (!container) return;

    try {
        const resp = await fetch('/api/users/all');
        if (resp.ok) {
            const users = await resp.json();
            const currentMembers = (ChatApp.currentRoom && ChatApp.currentRoom.members)
                ? ChatApp.currentRoom.members.map(m => m.username.toLowerCase())
                : [];

            const eligible = users.filter(u => !currentMembers.includes(u.username.toLowerCase()));

            container.innerHTML = '';
            if (eligible.length === 0) {
                container.innerHTML = '<div style="padding:1rem; color:#888;">All known users are already in this group!</div>';
                return;
            }

            eligible.forEach(u => {
                const label = document.createElement('label');
                label.className = 'member-checkbox-label';
                label.innerHTML = `
                    <input type="checkbox" name="newGroupMembers" value="${u.username}">
                    <img src="${u.avatarUrl || '/images/default-avatar.svg'}" style="width:28px; height:28px; border-radius:50%;">
                    <span>${u.fullName} (@${u.username})</span>
                `;
                container.appendChild(label);
            });
        }
    } catch (e) {
        console.error('Error fetching eligible members:', e);
    }
}

async function submitAddMembers() {
    if (!ChatApp.currentRoomId) return;
    const checkboxes = document.querySelectorAll('input[name="newGroupMembers"]:checked');
    const usernames = Array.from(checkboxes).map(c => c.value);
    if (usernames.length === 0) {
        alert('Please select at least one user');
        return;
    }

    try {
        const resp = await fetch(`/api/chats/${ChatApp.currentRoomId}/members`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ usernames: usernames })
        });
        if (resp.ok) {
            closeAddMembersModal();
            ChatApp.selectRoom(ChatApp.currentRoomId);
        }
    } catch (e) {
        console.error('Failed to add members:', e);
    }
}

document.addEventListener('DOMContentLoaded', () => {
    ChatApp.init();
});

