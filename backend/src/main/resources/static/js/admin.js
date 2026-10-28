/**
 * ====================================================================
 * DOCTOR AVAILABILITY & SCHEDULING SYSTEM
 * Administrator Portal & Analytics (admin.js)
 * ====================================================================
 */

let statusChartInstance = null;
let trendChartInstance = null;
let specChartInstance = null;

document.addEventListener('DOMContentLoaded', async () => {
  verifyAdminSession();
  await loadAdminDashboard();
  setupAdminTabs();
  setupAddDoctorModal();
  setupAddSpecModal();
  setupSettingsForm();
  setupCsvExport();
});

function verifyAdminSession() {
  const user = Api.getUser();
  if (!user || user.role !== 'ADMIN' || !Api.getToken()) {
    window.location.href = 'login.html';
  }
}

async function loadAdminDashboard() {
  try {
    const data = await Api.get('/admin/dashboard');

    // KPI Cards
    document.getElementById('stat-total-doctors').textContent = data.totalDoctors;
    document.getElementById('stat-total-patients').textContent = data.totalPatients;
    document.getElementById('stat-today-apps').textContent = data.todayAppointments;
    document.getElementById('stat-pending-apps').textContent = data.pendingAppointments;
    document.getElementById('stat-completed-apps').textContent = data.completedAppointments;
    document.getElementById('stat-cancelled-apps').textContent = data.cancelledAppointments;
    document.getElementById('stat-total-revenue').textContent = Api.formatCurrency(data.totalRevenueCollected);

    // Render Charts
    renderCharts(data);

    // Recent Appointments Table
    renderRecentAppointments(data.recentAppointments);
  } catch (err) {
    Api.showToast('Failed to load admin dashboard: ' + err.message, 'error');
  }
}

function renderCharts(data) {
  if (typeof Chart === 'undefined') return;

  // 1. Status Breakdown Doughnut Chart
  const statusCtx = document.getElementById('statusChart')?.getContext('2d');
  if (statusCtx && data.appointmentsByStatus) {
    if (statusChartInstance) statusChartInstance.destroy();

    const labels = Object.keys(data.appointmentsByStatus);
    const counts = Object.values(data.appointmentsByStatus);

    statusChartInstance = new Chart(statusCtx, {
      type: 'doughnut',
      data: {
        labels: labels.map(l => l.replace('_', ' ')),
        datasets: [{
          data: counts,
          backgroundColor: [
            '#15803D', // Confirmed (green)
            '#B45309', // Pending (amber)
            '#0F766E', // Checked In (teal)
            '#2563EB', // In Progress (blue)
            '#0D655E', // Completed (dark teal)
            '#B91C1C', // Cancelled (red)
            '#64748B'  // No Show (gray)
          ],
          borderWidth: 2,
          borderColor: '#FFFFFF'
        }]
      },
      options: {
        responsive: true,
        plugins: {
          legend: { position: 'bottom', labels: { boxWidth: 12, font: { size: 11 } } }
        }
      }
    });
  }

  // 2. Trend Line Chart (Last 7 Days)
  const trendCtx = document.getElementById('trendChart')?.getContext('2d');
  if (trendCtx && data.appointmentsLast7Days) {
    if (trendChartInstance) trendChartInstance.destroy();

    const days = Object.keys(data.appointmentsLast7Days);
    const dayCounts = Object.values(data.appointmentsLast7Days);

    trendChartInstance = new Chart(trendCtx, {
      type: 'line',
      data: {
        labels: days,
        datasets: [{
          label: 'Appointments',
          data: dayCounts,
          borderColor: '#2563EB',
          backgroundColor: 'rgba(37, 99, 235, 0.08)',
          tension: 0.3,
          fill: true,
          pointBackgroundColor: '#2563EB',
          pointRadius: 4
        }]
      },
      options: {
        responsive: true,
        scales: {
          y: { beginAtZero: true, ticks: { precision: 0 } }
        },
        plugins: {
          legend: { display: false }
        }
      }
    });
  }

  // 3. Specialization Bar Chart
  const specCtx = document.getElementById('specChart')?.getContext('2d');
  if (specCtx && data.appointmentsBySpecialization) {
    if (specChartInstance) specChartInstance.destroy();

    const specNames = Object.keys(data.appointmentsBySpecialization);
    const specCounts = Object.values(data.appointmentsBySpecialization);

    specChartInstance = new Chart(specCtx, {
      type: 'bar',
      data: {
        labels: specNames,
        datasets: [{
          label: 'Bookings',
          data: specCounts,
          backgroundColor: '#0F766E',
          borderRadius: 6
        }]
      },
      options: {
        responsive: true,
        scales: {
          y: { beginAtZero: true, ticks: { precision: 0 } }
        },
        plugins: {
          legend: { display: false }
        }
      }
    });
  }
}

