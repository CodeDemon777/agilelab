/**
 * AgileLab AI Workspace Client (Vercel Frontend)
 * Handles RAG Chat Assistant, Action Items Kanban, Meeting Minutes, and Translation.
 */

const AiWorkspace = {
    actionItems: [],
    meetingSummaries: [],

    init: async function () {
        await this.loadWorkspaceData();
    },

    loadWorkspaceData: async function () {
        // 1. Load Insights
        try {
            const res = await AppConfig.fetch('/api/ai/insights');
            if (res.ok) {
                const insights = await res.json();
                this.renderInsights(insights);
            }
        } catch (e) {}

        // 2. Load Rooms for Selector
        try {
            const res = await AppConfig.fetch('/api/chats');
            if (res.ok) {
                const rooms = await res.json();
                this.populateRoomSelectors(rooms);
            }
        } catch (e) {}

        // 3. Load Action Items
        await this.loadActionItems();

        // 4. Load Meeting Summaries
        await this.loadMeetingSummaries();
    },

    renderInsights: function (data) {
        const elMsgs = document.getElementById('kpi-msgs-val');
        const elTasks = document.getElementById('kpi-tasks-val');
        const elRate = document.getElementById('kpi-rate-val');
        const elNotes = document.getElementById('kpi-notes-val');

        if (elMsgs) elMsgs.innerText = data.totalMessages || 0;
        if (elTasks) elTasks.innerText = data.totalActionItems || 0;
        if (elRate) elRate.innerText = `${data.taskCompletionRate || 0}%`;
        if (elNotes) elNotes.innerText = data.totalMeetingSummaries || 0;

        // Breakdown bars
        const compVal = document.getElementById('insights-comp-count');
        const inprogVal = document.getElementById('insights-inprog-count');
        const pendVal = document.getElementById('insights-pend-count');
        const compBar = document.getElementById('insights-comp-bar');

        if (compVal) compVal.innerText = data.completedActionItems || 0;
        if (inprogVal) inprogVal.innerText = data.inProgressActionItems || 0;
        if (pendVal) pendVal.innerText = data.pendingActionItems || 0;
        if (compBar) compBar.style.width = `${data.taskCompletionRate || 0}%`;

        // User activity
        const userContrList = document.getElementById('insights-user-list');
        if (userContrList && data.userContributions) {
            userContrList.innerHTML = '';
            for (const [name, count] of Object.entries(data.userContributions)) {
                const row = document.createElement('div');
                row.style.cssText = 'display:flex; justify-content:space-between; font-size:0.88rem; padding:6px 0; border-bottom:1px solid #e2e8f0;';
                row.innerHTML = `<span style="font-weight:600; color:#334155;">${escapeHtml(name)}</span><span style="color:#0284c7; font-weight:700;">${count} messages</span>`;
                userContrList.appendChild(row);
            }
        }
    },

    populateRoomSelectors: function (rooms) {
        const ragSelect = document.getElementById('rag-room-scope');
        const taskFilter = document.getElementById('action-room-filter');
        const modalSelect = document.getElementById('ai-task-room');

        [ragSelect, taskFilter, modalSelect].forEach(sel => {
            if (!sel) return;
            const firstOpt = sel.firstElementChild;
            sel.innerHTML = '';
            if (firstOpt) sel.appendChild(firstOpt);

            rooms.forEach(r => {
                const opt = document.createElement('option');
                opt.value = r.id;
                opt.innerText = r.displayName;
                sel.appendChild(opt);
            });
        });
    },

    loadActionItems: async function () {
        const grid = document.getElementById('action-items-grid');
        if (!grid) return;

        try {
            const res = await AppConfig.fetch('/api/ai/action-items');
            if (res.ok) {
                this.actionItems = await res.json();
                this.renderActionGrid(this.actionItems);
            }
        } catch (e) {
            console.error('Error loading action items:', e);
        }
    },

    renderActionGrid: function (items) {
        const grid = document.getElementById('action-items-grid');
        if (!grid) return;

        const countBadge = document.getElementById('action-items-count');
        if (countBadge) countBadge.innerText = items.length;

        if (items.length === 0) {
            grid.innerHTML = '<div style="grid-column:1/-1; text-align:center; padding:3rem; color:#94a3b8;"><p style="font-weight:600; font-size:1.05rem;">No action items found.</p><p>Click "Auto-Extract from Room" to scan chat messages for tasks!</p></div>';
            return;
        }

        grid.innerHTML = '';
        items.forEach(item => {
            const isDone = item.status === 'COMPLETED';
            const card = document.createElement('div');
            card.className = `action-card ${isDone ? 'completed' : ''}`;
            card.setAttribute('data-status', item.status);
            card.setAttribute('data-id', item.id);

            card.innerHTML = `
                <div>
                    <div class="action-card-header">
                        <input type="checkbox" class="action-checkbox" ${isDone ? 'checked' : ''} onchange="AiWorkspace.toggleTaskStatus(${item.id}, this.checked)">
                        <div style="flex:1;">
                            <h4 class="action-title">${escapeHtml(item.title)}</h4>
                            <p class="action-desc">${escapeHtml(item.description || '')}</p>
                        </div>
                    </div>
                </div>

                <div>
                    <div style="display:flex; justify-content:space-between; align-items:center; font-size:0.8rem; color:#64748b; margin-top:0.5rem;">
                        <span>${item.roomName ? escapeHtml(item.roomName) : ''}</span>
                        <span>${item.formattedDueDate ? 'Due: ' + escapeHtml(item.formattedDueDate) : ''}</span>
                    </div>

                    <div class="action-card-footer">
                        <div class="assignee-chip">
                            <img src="${item.assignedToAvatar || '/images/default-avatar.svg'}" class="assignee-avatar" alt="Assignee">
                            <span>${escapeHtml(item.assignedToName || 'Unassigned')}</span>
                        </div>
                        <div style="display:flex; align-items:center; gap:8px;">
                            <span class="priority-badge ${item.priority || 'MEDIUM'}">${escapeHtml(item.priority || 'MEDIUM')}</span>
                            <button style="background:none; border:none; cursor:pointer; color:#94a3b8; display:flex; align-items:center;" title="Delete" onclick="AiWorkspace.deleteActionItem(${item.id})" aria-label="Delete Action Item">
                                <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                                    <polyline points="3 6 5 6 21 6"></polyline>
                                    <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path>
                                </svg>
                            </button>
                        </div>
                    </div>
                </div>
            `;
            grid.appendChild(card);
        });
    },

    toggleTaskStatus: async function (id, isChecked) {
        const status = isChecked ? 'COMPLETED' : 'PENDING';
        try {
            const res = await AppConfig.fetch(`/api/ai/action-items/${id}/status`, {
                method: 'POST',
                body: JSON.stringify({ status })
            });
            if (res.ok) {
                const card = document.querySelector(`.action-card[data-id="${id}"]`);
                if (card) {
                    card.setAttribute('data-status', status);
                    card.classList.toggle('completed', isChecked);
                }
            }
        } catch (e) {}
    },

    deleteActionItem: async function (id) {
        if (!confirm('Are you sure you want to delete this action item?')) return;
        try {
            const res = await AppConfig.fetch(`/api/ai/action-items/${id}`, { method: 'DELETE' });
            if (res.ok) {
                const card = document.querySelector(`.action-card[data-id="${id}"]`);
                if (card) card.remove();
            }
        } catch (e) {}
    },

    submitNewActionItem: async function (e) {
        e.preventDefault();
        const title = document.getElementById('ai-task-title').value.trim();
        const description = document.getElementById('ai-task-desc').value.trim();
        const assignedToUsername = document.getElementById('ai-task-assignee').value;
        const dueDate = document.getElementById('ai-task-due').value || null;
        const roomId = document.getElementById('ai-task-room').value ? parseInt(document.getElementById('ai-task-room').value) : null;
        const priority = document.getElementById('ai-task-priority').value;

        try {
            const res = await AppConfig.fetch('/api/ai/action-items', {
                method: 'POST',
                body: JSON.stringify({ title, description, assignedToUsername, dueDate, roomId, priority })
            });
            if (res.ok) {
                closeModal('modal-action-item');
                await this.loadActionItems();
            }
        } catch (e) {}
    },

    triggerAutoExtractFromFilter: async function () {
        const select = document.getElementById('action-room-filter');
        const roomId = select ? (select.value || 1) : 1;

        try {
            const res = await AppConfig.fetch(`/api/ai/action-items/extract/${roomId}`, { method: 'POST' });
            if (res.ok) {
                await this.loadActionItems();
                alert('Action items extracted from chat messages!');
            }
        } catch (e) {}
    },

    loadMeetingSummaries: async function () {
        const list = document.getElementById('meeting-summaries-list');
        if (!list) return;

        try {
            const res = await AppConfig.fetch('/api/ai/meeting-notes');
            if (res.ok) {
                this.meetingSummaries = await res.json();
                this.renderMeetingList(this.meetingSummaries);
            }
        } catch (e) {}
    },

    renderMeetingList: function (items) {
        const list = document.getElementById('meeting-summaries-list');
        if (!list) return;

        if (items.length === 0) {
            list.innerHTML = '<div style="text-align:center; padding:3rem; color:#94a3b8;"><p style="font-weight:600; font-size:1.05rem;">No meeting summaries yet.</p><p>Use "Live AI Transcript" in calls, or click "+ Ingest Transcript" above!</p></div>';
            return;
        }

        list.innerHTML = '';
        items.forEach(m => {
            const card = document.createElement('div');
            card.className = 'meeting-card';
            card.innerHTML = `
                <div class="meeting-card-header">
                    <div>
                        <h4 style="margin:0; font-size:1.15rem; font-weight:700; color:#0f172a;">${escapeHtml(m.title)}</h4>
                        <span style="font-size:0.82rem; color:#64748b;">Recorded on ${escapeHtml(m.formattedCreatedAt || 'Recent')}</span>
                    </div>
                    <span class="meeting-badge">AI Processed</span>
                </div>

                <div style="margin-bottom:1rem;">
                    <h5 style="margin:0 0 0.4rem; font-size:0.92rem; text-transform:uppercase; color:#0369a1; letter-spacing:0.5px;">Executive Summary</h5>
                    <p style="margin:0; font-size:0.95rem; color:#334155; line-height:1.5;">${escapeHtml(m.summaryText)}</p>
                </div>

                <div style="display:grid; grid-template-columns: repeat(auto-fit, minmax(280px, 1fr)); gap:1rem; margin-bottom:1rem;">
                    <div style="background:#f8fafc; border-radius:8px; padding:0.85rem 1rem; border:1px solid #e2e8f0;">
                        <h5 style="margin:0 0 0.4rem; font-size:0.88rem; font-weight:700; color:#0f172a;">Key Strategic Decisions</h5>
                        <ul style="margin:0; padding-left:1.2rem; font-size:0.86rem; color:#475569;">
                            ${(m.keyDecisions || []).map(d => `<li>${escapeHtml(d)}</li>`).join('')}
                        </ul>
                    </div>
                    <div style="background:#f8fafc; border-radius:8px; padding:0.85rem 1rem; border:1px solid #e2e8f0;">
                        <h5 style="margin:0 0 0.4rem; font-size:0.88rem; font-weight:700; color:#0f172a;">Follow-Up Action Items</h5>
                        <ul style="margin:0; padding-left:1.2rem; font-size:0.86rem; color:#475569;">
                            ${(m.actionItems || []).map(a => `<li>${escapeHtml(a)}</li>`).join('')}
                        </ul>
                    </div>
                </div>

                <details style="font-size:0.85rem; color:#64748b; background:#f1f5f9; padding:0.75rem 1rem; border-radius:8px;">
                    <summary style="cursor:pointer; font-weight:600;">View Raw Transcript</summary>
                    <pre style="margin-top:0.5rem; white-space:pre-wrap; font-family:monospace; font-size:0.82rem; color:#334155;">${escapeHtml(m.transcript || '')}</pre>
                </details>
            `;
            list.appendChild(card);
        });
    },

    submitMeetingNotes: async function (e) {
        e.preventDefault();
        const title = document.getElementById('meeting-title-input').value.trim();
        const transcript = document.getElementById('meeting-transcript-input').value.trim();

        try {
            const res = await AppConfig.fetch('/api/ai/meeting-notes', {
                method: 'POST',
                body: JSON.stringify({ title, transcript, roomId: 'call-custom' })
            });
            if (res.ok) {
                closeModal('modal-meeting-notes');
                await this.loadMeetingSummaries();
                await this.loadActionItems();
            }
        } catch (e) {}
    }
};

