/**
 * ====================================================================
 * DOCTOR AVAILABILITY & SCHEDULING SYSTEM
 * Patient Dashboard Management (patient.js)
 * ====================================================================
 */

let activeRescheduleAppId = null;
let rescheduleSelectedDate = null;
let rescheduleSelectedSlot = null;

document.addEventListener('DOMContentLoaded', async () => {
  verifyPatientSession();
  await loadPatientDashboard();
  setupPatientTabs();
  setupRescheduleModal();
  setupCancelModal();
});

function verifyPatientSession() {
  const user = Api.getUser();
  if (!user || user.role !== 'PATIENT' || !Api.getToken()) {
    window.location.href = 'login.html';
  }
}

async function loadPatientDashboard() {
  const user = Api.getUser();
  const greetingEl = document.getElementById('patient-greeting');
  if (greetingEl) {
    greetingEl.textContent = `Welcome, ${user.fullName}`;
  }

  try {
    const data = await Api.get('/patients/dashboard');

    // KPI Cards
    document.getElementById('stat-upcoming').textContent = data.upcomingCount;
    document.getElementById('stat-completed').textContent = data.completedCount;
    document.getElementById('stat-pending').textContent = data.pendingCount;
    document.getElementById('stat-cancelled').textContent = data.cancelledCount;

    // Render Live OPD Queue Radar if patient has an appointment
    const cardNameEl = document.getElementById('vault-card-name');
    if (cardNameEl && user) cardNameEl.textContent = user.fullName;

    const todayStr = new Date().toISOString().substring(0, 10);
    const todayApp = (data.upcomingAppointments || []).find(a => a.appointmentDate === todayStr) || (data.upcomingAppointments || [])[0];
    if (todayApp && window.OpdQueueManager) {
      OpdQueueManager.renderPatientRadar('patient-opd-radar-container', todayApp);
    }

    // Upcoming Appointments List
    renderUpcomingAppointments(data.upcomingAppointments);

    // Past Appointments List
    renderPastAppointments(data.pastAppointments);

    // Render Health Vault Prescriptions
    renderHealthVaultPrescriptions(data.pastAppointments || []);

    // Initialize AI Lab Report & Diagnostic Biomarker Analyzer
    if (window.AiLabAnalyzer) {
      AiLabAnalyzer.init('patient-lab-analyzer-wrap');
    }

    // Initialize 24/7 Kochi Emergency SOS Beacon
    if (window.KochiEmergencySOS) {
      KochiEmergencySOS.init();
    }

    // Setup Health Vault Interactive Vitals Calculator
    setupVitalsCalculator();

    // Setup Print Digital Health Pass
    document.getElementById('btn-print-health-card')?.addEventListener('click', () => {
      window.print();
    });
  } catch (err) {
    Api.showToast('Failed to load dashboard: ' + err.message, 'error');
  }
}

function setupVitalsCalculator() {
  const calcBtn = document.getElementById('btn-calc-vitals');
  if (!calcBtn || calcBtn.dataset.wired) return;
  calcBtn.dataset.wired = 'true';

  calcBtn.addEventListener('click', () => {
    const height = parseFloat(document.getElementById('vitals-height')?.value) || 172;
    const weight = parseFloat(document.getElementById('vitals-weight')?.value) || 68;
    const sys = parseInt(document.getElementById('vitals-systolic')?.value) || 120;
    const dia = parseInt(document.getElementById('vitals-diastolic')?.value) || 80;

    if (window.HealthVitalsCalculator) {
      const res = HealthVitalsCalculator.calculate(height, weight, sys, dia);
      const bmiVal = document.getElementById('vitals-bmi-val');
      const weightCat = document.getElementById('vitals-weight-cat');
      const bpCat = document.getElementById('vitals-bp-cat');
      const dietTip = document.getElementById('kerala-diet-tip');

      if (bmiVal) bmiVal.textContent = `${res.bmi} kg/m²`;
      if (weightCat) {
        weightCat.textContent = res.weightCategory;
        weightCat.className = `badge ${res.bmiBadgeClass}`;
      }
      if (bpCat) {
        bpCat.textContent = res.bpCategory;
        bpCat.className = `badge ${res.bpBadgeClass}`;
      }
      if (dietTip) {
        dietTip.innerHTML = res.dietTip;
      }
      Api.showToast('Health vitals telemetry & Kerala wellness advice updated!', 'success');
    }
  });
}

