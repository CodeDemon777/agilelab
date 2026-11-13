/**
 * AgileLab AI Workspace Client Scripts
 * Handles RAG Chat Assistant, Action Items Kanban, Meeting Summaries, and Translation.
 */

function switchAiTab(tabId) {
    document.querySelectorAll('.ai-tab-btn').forEach(btn => {
        btn.classList.toggle('active', btn.getAttribute('data-tab') === tabId);
    });
    document.querySelectorAll('.ai-tab-pane').forEach(pane => {
        pane.classList.toggle('active', pane.id === tabId);
    });
}

// ----------------------------------------------------
// TAB 1: RAG Chat Assistant
// ----------------------------------------------------

async function submitRagQuery() {
    const input = document.getElementById('rag-query-input');
    const query = input.value.trim();
    if (!query) return;

    const roomSelect = document.getElementById('rag-room-scope');
    const roomId = roomSelect.value ? parseInt(roomSelect.value) : null;

    input.value = '';
    const stream = document.getElementById('rag-chat-stream');

    // 1. Append User Message
    const userMsg = document.createElement('div');
    userMsg.className = 'rag-msg user';
    userMsg.innerHTML = `<div class="rag-bubble">${escapeHtml(query)}</div>`;
    stream.appendChild(userMsg);

    // 2. Append Loading Assistant Bubble
    const loadingMsg = document.createElement('div');
    loadingMsg.className = 'rag-msg assistant';
    loadingMsg.id = 'rag-loading-bubble';
    loadingMsg.innerHTML = `<div class="rag-bubble"><em>Thinking &amp; searching chat knowledge base... ⏳</em></div>`;
    stream.appendChild(loadingMsg);
    stream.scrollTop = stream.scrollHeight;

    try {
        const response = await fetch('/api/ai/ask', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
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
                    <strong>🔍 Grounded Sources from Chat:</strong>
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
        console.error('Error querying AI assistant:', err);
        loadingMsg.remove();
        const errorMsg = document.createElement('div');
        errorMsg.className = 'rag-msg assistant';
        errorMsg.innerHTML = `<div class="rag-bubble" style="color: #ef4444;">Sorry, I encountered an error searching chat history. Please try again.</div>`;
        stream.appendChild(errorMsg);
        stream.scrollTop = stream.scrollHeight;
    }
}

function askQuickPrompt(text) {
    document.getElementById('rag-query-input').value = text;
    submitRagQuery();
}

// ----------------------------------------------------
// TAB 2: Action Items Hub
// ----------------------------------------------------

function filterActionItems(status, pillEl) {
    document.querySelectorAll('.filter-pill').forEach(p => p.classList.remove('active'));
    if (pillEl) pillEl.classList.add('active');

    const cards = document.querySelectorAll('.action-card');
    cards.forEach(card => {
        const cardStatus = card.getAttribute('data-status');
        if (status === 'ALL' || cardStatus === status) {
            card.style.display = 'flex';
        } else {
            card.style.display = 'none';
        }
    });
}

async function toggleTaskStatus(id, isChecked) {
    const status = isChecked ? 'COMPLETED' : 'PENDING';
    try {
        const res = await fetch(`/api/ai/action-items/${id}/status`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ status })
        });
        if (res.ok) {
            const card = document.querySelector(`.action-card[data-id="${id}"]`);
            if (card) {
                card.setAttribute('data-status', status);
                card.classList.toggle('completed', isChecked);
            }
        }
    } catch (err) {
        console.error('Error toggling task status:', err);
    }
}

async function deleteActionItem(id) {
    if (!confirm('Are you sure you want to delete this action item?')) return;
    try {
        const res = await fetch(`/api/ai/action-items/${id}`, { method: 'DELETE' });
        if (res.ok) {
            const card = document.querySelector(`.action-card[data-id="${id}"]`);
            if (card) card.remove();
            updateCountBadge();
        }
    } catch (err) {
        console.error('Error deleting action item:', err);
    }
}

async function triggerAutoExtractFromFilter() {
    const select = document.getElementById('action-room-filter');
    const roomId = select.value || 1; // Default to room 1 if none selected

    try {
        const res = await fetch(`/api/ai/action-items/extract/${roomId}`, { method: 'POST' });
        if (res.ok) {
            window.location.reload();
        }
    } catch (err) {
        console.error('Error auto-extracting action items:', err);
    }
}

async function submitNewActionItem(e) {
    e.preventDefault();
    const title = document.getElementById('ai-task-title').value.trim();
    const description = document.getElementById('ai-task-desc').value.trim();
    const assignedToUsername = document.getElementById('ai-task-assignee').value;
    const dueDate = document.getElementById('ai-task-due').value || null;
    const roomId = document.getElementById('ai-task-room').value ? parseInt(document.getElementById('ai-task-room').value) : null;
    const priority = document.getElementById('ai-task-priority').value;

    try {
        const res = await fetch('/api/ai/action-items', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ title, description, assignedToUsername, dueDate, roomId, priority })
        });
        if (res.ok) {
            closeModal('modal-action-item');
            window.location.reload();
        }
    } catch (err) {
        console.error('Error creating action item:', err);
    }
}

function updateCountBadge() {
    const count = document.querySelectorAll('.action-card').length;
    const badge = document.getElementById('action-items-count');
    if (badge) badge.innerText = count;
}

// ----------------------------------------------------
// TAB 3: Meeting Notes Hub
// ----------------------------------------------------

async function submitMeetingNotes(e) {
    e.preventDefault();
    const title = document.getElementById('meeting-title-input').value.trim();
    const transcript = document.getElementById('meeting-transcript-input').value.trim();

    try {
        const res = await fetch('/api/ai/meeting-notes', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ title, transcript, roomId: 'call-custom' })
        });
        if (res.ok) {
            closeModal('modal-meeting-notes');
            window.location.reload();
        }
    } catch (err) {
        console.error('Error generating meeting notes:', err);
    }
}

// ----------------------------------------------------
// TAB 4: AI Translation
// ----------------------------------------------------

async function performTranslation() {
    const input = document.getElementById('translate-input').value.trim();
    const targetLanguage = document.getElementById('translate-lang-select').value;
    const outputEl = document.getElementById('translate-output');

    if (!input) return;
    outputEl.value = 'Translating... ⏳';

    try {
        const res = await fetch('/api/ai/translate', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ text: input, targetLanguage })
        });
        const data = await res.json();
        outputEl.value = data.translatedText || '';
    } catch (err) {
        console.error('Error translating:', err);
        outputEl.value = 'Error during translation. Please try again.';
    }
}

// ----------------------------------------------------
// Modals and Utilities
// ----------------------------------------------------

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
    return text.replace(/[&<>"']/g, m => map[m]);
}

function formatMarkdown(text) {
    if (!text) return '';
    let html = escapeHtml(text);
    // Bold
    html = html.replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>');
    // Italic
    html = html.replace(/\*(.*?)\*/g, '<em>$1</em>');
    // Blockquote
    html = html.replace(/^&gt; (.*$)/gim, '<blockquote style="border-left: 3px solid #00bfff; padding-left: 8px; margin: 4px 0; color: #475569;">$1</blockquote>');
    // Newlines
    html = html.replace(/\n/g, '<br>');
    return html;
}

