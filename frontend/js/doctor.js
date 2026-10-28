/**
 * ====================================================================
 * DOCTOR AVAILABILITY & SCHEDULING SYSTEM
 * Doctor Clinical Workspace (doctor.js)
 * ====================================================================
 */

let activeCompleteAppId = null;

document.addEventListener('DOMContentLoaded', async () => {
  verifyDoctorSession();
  await loadDoctorDashboard();
  setupDoctorTabs();
  setupCompleteConsultationModal();
  setupLeaveRequestForm();
});

function verifyDoctorSession() {
  const user = Api.getUser();
  if (!user || user.role !== 'DOCTOR' || !Api.getToken()) {
    window.location.href = 'login.html';
  }
}

async function loadDoctorDashboard() {
  try {
    const data = await Api.get('/doctor/dashboard');

    document.getElementById('doc-welcome-title').textContent = data.doctorName;
    document.getElementById('doc-spec-badge').textContent = data.specialization;

    // Metrics
    document.getElementById('stat-today').textContent = data.todayCount;
    document.getElementById('stat-upcoming').textContent = data.upcomingCount;
    document.getElementById('stat-completed-today').textContent = data.completedTodayCount;
    document.getElementById('stat-pending').textContent = data.pendingCount;
    document.getElementById('stat-total-patients').textContent = data.totalPatients;
    document.getElementById('stat-earnings').textContent = Api.formatCurrency(data.monthlyEarnings);

    // Today's Queue
    renderTodayQueue(data.todayAppointments);

    // Upcoming appointments
    renderUpcomingQueue(data.upcomingAppointments);

    // Schedule table
    renderScheduleTable(data.currentSchedules);
  } catch (err) {
    Api.showToast('Failed to load doctor dashboard: ' + err.message, 'error');
  }
}

function renderTodayQueue(appointments) {
  const container = document.getElementById('today-queue-list');
  if (!container) return;

  if (!appointments || appointments.length === 0) {
    container.innerHTML = `
      <div class="empty-state">
        <div class="empty-state-icon" style="background:var(--surface-light-teal);color:var(--healthcare-teal);">
          <svg width="28" height="28" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"></path></svg>
        </div>
        <h4 class="empty-state-title">No Appointments Remaining Today</h4>
        <p class="empty-state-desc">You are all caught up on consultations for today.</p>
      </div>
    `;
    return;
  }

  container.innerHTML = '';
  appointments.forEach((app, idx) => {
    const card = document.createElement('div');
    card.className = 'card';
    card.style.marginBottom = '14px';

    const token = Api.getToken();
    const receiptPreviewUrl = `${API_BASE}/appointments/${app.appointmentId}/receipt/preview?token=${token}`;
    const tokenNum = idx + 1;

    card.innerHTML = `
      <div class="card-body" style="display:flex;align-items:center;justify-content:space-between;flex-wrap:wrap;gap:16px;">
        <div style="display:flex;align-items:center;gap:14px;">
          <div style="width:54px;height:54px;border-radius:10px;background:var(--surface-light-teal);color:var(--healthcare-teal);display:flex;flex-direction:column;align-items:center;justify-content:center;">
            <span style="font-weight:800;font-size:14px;line-height:1;">${app.startTime.substring(0, 5)}</span>
            <span style="font-size:9.5px;font-weight:700;color:#047857;margin-top:2px;">Token #${tokenNum}</span>
          </div>
          <div>
            <div style="display:flex;align-items:center;gap:10px;margin-bottom:2px;">
              <span style="font-weight:700;font-size:15px;color:var(--text-main);">${escapeHtml(app.patientName)}</span>
              ${Api.getStatusBadge(app.appointmentStatus)}
              <span class="badge ${app.appointmentType === 'VIDEO_CONSULTATION' ? 'badge-in-progress' : ''}" style="font-size:11px;">
                ${app.appointmentType === 'VIDEO_CONSULTATION' ? '📹 Tele-Health' : '🏥 In-Person OPD'}
              </span>
            </div>
            <div style="font-size:12px;color:var(--text-secondary);">
              <span>📞 ${escapeHtml(app.patientPhone || 'N/A')}</span> | 
              <span>Ref: <strong>${escapeHtml(app.appointmentReference)}</strong></span>
              ${app.patientNotes ? ` | <span>Notes: "${escapeHtml(app.patientNotes)}"</span>` : ''}
            </div>
          </div>
        </div>

        <div class="action-btn-group" style="display:flex;gap:8px;flex-wrap:wrap;align-items:center;">
          <button type="button" class="btn btn-outline btn-sm open-wa-btn" 
            data-patient="${escapeHtml(app.patientName)}"
            data-phone="${escapeHtml(app.patientPhone || '')}"
            data-date="${app.appointmentDate}"
            data-time="${Api.formatTime(app.startTime)}"
            data-ref="${escapeHtml(app.appointmentReference)}"
            data-token="${tokenNum}"
            title="Dispatch WhatsApp Alert">
            📲 WhatsApp
          </button>

          <button type="button" class="btn btn-outline btn-sm open-telehealth-btn"
            data-id="${app.appointmentId}"
            data-patient="${escapeHtml(app.patientName)}"
            data-ref="${escapeHtml(app.appointmentReference)}"
            title="Start HD Video Telemedicine Room">
            📹 Tele-OPD
          </button>

          ${renderDoctorStatusButtons(app)}
          <a href="${receiptPreviewUrl}" target="_blank" class="btn btn-outline btn-sm">Pass</a>
        </div>
      </div>
    `;

    container.appendChild(card);
  });

  attachQueueActionListeners();
}