function renderUpcomingAppointments(appointments) {
  const container = document.getElementById('upcoming-appointments-list');
  if (!container) return;

  if (!appointments || appointments.length === 0) {
    container.innerHTML = `
      <div class="empty-state">
        <div class="empty-state-icon">
          <svg width="28" height="28" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z"></path></svg>
        </div>
        <h3 class="empty-state-title">No Upcoming Appointments</h3>
        <p class="empty-state-desc">You do not have any scheduled doctor consultations right now.</p>
        <a href="booking.html" class="btn btn-primary btn-sm">Book an Appointment</a>
      </div>
    `;
    return;
  }

  container.innerHTML = '';
  appointments.forEach(app => {
    const card = document.createElement('div');
    card.className = 'card';
    card.style.marginBottom = '16px';

    const token = Api.getToken();
    const receiptDownloadUrl = `${API_BASE}/appointments/${app.appointmentId}/receipt?token=${token}`;
    const receiptPreviewUrl = `${API_BASE}/appointments/${app.appointmentId}/receipt/preview?token=${token}`;

    card.innerHTML = `
      <div class="card-body" style="display:flex;align-items:center;justify-content:space-between;flex-wrap:wrap;gap:16px;">
        <div style="display:flex;align-items:center;gap:16px;">
          <div style="width:48px;height:48px;border-radius:12px;background:var(--surface-light-blue);color:var(--primary-brand);display:flex;align-items:center;justify-content:center;font-weight:700;">
            <svg width="24" height="24" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z"></path></svg>
          </div>
          <div>
            <div style="display:flex;align-items:center;gap:10px;margin-bottom:4px;">
              <span style="font-weight:700;color:var(--text-main);font-size:15px;">${escapeHtml(app.doctorName)}</span>
              <span class="doctor-spec" style="font-size:11px;">${escapeHtml(app.specializationName)}</span>
              ${Api.getStatusBadge(app.appointmentStatus)}
            </div>
            <div style="font-size:13px;color:var(--text-secondary);display:flex;gap:16px;flex-wrap:wrap;">
              <span>📅 ${Api.formatDate(app.appointmentDate)}</span>
              <span>⏰ ${Api.formatTime(app.startTime)} - ${Api.formatTime(app.endTime)}</span>
              <span>🔖 Ref: <strong>${escapeHtml(app.appointmentReference)}</strong></span>
            </div>
          </div>
        </div>

        <div class="action-btn-group" style="display:flex;gap:8px;flex-wrap:wrap;align-items:center;">
          <button type="button" class="btn btn-teal btn-sm join-telehealth-btn"
            data-id="${app.appointmentId}"
            data-docname="${escapeHtml(app.doctorName)}"
            data-ref="${escapeHtml(app.appointmentReference)}"
            title="Join Secure HD Tele-OPD Video Clinic">
            📹 Join Tele-OPD
          </button>
          <a href="${receiptPreviewUrl}" target="_blank" class="btn btn-outline btn-sm" title="Preview PDF Document">
            <svg width="14" height="14" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"></path><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z"></path></svg>
            Pass
          </a>
          <a href="${receiptDownloadUrl}" class="btn btn-secondary btn-sm" title="Download Receipt PDF">
            PDF
          </a>
          <button class="btn btn-outline btn-sm reschedule-btn" data-id="${app.appointmentId}" data-docid="${app.doctorId}" data-docname="${escapeHtml(app.doctorName)}">
            Reschedule
          </button>
          <button class="btn btn-outline btn-sm cancel-btn" style="color:var(--status-error);border-color:#FCA5A5;" data-id="${app.appointmentId}" data-ref="${escapeHtml(app.appointmentReference)}">
            Cancel
          </button>
        </div>
      </div>
    `;

    container.appendChild(card);
  });

  // Attach Telehealth listeners
  document.querySelectorAll('.join-telehealth-btn').forEach(btn => {
    btn.addEventListener('click', () => {
      const user = Api.getUser();
      if (window.TelemedicineSuite) {
        TelemedicineSuite.startCall({
          appointmentId: btn.dataset.id,
          patientName: user ? user.fullName : 'Patient',
          doctorName: btn.dataset.docname.replace(/^Dr\.\s*/i, ''),
          role: 'PATIENT',
          appointmentRef: btn.dataset.ref,
          onEnded: (sec) => {
            Api.showToast(`Consultation call ended (${Math.floor(sec/60)}m ${sec%60}s).`, 'info');
          }
        });
      }
    });
  });

  // Attach Reschedule & Cancel Listeners
  document.querySelectorAll('.reschedule-btn').forEach(btn => {
    btn.addEventListener('click', () => {
      openRescheduleModal(btn.dataset.id, btn.dataset.docid, btn.dataset.docname);
    });
  });

  document.querySelectorAll('.cancel-btn').forEach(btn => {
    btn.addEventListener('click', () => {
      openCancelModal(btn.dataset.id, btn.dataset.ref);
    });
  });
}