function renderRecentAppointments(appointments) {
  const tbody = document.getElementById('recent-apps-tbody');
  if (!tbody || !appointments) return;

  tbody.innerHTML = '';
  appointments.forEach(app => {
    const tr = document.createElement('tr');
    tr.innerHTML = `
      <td><strong>${escapeHtml(app.appointmentReference)}</strong></td>
      <td>${escapeHtml(app.patientName)}</td>
      <td>${escapeHtml(app.doctorName)}</td>
      <td>${Api.formatDate(app.appointmentDate)} ${Api.formatTime(app.startTime)}</td>
      <td>${Api.getStatusBadge(app.appointmentStatus)}</td>
      <td>${Api.formatCurrency(app.consultationFee)}</td>
      <td>
        <a href="${API_BASE}/appointments/${app.appointmentId}/receipt/preview?token=${Api.getToken()}" target="_blank" class="btn btn-outline btn-sm">
          Pass
        </a>
      </td>
    `;
    tbody.appendChild(tr);
  });
}

function setupAdminTabs() {
  const tabs = document.querySelectorAll('.admin-tab-btn');
  tabs.forEach(tab => {
    tab.addEventListener('click', () => {
      tabs.forEach(t => t.classList.remove('active'));
      tab.classList.add('active');

      const target = tab.dataset.target;
      document.querySelectorAll('.tab-content-panel').forEach(p => p.style.display = 'none');
      const panel = document.getElementById(target);
      if (panel) panel.style.display = 'block';

      if (target === 'panel-doctors') loadDoctorsManagement();
      else if (target === 'panel-patients') loadPatientsManagement();
      else if (target === 'panel-appointments') loadAllAppointmentsManagement();
      else if (target === 'panel-specializations') loadSpecializationsManagement();
      else if (target === 'panel-audit') loadAuditLogs();
      else if (target === 'panel-settings') loadSettings();
    });
  });
}

// --- Doctors Management ---
async function loadDoctorsManagement() {
  const tbody = document.getElementById('doctors-mgmt-tbody');
  if (!tbody) return;

  tbody.innerHTML = `<tr><td colspan="7" style="text-align:center;padding:24px;">Loading doctors...</td></tr>`;

  try {
    const doctors = await Api.get('/doctors?approvedOnly=false');
    tbody.innerHTML = '';

    doctors.forEach(doc => {
      const tr = document.createElement('tr');
      const isApproved = doc.approvalStatus === 'APPROVED';

      tr.innerHTML = `
        <td>
          <div style="display:flex;align-items:center;gap:10px;">
            <img src="${doc.profileImageUrl || 'images/default-doctor.svg'}" onerror="this.onerror=null; this.src='images/default-doctor.svg';" style="width:36px;height:36px;border-radius:50%;object-fit:cover;">
            <div>
              <strong>${escapeHtml(doc.fullName)}</strong>
              <div style="font-size:11px;color:var(--text-secondary);">${escapeHtml(doc.email)}</div>
            </div>
          </div>
        </td>
        <td>${escapeHtml(doc.specializationName)}</td>
        <td>${escapeHtml(doc.licenseNumber)}</td>
        <td>${doc.experienceYears} Yrs</td>
        <td>${Api.formatCurrency(doc.consultationFee)}</td>
        <td>
          <span class="badge ${isApproved ? 'badge-confirmed' : 'badge-pending'}">
            ${escapeHtml(doc.approvalStatus)}
          </span>
        </td>
        <td>
          <button class="btn btn-outline btn-sm toggle-doc-approval-btn" data-id="${doc.doctorId}" data-status="${isApproved ? 'REJECTED' : 'APPROVED'}">
            ${isApproved ? 'Suspend' : 'Approve'}
          </button>
        </td>
      `;
      tbody.appendChild(tr);
    });

    document.querySelectorAll('.toggle-doc-approval-btn').forEach(btn => {
      btn.addEventListener('click', async () => {
        const id = btn.dataset.id;
        const newStatus = btn.dataset.status;
        try {
          await Api.patch(`/admin/doctors/${id}/approval`, { status: newStatus });
          Api.showToast(`Doctor status updated to ${newStatus}`, 'success');
          loadDoctorsManagement();
        } catch (err) {
          Api.showToast(err.message, 'error');
        }
      });
    });
  } catch (err) {
    tbody.innerHTML = `<tr><td colspan="7" style="color:var(--status-error);text-align:center;">Failed to load doctors: ${err.message}</td></tr>`;
  }
}

