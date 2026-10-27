/**
 * ====================================================================
 * DOCTOR AVAILABILITY & SCHEDULING SYSTEM
 * Interactive Booking Workflow Engine (booking.js)
 * ====================================================================
 */

let selectedDoctor = null;
let selectedDate = null;
let selectedSlot = null;
let currentCalendarMonth = new Date();

document.addEventListener('DOMContentLoaded', async () => {
  initBookingWizard();
});

async function initBookingWizard() {
  const urlParams = new URLSearchParams(window.location.search);
  const doctorIdParam = urlParams.get('doctorId');

  await loadDoctorDirectoryDropdown(doctorIdParam);
  setupCalendarControls();
  setupNotesInput();
  setupConfirmButton();
}

async function loadDoctorDirectoryDropdown(preselectedId) {
  const select = document.getElementById('doctor-select');
  if (!select) return;

  try {
    const doctors = await Api.get('/doctors?approvedOnly=true');
    select.innerHTML = '<option value="">-- Choose a Specialist Doctor --</option>';

    doctors.forEach(doc => {
      const opt = document.createElement('option');
      opt.value = doc.doctorId;
      opt.textContent = `${doc.fullName} (${doc.specializationName}) - $${doc.consultationFee}`;
      opt.dataset.doctor = JSON.stringify(doc);
      if (preselectedId && String(doc.doctorId) === String(preselectedId)) {
        opt.selected = true;
      }
      select.appendChild(opt);
    });

    select.addEventListener('change', () => {
      const selectedOpt = select.options[select.selectedIndex];
      if (selectedOpt && selectedOpt.value) {
        selectedDoctor = JSON.parse(selectedOpt.dataset.doctor);
        renderDoctorSummary(selectedDoctor);
        renderCalendar();
        resetSlotSelection();
      } else {
        selectedDoctor = null;
        renderDoctorSummary(null);
      }
    });

    if (preselectedId) {
      select.dispatchEvent(new Event('change'));
    }
  } catch (err) {
    Api.showToast('Failed to load doctors: ' + err.message, 'error');
  }
}

function renderDoctorSummary(doc) {
  const card = document.getElementById('summary-doc-preview');
  const feeLabel = document.getElementById('summary-fee-amount');
  const totalLabel = document.getElementById('summary-total-amount');

  if (!doc) {
    if (card) card.innerHTML = `<p style="font-size:13px;color:var(--text-secondary);">Select a doctor to begin booking</p>`;
    if (feeLabel) feeLabel.textContent = '$0.00';
    if (totalLabel) totalLabel.textContent = '$0.00';
    return;
  }

  if (card) {
    card.innerHTML = `
      <img src="${doc.profileImageUrl || 'https://images.unsplash.com/photo-1559839734-2b71ea197ec2?auto=format&fit=crop&q=80&w=200'}" class="doctor-avatar" style="width:54px;height:54px;" alt="Doctor">
      <div>
        <h4 style="font-size:14px;font-weight:700;color:var(--text-main);">${escapeHtml(doc.fullName)}</h4>
        <span class="doctor-spec" style="font-size:11px;">${escapeHtml(doc.specializationName)}</span>
        <p style="font-size:11px;color:var(--text-secondary);margin-top:2px;">${escapeHtml(doc.qualifications || '')}</p>
      </div>
    `;
  }

  const fee = Number(doc.consultationFee || 0).toFixed(2);
  if (feeLabel) feeLabel.textContent = `$${fee}`;
  if (totalLabel) totalLabel.textContent = `$${fee}`;
}

function setupCalendarControls() {
  document.getElementById('cal-prev-btn')?.addEventListener('click', () => {
    currentCalendarMonth.setMonth(currentCalendarMonth.getMonth() - 1);
    renderCalendar();
  });

  document.getElementById('cal-next-btn')?.addEventListener('click', () => {
    currentCalendarMonth.setMonth(currentCalendarMonth.getMonth() + 1);
    renderCalendar();
  });
}