function renderPastAppointments(appointments) {
  const container = document.getElementById('past-appointments-list');
  if (!container) return;

  if (!appointments || appointments.length === 0) {
    container.innerHTML = `<p style="font-size:13px;color:var(--text-secondary);padding:16px;">No historical appointments recorded.</p>`;
    return;
  }

  container.innerHTML = '';
  appointments.forEach(app => {
    const card = document.createElement('div');
    card.className = 'card';
    card.style.marginBottom = '12px';

    const token = Api.getToken();
    const receiptDownloadUrl = `${API_BASE}/appointments/${app.appointmentId}/receipt?token=${token}`;

    card.innerHTML = `
      <div class="card-body" style="padding:16px 20px;display:flex;align-items:center;justify-content:space-between;flex-wrap:wrap;gap:12px;">
        <div>
          <div style="display:flex;align-items:center;gap:10px;margin-bottom:4px;">
            <span style="font-weight:700;color:var(--text-main);">${escapeHtml(app.doctorName)}</span>
            <span class="doctor-spec" style="font-size:10px;">${escapeHtml(app.specializationName)}</span>
            ${Api.getStatusBadge(app.appointmentStatus)}
          </div>
          <p style="font-size:12px;color:var(--text-secondary);">
            ${Api.formatDate(app.appointmentDate)} at ${Api.formatTime(app.startTime)} | Ref: ${escapeHtml(app.appointmentReference)}
          </p>
        </div>
        <div>
          <a href="${receiptDownloadUrl}" class="btn btn-outline btn-sm">Download Receipt</a>
        </div>
      </div>
    `;
    container.appendChild(card);
  });
}

function renderHealthVaultPrescriptions(appointments) {
  const container = document.getElementById('vault-prescriptions-list');
  if (!container) return;

  const withPrescriptions = (appointments || []).filter(a => a.doctorInstructions && a.doctorInstructions.trim().length > 0);

  if (withPrescriptions.length === 0) {
    container.innerHTML = `
      <div style="padding:20px;text-align:center;color:var(--text-secondary);font-size:13px;">
        <span style="font-size:24px;display:block;margin-bottom:6px;">📋</span>
        No digital prescriptions filed yet. When your doctor completes a consultation with an electronic Rx, it will be securely archived here.
      </div>
    `;
    return;
  }

  container.innerHTML = '';
  withPrescriptions.forEach(app => {
    const card = document.createElement('div');
    card.style.background = '#F8FAFC';
    card.style.border = '1px solid var(--border-color)';
    card.style.borderRadius = 'var(--radius-md)';
    card.style.padding = '14px 16px';
    card.style.marginBottom = '12px';

    const token = Api.getToken();
    const receiptDownloadUrl = `${API_BASE}/appointments/${app.appointmentId}/receipt?token=${token}`;

    card.innerHTML = `
      <div style="display:flex;justify-content:space-between;align-items:flex-start;flex-wrap:wrap;gap:10px;margin-bottom:8px;">
        <div>
          <strong style="color:var(--healthcare-teal);font-size:14px;">Dr. ${escapeHtml(app.doctorName)}</strong>
          <span style="font-size:11px;color:var(--text-secondary);margin-left:6px;">(${escapeHtml(app.specializationName)})</span>
          <div style="font-size:11.5px;color:var(--text-secondary);margin-top:2px;">
            Consultation Date: <strong>${Api.formatDate(app.appointmentDate)}</strong> • Ref: <code>${escapeHtml(app.appointmentReference)}</code>
          </div>
        </div>
        <div style="display:flex;gap:8px;">
          <a href="${receiptDownloadUrl}" class="btn btn-outline btn-sm" title="Download Official Signed PDF Pass">
            📥 Download Rx PDF
          </a>
          <a href="booking.html?doctorId=${app.doctorId}" class="btn btn-primary btn-sm">
            Book Follow-up
          </a>
        </div>
      </div>
      <div style="background:#FFFFFF;border:1px solid #E2E8F0;border-radius:8px;padding:12px;font-size:12px;color:var(--text-main);white-space:pre-wrap;line-height:1.5;max-height:160px;overflow-y:auto;font-family:inherit;">
${escapeHtml(app.doctorInstructions)}
      </div>
    `;

    container.appendChild(card);
  });
}

