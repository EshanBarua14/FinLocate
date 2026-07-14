# Finance Tracker — Enterprise-Grade Personal Wealth Management

[![Platform](https://img.shields.io/badge/Platform-Android-green.svg?style=flat-square)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.0-purple.svg?style=flat-square)](https://kotlinlang.org)
[![Backend](https://img.shields.io/badge/Backend-Node.js-blue.svg?style=flat-square)](https://nodejs.org)
[![License](https://img.shields.io/badge/License-MIT-lightgrey.svg?style=flat-square)](LICENSE)

An offline-first, enterprise-grade personal finance companion and secure ledger system. **Finance Tracker** is designed for modern professionals who demand data privacy, real-time international compliance, multi-wallet coordination, and secure, zero-knowledge synchronization. 

By combining a robust Jetpack Compose Android native client with a companion Express.js sync gateway, Finance Tracker offers a high-performance system for managing transactions, budgets, automated subscription tracking, multi-region tax deductions, and visual financial analytics.

---

## 🏛 Architecture Overview

Finance Tracker is built with a decoupled, modular, client-server topology designed for offline-first performance and maximum privacy.

```
       +---------------------------------------------+
       |             Android Native Client           |
       |  (Kotlin + Compose + Room DB + Local State) |
       +----------------------++---------------------+
                              || (Secure Encrypted JSON payload)
                              \/
       +---------------------------------------------+
       |             Node.js Sync Gateway            |
       |    (Express API + SQLite Ledger Indexer)    |
       +---------------------------------------------+
```

### 1. Android Native Client (`/app`)
* **Framework:** Jetpack Compose (Modern Material Design 3 theme system).
* **Architecture:** Model-View-ViewModel (MVVM) utilizing modern Kotlin Coroutines and asynchronous state streams via `StateFlow`.
* **Local Storage:** SQLite relational database managed securely via **Room ORM**, serving as the single source of truth.
* **Cryptography:** Local-first AES-256 data envelope encryption for cloud payload serialization.

### 2. Synchronization Gateway (`/backend`)
* **Runtime:** Node.js + Express.js.
* **Database:** SQLite relational datastore (production-ready and scaleable to PostgreSQL).
* **Security**: Zero-knowledge backup structure. Server indexes raw client payloads using hashed client passcodes; database states are pre-encrypted client-side with AES-256, meaning server operators can never decrypt or read transaction lists.

---

## 💎 Core Capabilities & Features

### 1. Multi-Wallet Ledger & Accounts Management
* **Account Types:** Seamlessly track cash assets, credit card limits, bank accounts, and Mobile Financial Services (MFS) configurations.
* **Global Portability:** Complete localized currency support with offline-ready dynamic rate converters.

### 2. Intelligent Budget Engine
* **Dynamic Budget Tracking:** Track spend velocity in real-time, matching monthly categories dynamically against customizable visual charts.
* **Subscription & Recurrence Scheduler:** Configure and schedule monthly salaries, utility bills, or platform subscriptions. Due transactions are processed and posted automatically upon app boot.

### 3. Real-Time Tax Compliance & Receipt Proof-of-Work
* **Regional Tax Schemas:** Toggle compliance modes across multiple country configurations (e.g., USA, Germany, India, Bangladesh).
* **Automated Deductions:** Identify and isolate tax-deductible expenses to estimate tax reliefs, VAT deductions, and gross taxable earnings in active fiscal cycles.
* **Official Real-Time Brackets:** Automated retrieval of official national tax brackets and VAT thresholds.
* **Receipt Capture System:** Capture receipt proofs-of-work directly via local camera integrations, saving image attachments locally and linking them to individual ledger receipts for instant audit readiness.

### 4. Enterprise-Grade Security & Privacy
* **Local PIN Authentication:** App accessibility is guarded via a secure local hash passcode and optional biometric credentials.
* **Local Backup & CSV Portability:** Export your entire database into standard encrypted files (.csv.enc) or import raw CSV records seamlessly.
* **Zero-Knowledge Cloud Sync:** Instantly sync states across devices via secure end-to-end encrypted tunnels.

---

## 🛠️ Client (Android App) Quickstart

The Android application is located in the `/app` directory.

### Prerequisites
* **Android SDK:** v34+
* **Java Development Kit:** JDK 17
* **Gradle:** v8.4+

### Build & Run
1. Open the project root in **Android Studio**.
2. Sync the project with Gradle files.
3. Build and launch the application on an emulator or physical device:
```bash
# Compile and install on target device
./gradlew installDebug
```

### Dependency Configuration
All external dependencies are managed using the central **Gradle Version Catalog** (`gradle/libs.versions.toml`):
* **Jetpack Compose:** Declarative UI layout components.
* **Room Database:** Local database storage.
* **Coil:** Efficient local and network image loading and receipt viewing.
* **KSP (Kotlin Symbol Processing):** Fast compilation of database entities and schema managers.

---

## 🐳 Backend Sync Gateway Quickstart

The Node.js synchronization gateway is located in the `/backend` directory.

### Prerequisites
* **Node.js:** v18+
* **Package Manager:** `npm` or `yarn`

### Quick Launch
1. Navigate to the backend folder:
```bash
cd backend
```
2. Install dependencies:
```bash
npm install
```
3. Create a `.env` configuration file in the `/backend` directory:
```env
PORT=5000
NODE_ENV=production
```
4. Start the server:
```bash
npm start
```
5. Check service health at `http://localhost:5000/api/status`.

---

## 🔒 Cryptographic Specification (Zero-Knowledge Sync)

Privacy is the core pillar of Finance Tracker's synchronization protocol:

1. **Passcode Hashing:** The user's passcode is passed through a client-side PBKDF2 function to derive a master sync key and a unique backup storage identifier.
2. **Local Encryption:** Before transferring data to the server, the Room Database records are serialized to JSON and encrypted using the AES-256-CBC algorithm using the master sync key.
3. **Transmission:** The encrypted payload, along with the backup storage identifier, is securely transmitted over TLS to the backend API.
4. **Server Storage:** The server indexes the backup record purely by the backup storage identifier. It stores the raw encrypted string. Because the backend does not have the master sync key or passcode, it is mathematically impossible for the host to view balances, accounts, or ledger logs.

---

## 📜 License

Distributed under the MIT License. See `LICENSE` for more information.
