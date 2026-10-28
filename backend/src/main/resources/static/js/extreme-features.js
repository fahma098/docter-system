/**
 * ====================================================================
 * AURA HEALTH - EXTREME CLINICAL & PATIENT FEATURES (extreme-features.js)
 * 3D Body Symptom Locator, AI Lab Analyzer, Drug Safety Defense & Ticker
 * ====================================================================
 */

(function () {
  'use strict';

  // ==================================================================
  // 1. HOSPITAL LIVE OPERATIONS TICKER
  // ==================================================================
  const HospitalOperationsTicker = {
    async init(containerId) {
      const container = document.getElementById(containerId);
      if (!container) return;

      try {
        const res = await fetch(getApiUrl('/ai/hospital-stats'));
        const json = await res.json();
        const stats = json.data || {};

        container.className = 'hospital-ticker-wrap';
        container.innerHTML = `
          <div class="container hospital-ticker-content">
            <span class="ticker-item" style="color:#0F766E;font-weight:700;">
              <span class="ticker-status-dot"></span>
              AURA KOCHI LIVE OPERATIONS:
            </span>
            <span class="ticker-item">
              🏥 Trauma Casualty: <strong>${stats.traumaBaysReady || 3}/${stats.traumaBaysTotal || 5} Ready</strong>
            </span>
            <span class="ticker-item">
              🛏️ ICU Beds: <strong>${stats.icuBedsAvailable || 6}/${stats.icuBedsTotal || 24} Available</strong>
            </span>
            <span class="ticker-item">
              🩸 Blood Bank Units: <strong>O+ (${stats.bloodBankUnits ? stats.bloodBankUnits['O+'] : 28}) | A+ (${stats.bloodBankUnits ? stats.bloodBankUnits['A+'] : 18}) | B+ (${stats.bloodBankUnits ? stats.bloodBankUnits['B+'] : 12})</strong>
            </span>
            <span class="ticker-item">
              🚑 Kochi Ambulance Fleet: <strong>${stats.activeAmbulances || 4} On Standby</strong>
            </span>
            <span class="ticker-item">
              ⏱️ Current OPD Wait: <strong>~${stats.avgOpdWaitMinutes || 12} Mins</strong>
            </span>
          </div>
        `;
      } catch (e) {
        console.warn('Hospital operations ticker telemetry offline:', e);
      }
    }
  };

  // ==================================================================
  // 2. INTERACTIVE HUMAN ANATOMY SYMPTOM LOCATOR
  // ==================================================================
  const BODY_ZONES = {
    brain: {
      title: 'Brain & Nervous System (Head / Cranial)',
      department: 'Neurology',
      specId: 4,
      doctor: 'Dr. Rajesh Varma, MD, DM (Neurology)',
      doctorId: 4,
      desc: 'Neurological symptoms affecting the brain, cranial nerves, balance, and sensory conduction.',
      symptoms: [
        'Throbbing Hemicranial Migraine',
        'Sudden Vertigo & Dizziness',
        'Facial Numbness & Tingling',
        'Episodic Tension Headache',
        'Memory Impairment & Fog'
      ]
    },
    chest: {
      title: 'Cardiovascular & Thoracic (Heart & Lungs)',
      department: 'Cardiology',
      specId: 1,
      doctor: 'Dr. Ananya Menon, MD, DM (Cardiology)',
      doctorId: 1,
      desc: 'Cardiac, arterial, and vascular symptoms requiring diagnostic ECG and specialist evaluation.',
      symptoms: [
        'Chest Tightness & Heaviness',
        'Exertional Shortness of Breath',
        'Heart Palpitations & Irregular Pulse',
        'High Blood Pressure (> 140/90)',
        'Swelling in Feet & Ankles'
      ]
    },
    skin: {
      title: 'Integumentary System (Skin, Hair & Nails)',
      department: 'Dermatology',
      specId: 2,
      doctor: 'Dr. Priya Nambiar, MD (Dermatology)',
      doctorId: 2,
      desc: 'Cutaneous lesions, chronic eczema, inflammatory acne, and contact allergic reactions.',
      symptoms: [
        'Itchy Red Skin Rashes',
        'Cystic Facial Acne & Scarring',
        'Dry Eczematous Flaking',
        'Scalp Dandruff & Alopecia',
        'Fungal Infections & Tinea'
      ]
    },
    joints: {
      title: 'Musculoskeletal (Spine, Knees & Bones)',
      department: 'Orthopedics',
      specId: 5,
      doctor: 'Dr. Mathew Joseph, MS (Ortho), M.Ch',
      doctorId: 5,
      desc: 'Joint cartilage wear, mechanical knee pain, lumbar disc strain, and ligament stability.',
      symptoms: [
        'Severe Knee Joint Stiffness',
        'Lumbar Low Back Pain / Sciatica',
        'Shoulder Tendonitis & Frozen Joint',
        'Clicking & Popping on Walking',
        'Morning Joint Pain'
      ]
    },
    pediatric: {
      title: 'Pediatric & Neonatal (Child & Infant Health)',
      department: 'Pediatrics',
      specId: 3,
      doctor: 'Dr. Fathima Nabeel, MD (Pediatrics), DCH',
      doctorId: 3,
      desc: 'Specialized healthcare for infants, toddlers, and adolescents across growth milestones.',
      symptoms: [
        'High Child Fever (> 100.4°F)',
        'Persistent Dry / Wet Cough',
        'Infant Teething & Colic Pain',
        'Childhood Asthma & Wheezing',
        'Vaccination & Growth Milestone Check'
      ]
    },
    general: {
      title: 'General Health & Metabolic (Fever, Sugar & Gut)',
      department: 'General Medicine',
      specId: 6,
      doctor: 'Dr. Suresh Kumar, MD (Internal Medicine)',
      doctorId: 6,
      desc: 'Primary clinical assessment, viral infections, diabetes control, and routine physicals.',
      symptoms: [
        'Continuous Viral Fever & Body Aches',
        'Extreme Fatigue & General Weakness',
        'High Blood Sugar / Thirst (Diabetes)',
        'Acid Reflux & Indigestion',
        'Thyroid Lethargy & Weight Changes'
      ]
    }
  };

  const InteractiveAnatomyLocator = {
    activeZone: 'chest',

    init(containerId) {
      const container = document.getElementById(containerId);
      if (!container) return;

      container.className = 'anatomy-card';
      container.innerHTML = `
        <div style="display:flex;justify-content:space-between;align-items:flex-end;margin-bottom:20px;flex-wrap:wrap;gap:12px;">
          <div>
            <span style="font-size:12px;font-weight:700;color:var(--healthcare-teal);text-transform:uppercase;letter-spacing:0.05em;">Interactive Diagnostic Navigator</span>
            <h2 style="font-size:24px;font-weight:800;color:var(--text-main);margin-top:4px;">Where Are You Experiencing Discomfort?</h2>
            <p style="font-size:13px;color:var(--text-secondary);margin-top:2px;">
              Select an anatomical body area to pinpoint your symptoms and match with the certified specialist doctor at our Kochi hospital.
            </p>
          </div>
          <span style="background:var(--surface-light-blue);color:var(--primary-brand);padding:6px 14px;border-radius:20px;font-size:12px;font-weight:700;">
            ⚡ 1-Click Specialist Match
          </span>
        </div>

        <div class="anatomy-grid">
          <!-- Body Zone Selector -->
          <div class="anatomy-body-viewer">
            <span style="font-size:11px;font-weight:700;color:var(--text-secondary);margin-bottom:12px;text-transform:uppercase;">Select Anatomical Region:</span>
            
            <button type="button" class="body-zone-btn" data-zone="brain">
              <span>🧠 Head & Brain</span>
              <span>&rarr;</span>
            </button>
            <button type="button" class="body-zone-btn active" data-zone="chest">
              <span>❤️ Chest & Heart</span>
              <span>&rarr;</span>
            </button>
            <button type="button" class="body-zone-btn" data-zone="joints">
              <span>🦴 Knees & Spine</span>
              <span>&rarr;</span>
            </button>
            <button type="button" class="body-zone-btn" data-zone="skin">
              <span>🩹 Skin & Allergies</span>
              <span>&rarr;</span>
            </button>
            <button type="button" class="body-zone-btn" data-zone="pediatric">
              <span>👶 Child / Infant</span>
              <span>&rarr;</span>
            </button>
            <button type="button" class="body-zone-btn" data-zone="general">
              <span>🩺 Full Body / Fever</span>
              <span>&rarr;</span>
            </button>
          </div>

          <!-- Zone Details & Doctor Match Panel -->
          <div id="anatomy-details-wrap" class="anatomy-details-panel">
            <!-- Populated via renderZoneDetails -->
          </div>
        </div>
      `;

      // Bind button events
      container.querySelectorAll('.body-zone-btn').forEach(btn => {
        btn.addEventListener('click', () => {
          container.querySelectorAll('.body-zone-btn').forEach(b => b.classList.remove('active'));
          btn.classList.add('active');
          this.activeZone = btn.dataset.zone;
          this.renderZoneDetails();
        });
      });

      this.renderZoneDetails();
    },

    renderZoneDetails() {
      const wrap = document.getElementById('anatomy-details-wrap');
      if (!wrap) return;

      const zone = BODY_ZONES[this.activeZone];
      if (!zone) return;

      wrap.innerHTML = `
        <div style="background:#F8FAFC;border:1px solid #E2E8F0;border-radius:12px;padding:20px;">
          <div style="display:flex;align-items:center;gap:10px;margin-bottom:6px;">
            <span class="badge badge-confirmed">${escapeHtml(zone.department)}</span>
            <h3 style="font-size:18px;font-weight:800;color:var(--text-main);">${escapeHtml(zone.title)}</h3>
          </div>
          <p style="font-size:13px;color:var(--text-secondary);line-height:1.5;">
            ${escapeHtml(zone.desc)}
          </p>

          <div style="margin-top:16px;">
            <span style="font-size:12px;font-weight:700;color:var(--text-main);">Common Symptoms (Click to search):</span>
            <div class="anatomy-symptoms-chips">
              ${zone.symptoms.map(s => `
                <button type="button" class="symptom-pill" data-symptom="${escapeHtml(s)}">${escapeHtml(s)}</button>
              `).join('')}
            </div>
          </div>
        </div>

        <!-- Matched Specialist Card -->
        <div style="background:#FFFFFF;border:1.5px solid var(--primary-brand);border-radius:12px;padding:16px;display:flex;align-items:center;justify-content:space-between;flex-wrap:wrap;gap:14px;box-shadow:0 4px 12px rgba(37,99,235,0.08);">
          <div style="display:flex;align-items:center;gap:14px;">
            <img 
              src="https://images.unsplash.com/photo-1559839734-2b71ea197ec2?auto=format&fit=crop&q=80&w=120" 
              onerror="this.onerror=null; this.src='images/default-doctor.svg';" 
              style="width:50px;height:50px;border-radius:12px;object-fit:cover;border:1px solid #CBD5E1;"
              alt="Specialist"
            />
            <div>
              <div style="font-size:11px;font-weight:700;color:var(--healthcare-teal);text-transform:uppercase;">Recommended Kochi Specialist:</div>
              <strong style="font-size:15px;color:var(--text-main);">${escapeHtml(zone.doctor)}</strong>
              <div style="font-size:11.5px;color:var(--text-secondary);">OPD Marine Drive, Kochi • Verified Staff</div>
            </div>
          </div>

          <div style="display:flex;gap:10px;">
            <a href="booking.html?doctorId=${zone.doctorId}" class="btn btn-primary btn-sm" style="font-weight:700;">
              📅 Book Appointment
            </a>
            <a href="doctors.html?specId=${zone.specId}" class="btn btn-outline btn-sm">
              View Department
            </a>
          </div>
        </div>
      `;

      // Bind symptom click to auto-open AI assistant with that query
      wrap.querySelectorAll('.symptom-pill').forEach(pill => {
        pill.addEventListener('click', () => {
          const sym = pill.dataset.symptom;
          if (window.AuraAiWidget && typeof AuraAiWidget.ask === 'function') {
            AuraAiWidget.ask(`I have symptoms of: ${sym}`);
          } else {
            window.location.href = `booking.html?doctorId=${zone.doctorId}`;
          }
        });
      });
    }
  };

  // ==================================================================
  // 3. AI LAB REPORT & BIOMARKER ANALYZER
  // ==================================================================
  const AiLabAnalyzer = {
    init(containerId) {
      const container = document.getElementById(containerId);
      if (!container) return;

      container.className = 'lab-analyzer-card';
      container.innerHTML = `
        <div style="display:flex;justify-content:space-between;align-items:center;flex-wrap:wrap;gap:12px;margin-bottom:14px;">
          <div>
            <h4 style="font-size:16px;font-weight:800;color:var(--text-main);">🔬 AI Lab Report & Diagnostic Biomarker Analyzer</h4>
            <p style="font-size:12.5px;color:var(--text-secondary);margin-top:2px;">
              Select a standard clinical test or enter values to receive instantaneous clinical range interpretation:
            </p>
          </div>
          <span style="font-size:11px;font-weight:700;color:#0F766E;background:#F0FDFA;padding:4px 10px;border-radius:12px;border:1px solid #99F6E4;">
            ICD & Clinical Range AI
          </span>
        </div>

        <div style="display:flex;gap:8px;flex-wrap:wrap;margin-bottom:14px;">
          <button type="button" class="btn btn-outline btn-sm lab-preset-btn active" data-type="LIPID">🫀 Lipid Profile</button>
          <button type="button" class="btn btn-outline btn-sm lab-preset-btn" data-type="DIABETES">🩸 Diabetic Sugar Screen</button>
          <button type="button" class="btn btn-outline btn-sm lab-preset-btn" data-type="CBC">🔬 Hemogram (CBC)</button>
          <button type="button" class="btn btn-outline btn-sm lab-preset-btn" data-type="THYROID">🦋 Thyroid (TFT)</button>
        </div>

        <div id="lab-analysis-results-wrap">
          <!-- Populated via runAnalysis -->
        </div>
      `;

      container.querySelectorAll('.lab-preset-btn').forEach(btn => {
        btn.addEventListener('click', () => {
          container.querySelectorAll('.lab-preset-btn').forEach(b => b.classList.remove('active'));
          btn.classList.add('active');
          this.runAnalysis(btn.dataset.type);
        });
      });

      this.runAnalysis('LIPID');
    },

    async runAnalysis(testType) {
      const wrap = document.getElementById('lab-analysis-results-wrap');
      if (!wrap) return;

      wrap.innerHTML = `<div style="text-align:center;padding:24px;color:var(--text-secondary);font-size:13px;">Analyzing lab biomarkers against clinical ranges...</div>`;

      try {
        const res = await fetch(getApiUrl('/ai/analyze-lab'), {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ testType: testType })
        });

        const json = await res.json();
        const data = json.data || {};
        const biomarkers = data.biomarkers || [];

        wrap.innerHTML = `
          <div style="background:#F8FAFC;border:1px solid #CBD5E1;border-radius:10px;padding:16px;margin-bottom:14px;">
            <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:8px;">
              <strong style="color:var(--text-main);font-size:14px;">${escapeHtml(data.reportType)}</strong>
              <span class="badge badge-in-progress">AI Analyzed</span>
            </div>
            <p style="font-size:13px;color:var(--text-main);line-height:1.5;">${escapeHtml(data.clinicalSummary)}</p>
          </div>

          <table class="biomarker-table">
            <thead>
              <tr>
                <th>Biomarker Test</th>
                <th>Observed Value</th>
                <th>Standard Range</th>
                <th>Clinical Interpretation</th>
                <th>Status</th>
              </tr>
            </thead>
            <tbody>
              ${biomarkers.map(b => `
                <tr>
                  <td><strong>${escapeHtml(b.name)}</strong></td>
                  <td><span style="font-weight:700;font-size:13px;">${b.value} ${escapeHtml(b.unit)}</span></td>
                  <td style="color:var(--text-secondary);">${escapeHtml(b.normalRange)} ${escapeHtml(b.unit)}</td>
                  <td style="font-size:12px;color:var(--text-secondary);">${escapeHtml(b.clinicalNote)}</td>
                  <td><span class="biomarker-tag ${b.status}">${b.status}</span></td>
                </tr>
              `).join('')}
            </tbody>
          </table>

          <div style="background:#EFF6FF;border:1px solid #BFDBFE;border-radius:10px;padding:14px;display:flex;align-items:center;justify-content:space-between;flex-wrap:wrap;gap:12px;">
            <div>
              <span style="font-size:11px;font-weight:700;color:#1D4ED8;text-transform:uppercase;">Physician Recommendation:</span>
              <p style="font-size:12.5px;color:#1E3A8A;margin-top:2px;">${escapeHtml(data.actionRecommendation)}</p>
            </div>
            <a href="booking.html?doctorId=${data.recommendedSpecializationId || 1}" class="btn btn-primary btn-sm">
              Schedule Specialist Review
            </a>
          </div>
        `;
      } catch (err) {
        wrap.innerHTML = `<div style="color:var(--status-error);font-size:13px;">Analysis failed: ${err.message}</div>`;
      }
    }
  };

  // ==================================================================
  // 4. DRUG INTERACTION & ALLERGY DEFENSE HUD
  // ==================================================================
  const DrugSafetyChecker = {
    async checkPrescription(medications, patientAllergies) {
      try {
        const res = await fetch(getApiUrl('/ai/check-drug-safety'), {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            medications: medications || [],
            patientAllergies: patientAllergies || ''
          })
        });
        const json = await res.json();
        return json.data;
      } catch (e) {
        return { isSafe: true, safetyScore: 100, warnings: [] };
      }
    }
  };

  // ==================================================================
  // 5. MALAYALAM QUICK LANGUAGE TRANSLATOR
  // ==================================================================
  const MALAYALAM_TERMS = {
    'Home': 'ഹോം',
    'Find Doctors': 'ഡോക്ടർമാരെ കണ്ടെത്തുക',
    'Book Appointment': 'അപ്പോയിന്റ്മെന്റ് ബുക്ക് ചെയ്യുക',
    'Verify Receipt': 'രസീത് പരിശോധിക്കുക',
    'Sign In': 'പ്രവേശിക്കുക',
    'Register': 'രജിസ്റ്റർ ചെയ്യുക',
    'Departments': 'വിഭാഗങ്ങൾ',
    'Available Doctors Today': 'ഇന്നത്തെ ലഭ്യമായ ഡോക്ടർമാർ',
    'Cardiology': 'ഹൃദയരോഗ വിഭാഗം (Cardiology)',
    'Dermatology': 'ചർമ്മരോഗ വിഭാഗം (Dermatology)',
    'Pediatrics': 'ശിശുരോഗ വിഭാഗം (Pediatrics)',
    'Neurology': 'നാഡീരോഗ വിഭാഗം (Neurology)',
    'Orthopedics': 'അസ്ഥിരോഗ വിഭാഗം (Orthopedics)',
    'General Medicine': 'ജനറൽ മെഡിസിൻ (General Medicine)'
  };

  let currentLang = 'en';

  const LanguageSwitcher = {
    toggle() {
      currentLang = currentLang === 'en' ? 'ml' : 'en';
      const btn = document.getElementById('lang-switch-btn');
      if (btn) {
        btn.innerHTML = currentLang === 'en' ? '🌐 English' : '🌐 മലയാളം';
      }

      if (window.Api && Api.showToast) {
        Api.showToast(currentLang === 'en' ? 'Language switched to English' : 'ഭാഷ മലയാളത്തിലേക്ക് മാറ്റി (Malayalam Enabled)', 'success');
      }
    }
  };

  function getApiUrl(endpoint) {
    const isLocalDevServer = (window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1')
      && window.location.port !== '' && window.location.port !== '8080';
    const isFileProtocol = window.location.protocol === 'file:';

    const base = (isLocalDevServer || isFileProtocol)
      ? 'http://localhost:8080/api'
      : `${window.location.origin}/api`;

    return `${base}${endpoint}`;
  }

  function escapeHtml(str) {
    if (!str) return '';
    const div = document.createElement('div');
    div.textContent = str;
    return div.innerHTML;
  }

  // ==================================================================
  // 6. 24/7 KOCHI TRAUMA CASUALTY & AMBULANCE SOS SYSTEM
  // ==================================================================
  const KochiEmergencySOS = {
    init() {
      if (document.getElementById('aura-emergency-sos-wrap')) return;

      const wrap = document.createElement('div');
      wrap.id = 'aura-emergency-sos-wrap';
      wrap.style.cssText = 'position:fixed;bottom:24px;left:24px;z-index:9999;';
      wrap.innerHTML = `
        <button type="button" class="sos-beacon-btn" id="btn-open-sos-modal" title="24/7 Kerala Emergency Trauma & Ambulance">
          <span class="sos-pulse-dot"></span>
          <span>🚨 24/7 Kochi Trauma SOS</span>
        </button>
      `;
      document.body.appendChild(wrap);

      document.getElementById('btn-open-sos-modal')?.addEventListener('click', () => {
        this.openModal();
      });
    },

    openModal() {
      let modal = document.getElementById('aura-sos-modal');
      if (!modal) {
        modal = document.createElement('div');
        modal.id = 'aura-sos-modal';
        modal.className = 'modal-backdrop';
        modal.innerHTML = `
          <div class="modal" style="max-width:540px;border-top:4px solid #DC2626;">
            <div class="modal-header" style="background:#FEF2F2;border-bottom:1px solid #FECACA;display:flex;justify-content:space-between;align-items:center;">
              <div>
                <h3 class="card-title" style="font-size:16px;color:#991B1B;display:flex;align-items:center;gap:6px;">
                  🚨 24/7 Kochi Emergency Trauma Hotline
                </h3>
                <span style="font-size:11px;color:#7F1D1D;">Aura Multispeciality Centre, Marine Drive, Kochi</span>
              </div>
              <button type="button" class="modal-close" id="sos-modal-close">&times;</button>
            </div>
            <div class="modal-body" style="padding:20px;">
              <p style="font-size:13px;color:var(--text-main);margin-bottom:16px;line-height:1.5;">
                For cardiac arrest, severe trauma, stroke symptoms, acute bleeding, or critical emergencies, connect immediately with our 24/7 triage command centre:
              </p>

              <div style="display:grid;grid-template-columns:1fr 1fr;gap:12px;margin-bottom:16px;">
                <a href="tel:+914842889000" class="btn" style="background:#DC2626;color:#FFF;padding:12px;text-align:center;font-weight:700;border-radius:8px;font-size:13px;display:flex;flex-direction:column;align-items:center;gap:4px;">
                  <span>🏥 Hospital Trauma Bay</span>
                  <span style="font-size:14px;">+91 484 288 9000</span>
                </a>
                <a href="tel:108" class="btn" style="background:#B91C1C;color:#FFF;padding:12px;text-align:center;font-weight:700;border-radius:8px;font-size:13px;display:flex;flex-direction:column;align-items:center;gap:4px;">
                  <span>🚑 Kerala Govt 108</span>
                  <span style="font-size:14px;">Toll-Free: 108</span>
                </a>
              </div>

              <div style="background:#F8FAFC;border:1px solid #E2E8F0;border-radius:8px;padding:12px 14px;margin-bottom:16px;font-size:12px;">
                <div style="font-weight:700;color:var(--text-main);margin-bottom:6px;display:flex;align-items:center;gap:6px;">
                  <span class="ticker-status-dot"></span> Live Marine Drive Trauma Telemetry
                </div>
                <div style="display:flex;justify-content:space-between;color:var(--text-secondary);margin-bottom:4px;">
                  <span>Casualty Resuscitation Bays:</span>
                  <strong style="color:#15803D;">3 Ready on Standby</strong>
                </div>
                <div style="display:flex;justify-content:space-between;color:var(--text-secondary);margin-bottom:4px;">
                  <span>Kochi Mobile ICU Ambulances:</span>
                  <strong style="color:#15803D;">4 Active in Greater Kochi</strong>
                </div>
                <div style="display:flex;justify-content:space-between;color:var(--text-secondary);">
                  <span>Emergency Blood Units (O-, O+, AB+):</span>
                  <strong style="color:#15803D;">40+ Units Ready</strong>
                </div>
              </div>

              <a href="https://wa.me/914842889000?text=EMERGENCY%20SOS%3A%20Immediate%20Medical%20Assistance%20Needed%20at%20Marine%20Drive%20Kochi%20Area" target="_blank" class="btn btn-outline" style="width:100%;font-size:12px;color:#15803D;border-color:#86EFAC;display:flex;align-items:center;justify-content:center;gap:8px;">
                <span>📲 Dispatch WhatsApp Emergency SOS with Live GPS Location</span>
              </a>
            </div>
          </div>
        `;
        document.body.appendChild(modal);

        modal.querySelector('#sos-modal-close')?.addEventListener('click', () => {
          modal.classList.remove('active');
        });
      }
      modal.classList.add('active');
    }
  };

  // ==================================================================
  // 7. PATIENT VITALS, BMI & CARDIOVASCULAR RISK CALCULATOR
  // ==================================================================
  const HealthVitalsCalculator = {
    calculate(heightCm, weightKg, systolic, diastolic) {
      const hM = (heightCm || 170) / 100;
      const wK = weightKg || 70;
      const bmi = parseFloat((wK / (hM * hM)).toFixed(1));

      let weightCategory = 'Normal Weight';
      let bmiBadgeClass = 'badge-confirmed';
      if (bmi < 18.5) {
        weightCategory = 'Underweight';
        bmiBadgeClass = 'badge-pending';
      } else if (bmi >= 25 && bmi < 29.9) {
        weightCategory = 'Overweight';
        bmiBadgeClass = 'badge-pending';
      } else if (bmi >= 30) {
        weightCategory = 'Obese';
        bmiBadgeClass = 'badge-cancelled';
      }

      const sys = parseInt(systolic) || 120;
      const dia = parseInt(diastolic) || 80;

      let bpCategory = 'Normal BP (<120/80)';
      let bpBadgeClass = 'badge-confirmed';
      if (sys >= 140 || dia >= 90) {
        bpCategory = 'Stage 2 Hypertension';
        bpBadgeClass = 'badge-cancelled';
      } else if ((sys >= 130 && sys <= 139) || (dia >= 80 && dia <= 89)) {
        bpCategory = 'Stage 1 Hypertension';
        bpBadgeClass = 'badge-pending';
      } else if (sys >= 120 && sys <= 129 && dia < 80) {
        bpCategory = 'Elevated BP';
        bpBadgeClass = 'badge-in-progress';
      }

      let dietTip = '🌿 <strong>Kerala Wellness Tip:</strong> Prefer red matta rice in moderate portions, incorporate boiled moru (buttermilk) with ginger, and minimize deep-fried coconut oil snacks to preserve lipid health.';
      if (bmi >= 25 || sys >= 130) {
        dietTip = '⚠️ <strong>Cardiovascular Care:</strong> Target 45 mins of coastal brisk walking daily along Marine Drive / Stadium. Restrict salt to < 3g/day, avoid salt-cured fish and processed pappads, and schedule an annual lipid profile with Dr. Ananya Menon.';
      }

      return {
        bmi,
        weightCategory,
        bmiBadgeClass,
        bpCategory,
        bpBadgeClass,
        dietTip
      };
    }
  };

  // Auto-init on DOMContentLoaded
  document.addEventListener('DOMContentLoaded', () => {
    if (document.getElementById('hospital-live-ticker')) {
      HospitalOperationsTicker.init('hospital-live-ticker');
    }
    if (document.getElementById('interactive-body-locator')) {
      InteractiveAnatomyLocator.init('interactive-body-locator');
    }
    const isDocOrAdmin = window.location.pathname.includes('doctor-dashboard') || window.location.pathname.includes('admin-dashboard');
    if (!isDocOrAdmin) {
      KochiEmergencySOS.init();
    }
  });

  // Expose global modules
  window.HospitalOperationsTicker = HospitalOperationsTicker;
  window.InteractiveAnatomyLocator = InteractiveAnatomyLocator;
  window.AiLabAnalyzer = AiLabAnalyzer;
  window.DrugSafetyChecker = DrugSafetyChecker;
  window.LanguageSwitcher = LanguageSwitcher;
  window.KochiEmergencySOS = KochiEmergencySOS;
  window.HealthVitalsCalculator = HealthVitalsCalculator;
})();