// --- Add Doctor Modal ---
function setupAddDoctorModal() {
  const modal = document.getElementById('add-doctor-modal');
  const openBtn = document.getElementById('open-add-doc-modal-btn');
  const closeBtn = document.getElementById('add-doc-modal-close');
  const cancelBtn = document.getElementById('add-doc-modal-cancel');
  const form = document.getElementById('add-doctor-form');

  openBtn?.addEventListener('click', async () => {
    // Populate specialization dropdown in modal
    const specSelect = document.getElementById('new-doc-spec');
    if (specSelect) {
      const specs = await Api.get('/specializations');
      specSelect.innerHTML = specs.map(s => `<option value="${s.specializationId}">${escapeHtml(s.specializationName)}</option>`).join('');
    }
    modal?.classList.add('active');
  });

  const close = () => modal?.classList.remove('active');
  closeBtn?.addEventListener('click', close);
  cancelBtn?.addEventListener('click', close);

  form?.addEventListener('submit', async (e) => {
    e.preventDefault();
    try {
      const payload = {
        fullName: document.getElementById('new-doc-name').value.trim(),
        email: document.getElementById('new-doc-email').value.trim(),
        phone: document.getElementById('new-doc-phone').value.trim(),
        password: document.getElementById('new-doc-password').value,
        specializationId: parseInt(document.getElementById('new-doc-spec').value, 10),
        licenseNumber: document.getElementById('new-doc-license').value.trim(),
        qualifications: document.getElementById('new-doc-qual').value.trim(),
        experienceYears: parseInt(document.getElementById('new-doc-exp').value, 10) || 0,
        consultationFee: parseFloat(document.getElementById('new-doc-fee').value) || 50.00,
        defaultAppointmentDuration: 30,
        professionalBio: document.getElementById('new-doc-bio').value.trim(),
        profileImageUrl: document.getElementById('new-doc-img').value.trim() || 'https://images.unsplash.com/photo-1559839734-2b71ea197ec2?auto=format&fit=crop&q=80&w=400'
      };

      await Api.post('/admin/doctors', payload);
      Api.showToast('Doctor account registered and scheduled successfully!', 'success');
      form.reset();
      close();
      loadDoctorsManagement();
    } catch (err) {
      Api.showToast(err.message, 'error');
    }
  });
}

// --- Patients Management ---
async function loadPatientsManagement() {
  const tbody = document.getElementById('patients-mgmt-tbody');
  if (!tbody) return;

  tbody.innerHTML = `<tr><td colspan="6" style="text-align:center;padding:24px;">Loading registered patients...</td></tr>`;

  try {
    const patients = await Api.get('/admin/patients');
    tbody.innerHTML = '';

    patients.forEach(p => {
      const tr = document.createElement('tr');
      tr.innerHTML = `
        <td>#${p.patientId}</td>
        <td><strong>${escapeHtml(p.fullName)}</strong></td>
        <td>${escapeHtml(p.email)}</td>
        <td>${escapeHtml(p.phone)}</td>
        <td>${p.bloodGroup || 'N/A'}</td>
        <td>${p.emergencyContact || 'N/A'}</td>
      `;
      tbody.appendChild(tr);
    });
  } catch (err) {
    tbody.innerHTML = `<tr><td colspan="6" style="color:var(--status-error);text-align:center;">Failed: ${err.message}</td></tr>`;
  }
}

// --- Specializations Management ---
async function loadSpecializationsManagement() {
  const tbody = document.getElementById('specs-mgmt-tbody');
  if (!tbody) return;

  try {
    const specs = await Api.get('/specializations');
    tbody.innerHTML = '';

    specs.forEach(s => {
      const tr = document.createElement('tr');
      tr.innerHTML = `
        <td>#${s.specializationId}</td>
        <td><strong>${escapeHtml(s.specializationName)}</strong></td>
        <td>${escapeHtml(s.description || 'N/A')}</td>
        <td><span class="badge ${s.activeStatus ? 'badge-confirmed' : 'badge-cancelled'}">${s.activeStatus ? 'Active' : 'Inactive'}</span></td>
      `;
      tbody.appendChild(tr);
    });
  } catch (err) {
    tbody.innerHTML = `<tr><td colspan="4" style="color:var(--status-error);">Failed: ${err.message}</td></tr>`;
  }
}

function setupAddSpecModal() {
  const modal = document.getElementById('add-spec-modal');
  const openBtn = document.getElementById('open-add-spec-btn');
  const closeBtn = document.getElementById('add-spec-modal-close');
  const cancelBtn = document.getElementById('add-spec-modal-cancel');
  const form = document.getElementById('add-spec-form');

  openBtn?.addEventListener('click', () => modal?.classList.add('active'));
  const close = () => modal?.classList.remove('active');
  closeBtn?.addEventListener('click', close);
  cancelBtn?.addEventListener('click', close);

  form?.addEventListener('submit', async (e) => {
    e.preventDefault();
    try {
      const payload = {
        specializationName: document.getElementById('spec-name-input').value.trim(),
        description: document.getElementById('spec-desc-input').value.trim(),
        activeStatus: true
      };
      await Api.post('/admin/specializations', payload);
      Api.showToast('Specialization created!', 'success');
      form.reset();
      close();
      loadSpecializationsManagement();
    } catch (err) {
      Api.showToast(err.message, 'error');
    }
  });
}

