# Doctor Availability & Scheduling System
### Enterprise Healthcare SaaS Platform

A full-stack, enterprise-grade **Doctor Availability & Scheduling System** built with **Java 17**, **Spring Boot 3**, **Spring JDBC / JdbcTemplate**, **MySQL**, **OpenPDF**, **ZXing QR Code Engine**, and a **Modern Responsive Light Theme Frontend (HTML5 / CSS3 / JavaScript)**.

---

## 🏥 Architecture Overview

```
                  ┌──────────────────────────────────────────────┐
                  │   Modern Premium Light Frontend (SaaS UI)    │
                  │   HTML5 • CSS3 Custom Properties • Fetch API │
                  │   Inter Typography • Chart.js • Lucide Icons │
                  └──────────────────────┬───────────────────────┘
                                         │ HTTP / REST / JSON & PDF
                                         ▼
                  ┌──────────────────────────────────────────────┐
                  │       Spring Boot 3 REST Controllers         │
                  │  Auth • Doctors • Schedules • Appointments   │
                  │  Patients • Admin • PDF Receipts • Verify QR │
                  └──────────────────────┬───────────────────────┘
                                         │
                                         ▼
                  ┌──────────────────────────────────────────────┐
                  │            Service & Security Layer          │
                  │  BCrypt Hashing • Token/Session Manager      │
                  │  Pessimistic Locking • State Lifecycle Engine│
                  │  OpenPDF Receipt Generator • ZXing QR Engine │
                  └──────────────────────┬───────────────────────┘
                                         │
                                         ▼
                  ┌──────────────────────────────────────────────┐
                  │          JDBC / Data Access Layer            │
                  │  Spring JdbcTemplate • Prepared Statements   │
                  │  Transaction Isolation (Repeatable Read)     │
                  └──────────────────────┬───────────────────────┘
                                         │
                                         ▼
                  ┌──────────────────────────────────────────────┐
                  │               MySQL Database                 │
                  │  doctor_scheduling_system • InnoDB Engines   │
                  │  Foreign Keys • Unique Indexes • Row Locks   │
                  └──────────────────────────────────────────────┘
```

---

## 🎨 Premium Light Theme Palette

Adhering strictly to modern clinical SaaS design requirements:

| Element | Color Hex | Visual Purpose |
| :--- | :--- | :--- |
| **Main Background** | `#F5F8FC` | Soft clinical neutral background |
| **Card Background** | `#FFFFFF` | Crisp elevated white containers with subtle shadow |
| **Primary Brand** | `#2563EB` | Royal healthcare blue for primary actions & branding |
| **Secondary Blue** | `#3B82F6` | Accent blue for interactive states & highlights |
| **Healthcare Teal**| `#0F766E` | Medical teal for verified credentials, badges & pass stamps |
| **Light Blue Surface** | `#EFF6FF` | Tonal pill backgrounds and active tabs |
| **Light Teal Surface** | `#F0FDFA` | Success highlights and confirmation backgrounds |
| **Main Typography** | `#172033` | Deep high-contrast navy for readability |
| **Secondary Text** | `#64748B` | Slate gray for metadata, descriptions & timestamps |
| **Borders & Dividers** | `#E2E8F0` | Subtle clean card borders and table rules |

---

## 🔒 Security & Concurrency: Double-Booking Prevention

Double-booking is prevented using **server-side atomic locking** rather than frontend checks:

1. **Pessimistic Row-Level Locking (`FOR UPDATE`)**:
   During booking execution, the backend runs inside a `@Transactional(isolation = Isolation.REPEATABLE_READ)` boundary and executes:
   ```sql
   SELECT COUNT(*) FROM appointments 
   WHERE doctor_id = ? 
     AND appointment_date = ? 
     AND appointment_status NOT IN ('CANCELLED')
     AND ((start_time < ? AND end_time > ?) OR (start_time >= ? AND start_time < ?))
   FOR UPDATE;
   ```
2. **Doctor Availability Validation**:
   - Verifies the slot falls strictly inside the doctor's active `doctor_schedules` for that day of week.
   - Verifies the slot does not intersect scheduled lunch/tea breaks.
   - Verifies the doctor is not on approved leave (`doctor_unavailable_periods`).
   - Verifies minimum advance booking notice (minimum 2 hours in advance) and calendar window limits.
3. **Collision Handling**:
   If a simultaneous booking attempt locks the slot first, the backend throws `SlotAlreadyBookedException`, rolling back any changes and sending a clean HTTP 409 conflict payload back to the client.

---

## 📄 Downloadable PDF Appointment Receipts & QR Verification

Every confirmed appointment generates a genuine, official **A4 PDF Appointment Receipt & Clinic Pass** using **OpenPDF** and **ZXing QR Code**:

- **Official Header**: Aura Health & Medical Specialist Center branding, address, and phone helpline.
- **Appointment Reference**: Unique reference code (e.g. `DOC-20261028-A101`).
- **Embedded Dynamic QR Code**: Contains a live verification link pointing to `/verify-receipt.html?ref=...`.
- **Patient & Doctor Profile**: Name, contact details, medical qualifications, specialization, and consultation type.
- **Timing & Schedule Breakdown**: Date, exact start and end times, and estimated duration.
- **Financial Breakdown**: Itemized consultation fee, clinic facility charge waiver, taxes, and payment status (`PAID` or `PENDING`).
- **Clinic Policy Notice**: Instructions on arrival time, cancellation policies, and document generation timestamp.

---

## 👥 Role Capabilities

### 1. Patient Portal
- Register account and log in with BCrypt password security.
- Browse certified doctors, filter by medical specialization or keyword.
- Interactive 6-step appointment booking wizard with real-time slot generation.
- Download genuine PDF appointment receipts and clinic passes.
- View upcoming consultations and full appointment history.
- Reschedule eligible appointments to a new open slot.
- Cancel appointments with transparent cancellation policies.
- Update personal contact and emergency details.

### 2. Doctor Workspace
- Secure clinical login.
- View today's clinical queue in chronological order.
- Action status lifecycle transitions:
  - `Pending Confirmation` ➔ `Confirmed`
  - `Confirmed` ➔ `Checked In`
  - `Checked In` ➔ `In Progress (Consultation Started)`
  - `In Progress` ➔ `Completed` (with clinical instructions and advice entry)
  - `Mark No-Show` where authorized.
- Configure weekly working hours (start time, end time, breaks, slot duration).
- Request leave or block unavailable time windows.
- Monitor personal workload and monthly consultation statistics.

### 3. Administrator Console
- Executive dashboard with Chart.js analytics:
  - 7-day appointment trend (line graph)
  - Appointment status distribution (doughnut chart)
  - Bookings by specialization (bar chart)
- Manage doctors: Add new doctor accounts, review licenses, approve or suspend doctor profiles.
- Patient registry: View all registered patient accounts.
- Specialization management: Add or update medical departments.
- Master appointment oversight with multi-field filtering and override controls.
- Audit logs: Immutable trail of security events, logins, and status updates.
- Booking rules configuration: Minimum advance booking hours, maximum booking calendar window, cancellation deadline.
- Export appointments report to CSV (with formula injection defense).

---

## 🔑 Default Seed Credentials

For immediate testing, the database initializer pre-configures all seed accounts with password **`Password123!`**:

| Role | Email | Password | Description |
| :--- | :--- | :--- | :--- |
| **Administrator** | `admin@healthcare.com` | `Password123!` | System administrator with full access |
| **Doctor** | `doctor.sarah@healthcare.com` | `Password123!` | Dr. Sarah Jenkins (Cardiology) |
| **Doctor** | `doctor.james@healthcare.com` | `Password123!` | Dr. James Wilson (Neurology) |
| **Doctor** | `doctor.emily@healthcare.com` | `Password123!` | Dr. Emily Chen (Pediatrics) |
| **Doctor** | `doctor.marcus@healthcare.com` | `Password123!` | Dr. Marcus Vance (Dermatology) |
| **Patient** | `patient.john@healthcare.com` | `Password123!` | Johnathan Miller |
| **Patient** | `patient.emma@healthcare.com` | `Password123!` | Emma Watson |

*(The login screen also features 1-click **Demo Role Pills** to pre-fill credentials instantly!)*

---

## 💻 Windows Local Execution Instructions

