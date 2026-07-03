# GodsMove Frontend Agent Guide

GodsMove frontend is a native iOS app (`ChamChamCham`) written in SwiftUI.

Common project rules still come from the root [AGENTS.md](../AGENTS.md).

## Stack

- SwiftUI, iOS 17+ minimum deployment target.
- Observation framework (`@Observable`) for view models — not Combine or `ObservableObject`.
- SwiftData for local persistence — not CoreData or Realm.
- Native `URLSession` + async/await for networking — no Alamofire or other third-party networking library.
- Swift Package Manager only. No local SPM packages at MVP stage.
- Swift Testing (`@Test`) for new unit tests.

## Core Constraint: Offline-First

Most features must default to local-first storage with background sync, not a thin API client that assumes network availability — target users (farmers) often have poor connectivity. Every write goes to SwiftData first and is never blocked on network. See the full design rationale in
[Frontend Architecture Design](../docs/superpowers/specs/2026-07-02-frontend-architecture-design.md).

## Module Structure

Feature-first folders inside a single app target (`ChamChamCham/App`, `ChamChamCham/Core`, `ChamChamCham/Features/<Feature>/{Data,Domain,Presentation}`). Full layout and rationale in the architecture design doc linked above. Do not introduce local SPM packages without revisiting that decision first.

## Product Source of Truth

Do not duplicate product behavior into this file. Read from:

- [Business Rule.md](docs/Business%20Rule.md) — when/under-what-conditions behavior (`BR-*` rule IDs).
- [ERD 초안.md](docs/ERD%20초안.md) — data shape and requirements.
- [와이어프레임 초안](docs/와이어프레임%20초안/) — screen flow references.
- [docs/superpowers/specs/](../docs/superpowers/specs/) and [docs/superpowers/plans/](../docs/superpowers/plans/) — architecture and feature design docs, including the [Onboarding Flow Plan](../docs/superpowers/specs/2026-07-02-frontend-onboarding-flow-plan.md).

Reference `BR-*` rule IDs in commits/PRs when a change implements or affects a specific business rule, matching backend convention.

## Run and Build

Run from the `frontend/ChamChamCham` directory.

```bash
xcodebuild -scheme ChamChamCham -destination 'platform=iOS Simulator,name=iPhone 17' build
```

```bash
xcodebuild -scheme ChamChamCham -destination 'platform=iOS Simulator,name=iPhone 17' test
```

The exact simulator name depends on what Xcode has installed locally (`xcrun simctl list devices available`) and drifts across Xcode versions — swap it if the build fails with "Unable to find a destination".

Use the IDE run button / Simulator for interactive development.

## Backend Integration

The backend is Spring Boot Kotlin with a `member`-centric domain (UUID ids). See [backend/AGENTS.md](../backend/AGENTS.md). A draft API spec exists at `docs/API명세서(260702)/` (endpoint index + DTO name registry) covering auth (Kakao/Naver/Apple), crops, farms, farming records, voice sessions, community, and onboarding completion — but it is an unconfirmed work-in-progress export with no field-level DTO shapes yet. Do not scaffold networking code against exact field names from it; see the [Onboarding Flow Plan](../docs/superpowers/specs/2026-07-02-frontend-onboarding-flow-plan.md) for what's confirmed vs. still open.

## Development Loop

1. Plan non-trivial changes before writing code (architectural decisions, multi-file changes).
2. Implement, then build and run in the Simulator to verify the golden path.
3. Prefer sequential, plan-gated feature work over multi-agent parallel fan-out — this is a single app target with shared files under `Core/` and `App/` that parallel agents would collide on.
4. Add tests alongside behavior, prioritizing view-model tests and the voice-session state machine (BR-VOICE-*, BR-STATE-001) — these are pure and rule-dense.
