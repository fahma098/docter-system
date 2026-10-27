/**
 * ====================================================================
 * DOCTOR AVAILABILITY & SCHEDULING SYSTEM
 * Authentication & Session Management (auth.js)
 * ====================================================================
 */

document.addEventListener('DOMContentLoaded', () => {
  setupNavbarAuth();
  setupLoginForm();
  setupRegisterForm();
  setupDemoPills();
  setupQuickLoginButtons();
});

function setupNavbarAuth() {
  const user = Api.getUser();
  const navActions = document.getElementById('nav-auth-actions');
  if (!navActions) return;

  if (user && Api.getToken()) {
    let dashboardLink = 'patient-dashboard.html';
    if (user.role === 'DOCTOR') dashboardLink = 'doctor-dashboard.html';
    if (user.role === 'ADMIN') dashboardLink = 'admin-dashboard.html';

    navActions.innerHTML = `
      <div style="display:flex;align-items:center;gap:12px;">
        <span style="font-size:13px;color:var(--text-secondary);font-weight:600;">
          Hi, <strong style="color:var(--text-main);">${escapeHtml(user.fullName.split(' ')[0])}</strong>
        </span>
        <a href="${dashboardLink}" class="btn btn-secondary btn-sm">Dashboard</a>
        <button id="logout-btn" class="btn btn-outline btn-sm">Logout</button>
      </div>
    `;

    document.getElementById('logout-btn')?.addEventListener('click', handleLogout);
  } else {
    navActions.innerHTML = `
      <a href="login.html" class="btn btn-outline btn-sm" style="display:inline-flex;align-items:center;gap:4px;border-color:var(--primary-brand);color:var(--primary-brand);">
        <span>⚡ Demo Sign In</span>
      </a>
      <a href="register.html" class="btn btn-primary btn-sm">Register</a>
    `;
  }
}

function setupLoginForm() {
  const form = document.getElementById('login-form');
  if (!form) return;

  const btn = document.getElementById('login-btn');
  const errorAlert = document.getElementById('login-error-alert');

  form.addEventListener('submit', async (e) => {
    e.preventDefault();
    if (errorAlert) errorAlert.style.display = 'none';

    const email = document.getElementById('email').value.trim();
    const password = document.getElementById('password').value;

    if (!email || !password) {
      showError('Please enter both email and password.');
      return;
    }

    try {
      btn.disabled = true;
      btn.innerHTML = `<span class="spinner"></span> Signing in...`;

      const resp = await Api.post('/auth/login', { email, password });
      Api.setToken(resp.token);
      Api.setUser(resp);
      Api.showToast(`Welcome back, ${resp.fullName}!`, 'success');

      // Role-based redirection
      setTimeout(() => {
        if (resp.role === 'DOCTOR') window.location.href = 'doctor-dashboard.html';
        else if (resp.role === 'ADMIN') window.location.href = 'admin-dashboard.html';
        else window.location.href = 'patient-dashboard.html';
      }, 600);
    } catch (err) {
      showError(err.message || 'Invalid credentials. Please try again.');
    } finally {
      btn.disabled = false;
      btn.innerHTML = `Sign In to Portal`;
    }
  });

  function showError(msg) {
    if (errorAlert) {
      errorAlert.textContent = msg;
      errorAlert.style.display = 'block';
    } else {
      Api.showToast(msg, 'error');
    }
  }
}