// --- Master Appointments Management ---
async function loadAllAppointmentsManagement() {
  const tbody = document.getElementById('all-apps-tbody');
  if (!tbody) return;

  tbody.innerHTML = `<tr><td colspan="8" style="text-align:center;padding:24px;">Loading appointments...</td></tr>`;

  try {
    const apps = await Api.get('/admin/appointments');
    tbody.innerHTML = '';

    apps.forEach(app => {
      const tr = document.createElement('tr');
      tr.innerHTML = `
        <td><strong>${escapeHtml(app.appointmentReference)}</strong></td>
        <td>${escapeHtml(app.patientName)}</td>
        <td>${escapeHtml(app.doctorName)} (${escapeHtml(app.specializationName)})</td>
        <td>${Api.formatDate(app.appointmentDate)}</td>
        <td>${Api.formatTime(app.startTime)}</td>
        <td>${Api.getStatusBadge(app.appointmentStatus)}</td>
        <td>${Api.formatCurrency(app.consultationFee)}</td>
        <td>
          <a href="${API_BASE}/appointments/${app.appointmentId}/receipt/preview?token=${Api.getToken()}" target="_blank" class="btn btn-outline btn-sm">
            PDF
          </a>
        </td>
      `;
      tbody.appendChild(tr);
    });
  } catch (err) {
    tbody.innerHTML = `<tr><td colspan="8" style="color:var(--status-error);text-align:center;">Failed: ${err.message}</td></tr>`;
  }
}

// --- Audit Logs ---
async function loadAuditLogs() {
  const tbody = document.getElementById('audit-logs-tbody');
  if (!tbody) return;

  try {
    const logs = await Api.get('/admin/audit-logs?limit=50');
    tbody.innerHTML = '';

    logs.forEach(l => {
      const tr = document.createElement('tr');
      tr.innerHTML = `
        <td style="font-size:11px;color:var(--text-secondary);">${l.createdAt ? l.createdAt.replace('T', ' ').substring(0, 19) : ''}</td>
        <td><strong>${escapeHtml(l.action)}</strong></td>
        <td>${escapeHtml(l.actorName || 'System')} <span style="font-size:11px;color:var(--text-secondary)">(${l.actorRole || 'SYSTEM'})</span></td>
        <td>${escapeHtml(l.entityType)} [${escapeHtml(l.entityId || '')}]</td>
        <td style="font-size:12px;">${escapeHtml(l.details || '')}</td>
      `;
      tbody.appendChild(tr);
    });
  } catch (err) {
    tbody.innerHTML = `<tr><td colspan="5" style="color:var(--status-error);">Failed: ${err.message}</td></tr>`;
  }
}

// --- Settings ---
async function loadSettings() {
  try {
    const settings = await Api.get('/admin/settings');
    for (const [k, v] of Object.entries(settings)) {
      const input = document.getElementById(`setting-${k}`);
      if (input) input.value = v;
    }
  } catch (err) {
    Api.showToast('Failed to load settings: ' + err.message, 'error');
  }
}

function setupSettingsForm() {
  const form = document.getElementById('settings-form');
  form?.addEventListener('submit', async (e) => {
    e.preventDefault();
    try {
      const fields = [
        'CLINIC_NAME',
        'CLINIC_PHONE',
        'CLINIC_EMAIL',
        'CLINIC_ADDRESS',
        'MIN_ADVANCE_BOOKING_HOURS',
        'MAX_ADVANCE_BOOKING_DAYS',
        'CANCELLATION_DEADLINE_HOURS'
      ];

      for (const field of fields) {
        const input = document.getElementById(`setting-${field}`);
        if (input) {
          await Api.put('/admin/settings', { key: field, value: input.value });
        }
      }

      Api.showToast('System policies & clinic settings updated!', 'success');
    } catch (err) {
      Api.showToast('Failed to save settings: ' + err.message, 'error');
    }
  });
}

function setupCsvExport() {
  document.getElementById('export-csv-btn')?.addEventListener('click', () => {
    const token = Api.getToken();
    window.open(`${API_BASE}/admin/reports/csv?token=${token}`, '_blank');
  });
}

function escapeHtml(str) {
  if (!str) return '';
  return str.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;");
}