### Prerequisites
1. **JDK 17 or higher**: [Download Eclipse Temurin 17](https://adoptium.net/temurin/releases/?version=17) or Oracle JDK 17+.
   Verify in Command Prompt or PowerShell:
   ```powershell
   java -version
   ```
2. **Apache Maven**: [Download Maven](https://maven.apache.org/download.cgi) and ensure `mvn` is in your `PATH`.
3. **MySQL Server 8.0+**: (Optional for production; or run with the zero-setup in-memory H2 profile).

---

### Option A: Running with MySQL (Recommended Production Mode)

1. **Start MySQL Service** (via Windows Services or MySQL Workbench / XAMPP).
2. **Create the Database and Run Schema Scripts**:
   Open MySQL Command Line Client or Workbench:
   ```sql
   CREATE DATABASE doctor_scheduling_system CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   ```
   Execute the schema and sample data:
   ```powershell
   mysql -u root -p doctor_scheduling_system < "database/schema.sql"
   mysql -u root -p doctor_scheduling_system < "database/sample_data.sql"
   ```
3. **Configure Database Credentials**:
   Edit `backend/src/main/resources/application.properties` (or set environment variables `DB_USERNAME` and `DB_PASSWORD`):
   ```properties
   spring.datasource.url=jdbc:mysql://localhost:3306/doctor_scheduling_system?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
   spring.datasource.username=root
   spring.datasource.password=YOUR_MYSQL_PASSWORD
   ```
4. **Build and Run Backend**:
   Navigate to the `backend` folder:
   ```powershell
   cd "backend"
   mvn clean package -DskipTests
   mvn spring-boot:run
   ```

---

### Option B: Zero-Setup Offline Mode (H2 In-Memory)

If MySQL is not installed on your machine yet, you can run the entire system instantly using the built-in `h2` profile:

```powershell
cd "backend"
mvn spring-boot:run -Dspring-boot.run.profiles=h2
```
This automatically boots an in-memory MySQL-compatible database, runs the schema and seed accounts, and starts the system on port 8080!

---

### 🌐 Accessing the Application

Once the backend is running, open your web browser:

- **Landing Page**: [http://localhost:8080/index.html](http://localhost:8080/index.html)
- **Sign In Portal**: [http://localhost:8080/login.html](http://localhost:8080/login.html)
- **Doctor Directory**: [http://localhost:8080/doctors.html](http://localhost:8080/doctors.html)
- **Booking Wizard**: [http://localhost:8080/booking.html](http://localhost:8080/booking.html)
- **Patient Dashboard**: [http://localhost:8080/patient-dashboard.html](http://localhost:8080/patient-dashboard.html)
- **Doctor Dashboard**: [http://localhost:8080/doctor-dashboard.html](http://localhost:8080/doctor-dashboard.html)
- **Admin Dashboard**: [http://localhost:8080/admin-dashboard.html](http://localhost:8080/admin-dashboard.html)
- **QR Receipt Verification**: [http://localhost:8080/verify-receipt.html](http://localhost:8080/verify-receipt.html)

---

## 🧪 Automated Testing

Unit and integration tests are located in `backend/src/test/java/com/healthcare/app/`:

- `AppointmentServiceTest`: Tests slot conflict detection, double-booking prevention, past date rejection, and state lifecycle validation.
- `PdfReceiptServiceTest`: Verifies PDF byte generation and `%PDF-` document magic bytes.
- `AuthServiceTest`: Verifies BCrypt password authentication and session token generation.

Execute tests using Maven:
```powershell
cd "backend"
mvn test
```

---

## 📁 Project Directory Structure

```
c:\Users\kefah\Desktop\java program\
├── backend\
│   ├── pom.xml
│   ├── src\
│   │   ├── main\
│   │   │   ├── java\com\healthcare\app\
│   │   │   │   ├── DoctorSchedulingApplication.java
│   │   │   │   ├── config\
│   │   │   │   │   ├── AppConfig.java
│   │   │   │   │   └── DatabaseInitializer.java
│   │   │   │   ├── controller\
│   │   │   │   │   ├── AuthController.java
│   │   │   │   │   ├── DoctorController.java
│   │   │   │   │   ├── SpecializationController.java
│   │   │   │   │   ├── AppointmentController.java
│   │   │   │   │   ├── ScheduleController.java
│   │   │   │   │   ├── DoctorDashboardController.java
│   │   │   │   │   ├── PatientController.java
│   │   │   │   │   ├── AdminController.java
│   │   │   │   │   ├── NotificationController.java
│   │   │   │   │   └── PdfController.java
│   │   │   │   ├── dto\
│   │   │   │   ├── entity\
│   │   │   │   ├── exception\
│   │   │   │   ├── repository\
│   │   │   │   ├── security\
│   │   │   │   ├── service\
│   │   │   │   └── pdf\
│   │   │   │       └── PdfReceiptService.java
│   │   │   └── resources\
│   │   │       ├── application.properties
│   │   │       ├── application-h2.properties
│   │   │       ├── schema.sql
│   │   │       ├── data.sql
│   │   │       └── static\ (Mirrored frontend files served on port 8080)
│   │   └── test\
│   │       └── java\com\healthcare\app\
├── frontend\
│   ├── index.html
│   ├── login.html
│   ├── register.html
│   ├── doctors.html
│   ├── booking.html
│   ├── patient-dashboard.html
│   ├── doctor-dashboard.html
│   ├── admin-dashboard.html
│   ├── verify-receipt.html
│   ├── css\
│   │   ├── theme.css
│   │   ├── components.css
│   │   └── style.css
│   └── js\
│       ├── api.js
│       ├── auth.js
│       ├── booking.js
│       ├── patient.js
│       ├── doctor.js
│       └── admin.js
├── database\
│   ├── schema.sql
│   └── sample_data.sql
├── .env.example
├── .gitignore
└── README.md
```

---

## 🛠️ Troubleshooting Guide

1. **Port 8080 Already in Use**:
   Change `server.port=8080` in `backend/src/main/resources/application.properties` to `server.port=8081` (and update `API_BASE` in `frontend/js/api.js` if running independently).
2. **MySQL Access Denied for User 'root'**:
   Verify your MySQL root password and update `spring.datasource.password` in `application.properties`.
3. **PowerShell Script Execution Policy Error on Windows**:
   Run PowerShell with:
   ```powershell
   Set-ExecutionPolicy -Scope CurrentUser -ExecutionPolicy RemoteSigned
   ```
4. **PDF Generation Missing Font on Minimal Windows Server**:
   OpenPDF bundles standard Helvetica, Times, and Courier fonts natively; no external font installation is required.

---
Developed with clean code architecture, defensive validation, and healthcare software standards.