function setupPatientTabs() {
  const tabs = document.querySelectorAll('.patient-tab-btn');
  tabs.forEach(tab => {
    tab.addEventListener('click', () => {
      tabs.forEach(t => t.classList.remove('active'));
      tab.classList.add('active');

      const target = tab.dataset.target;
      document.querySelectorAll('.tab-content-panel').forEach(p => p.style.display = 'none');
      const panel = document.getElementById(target);
      if (panel) panel.style.display = 'block';

      if (target === 'panel-profile') loadPatientProfile();
    });
  });
}

async function loadPatientProfile() {
  try {
    const profile = await Api.get('/patients/profile');
    document.getElementById('prof-name').value = profile.fullName || '';
    document.getElementById('prof-email').value = profile.email || '';
    document.getElementById('prof-phone').value = profile.phone || '';
    document.getElementById('prof-dob').value = profile.dateOfBirth || '';
    document.getElementById('prof-gender').value = profile.gender || '';
    document.getElementById('prof-blood').value = profile.bloodGroup || '';
    document.getElementById('prof-address').value = profile.address || '';
    document.getElementById('prof-emergency').value = profile.emergencyContact || '';
  } catch (err) {
    Api.showToast('Error loading profile: ' + err.message, 'error');
  }
}

document.getElementById('profile-form')?.addEventListener('submit', async (e) => {
  e.preventDefault();
  try {
    const payload = {
      dateOfBirth: document.getElementById('prof-dob').value || null,
      gender: document.getElementById('prof-gender').value || null,
      bloodGroup: document.getElementById('prof-blood').value || null,
      address: document.getElementById('prof-address').value || null,
      emergencyContact: document.getElementById('prof-emergency').value || null
    };

    await Api.put('/patients/profile', payload);
    Api.showToast('Profile information updated successfully!', 'success');
  } catch (err) {
    Api.showToast('Failed to update profile: ' + err.message, 'error');
  }
});

// --- Reschedule Modal ---
function setupRescheduleModal() {
  const modal = document.getElementById('reschedule-modal');
  const closeBtn = document.getElementById('reschedule-modal-close');
  const cancelBtn = document.getElementById('reschedule-modal-cancel');
  const submitBtn = document.getElementById('reschedule-submit-btn');

  const close = () => {
    modal?.classList.remove('active');
    activeRescheduleAppId = null;
    rescheduleSelectedDate = null;
    rescheduleSelectedSlot = null;
  };

  closeBtn?.addEventListener('click', close);
  cancelBtn?.addEventListener('click', close);

  submitBtn?.addEventListener('click', async () => {
    if (!rescheduleSelectedDate || !rescheduleSelectedSlot) {
      Api.showToast('Please select a new date and time slot.', 'error');
      return;
    }

    try {
      submitBtn.disabled = true;
      submitBtn.innerHTML = 'Rescheduling...';

      const payload = {
        newDate: rescheduleSelectedDate,
        newStartTime: rescheduleSelectedSlot.startTime,
        reason: document.getElementById('reschedule-reason')?.value || 'Patient requested reschedule'
      };

      await Api.post(`/appointments/${activeRescheduleAppId}/reschedule`, payload);
      Api.showToast('Appointment successfully rescheduled!', 'success');
      close();
      loadPatientDashboard();
    } catch (err) {
      Api.showToast(err.message, 'error');
    } finally {
      submitBtn.disabled = false;
      submitBtn.innerHTML = 'Confirm Reschedule';
    }
  });
}