// ----------------------------------------------------
// TAB 1: RAG Chatbot
// ----------------------------------------------------
async function submitRagQuery() {
    const input = document.getElementById('rag-query-input');
    const query = input.value.trim();
    if (!query) return;

    const roomSelect = document.getElementById('rag-room-scope');
    const roomId = roomSelect.value ? parseInt(roomSelect.value) : null;

    input.value = '';
    const stream = document.getElementById('rag-chat-stream');

    const userMsg = document.createElement('div');
    userMsg.className = 'rag-msg user';
    userMsg.innerHTML = `<div class="rag-bubble">${escapeHtml(query)}</div>`;
    stream.appendChild(userMsg);

    const loadingMsg = document.createElement('div');
    loadingMsg.className = 'rag-msg assistant';
    loadingMsg.innerHTML = `<div class="rag-bubble"><em>Thinking &amp; searching chat knowledge base...</em></div>`;
    stream.appendChild(loadingMsg);
    stream.scrollTop = stream.scrollHeight;

    try {
        const response = await AppConfig.fetch('/api/ai/ask', {
            method: 'POST',
            body: JSON.stringify({ query, roomId })
        });

        const data = await response.json();
        loadingMsg.remove();

        const assistantMsg = document.createElement('div');
        assistantMsg.className = 'rag-msg assistant';

        let citationsHtml = '';
        if (data.citations && data.citations.length > 0) {
            citationsHtml = `
                <div class="rag-citations-box">
                    <strong>Grounded Sources from Chat:</strong>
                    <ul style="margin: 4px 0 0; padding-left: 1.2rem;">
                        ${data.citations.map(c => `
                            <li><strong>${escapeHtml(c.senderName)}</strong> (${escapeHtml(c.formattedTime)} in <em>${escapeHtml(c.roomName || 'Chat')}</em>): "${escapeHtml(c.messageSnippet)}"</li>
                        `).join('')}
                    </ul>
                </div>
            `;
        }

        assistantMsg.innerHTML = `
            <div class="rag-bubble">
                ${formatMarkdown(data.answer)}
                ${citationsHtml}
                <div style="font-size: 0.72rem; color: #94a3b8; text-align: right; margin-top: 6px;">${escapeHtml(data.formattedTimestamp || 'Just now')}</div>
            </div>
        `;
        stream.appendChild(assistantMsg);
        stream.scrollTop = stream.scrollHeight;
    } catch (err) {
        loadingMsg.remove();
        const errorMsg = document.createElement('div');
        errorMsg.className = 'rag-msg assistant';
        errorMsg.innerHTML = `<div class="rag-bubble" style="color: #ef4444;">Sorry, I encountered an error searching chat history. Please try again.</div>`;
        stream.appendChild(errorMsg);
    }
}

