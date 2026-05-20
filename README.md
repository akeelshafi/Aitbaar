<div align="center">

<img src="https://img.shields.io/badge/Platform-Android-3DDC84?style=for-the-badge&logo=android&logoColor=white" />
<img src="https://img.shields.io/badge/Language-Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white" />
<img src="https://img.shields.io/badge/Architecture-MVVM-FF6B35?style=for-the-badge" />
<img src="https://img.shields.io/badge/Database-Firebase%20%7C%20Room-FFCA28?style=for-the-badge&logo=firebase&logoColor=black" />

# 🤝 Aitbaar — Digital Ledger of Trust

### *Replacing handwritten khatas with a real-time, dispute-free digital ledger*

</div>

---

## 📖 What is Aitbaar?

**Aitbaar** (Urdu: اعتبار — *Trust*) is an Android application that solves a problem millions of small vendors and customers face every day — unreliable, dispute-prone, manual transaction recording.

In traditional setups, a vendor writes down a transaction in a notebook (khata). The customer has no record. Disputes happen. Trust breaks.

**Aitbaar fixes this.**

Every transaction added by a vendor is sent to the customer's app in real time. The customer can **Accept or Reject** it. Both sides have the same record. No disputes. Full transparency. Complete trust.

---

## 🚀 The Problem We Solve

| ❌ Traditional Khata | ✅ Aitbaar |
|---|---|
| Vendor writes, customer has no copy | Both parties have the same record |
| Disputes with no proof | Log-and-confirm mechanism eliminates disputes |
| No real-time updates | Firebase real-time sync across both apps |
| Lost or damaged notebooks | Cloud-backed, always accessible |
| No offline access | Room Database enables full offline functionality |

---

## ✨ Key Features

### 🔐 Log & Confirm Mechanism
The core innovation of Aitbaar. When a vendor logs a transaction:
1. It appears **instantly** on the customer's app
2. The customer sees an **Accept ✅ or Reject ❌** button
3. Only confirmed transactions are locked into the ledger
4. Both parties always share the **same source of truth**

### ⚡ Real-Time Sync
- Powered by **Firebase Firestore**
- Transactions reflect instantly on both the vendor and customer app
- No refresh needed — changes appear live

### 📴 Offline-First Architecture
- All data persists locally via **Room Database**
- App works fully without internet
- Automatically syncs to Firestore when connectivity is restored

### 👥 Dual-Side Experience
- **Vendor App** — Add transactions, view customer ledger history, track balances
- **Customer App** — Receive transaction requests, accept or reject, view full history

---

## 🛠️ Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin |
| Architecture | MVVM + Repository Pattern |
| Cloud Database | Firebase Firestore |
| Local Database | Room Database |
| Async Operations | Kotlin Coroutines + Flow |
| Real-Time Updates | Firestore Snapshot Listeners |
| UI | Material Design 3 |
| IDE | Android Studio |

---

## 🏗️ Architecture

```
├── ui/
│   ├── vendor/          # Vendor-side screens
│   └── customer/        # Customer-side screens
├── viewmodel/           # ViewModels (MVVM)
├── repository/          # Single source of truth
├── data/
│   ├── local/           # Room Database (offline)
│   └── remote/          # Firebase Firestore (cloud)
└── model/               # Data classes
```

**Data Flow:**
```
UI → ViewModel → Repository → Room (local) ↔ Firestore (cloud)
```

The Repository pattern ensures the UI never talks directly to the database. All data flows through a single, testable layer.

---

## 📱 App Flow

```
Vendor adds transaction
        ↓
Firestore receives entry (real-time)
        ↓
Customer app gets notification
        ↓
Customer taps Accept ✅ or Reject ❌
        ↓
Both ledgers update instantly
        ↓
Room DB syncs for offline access
```

---

## 💡 Why Aitbaar Stands Out

- **Solves a real problem** — Built for the millions of small businesses that still use paper khatas
- **Trust by design** — The accept/reject mechanism isn't just a feature, it's the entire philosophy of the app
- **Production-ready architecture** — MVVM, Repository pattern, offline-first; built to scale
- **Dual-app real-time system** — Two separate user roles with live data sync between them

---

## 👨‍💻 Developer

**Akeel Shafi** — Android Application Developer

[![LinkedIn](https://img.shields.io/badge/LinkedIn-Connect-0A66C2?style=flat&logo=linkedin)](https://linkedin.com)
[![GitHub](https://img.shields.io/badge/GitHub-Follow-181717?style=flat&logo=github)](https://github.com)
[![Email](https://img.shields.io/badge/Email-akeelsha20%40gmail.com-EA4335?style=flat&logo=gmail)](mailto:akeelsha20@gmail.com)

---

<div align="center">

*Built with ❤️ to bring trust back to everyday transactions*

</div>
