/**
 * ====================================================================
 * DOCTOR AVAILABILITY & SCHEDULING SYSTEM
 * Centralized API & Utility Module (api.js)
 * ====================================================================
 */

// Automatically use current origin /api in production (Render, AWS, custom domain, etc.)
// or fallback to localhost:8080 when running through standalone static servers (Live Server, file://)
const isLocalDevServer = (window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1') 
  && window.location.port !== '' && window.location.port !== '8080';
const isFileProtocol = window.location.protocol === 'file:';

const API_BASE = (isLocalDevServer || isFileProtocol)
  ? 'http://localhost:8080/api'
  : `${window.location.origin}/api`;

const Api = {
  getToken() {
    return localStorage.getItem('aura_token');
  },

  setToken(token) {
    if (token) localStorage.setItem('aura_token', token);
    else localStorage.removeItem('aura_token');
  },

  getUser() {
    const raw = localStorage.getItem('aura_user');
    try {
      return raw ? JSON.parse(raw) : null;
    } catch {
      return null;
    }
  },

  setUser(user) {
    if (user) localStorage.setItem('aura_user', JSON.stringify(user));
    else localStorage.removeItem('aura_user');
  },

  clearSession() {
    localStorage.removeItem('aura_token');
    localStorage.removeItem('aura_user');
  },

  async request(endpoint, options = {}) {
    const url = endpoint.startsWith('http') ? endpoint : `${API_BASE}${endpoint}`;
    const token = this.getToken();

    const headers = {
      'Content-Type': 'application/json',
      ...(token ? { 'Authorization': `Bearer ${token}` } : {}),
      ...(options.headers || {})
    };

    try {
      const resp = await fetch(url, { ...options, headers });

      if (resp.status === 401) {
        this.clearSession();
        // Redirect to login if on protected page
        if (!window.location.pathname.endsWith('login.html') &&
            !window.location.pathname.endsWith('register.html') &&
            !window.location.pathname.endsWith('index.html') &&
            !window.location.pathname.endsWith('doctors.html')) {
          window.location.href = 'login.html';
        }
        throw new Error('Authentication required. Please sign in again.');
      }

      // Check if response is binary (PDF, CSV)
      const contentType = resp.headers.get('content-type') || '';
      if (contentType.includes('application/pdf') || contentType.includes('text/csv')) {
        if (!resp.ok) {
          throw new Error(`Request failed with status ${resp.status}`);
        }
        return await resp.blob();
      }

      const json = await resp.json();

      if (!resp.ok || json.success === false) {
        throw new Error(json.message || 'An error occurred during operation');
      }

      return json.data;
    } catch (err) {
      console.error(`API Error [${endpoint}]:`, err);
      throw err;
    }
  },

  get(endpoint) {
    return this.request(endpoint, { method: 'GET' });
  },

  post(endpoint, body) {
    return this.request(endpoint, {
      method: 'POST',
      body: JSON.stringify(body)
    });
  },

  put(endpoint, body) {
    return this.request(endpoint, {
      method: 'PUT',
      body: JSON.stringify(body)
    });
  },

  patch(endpoint, body = {}) {
    return this.request(endpoint, {
      method: 'PATCH',
      body: JSON.stringify(body)
    });
  },

  delete(endpoint) {
    return this.request(endpoint, { method: 'DELETE' });
  },

  // Helper: Toast Notifications
  showToast(message, type = 'info') {
    let container = document.getElementById('toast-container');
    if (!container) {
      container = document.createElement('div');
      container.id = 'toast-container';
      container.className = 'toast-container';
      document.body.appendChild(container);
    }

    const toast = document.createElement('div');
    toast.className = `toast toast-${type}`;

    const icon = type === 'success' ? '✓' : type === 'error' ? '✕' : 'ℹ';
    toast.innerHTML = `<span style="font-weight:700;font-size:16px;">${icon}</span> <span>${message}</span>`;
    container.appendChild(toast);

    setTimeout(() => {
      toast.style.opacity = '0';
      toast.style.transform = 'translateY(10px)';
      toast.style.transition = 'all 200ms ease';
      setTimeout(() => toast.remove(), 250);
    }, 4000);
  },

  // Formatting helpers
  formatDate(dateStr) {
    if (!dateStr) return 'N/A';
    const date = new Date(dateStr + 'T00:00:00');
    return date.toLocaleDateString('en-US', {
      weekday: 'short',
      year: 'numeric',
      month: 'short',
      day: 'numeric'
    });
  },

  formatTime(timeStr) {
    if (!timeStr) return '';
    const parts = timeStr.split(':');
    let h = parseInt(parts[0], 10);
    const m = parts[1];
    const ampm = h >= 12 ? 'PM' : 'AM';
    h = h % 12 || 12;
    return `${h}:${m} ${ampm}`;
  },

  formatCurrency(amount) {
    const val = Number(amount || 0);
    return '₹' + val.toLocaleString('en-IN', {
      maximumFractionDigits: 2
    });
  },

  getStatusBadge(status) {
    const s = (status || '').toUpperCase();
    switch (s) {
      case 'CONFIRMED':
        return `<span class="badge badge-confirmed"><span class="badge-dot"></span>Confirmed</span>`;
      case 'PENDING_CONFIRMATION':
        return `<span class="badge badge-pending"><span class="badge-dot"></span>Pending</span>`;
      case 'CHECKED_IN':
        return `<span class="badge badge-checked-in"><span class="badge-dot"></span>Checked In</span>`;
      case 'IN_PROGRESS':
        return `<span class="badge badge-in-progress"><span class="badge-dot"></span>In Progress</span>`;
      case 'COMPLETED':
        return `<span class="badge badge-completed"><span class="badge-dot"></span>Completed</span>`;
      case 'CANCELLED':
        return `<span class="badge badge-cancelled"><span class="badge-dot"></span>Cancelled</span>`;
      case 'NO_SHOW':
        return `<span class="badge badge-no-show"><span class="badge-dot"></span>No Show</span>`;
      default:
        return `<span class="badge"><span class="badge-dot"></span>${s}</span>`;
    }
  }
};