function renderDoctorStatusButtons(app) {
  const status = app.appointmentStatus;
  const id = app.appointmentId;

  if (status === 'PENDING_CONFIRMATION') {
    return `<button class="btn btn-primary btn-sm status-action-btn" data-id="${id}" data-status="CONFIRMED">Confirm</button>`;
  }
  if (status === 'CONFIRMED') {
    return `
      <button class="btn btn-teal btn-sm status-action-btn" data-id="${id}" data-status="CHECKED_IN">Check In</button>
      <button class="btn btn-outline btn-sm status-action-btn" style="color:var(--status-error);" data-id="${id}" data-status="NO_SHOW">Mark No-Show</button>
    `;
  }
  if (status === 'CHECKED_IN') {
    return `
      <button class="btn btn-primary btn-sm status-action-btn" data-id="${id}" data-status="IN_PROGRESS">Start Consultation</button>
      <button class="btn btn-outline btn-sm status-action-btn" style="color:var(--status-error);" data-id="${id}" data-status="NO_SHOW">No-Show</button>
    `;
  }
  if (status === 'IN_PROGRESS') {
    return `
      <button class="btn btn-teal btn-sm open-complete-modal-btn" data-id="${id}" data-patient="${escapeHtml(app.patientName)}">
        Complete Consultation
      </button>
    `;
  }
  return '';
}