function openRescheduleModal(appointmentId, doctorId, doctorName) {
  activeRescheduleAppId = appointmentId;
  const modal = document.getElementById('reschedule-modal');
  if (!modal) return;

  document.getElementById('reschedule-doc-name').textContent = doctorName;
  const dateInput = document.getElementById('reschedule-date-input');

  const minDate = new Date();
  minDate.setDate(minDate.getDate() + 1);
  dateInput.min = minDate.toISOString().split('T')[0];
  dateInput.value = '';

  const slotsContainer = document.getElementById('reschedule-slots-container');
  slotsContainer.innerHTML = '<p style="font-size:12px;color:var(--text-secondary);">Choose a new date above to see open slots</p>';

  dateInput.onchange = async () => {
    rescheduleSelectedDate = dateInput.value;
    if (!rescheduleSelectedDate) return;

    slotsContainer.innerHTML = '<span class="skeleton-text"></span>';
    try {
      const avail = await Api.get(`/doctors/${doctorId}/availability?date=${rescheduleSelectedDate}`);
      slotsContainer.innerHTML = '';

      if (!avail.slots || avail.slots.length === 0) {
        slotsContainer.innerHTML = '<p style="font-size:12px;color:var(--text-secondary);">No slots available on this date.</p>';
        return;
      }

      avail.slots.forEach(slot => {
        const btn = document.createElement('button');
        btn.type = 'button';
        btn.className = 'slot-btn';
        btn.textContent = slot.formattedSlot;
        if (!slot.available) {
          btn.disabled = true;
        } else {
          btn.onclick = () => {
            slotsContainer.querySelectorAll('.slot-btn').forEach(b => b.classList.remove('selected'));
            btn.classList.add('selected');
            rescheduleSelectedSlot = slot;
          };
        }
        slotsContainer.appendChild(btn);
      });
    } catch (err) {
      slotsContainer.innerHTML = `<p style="font-size:12px;color:var(--status-error);">Failed: ${err.message}</p>`;
    }
  };

  modal.classList.add('active');
}

// --- Cancel Modal ---
function setupCancelModal() {
  const modal = document.getElementById('cancel-modal');
  const closeBtn = document.getElementById('cancel-modal-close');
  const cancelBtn = document.getElementById('cancel-modal-dismiss');
  const confirmBtn = document.getElementById('cancel-modal-confirm');

  let targetId = null;

  window.openCancelModal = (appointmentId, reference) => {
    targetId = appointmentId;
    document.getElementById('cancel-modal-ref').textContent = reference;
    modal?.classList.add('active');
  };

  const close = () => {
    modal?.classList.remove('active');
    targetId = null;
  };

  closeBtn?.addEventListener('click', close);
  cancelBtn?.addEventListener('click', close);

  confirmBtn?.addEventListener('click', async () => {
    if (!targetId) return;

    try {
      confirmBtn.disabled = true;
      confirmBtn.innerHTML = 'Cancelling...';

      const reason = document.getElementById('cancel-reason')?.value || 'Patient personal reason';
      await Api.post(`/appointments/${targetId}/cancel`, { reason });

      Api.showToast('Appointment cancelled successfully.', 'info');
      close();
      loadPatientDashboard();
    } catch (err) {
      Api.showToast(err.message, 'error');
    } finally {
      confirmBtn.disabled = false;
      confirmBtn.innerHTML = 'Yes, Cancel Appointment';
    }
  });
}

function escapeHtml(str) {
  if (!str) return '';
  return str.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;");
}