function setupRegisterForm() {
  const form = document.getElementById('register-form');
  if (!form) return;

  const btn = document.getElementById('register-btn');
  const errorAlert = document.getElementById('register-error-alert');

  form.addEventListener('submit', async (e) => {
    e.preventDefault();
    if (errorAlert) errorAlert.style.display = 'none';

    const fullName = document.getElementById('reg-name').value.trim();
    const email = document.getElementById('reg-email').value.trim();
    const phone = document.getElementById('reg-phone').value.trim();
    const password = document.getElementById('reg-password').value;
    const confirmPassword = document.getElementById('reg-confirm-password').value;
    
    const dobVal = document.getElementById('reg-dob')?.value?.trim();
    const dateOfBirth = dobVal ? dobVal : null;
    
    const gender = document.getElementById('reg-gender')?.value || null;
    const bloodGroup = document.getElementById('reg-blood')?.value || null;
    const address = document.getElementById('reg-address')?.value?.trim() || null;
    const emergencyContact = document.getElementById('reg-emergency')?.value?.trim() || null;

    if (password !== confirmPassword) {
      showError('Passwords do not match. Please verify.');
      return;
    }

    try {
      btn.disabled = true;
      btn.innerHTML = `<span class="spinner"></span> Creating Account...`;

      const payload = {
        fullName,
        email,
        phone,
        password,
        confirmPassword,
        dateOfBirth,
        gender,
        bloodGroup,
        emergencyContact,
        address
      };

      const resp = await Api.post('/auth/register', payload);
      Api.setToken(resp.token);
      Api.setUser(resp);
      Api.showToast('Account created successfully! Welcome to Aura Health.', 'success');

      setTimeout(() => {
        window.location.href = 'patient-dashboard.html';
      }, 700);
    } catch (err) {
      showError(err.message || 'Registration failed. Please check your details.');
    } finally {
      btn.disabled = false;
      btn.innerHTML = `Complete Registration`;
    }
  });

  function showError(msg) {
    if (errorAlert) {
      errorAlert.textContent = msg;
      errorAlert.style.display = 'block';
      errorAlert.scrollIntoView({ behavior: 'smooth', block: 'center' });
    } else {
      Api.showToast(msg, 'error');
    }
  }
}

function setupDemoPills() {
  const pills = document.querySelectorAll('.demo-pill-btn');
  if (!pills.length) return;

  pills.forEach(pill => {
    pill.addEventListener('click', () => {
      pills.forEach(p => p.classList.remove('active'));
      pill.classList.add('active');

      const role = pill.dataset.role;
      const emailInput = document.getElementById('email');
      const passwordInput = document.getElementById('password');

      if (!emailInput || !passwordInput) return;

      if (role === 'patient') {
        emailInput.value = 'patient.john@healthcare.com';
        passwordInput.value = 'Password123!';
      } else if (role === 'doctor') {
        emailInput.value = 'doctor.sarah@healthcare.com';
        passwordInput.value = 'Password123!';
      } else if (role === 'admin') {
        emailInput.value = 'admin@healthcare.com';
        passwordInput.value = 'Password123!';
      }
    });
  });
}

function setupQuickLoginButtons() {
  document.querySelectorAll('.quick-login-btn').forEach(btn => {
    btn.addEventListener('click', async (e) => {
      e.preventDefault();
      const email = btn.dataset.email;
      const password = btn.dataset.pwd || 'Password123!';
      const redirect = btn.dataset.redirect;
      await fastLogin(email, password, redirect);
    });
  });
}

async function fastLogin(email, password, redirectUrl) {
  try {
    Api.showToast(`Signing in as ${email}...`, 'info');
    const resp = await Api.post('/auth/login', { email, password });
    Api.setToken(resp.token);
    Api.setUser(resp);
    Api.showToast(`Welcome, ${resp.fullName}!`, 'success');
    setTimeout(() => {
      if (redirectUrl) {
        window.location.href = redirectUrl;
      } else {
        if (resp.role === 'DOCTOR') window.location.href = 'doctor-dashboard.html';
        else if (resp.role === 'ADMIN') window.location.href = 'admin-dashboard.html';
        else window.location.href = 'patient-dashboard.html';
      }
    }, 400);
  } catch (err) {
    Api.showToast(err.message || 'Demo login failed', 'error');
  }
}

async function handleLogout() {
  try {
    await Api.post('/auth/logout', {});
  } catch (ignored) {}
  Api.clearSession();
  Api.showToast('Signed out safely', 'info');
  setTimeout(() => {
    window.location.href = 'login.html';
  }, 400);
}

function escapeHtml(str) {
  if (!str) return '';
  return str.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;");
}