function attachQueueActionListeners() {
  document.querySelectorAll('.status-action-btn').forEach(btn => {
    btn.addEventListener('click', async () => {
      const id = btn.dataset.id;
      const newStatus = btn.dataset.status;

      try {
        btn.disabled = true;
        await Api.patch(`/appointments/${id}/status`, {
          newStatus,
          reason: `Doctor transitioned status to ${newStatus}`
        });
        Api.showToast(`Appointment status updated to ${newStatus}`, 'success');
        loadDoctorDashboard();
      } catch (err) {
        Api.showToast(err.message, 'error');
        btn.disabled = false;
      }
    });
  });

  document.querySelectorAll('.open-complete-modal-btn').forEach(btn => {
    btn.addEventListener('click', () => {
      openCompleteModal(btn.dataset.id, btn.dataset.patient);
    });
  });

  document.querySelectorAll('.open-wa-btn').forEach(btn => {
    btn.addEventListener('click', () => {
      const u = Api.getUser();
      if (window.WhatsAppAlertDispatcher) {
        WhatsAppAlertDispatcher.openModal({
          patientName: btn.dataset.patient,
          phone: btn.dataset.phone,
          doctorName: u ? u.fullName.replace(/^Dr\.\s*/i, '') : 'Specialist',
          specName: 'OPD Specialist',
          date: btn.dataset.date,
          time: btn.dataset.time,
          ref: btn.dataset.ref,
          token: btn.dataset.token
        });
      }
    });
  });

  document.querySelectorAll('.open-telehealth-btn').forEach(btn => {
    btn.addEventListener('click', () => {
      const u = Api.getUser();
      if (window.TelemedicineSuite) {
        TelemedicineSuite.startCall({
          appointmentId: btn.dataset.id,
          patientName: btn.dataset.patient,
          doctorName: u ? u.fullName.replace(/^Dr\.\s*/i, '') : 'Consultant',
          role: 'DOCTOR',
          appointmentRef: btn.dataset.ref,
          onEnded: (sec) => {
            Api.showToast(`Telemedicine session concluded (${Math.floor(sec/60)}m ${sec%60}s).`, 'info');
          }
        });
      }
    });
  });
}

function openCompleteModal(id, patientName) {
  activeCompleteAppId = id;
  const nameEl = document.getElementById('complete-patient-name');
  if (nameEl) nameEl.textContent = patientName;
  const condInput = document.getElementById('ai-rx-condition-input');
  if (condInput) condInput.value = '';
  document.getElementById('complete-modal')?.classList.add('active');
}

function renderUpcomingQueue(appointments) {
  const container = document.getElementById('upcoming-queue-list');
  if (!container) return;

  if (!appointments || appointments.length === 0) {
    container.innerHTML = `<p style="font-size:13px;color:var(--text-secondary);padding:16px;">No upcoming appointments this week.</p>`;
    return;
  }

  container.innerHTML = '';
  appointments.forEach(app => {
    const card = document.createElement('div');
    card.className = 'card';
    card.style.marginBottom = '10px';

    card.innerHTML = `
      <div class="card-body" style="padding:14px 20px;display:flex;align-items:center;justify-content:space-between;flex-wrap:wrap;gap:12px;">
        <div>
          <div style="display:flex;align-items:center;gap:10px;margin-bottom:2px;">
            <span style="font-weight:700;color:var(--text-main);">${escapeHtml(app.patientName)}</span>
            ${Api.getStatusBadge(app.appointmentStatus)}
          </div>
          <span style="font-size:12px;color:var(--text-secondary);">
            📅 ${Api.formatDate(app.appointmentDate)} at ${Api.formatTime(app.startTime)} (${escapeHtml(app.appointmentType)})
          </span>
        </div>
        <div style="font-size:12px;font-weight:600;color:var(--primary-brand);">
          Ref: ${escapeHtml(app.appointmentReference)}
        </div>
      </div>
    `;
    container.appendChild(card);
  });
}

function renderScheduleTable(schedules) {
  const tbody = document.getElementById('schedule-tbody');
  if (!tbody || !schedules) return;

  tbody.innerHTML = '';
  schedules.forEach(s => {
    const tr = document.createElement('tr');
    tr.innerHTML = `
      <td><strong>${escapeHtml(s.workingDay)}</strong></td>
      <td>${s.activeStatus ? `${s.startTime} - ${s.endTime}` : '<span style="color:var(--text-secondary)">Off Day</span>'}</td>
      <td>${s.breakStartTime && s.breakEndTime ? `${s.breakStartTime} - ${s.breakEndTime}` : 'None'}</td>
      <td>${s.slotDurationMinutes} Mins</td>
      <td>
        <span class="badge ${s.activeStatus ? 'badge-confirmed' : 'badge-cancelled'}">
          ${s.activeStatus ? 'Active' : 'Inactive'}
        </span>
      </td>
    `;
    tbody.appendChild(tr);
  });
}

