# Live Sustainably

An Android app for exploring sustainable habits, sharing encouragement and viewing your impact build with Kotlin and Jetpack Compose

## App Architecture

The app uses **unidirectional data flow with MVVM**: screens send actions to ViewModels and render their `StateFlow` state. Feed, Reports, Chat and Profile use **MVI-style action/state contracts** to handle their larger set of user actions.

Code is split into `ui`, `domain` and `data` layers. Repositories handle data access, while domain functions share calculations and contain UI data models. Hilt provides dependency injection and Navigation 3 manages typed routes and separate tab back stacks.

## APIs/Libs used

- **Kotlin, Jetpack Compose and Material 3** — app logic and UI.
- **ViewModel, StateFlow and Coroutines** — state and asynchronous work.
- **Navigation 3 and Kotlin Serialization** — navigation, saved state and JSON.
- **Hilt / KSP** — dependency injection.
- **Firebase Authentication and Realtime Database** — email/password login and app data.
- **Google Maps SDK / Maps Compose** — map and friend markers.
- **Vico** — report charts.
- **JUnit 4 and Coroutines Test** — unit tests.

## Testing summary

Two focused unit tests cover:

- **Comment retries:** a lost save acknowledgement, timeout and ViewModel recreation. Ensures retries reuse the original comment instead of creating duplicates.
- **Report totals:** timezone/daylight-saving boundaries, category filters, duplicate activities, empty days and large point totals. Protects against missing or double-counting activities.

## Test Credentials with pre-seeded data
- **Email: test1@test.com**
- **Pass: test11**

## Known limitations

- Demo friends have fixed locations and statuses. Nearby filtering uses a fixed map center, there is no foreground or background location tracking.
- Friendships are preconfigured
- History, posts and peer scores include seeded demo data, activity creation to be seeded directly to RTDB
- Peer scores have no automatic backend aggregation
- No offline-first architecture or durable local sync layer. Fresh Firebase data and confirmed saves need connectivity
- Chat uses local JSON responses, with limited vocabulary and no generative AI or permanent history
- Email/password sign-in no 3rd party SSO
- No pagination: the latest 30 feed posts and 100 comments per thread are shown. Designed for a small friend list.