function askQuickPrompt(text) {
    document.getElementById('rag-query-input').value = text;
    submitRagQuery();
}

function switchAiTab(tabId) {
    document.querySelectorAll('.ai-tab-btn').forEach(btn => {
        btn.classList.toggle('active', btn.getAttribute('data-tab') === tabId);
    });
    document.querySelectorAll('.ai-tab-pane').forEach(pane => {
        pane.classList.toggle('active', pane.id === tabId);
    });
}

function filterActionItems(status, pillEl) {
    document.querySelectorAll('.filter-pill').forEach(p => p.classList.remove('active'));
    if (pillEl) pillEl.classList.add('active');

    document.querySelectorAll('.action-card').forEach(card => {
        const cardStatus = card.getAttribute('data-status');
        card.style.display = (status === 'ALL' || cardStatus === status) ? 'flex' : 'none';
    });
}

async function performTranslation() {
    const input = document.getElementById('translate-input').value.trim();
    const targetLanguage = document.getElementById('translate-lang-select').value;
    const outputEl = document.getElementById('translate-output');

    if (!input) return;
    outputEl.value = 'Translating message...';

    try {
        const res = await AppConfig.fetch('/api/ai/translate', {
            method: 'POST',
            body: JSON.stringify({ text: input, targetLanguage })
        });
        const data = await res.json();
        outputEl.value = data.translatedText || '';
    } catch (err) {
        outputEl.value = 'Error during translation. Please try again.';
    }
}

function openNewActionItemModal() {
    document.getElementById('modal-action-item').classList.add('open');
}

function openNewMeetingNotesModal() {
    document.getElementById('modal-meeting-notes').classList.add('open');
}

function closeModal(modalId) {
    const modal = document.getElementById(modalId);
    if (modal) modal.classList.remove('open');
}

function escapeHtml(text) {
    if (!text) return '';
    const map = { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#039;' };
    return String(text).replace(/[&<>"']/g, m => map[m]);
}

function formatMarkdown(text) {
    if (!text) return '';
    let html = escapeHtml(text);
    html = html.replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>');
    html = html.replace(/\*(.*?)\*/g, '<em>$1</em>');
    html = html.replace(/^&gt; (.*$)/gim, '<blockquote style="border-left: 3px solid #4f46e5; padding-left: 8px; margin: 4px 0; color: #475569;">$1</blockquote>');
    html = html.replace(/\n/g, '<br>');
    return html;
}

document.addEventListener('DOMContentLoaded', () => {
    AiWorkspace.init();
});