function setupDoctorTabs() {
  const tabs = document.querySelectorAll('.doctor-tab-btn');
  tabs.forEach(tab => {
    tab.addEventListener('click', () => {
      tabs.forEach(t => t.classList.remove('active'));
      tab.classList.add('active');

      const target = tab.dataset.target;
      document.querySelectorAll('.tab-content-panel').forEach(p => p.style.display = 'none');
      const panel = document.getElementById(target);
      if (panel) panel.style.display = 'block';
    });
  });
}

function setupCompleteConsultationModal() {
  const modal = document.getElementById('complete-modal');
  const closeBtn = document.getElementById('complete-modal-close');
  const cancelBtn = document.getElementById('complete-modal-cancel');
  const submitBtn = document.getElementById('complete-submit-btn');

  const close = () => {
    modal?.classList.remove('active');
    activeCompleteAppId = null;
  };

  const aiRxBtn = document.getElementById('ai-generate-rx-btn');
  aiRxBtn?.addEventListener('click', async () => {
    const condInput = document.getElementById('ai-rx-condition-input');
    const condition = condInput ? condInput.value.trim() : '';
    const user = Api.getUser();
    const docName = user ? user.fullName : 'Consulting Doctor';

    try {
      aiRxBtn.disabled = true;
      aiRxBtn.innerHTML = '✨ Generating...';

      const res = await fetch(`${API_BASE}/ai/generate-prescription`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          condition: condition || 'General Health Checkup & Evaluation',
          doctorName: docName,
          specialization: 'Kerala Medical Council Specialist'
        })
      });

      const json = await res.json();
      if (!res.ok || json.success === false) {
        throw new Error(json.message || 'Failed to generate prescription');
      }

      const rxText = json.data?.formattedRx || '';
      document.getElementById('doctor-instructions-input').value = rxText;
      Api.showToast('Clinical Rx & Orders generated successfully!', 'success');
    } catch (err) {
      Api.showToast(err.message, 'error');
    } finally {
      aiRxBtn.disabled = false;
      aiRxBtn.innerHTML = '✨ Auto-Generate Rx';
    }
  });

  submitBtn?.addEventListener('click', async () => {
    if (!activeCompleteAppId) return;

    const instructions = document.getElementById('doctor-instructions-input').value.trim();

    try {
      submitBtn.disabled = true;
      submitBtn.innerHTML = 'Filing Consultation...';

      await Api.patch(`/appointments/${activeCompleteAppId}/status`, {
        newStatus: 'COMPLETED',
        reason: 'Consultation completed by doctor',
        doctorInstructions: instructions
      });

      Api.showToast('Consultation marked completed successfully!', 'success');
      close();
      loadDoctorDashboard();
    } catch (err) {
      Api.showToast(err.message, 'error');
    } finally {
      submitBtn.disabled = false;
      submitBtn.innerHTML = 'Complete & Save';
    }
  });
}

function setupLeaveRequestForm() {
  const form = document.getElementById('leave-request-form');
  if (!form) return;

  form.addEventListener('submit', async (e) => {
    e.preventDefault();

    const start = document.getElementById('leave-start').value;
    const end = document.getElementById('leave-end').value;
    const reason = document.getElementById('leave-reason').value.trim();

    if (!start || !end) {
      Api.showToast('Please select leave start and end dates.', 'error');
      return;
    }

    try {
      await Api.post('/doctor/leave', {
        startDatetime: start + ':00',
        endDatetime: end + ':00',
        reason
      });

      Api.showToast('Leave requested and blocked in availability schedule.', 'success');
      form.reset();
      loadDoctorDashboard();
    } catch (err) {
      Api.showToast(err.message, 'error');
    }
  });
}

function escapeHtml(str) {
  if (!str) return '';
  return str.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;");
}

window.DoctorApp = {
  openCompleteModal
};
