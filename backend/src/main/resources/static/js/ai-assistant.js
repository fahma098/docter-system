/**
 * ====================================================================
 * AURA HEALTH - AI MEDICAL ASSISTANT WIDGET (ai-assistant.js)
 * Intelligent Symptom Triage & Specialist Doctor Matcher
 * ====================================================================
 */

(function () {
  'use strict';

  // State
  let isOpen = false;
  let isSending = false;
  let messageHistory = [];

  // Suggestion chips
  const QUICK_PROMPTS = [
    { label: '❤️ Chest Pain & Palpitations', query: 'I have sharp chest discomfort, shortness of breath, and palpitations.' },
    { label: '🩹 Skin Rash / Allergies', query: 'I developed an itchy red rash and hives on my arms.' },
    { label: '👶 Child Fever & Cold', query: 'My 5-year-old child has high fever, dry cough, and loss of appetite.' },
    { label: '🧠 Migraine & Dizziness', query: 'Severe throbbing headache on one side of my head with nausea and light sensitivity.' },
    { label: '🦴 Knee & Joint Pain', query: 'Severe knee pain and stiffness making it hard to walk down the stairs.' },
    { label: '🩺 General Fever & Fatigue', query: 'Continuous mild fever, body aches, and extreme fatigue for 3 days.' }
  ];

  function initAiWidget() {
    if (document.getElementById('ai-chat-launcher')) return;

    // Inject Widget HTML
    const widgetHtml = `
      <!-- AI Floating Launcher Button -->
      <button id="ai-chat-launcher" class="ai-launcher-btn" type="button" aria-label="Open AI Health Assistant">
        <div class="ai-launcher-icon-wrap">
          <svg width="20" height="20" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 10h.01M12 10h.01M16 10h.01M9 16H5a2 2 0 01-2-2V6a2 2 0 012-2h14a2 2 0 012 2v8a2 2 0 01-2 2h-5l-5 5v-5z"></path>
          </svg>
          <div class="ai-launcher-pulse"></div>
        </div>
        <span>AI Doctor Guide</span>
      </button>

      <!-- Backdrop Overlay -->
      <div id="ai-drawer-overlay" class="ai-drawer-overlay"></div>

      <!-- Right-Side Sliding Drawer -->
      <aside id="ai-chat-drawer" class="ai-drawer" aria-label="Kerala AI Health Assistant Panel">
        <!-- Header -->
        <div class="ai-drawer-header">
          <div class="ai-drawer-title-box">
            <div class="ai-drawer-avatar">
              <svg width="24" height="24" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9.75 17L9 20l-1 1h8l-1-1-.75-3M3 13h18M5 17h14a2 2 0 002-2V5a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z"></path>
              </svg>
            </div>
            <div>
              <div class="ai-drawer-title">Aura AI Health Guide</div>
              <div class="ai-drawer-subtitle">
                <span class="ai-status-indicator"></span>
                <span>Kerala Clinical Triage • Kochi</span>
              </div>
            </div>
          </div>
          <div class="ai-drawer-actions">
            <button id="ai-reset-btn" class="ai-btn-icon" title="Reset Conversation" type="button">
              <svg width="18" height="18" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"></path>
              </svg>
            </button>
            <button id="ai-close-btn" class="ai-btn-icon" title="Close Panel" type="button">
              <svg width="20" height="20" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"></path>
              </svg>
            </button>
          </div>
        </div>

        <!-- Chat Body -->
        <div id="ai-chat-body" class="ai-chat-body">
          <!-- Welcome message rendered via JS -->
        </div>

        <!-- Chat Input Footer -->
        <div class="ai-drawer-footer">
          <form id="ai-chat-form" class="ai-input-form">
            <input 
              type="text" 
              id="ai-user-input" 
              class="ai-input-field" 
              placeholder="Describe your symptoms (e.g., knee pain, skin rash)..." 
              autocomplete="off"
            />
            <button type="submit" id="ai-submit-btn" class="ai-send-btn" title="Send Message">
              <svg width="16" height="16" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 19l9 2-9-18-9 18 9-2zm0 0v-8"></path>
              </svg>
            </button>
          </form>
          <div class="ai-disclaimer">
            🏥 Aura AI provides symptom guidance and doctor referrals. In emergency, call <strong>108</strong>.
          </div>
        </div>
      </aside>
    `;

    const wrapper = document.createElement('div');
    wrapper.id = 'ai-assistant-container';
    wrapper.innerHTML = widgetHtml;
    document.body.appendChild(wrapper);

    bindEvents();
    renderInitialGreeting();
  }

  function bindEvents() {
    const launcher = document.getElementById('ai-chat-launcher');
    const overlay = document.getElementById('ai-drawer-overlay');
    const closeBtn = document.getElementById('ai-close-btn');
    const resetBtn = document.getElementById('ai-reset-btn');
    const form = document.getElementById('ai-chat-form');

    launcher.addEventListener('click', toggleDrawer);
    overlay.addEventListener('click', closeDrawer);
    closeBtn.addEventListener('click', closeDrawer);

    resetBtn.addEventListener('click', () => {
      messageHistory = [];
      sessionStorage.removeItem('aura_ai_history');
      renderInitialGreeting();
    });

    form.addEventListener('submit', handleFormSubmit);

    document.addEventListener('keydown', (e) => {
      if (e.key === 'Escape' && isOpen) {
        closeDrawer();
      }
    });
  }

  function toggleDrawer() {
    if (isOpen) {
      closeDrawer();
    } else {
      openDrawer();
    }
  }

  function openDrawer() {
    isOpen = true;
    document.getElementById('ai-chat-drawer').classList.add('open');
    document.getElementById('ai-drawer-overlay').classList.add('active');
    setTimeout(() => {
      document.getElementById('ai-user-input')?.focus();
    }, 200);
  }

  function closeDrawer() {
    isOpen = false;
    document.getElementById('ai-chat-drawer').classList.remove('open');
    document.getElementById('ai-drawer-overlay').classList.remove('active');
  }

  function renderInitialGreeting() {
    const chatBody = document.getElementById('ai-chat-body');
    if (!chatBody) return;

    chatBody.innerHTML = '';

    const greetingBubble = document.createElement('div');
    greetingBubble.className = 'ai-msg assistant';
    greetingBubble.innerHTML = `
      <div class="ai-msg-bubble">
        <p style="margin:0 0 8px 0;">
          <strong>Namaskaram! 🙏</strong> Welcome to <strong>Aura Multispeciality Hospital, Kochi</strong>.
        </p>
        <p style="margin:0 0 10px 0;">
          I am your intelligent <strong>AI Health Guide</strong>. Tell me what symptoms or health concerns you are experiencing, and I'll recommend the ideal specialist doctor and help you book an appointment in 1 click.
        </p>
        <div style="font-size:11.5px; font-weight:700; color:#64748B; margin-bottom:6px;">
          Common Symptoms (Click to test):
        </div>
        <div class="ai-chips-wrap">
          ${QUICK_PROMPTS.map(p => `<button type="button" class="ai-chip" data-query="${escapeHtml(p.query)}">${escapeHtml(p.label)}</button>`).join('')}
        </div>
      </div>
      <div class="ai-msg-time">${getCurrentTime()}</div>
    `;

    chatBody.appendChild(greetingBubble);

    // Bind chip clicks
    chatBody.querySelectorAll('.ai-chip').forEach(btn => {
      btn.addEventListener('click', () => {
        const query = btn.getAttribute('data-query');
        if (query) {
          sendUserMessage(query);
        }
      });
    });

    scrollToBottom();
  }

  async function handleFormSubmit(e) {
    e.preventDefault();
    const input = document.getElementById('ai-user-input');
    const text = (input?.value || '').trim();
    if (!text || isSending) return;

    input.value = '';
    await sendUserMessage(text);
  }

  async function sendUserMessage(text) {
    if (isSending) return;
    isSending = true;

    const chatBody = document.getElementById('ai-chat-body');
    const submitBtn = document.getElementById('ai-submit-btn');
    if (submitBtn) submitBtn.disabled = true;

    // 1. Render User Message Bubble
    appendUserBubble(text);
    scrollToBottom();

    // 2. Render Typing Indicator
    const typingEl = document.createElement('div');
    typingEl.className = 'ai-typing';
    typingEl.id = 'ai-typing-indicator';
    typingEl.innerHTML = `
      <span class="ai-typing-dot"></span>
      <span class="ai-typing-dot"></span>
      <span class="ai-typing-dot"></span>
      <span style="font-size:11px;color:#64748B;margin-left:4px;">Analyzing symptoms...</span>
    `;
    chatBody.appendChild(typingEl);
    scrollToBottom();

    try {
      // Determine patient ID if user is authenticated
      let patientId = null;
      if (window.Api && typeof Api.getUser === 'function') {
        const u = Api.getUser();
        if (u && (u.role === 'PATIENT' || u.patientId)) {
          patientId = u.patientId || u.userId;
        }
      }

      const res = await fetch(getApiUrl('/ai/chat'), {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          ...(window.Api && Api.getToken() ? { 'Authorization': `Bearer ${Api.getToken()}` } : {})
        },
        body: JSON.stringify({
          message: text,
          patientId: patientId
        })
      });

      const json = await res.json();
      typingEl.remove();

      if (!res.ok || json.success === false) {
        throw new Error(json.message || 'AI service temporarily unavailable');
      }

      appendAssistantBubble(json.data);
    } catch (err) {
      if (document.getElementById('ai-typing-indicator')) {
        document.getElementById('ai-typing-indicator').remove();
      }
      appendAssistantError(`I encountered an issue processing your request: ${err.message}. Please try again or browse our doctors directly.`);
    } finally {
      isSending = false;
      if (submitBtn) submitBtn.disabled = false;
      scrollToBottom();
    }
  }

  function appendUserBubble(text) {
    const chatBody = document.getElementById('ai-chat-body');
    const bubble = document.createElement('div');
    bubble.className = 'ai-msg user';
    bubble.innerHTML = `
      <div class="ai-msg-bubble">${escapeHtml(text)}</div>
      <div class="ai-msg-time">${getCurrentTime()}</div>
    `;
    chatBody.appendChild(bubble);
  }

  function appendAssistantBubble(data) {
    const chatBody = document.getElementById('ai-chat-body');
    const bubble = document.createElement('div');
    bubble.className = 'ai-msg assistant';

    let contentHtml = '';

    // Emergency Warning Box
    if (data.emergencyFlag) {
      contentHtml += `
        <div class="ai-emergency-box">
          <svg width="20" height="20" fill="none" stroke="currentColor" viewBox="0 0 24 24" style="flex-shrink:0;">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"></path>
          </svg>
          <div>
            <strong>URGENT MEDICAL ATTENTION ADVISED</strong>
            These symptoms may require immediate clinical evaluation. Please visit our 24/7 Emergency Wing at Marine Drive, Kochi or dial emergency ambulance <strong>108</strong>.
          </div>
        </div>
      `;
    }

    // Recommended Specialization Badge
    if (data.recommendedSpecialization) {
      contentHtml += `
        <div>
          <span class="ai-specialty-badge">
            <svg width="14" height="14" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z"></path>
            </svg>
            Recommended: ${escapeHtml(data.recommendedSpecialization)}
          </span>
        </div>
      `;
    }

    // Main AI text response formatted with linebreaks
    const formattedReply = formatMarkdownText(data.reply || '');
    contentHtml += `<div style="margin-top:8px;">${formattedReply}</div>`;

    // Recommended Doctors Cards
    if (data.recommendedDoctors && data.recommendedDoctors.length > 0) {
      contentHtml += `
        <div style="font-size:12px; font-weight:700; color:#172033; margin-top:12px;">
          Available Kerala Specialists for this Condition:
        </div>
        <div class="ai-doc-cards">
          ${data.recommendedDoctors.map(doc => renderDoctorCard(doc)).join('')}
        </div>
      `;
    }

    bubble.innerHTML = `
      <div class="ai-msg-bubble">${contentHtml}</div>
      <div class="ai-msg-time">${getCurrentTime()}</div>
    `;

    chatBody.appendChild(bubble);
  }

  function renderDoctorCard(doc) {
    const feeFormatted = (window.Api && typeof Api.formatCurrency === 'function')
      ? Api.formatCurrency(doc.consultationFee)
      : `₹${Number(doc.consultationFee || 0).toLocaleString('en-IN')}`;

    const profilePic = doc.profileImageUrl || 'images/default-doctor.svg';

    return `
      <div class="ai-doc-card">
        <div class="ai-doc-header">
          <img 
            src="${profilePic}" 
            onerror="this.onerror=null; this.src='images/default-doctor.svg';" 
            class="ai-doc-avatar" 
            alt="${escapeHtml(doc.fullName)}"
          />
          <div class="ai-doc-info">
            <div class="ai-doc-name">${escapeHtml(doc.fullName)}</div>
            <div class="ai-doc-spec">${escapeHtml(doc.specializationName || '')}</div>
            <div style="font-size:11px;color:#64748B;">${escapeHtml(doc.qualifications || '')}</div>
          </div>
        </div>
        <div class="ai-doc-meta">
          <span>Experience: <strong>${doc.experienceYears || 5} Yrs</strong></span>
          <span>Fee: <strong class="ai-doc-fee">${feeFormatted}</strong></span>
        </div>
        <a href="booking.html?doctorId=${doc.doctorId}" class="ai-doc-action-btn">
          <svg width="14" height="14" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z"></path>
          </svg>
          Book with ${escapeHtml(doc.fullName.split(',')[0])}
        </a>
      </div>
    `;
  }

  function appendAssistantError(errorMsg) {
    const chatBody = document.getElementById('ai-chat-body');
    const bubble = document.createElement('div');
    bubble.className = 'ai-msg assistant';
    bubble.innerHTML = `
      <div class="ai-msg-bubble" style="border-color:#FCA5A5; background:#FEF2F2; color:#991B1B;">
        ⚠️ ${escapeHtml(errorMsg)}
      </div>
      <div class="ai-msg-time">${getCurrentTime()}</div>
    `;
    chatBody.appendChild(bubble);
  }

  function formatMarkdownText(text) {
    if (!text) return '';
    return text
      .split('\n\n')
      .map(paragraph => {
        const lines = paragraph.split('\n');
        const formattedLines = lines.map(line => {
          let clean = escapeHtml(line);
          clean = clean.replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>');
          clean = clean.replace(/\*(.*?)\*/g, '<em>$1</em>');
          if (clean.trim().startsWith('- ') || clean.trim().startsWith('• ')) {
            return `<li style="margin-left:14px;">${clean.replace(/^[-•]\s*/, '')}</li>`;
          }
          return clean;
        });
        return `<p style="margin:4px 0;">${formattedLines.join('<br>')}</p>`;
      })
      .join('');
  }

  function getApiUrl(endpoint) {
    const isLocalDevServer = (window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1')
      && window.location.port !== '' && window.location.port !== '8080';
    const isFileProtocol = window.location.protocol === 'file:';

    const base = (isLocalDevServer || isFileProtocol)
      ? 'http://localhost:8080/api'
      : `${window.location.origin}/api`;

    return `${base}${endpoint}`;
  }

  function scrollToBottom() {
    const chatBody = document.getElementById('ai-chat-body');
    if (chatBody) {
      setTimeout(() => {
        chatBody.scrollTop = chatBody.scrollHeight;
      }, 50);
    }
  }

  function getCurrentTime() {
    const d = new Date();
    return d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
  }

  function escapeHtml(str) {
    if (!str) return '';
    const div = document.createElement('div');
    div.textContent = str;
    return div.innerHTML;
  }

  // Initialize once DOM is ready
  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', initAiWidget);
  } else {
    initAiWidget();
  }

  // Expose global controller if page wants to trigger it programmatically
  window.AuraAiWidget = {
    open: openDrawer,
    close: closeDrawer,
    toggle: toggleDrawer,
    ask: (q) => {
      openDrawer();
      sendUserMessage(q);
    }
  };
})();