function renderCalendar() {
  const grid = document.getElementById('calendar-grid');
  const title = document.getElementById('cal-month-title');
  if (!grid || !title) return;

  const year = currentCalendarMonth.getFullYear();
  const month = currentCalendarMonth.getMonth();

  title.textContent = currentCalendarMonth.toLocaleDateString('en-US', { month: 'long', year: 'numeric' });

  const firstDay = new Date(year, month, 1).getDay();
  const totalDays = new Date(year, month + 1, 0).getDate();

  grid.innerHTML = '';

  // Blank slots for start offset
  for (let i = 0; i < firstDay; i++) {
    const blank = document.createElement('div');
    blank.className = 'calendar-day-btn';
    blank.style.visibility = 'hidden';
    grid.appendChild(blank);
  }

  const today = new Date();
  today.setHours(0, 0, 0, 0);

  // Active working days of selected doctor
  const activeDays = (selectedDoctor && selectedDoctor.schedules)
    ? selectedDoctor.schedules.filter(s => s.activeStatus).map(s => s.workingDay.toUpperCase())
    : ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY'];

  const dayNames = ['SUNDAY', 'MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY'];

  for (let day = 1; day <= totalDays; day++) {
    const d = new Date(year, month, day);
    const dayName = dayNames[d.getDay()];

    const btn = document.createElement('button');
    btn.type = 'button';
    btn.className = 'calendar-day-btn';
    btn.textContent = day;

    const dateStr = `${year}-${String(month + 1).padStart(2, '0')}-${String(day).padStart(2, '0')}`;

    const isPast = d < today;
    const isWorkingDay = activeDays.includes(dayName);

    if (isPast || !isWorkingDay || !selectedDoctor) {
      btn.disabled = true;
    } else {
      btn.addEventListener('click', () => {
        document.querySelectorAll('.calendar-day-btn').forEach(b => b.classList.remove('selected'));
        btn.classList.add('selected');
        selectedDate = dateStr;
        document.getElementById('summary-date-value').textContent = Api.formatDate(dateStr);
        loadAvailableSlots(selectedDoctor.doctorId, dateStr);
      });
    }

    if (selectedDate === dateStr) {
      btn.classList.add('selected');
    }

    grid.appendChild(btn);
  }
}

async function loadAvailableSlots(doctorId, dateStr) {
  const container = document.getElementById('slots-container');
  const loader = document.getElementById('slots-loading');
  if (!container) return;

  container.innerHTML = '';
  if (loader) loader.style.display = 'block';

  try {
    const data = await Api.get(`/doctors/${doctorId}/availability?date=${dateStr}`);
    if (loader) loader.style.display = 'none';

    if (data.onLeave) {
      container.innerHTML = `
        <div style="grid-column: 1/-1; padding: 18px; text-align:center; background:#FEF3C7; border: 1px solid #FCD34D; border-radius:10px; color:#92400E; font-size:13px;">
          <strong>Doctor on Approved Leave:</strong> Dr. ${escapeHtml(selectedDoctor.fullName)} is unavailable on this date.
        </div>
      `;
      return;
    }

    if (!data.slots || data.slots.length === 0) {
      container.innerHTML = `
        <div style="grid-column: 1/-1; padding: 18px; text-align:center; color:var(--text-secondary); font-size:13px;">
          No available appointment slots on this date. Please select another date.
        </div>
      `;
      return;
    }

    data.slots.forEach(slot => {
      const btn = document.createElement('button');
      btn.type = 'button';
      btn.className = 'slot-btn';
      btn.textContent = slot.formattedSlot;

      if (!slot.available) {
        btn.disabled = true;
        if (slot.reason === 'BOOKED') btn.title = 'Slot Already Booked';
        else if (slot.reason === 'BREAK') btn.title = 'Scheduled Lunch/Break';
        else btn.title = 'Unavailable';
      } else {
        btn.addEventListener('click', () => {
          document.querySelectorAll('.slot-btn').forEach(b => b.classList.remove('selected'));
          btn.classList.add('selected');
          selectedSlot = slot;
          document.getElementById('summary-time-value').textContent = slot.formattedSlot;
          document.getElementById('confirm-booking-btn').disabled = false;
        });
      }

      container.appendChild(btn);
    });
  } catch (err) {
    if (loader) loader.style.display = 'none';
    container.innerHTML = `<div style="color:var(--status-error);font-size:13px;">Failed to load slots: ${err.message}</div>`;
  }
}

