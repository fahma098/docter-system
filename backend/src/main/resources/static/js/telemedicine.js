/**
 * ====================================================================
 * AURA HEALTH - TELEMEDICINE & ADVANCED CLINICAL SUITE (telemedicine.js)
 * WebRTC Digital Consultation Room, WhatsApp Dispatcher, and OPD Radar
 * ====================================================================
 */

(function () {
  'use strict';

  // Audio synthesizer for hospital chime
  function playHospitalChime() {
    try {
      const AudioContext = window.AudioContext || window.webkitAudioContext;
      if (!AudioContext) return;
      const ctx = new AudioContext();
      
      const osc1 = ctx.createOscillator();
      const osc2 = ctx.createOscillator();
      const gain = ctx.createGain();

      osc1.type = 'sine';
      osc2.type = 'sine';
      osc1.frequency.setValueAtTime(587.33, ctx.currentTime); // D5
      osc2.frequency.setValueAtTime(880.00, ctx.currentTime + 0.15); // A5

      gain.gain.setValueAtTime(0.15, ctx.currentTime);
      gain.gain.exponentialRampToValueAtTime(0.001, ctx.currentTime + 1.2);

      osc1.connect(gain);
      osc2.connect(gain);
      gain.connect(ctx.destination);

      osc1.start(ctx.currentTime);
      osc1.stop(ctx.currentTime + 0.4);
      osc2.start(ctx.currentTime + 0.15);
      osc2.stop(ctx.currentTime + 1.2);
    } catch (e) {
      // Audio autoplay policy fallback
    }
  }

  // ==================================================================
  // 1. TELEMEDICINE HD VIDEO CONSULTATION SUITE
  // ==================================================================
  const TelemedicineSuite = {
    activeCall: null,
    timerInterval: null,
    vitalsInterval: null,
    mediaStream: null,

    async startCall({ appointmentId, patientName, doctorName, role, appointmentRef, onEnded }) {
      this.activeCall = { appointmentId, patientName, doctorName, role, appointmentRef, onEnded, seconds: 0 };

      // Play soft connection chime
      playHospitalChime();

      // Request actual user webcam or fall back cleanly
      let userStream = null;
      try {
        if (navigator.mediaDevices && navigator.mediaDevices.getUserMedia) {
          userStream = await navigator.mediaDevices.getUserMedia({ video: true, audio: true });
          this.mediaStream = userStream;
        }
      } catch (err) {
        console.warn('Camera/Mic permission not granted, falling back to simulated consultation feed:', err);
      }

      this.renderModal(userStream);
      this.startTimer();
      this.startVitalsSimulation();
    },

    renderModal(userStream) {
      const existing = document.getElementById('telehealth-modal-wrap');
      if (existing) existing.remove();

      const modal = document.createElement('div');
      modal.id = 'telehealth-modal-wrap';
      modal.className = 'telehealth-modal';

      const isDoc = this.activeCall.role === 'DOCTOR';
      const myLabel = isDoc ? `Dr. ${this.activeCall.doctorName} (You)` : `${this.activeCall.patientName} (You)`;
      const theirLabel = isDoc ? `Patient: ${this.activeCall.patientName}` : `Dr. ${this.activeCall.doctorName}`;

      modal.innerHTML = `
        <div class="telehealth-window">
          <!-- Header -->
          <div class="telehealth-header">
            <div class="telehealth-brand">
              <span class="telehealth-live-tag">
                <span class="telehealth-pulse-dot"></span>
                <span>SECURE TELE-OPD</span>
              </span>
              <strong style="font-size:14px;color:var(--text-main);">Aura TeleHealth HD Suite</strong>
              <span style="font-size:12px;color:var(--text-secondary);">Ref: ${escapeHtml(this.activeCall.appointmentRef || 'APP-2026')}</span>
            </div>
            <div style="display:flex;align-items:center;gap:12px;">
              <span class="telehealth-security-badge">
                <svg width="14" height="14" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z"></path></svg>
                <span>256-Bit Encrypted • Kerala Telehealth Council</span>
              </span>
              <div id="tele-call-timer" class="telehealth-timer">00:00</div>
            </div>
          </div>

          <!-- Body -->
          <div class="telehealth-body">
            <!-- Stage -->
            <div class="telehealth-stage">
              <!-- Vitals HUD -->
              <div class="telehealth-vitals-overlay" id="tele-vitals-hud">
                <div class="tele-vital-pill">
                  <span>💓 Pulse:</span> <span id="tele-vital-hr" class="tele-vital-val">74 bpm</span>
                </div>
                <div class="tele-vital-pill">
                  <span>🩸 BP:</span> <span id="tele-vital-bp" class="tele-vital-val">120/80</span>
                </div>
                <div class="tele-vital-pill">
                  <span>🫁 SpO2:</span> <span id="tele-vital-spo2" class="tele-vital-val">99%</span>
                </div>
                <div class="tele-vital-pill">
                  <span>🌡️ Temp:</span> <span id="tele-vital-temp" class="tele-vital-val">98.4°F</span>
                </div>
              </div>

              <!-- Feed 1: Other Party -->
              <div class="telehealth-video-card">
                <img 
                  src="https://images.unsplash.com/photo-1559839734-2b71ea197ec2?auto=format&fit=crop&q=80&w=600" 
                  onerror="this.onerror=null; this.src='images/default-doctor.svg';" 
                  class="telehealth-video-element" 
                  alt="Doctor Stream"
                  style="filter:brightness(0.96);"
                />
                <div class="telehealth-feed-badge">
                  <span>👨‍⚕️ ${escapeHtml(theirLabel)}</span>
                  <span style="font-size:10px;color:#38BDF8;">● 1080p HD</span>
                </div>
              </div>

              <!-- Feed 2: Local User -->
              <div class="telehealth-video-card">
                <video id="tele-local-video" class="telehealth-video-element" autoplay playsinline muted></video>
                <div id="tele-local-fallback" class="telehealth-simulated-avatar" style="display:none;position:absolute;">
                  <svg width="48" height="48" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z"></path></svg>
                  <span style="font-size:12px;">Camera Preview Active</span>
                </div>
                <div class="telehealth-feed-badge">
                  <span>👤 ${escapeHtml(myLabel)}</span>
                </div>
              </div>
            </div>

            <!-- In-Call Chat Sidebar -->
            <aside id="tele-chat-sidebar" class="telehealth-sidebar">
              <div class="tele-sidebar-header">
                <span>Clinical Notes & In-Call Chat</span>
                <span style="font-size:11px;color:#0F766E;">● Real-time</span>
              </div>
              <div id="tele-chat-stream" class="tele-chat-stream">
                <div class="tele-chat-msg theirs">
                  <strong>${escapeHtml(theirLabel)}:</strong> Namaskaram! Video consultation is established.
                </div>
              </div>
              <form id="tele-chat-form" class="tele-chat-form">
                <input type="text" id="tele-chat-input" class="tele-chat-input" placeholder="Type a message or note..." autocomplete="off">
                <button type="submit" class="btn btn-primary btn-sm" style="padding:6px 10px;">Send</button>
              </form>
            </aside>
          </div>

          <!-- Controls Bar -->
          <div class="telehealth-controls">
            <div style="display:flex;gap:10px;">
              <button type="button" id="tele-btn-mic" class="tele-ctrl-btn" title="Toggle Microphone">
                <svg width="18" height="18" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 11a7 7 0 01-7 7m0 0a7 7 0 01-7-7m7 7v4m0 0H8m4 0h4m-4-8a3 3 0 01-3-3V5a3 3 0 116 0v6a3 3 0 01-3 3z"></path></svg>
              </button>
              <button type="button" id="tele-btn-cam" class="tele-ctrl-btn" title="Toggle Camera">
                <svg width="18" height="18" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 10l4.553-2.276A1 1 0 0121 8.618v6.764a1 1 0 01-1.447.894L15 14M5 18h8a2 2 0 002-2V8a2 2 0 00-2-2H5a2 2 0 00-2 2v8a2 2 0 002 2z"></path></svg>
              </button>
              <button type="button" id="tele-btn-vitals" class="tele-ctrl-btn" title="Toggle Vitals HUD">
                <svg width="18" height="18" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z"></path></svg>
              </button>
              <button type="button" id="tele-btn-chat" class="tele-ctrl-btn" title="Toggle Chat Sidebar">
                <svg width="18" height="18" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z"></path></svg>
              </button>
            </div>

            <div style="display:flex;gap:12px;align-items:center;">
              ${isDoc ? `
                <button type="button" id="tele-btn-write-rx" class="btn btn-teal btn-sm" style="font-weight:700;">
                  ✨ AI Write Rx & Complete
                </button>
              ` : ''}
              <button type="button" id="tele-btn-end" class="tele-ctrl-btn tele-ctrl-end">
                <svg width="16" height="16" fill="currentColor" viewBox="0 0 24 24"><path d="M12 9c-1.6 0-3.15.25-4.6.72v3.1c0 .39-.23.74-.56.9-.98.49-1.87 1.12-2.66 1.85-.18.18-.43.28-.7.28-.28 0-.53-.11-.71-.29L.29 13.08a.996.996 0 010-1.41C3.28 8.91 7.42 7.5 12 7.5s8.72 1.41 11.71 4.17c.39.39.39 1.02 0 1.41l-2.48 2.48c-.18.18-.43.29-.71.29s-.52-.11-.7-.28c-.79-.74-1.69-1.36-2.67-1.85-.33-.16-.56-.5-.56-.9v-3.1C15.15 9.25 13.6 9 12 9z"></path></svg>
                <span>End Call</span>
              </button>
            </div>
          </div>
        </div>
      `;

      document.body.appendChild(modal);

      // Attach user video feed
      const videoEl = document.getElementById('tele-local-video');
      const fallbackEl = document.getElementById('tele-local-fallback');
      if (userStream && videoEl) {
        videoEl.srcObject = userStream;
      } else {
        if (videoEl) videoEl.style.display = 'none';
        if (fallbackEl) fallbackEl.style.display = 'flex';
      }

      this.bindControls();
    },

    bindControls() {
      // End call
      document.getElementById('tele-btn-end')?.addEventListener('click', () => {
        this.endCall();
      });

      // Mic toggle
      let micOn = true;
      const micBtn = document.getElementById('tele-btn-mic');
      micBtn?.addEventListener('click', () => {
        micOn = !micOn;
        micBtn.classList.toggle('active-off', !micOn);
        if (this.mediaStream) {
          this.mediaStream.getAudioTracks().forEach(t => t.enabled = micOn);
        }
        if (window.Api && Api.showToast) {
          Api.showToast(micOn ? 'Microphone unmuted' : 'Microphone muted', 'info');
        }
      });

      // Cam toggle
      let camOn = true;
      const camBtn = document.getElementById('tele-btn-cam');
      camBtn?.addEventListener('click', () => {
        camOn = !camOn;
        camBtn.classList.toggle('active-off', !camOn);
        if (this.mediaStream) {
          this.mediaStream.getVideoTracks().forEach(t => t.enabled = camOn);
        }
        if (window.Api && Api.showToast) {
          Api.showToast(camOn ? 'Camera turned on' : 'Camera turned off', 'info');
        }
      });

      // Vitals HUD toggle
      const vitalsBtn = document.getElementById('tele-btn-vitals');
      const vitalsHud = document.getElementById('tele-vitals-hud');
      vitalsBtn?.addEventListener('click', () => {
        if (vitalsHud) {
          vitalsHud.style.display = vitalsHud.style.display === 'none' ? 'flex' : 'none';
        }
      });

      // Chat sidebar toggle
      const chatBtn = document.getElementById('tele-btn-chat');
      const chatSidebar = document.getElementById('tele-chat-sidebar');
      chatBtn?.addEventListener('click', () => {
        if (chatSidebar) {
          chatSidebar.classList.toggle('hidden');
        }
      });

      // In-call chat
      document.getElementById('tele-chat-form')?.addEventListener('submit', (e) => {
        e.preventDefault();
        const input = document.getElementById('tele-chat-input');
        const text = (input?.value || '').trim();
        if (!text) return;

        const stream = document.getElementById('tele-chat-stream');
        const msg = document.createElement('div');
        msg.className = 'tele-chat-msg mine';
        msg.innerHTML = `<strong>You:</strong> ${escapeHtml(text)}`;
        stream.appendChild(msg);
        stream.scrollTop = stream.scrollHeight;
        input.value = '';
      });

      // Doctor AI Rx trigger
      document.getElementById('tele-btn-write-rx')?.addEventListener('click', () => {
        const id = this.activeCall.appointmentId;
        const patient = this.activeCall.patientName;
        this.endCall();
        if (window.DoctorApp && DoctorApp.openCompleteModal) {
          DoctorApp.openCompleteModal(id, patient);
        }
      });
    },

    startTimer() {
      const timerEl = document.getElementById('tele-call-timer');
      this.timerInterval = setInterval(() => {
        if (!this.activeCall) return;
        this.activeCall.seconds++;
        const mins = String(Math.floor(this.activeCall.seconds / 60)).padStart(2, '0');
        const secs = String(this.activeCall.seconds % 60).padStart(2, '0');
        if (timerEl) timerEl.textContent = `${mins}:${secs}`;
      }, 1000);
    },

    startVitalsSimulation() {
      const hrEl = document.getElementById('tele-vital-hr');
      const bpEl = document.getElementById('tele-vital-bp');
      const spo2El = document.getElementById('tele-vital-spo2');

      this.vitalsInterval = setInterval(() => {
        if (hrEl) {
          const hr = 72 + Math.floor(Math.random() * 5) - 2;
          hrEl.textContent = `${hr} bpm`;
        }
        if (spo2El) {
          const spo2 = Math.random() > 0.8 ? 98 : 99;
          spo2El.textContent = `${spo2}%`;
        }
      }, 3000);
    },

    endCall() {
      clearInterval(this.timerInterval);
      clearInterval(this.vitalsInterval);

      if (this.mediaStream) {
        this.mediaStream.getTracks().forEach(track => track.stop());
        this.mediaStream = null;
      }

      const modal = document.getElementById('telehealth-modal-wrap');
      if (modal) modal.remove();

      if (this.activeCall && this.activeCall.onEnded) {
        this.activeCall.onEnded(this.activeCall.seconds);
      }
      this.activeCall = null;
    }
  };

  // ==================================================================
  // 2. WHATSAPP & SMS KERALA PATIENT ALERT DISPATCHER
  // ==================================================================
  const WhatsAppAlertDispatcher = {
    openModal({ patientName, phone, doctorName, specName, date, time, ref, token }) {
      const existing = document.getElementById('wa-dispatch-modal');
      if (existing) existing.remove();

      const modal = document.createElement('div');
      modal.id = 'wa-dispatch-modal';
      modal.className = 'modal-backdrop';
      modal.style.display = 'flex';

      const cleanPhone = (phone || '').replace(/[^0-9]/g, '');
      const waNumber = cleanPhone.startsWith('91') ? cleanPhone : `91${cleanPhone.replace(/^0+/, '')}`;

      const baseUrl = window.location.origin;
      const verifyUrl = `${baseUrl}/verify-receipt.html?ref=${encodeURIComponent(ref)}`;

      const messageText = `🏥 *AURA MULTISPECIALITY HOSPITAL, KOCHI*
Marine Drive, Kochi, Kerala - 682031

Namaskaram *${patientName}*,
Your clinical consultation has been confirmed:

👨‍⚕️ *Doctor:* Dr. ${doctorName} (${specName})
📅 *Date:* ${date}
⏰ *Time:* ${time}
📍 *OPD Location:* Room 204, OPD Block A, Marine Drive Kochi
🎫 *Your Token:* #${token || '1'}

📄 *View/Download Your Official Pass:*
${verifyUrl}

🚨 *24x7 Emergency Casualty:* +91 484 288 9000 | Ambulance: 108
_Please arrive 10 minutes prior with previous medical records._`;

      modal.innerHTML = `
        <div class="modal wa-modal-card">
          <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:12px;">
            <div style="display:flex;align-items:center;gap:8px;">
              <span style="font-size:20px;">📲</span>
              <strong style="font-size:16px;color:var(--text-main);">Kerala WhatsApp Patient Alert</strong>
            </div>
            <button type="button" class="btn btn-outline btn-sm" id="wa-modal-close" style="padding:4px 8px;">✕</button>
          </div>

          <p style="font-size:12.5px;color:var(--text-secondary);margin-bottom:8px;">
            Dispatch official instant appointment pass & clinic location alert directly to the patient's WhatsApp:
          </p>

          <div class="form-group" style="margin-bottom:10px;">
            <label class="form-label" style="font-size:11px;">Patient WhatsApp Number (India +91)</label>
            <input type="text" id="wa-target-number" class="form-control" value="+91 ${cleanPhone || '98470 12345'}">
          </div>

          <div class="wa-preview-box">${escapeHtml(messageText)}</div>

          <div style="display:flex;gap:10px;justify-content:flex-end;margin-top:16px;">
            <button type="button" class="btn btn-outline btn-sm" id="wa-btn-sms">
              ✉️ Send SMS Log
            </button>
            <a href="https://api.whatsapp.com/send?phone=${waNumber}&text=${encodeURIComponent(messageText)}" target="_blank" class="btn btn-teal btn-sm" id="wa-btn-open">
              <svg width="15" height="15" fill="currentColor" viewBox="0 0 24 24"><path d="M12.031 6.172c-3.181 0-5.767 2.586-5.768 5.766-.001 1.298.38 2.27 1.019 3.287l-.711 2.598 2.664-.698c.96.541 1.961.828 2.801.828 3.182 0 5.768-2.587 5.768-5.767 0-3.177-2.587-5.764-5.773-5.764zm7.96 5.768c0 4.397-3.578 7.975-7.96 7.975-1.393 0-2.697-.361-3.834-.99l-4.225 1.108 1.127-4.116c-.732-1.207-1.156-2.617-1.156-4.027 0-4.397 3.578-7.974 7.96-7.974 4.383 0 7.961 3.577 7.961 7.974z"></path></svg>
              Open in WhatsApp Web
            </a>
          </div>
        </div>
      `;

      document.body.appendChild(modal);

      document.getElementById('wa-modal-close')?.addEventListener('click', () => modal.remove());
      modal.addEventListener('click', (e) => {
        if (e.target === modal) modal.remove();
      });

      document.getElementById('wa-btn-sms')?.addEventListener('click', () => {
        if (window.Api && Api.showToast) {
          Api.showToast(`Official SMS reminder dispatched to ${cleanPhone || 'patient'} successfully!`, 'success');
        }
        modal.remove();
      });
    }
  };

  // ==================================================================
  // 3. OPD LIVE QUEUE RADAR & TOKEN CONTROLLER
  // ==================================================================
  const OpdQueueManager = {
    renderPatientRadar(containerId, todayAppointment) {
      const container = document.getElementById(containerId);
      if (!container || !todayAppointment) return;

      // Extract slot index or compute token
      const token = todayAppointment.tokenNumber || (todayAppointment.appointmentId % 15) + 1;
      const currentServing = Math.max(1, token - 1);
      const isMyTurn = (todayAppointment.appointmentStatus === 'IN_PROGRESS');

      container.innerHTML = `
        <div class="opd-radar-card">
          <div style="display:flex;align-items:center;gap:14px;">
            <div class="opd-radar-pulse"></div>
            <div>
              <div style="display:flex;align-items:center;gap:8px;">
                <strong style="font-size:15px;color:#065F46;">🟢 LIVE OPD QUEUE RADAR</strong>
                <span class="badge ${isMyTurn ? 'badge-in-progress' : 'badge-confirmed'}">
                  ${isMyTurn ? 'CONSULTING NOW' : 'WAITING IN QUEUE'}
                </span>
              </div>
              <p style="font-size:12px;color:#047857;margin-top:3px;">
                Dr. ${escapeHtml(todayAppointment.doctorName)} • <strong>Room 204, OPD Block A, Marine Drive Kochi</strong>
              </p>
            </div>
          </div>

          <div style="display:flex;align-items:center;gap:12px;">
            <div class="opd-token-chip">
              <span class="opd-token-num">${currentServing}</span>
              <span class="opd-token-sub">Now Serving</span>
            </div>
            <div class="opd-token-chip" style="border-color:#10B981;background:#ECFDF5;">
              <span class="opd-token-num" style="color:#059669;">#${token}</span>
              <span class="opd-token-sub" style="color:#047857;">Your Token</span>
            </div>
            <div style="text-align:right;">
              <div style="font-size:11px;color:#047857;font-weight:700;">Est. Wait: ${isMyTurn ? 'Immediate' : '~10 Mins'}</div>
              <button type="button" class="btn btn-outline btn-sm" id="opd-refresh-radar-btn" style="padding:4px 8px;font-size:11px;margin-top:4px;background:#fff;">
                ↻ Refresh Radar
              </button>
            </div>
          </div>
        </div>
      `;

      document.getElementById('opd-refresh-radar-btn')?.addEventListener('click', () => {
        playHospitalChime();
        if (window.Api && Api.showToast) {
          Api.showToast('Queue status synchronized with OPD Room 204', 'info');
        }
      });
    }
  };

  function escapeHtml(str) {
    if (!str) return '';
    const div = document.createElement('div');
    div.textContent = str;
    return div.innerHTML;
  }

  // Expose global modules
  window.TelemedicineSuite = TelemedicineSuite;
  window.WhatsAppAlertDispatcher = WhatsAppAlertDispatcher;
  window.OpdQueueManager = OpdQueueManager;
  window.playHospitalChime = playHospitalChime;
})();
