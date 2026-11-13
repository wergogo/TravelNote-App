# TravelNote

A collaborative trip journal for Android. Create a trip, invite friends with a
six-character code, and build a shared day-by-day itinerary of flights,
activities, expenses and notes. Everything syncs in real time, and spending is
tracked against the trip budget.

Built with **Kotlin**, **Jetpack Compose (Material 3)** and **Firebase**.

## Features

- **Accounts:** email/password sign-up and sign-in with a persistent session,
  change password, and an editable nickname and avatar.
- **Trips:** name, destination, dates, budget, currency and cover art. Owners
  can edit or delete a trip; members can leave.
- **Invite by code:** every trip gets a join code. Owners can regenerate it or
  lock the trip to stop new members joining.
- **Day-by-day itinerary:** flights (flight number, departure and arrival
  airports) and activities with time, location and cost, plus free-text notes.
- **Expenses:** categorised expenses with optional GPS location tagging and a
  "paid by" member. The trip shows a live budget bar, per-day totals, and how
  much each member has paid.
- **Real-time collaboration:** Firestore listeners keep every member's screens
  in sync, and each entry is tagged with the member who added or paid for it.

## Tech stack

| Area | Technology |
| --- | --- |
| UI | Jetpack Compose, Material 3, Navigation Compose |
| State | ViewModel, Compose state and `StateFlow`, Kotlin coroutines and `Flow` |
| Backend | Firebase Authentication, Cloud Firestore (snapshot listeners) |
| Device | Fused Location Provider (Google Play services) |
| Build | Gradle Kotlin DSL with a version catalog, AGP 9, Kotlin 2.2, min SDK 28, target SDK 36 |

## Architecture

MVVM with a thin repository layer and manual dependency injection:

```
Compose screen  →  ViewModel  →  Repository interface  →  Firebase implementation  →  Firestore / Auth
```

```
app/src/main/java/com/wergogo/travelnote/
├── data/
│   ├── AppContainer.kt         # manual DI, created once in TravelNoteApplication
│   ├── model/                  # domain models and form "draft" classes
│   ├── repository/             # Auth, Profile, Trip and Day repository interfaces
│   └── source/firebase/        # Firestore/Auth implementations exposing Flows via callbackFlow
├── ui/
│   ├── navigation/             # routes and NavHost
│   ├── components/             # shared composables (TripCard, BudgetBar, pickers, …)
│   ├── theme/                  # Material 3 colours and typography
│   └── <feature>/              # one package per screen: Screen + ViewModel
└── util/                       # bundled image lookup, location helper
```

Firestore data model:

```
users/{uid}
trips/{tripId}
├── members/{uid}               # nickname and avatar copied in for fast reads
└── days/{yyyy-MM-dd}
    ├── events/{eventId}
    ├── expenses/{expenseId}
    └── notes/{noteId}
joinCodes/{CODE}                # code → tripId lookup used to join a trip
```

## Security rules

Every Android app ships its Firebase API key, so the
[Firestore security rules](firebase/firestore.rules) are what protects the data:

- A trip and everything in it is visible only to its members.
- Profiles are private to their owner.
- Only the owner can change a trip's settings or delete it.
- A non-member can add only themselves, and only to an open trip they have the code for.
- Entries record who created them, and that can't be forged.

The rules are covered by emulator tests that replay each of the app's reads and writes, plus a
list of attacks that must be refused:

```bash
cd firebase
npm install
npm test      # starts the Firestore emulator (needs Java 21+) and runs the rules and migration tests
```

Projects with data from before 1.0.2 should run the one-off migration in
[`firebase/scripts/migrate-1.0.2.mjs`](firebase/scripts/migrate-1.0.2.mjs) before publishing the
rules. The script's header shows how to run it.

## Getting started

**Requirements:** a recent Android Studio (JDK 21), Android SDK 36, and a
Firebase project.

1. Clone the repository.
2. In the [Firebase console](https://console.firebase.google.com/), add an
   Android app with the package name `com.wergogo.travelnote`, then download
   its `google-services.json` into `app/`. The file is gitignored; the build
   fails without it.
3. Enable **Authentication → Email/Password** and create a **Cloud Firestore**
   database. Publish the rules in `firebase/firestore.rules` (Firestore →
   Rules).
4. Open the project in Android Studio and run it, or from the command line:

   ```bash
   ./gradlew installDebug
   ./gradlew testDebugUnitTest   # ViewModel unit tests
   ```

## Status

Version 1.0 was built as a university mobile-development course project. It is
now being developed further as a personal project.

Version 1.0.2 adds:
- membership-based Firestore rules with emulator tests;
- join-code lookups with atomic batched writes;
- ViewModel unit tests;
- R8 code and resource shrinking, which takes the release APK from ~60 MB to under 10 MB.