function resetSlotSelection() {
  selectedSlot = null;
  const container = document.getElementById('slots-container');
  if (container) container.innerHTML = `<p style="font-size:13px;color:var(--text-secondary);grid-column:1/-1;">Please select an active date from the calendar</p>`;
  const timeVal = document.getElementById('summary-time-value');
  if (timeVal) timeVal.textContent = 'Not Selected';
  const confirmBtn = document.getElementById('confirm-booking-btn');
  if (confirmBtn) confirmBtn.disabled = true;
}

function setupNotesInput() {
  const notes = document.getElementById('patient-notes');
  const typeSelect = document.getElementById('appointment-type');

  typeSelect?.addEventListener('change', () => {
    document.getElementById('summary-type-value').textContent = typeSelect.value;
  });
}

function setupConfirmButton() {
  const btn = document.getElementById('confirm-booking-btn');
  if (!btn) return;

  btn.addEventListener('click', async () => {
    const user = Api.getUser();
    if (!user || user.role !== 'PATIENT') {
      Api.showToast('Please sign in as a Patient to confirm your appointment.', 'info');
      setTimeout(() => {
        window.location.href = `login.html?redirect=booking.html?doctorId=${selectedDoctor ? selectedDoctor.doctorId : ''}`;
      }, 1000);
      return;
    }

    if (!selectedDoctor || !selectedDate || !selectedSlot) {
      Api.showToast('Please complete doctor, date, and slot selection.', 'error');
      return;
    }

    const appType = document.getElementById('appointment-type')?.value || 'IN_PERSON';
    const notes = document.getElementById('patient-notes')?.value || '';

    try {
      btn.disabled = true;
      btn.innerHTML = `<span class="spinner"></span> Confirming with Doctor Schedule...`;

      const payload = {
        doctorId: selectedDoctor.doctorId,
        appointmentDate: selectedDate,
        startTime: selectedSlot.startTime,
        appointmentType: appType,
        patientNotes: notes
      };

      const createdAppointment = await Api.post('/appointments', payload);
      showBookingSuccessModal(createdAppointment);
    } catch (err) {
      Api.showToast(err.message, 'error');
      // If double booking conflict, reload slots immediately!
      if (err.message.includes('no longer available') || err.message.includes('conflict')) {
        loadAvailableSlots(selectedDoctor.doctorId, selectedDate);
      }
    } finally {
      btn.disabled = false;
      btn.innerHTML = `Confirm & Schedule Appointment`;
    }
  });
}

function showBookingSuccessModal(app) {
  const modal = document.getElementById('booking-success-modal');
  if (!modal) {
    Api.showToast('Appointment booked successfully! Ref: ' + app.appointmentReference, 'success');
    window.location.href = 'patient-dashboard.html';
    return;
  }

  document.getElementById('modal-app-ref').textContent = app.appointmentReference;
  document.getElementById('modal-app-doc').textContent = app.doctorName;
  document.getElementById('modal-app-time').textContent = `${app.appointmentDate} at ${Api.formatTime(app.startTime)}`;

  // PDF Download Button
  const dlBtn = document.getElementById('modal-download-receipt-btn');
  if (dlBtn) {
    dlBtn.onclick = () => {
      window.open(`${API_BASE}/appointments/${app.appointmentId}/receipt?token=${Api.getToken()}`, '_blank');
    };
  }

  // PDF Preview Button
  const prevBtn = document.getElementById('modal-preview-receipt-btn');
  if (prevBtn) {
    prevBtn.onclick = () => {
      window.open(`${API_BASE}/appointments/${app.appointmentId}/receipt/preview?token=${Api.getToken()}`, '_blank');
    };
  }

  modal.classList.add('active');
}

function escapeHtml(str) {
  if (!str) return '';
  return str.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;");
}
