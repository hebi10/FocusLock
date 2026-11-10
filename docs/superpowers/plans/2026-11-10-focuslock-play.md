# FocusLock Play Internal Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add the approved verified-main, app-signed internal release workflow.

**Architecture:** Adapt the tested TripCanvas release scripts at `3ede780803e0d40479b0f3b012e2f8652cb24bf6`. Require exact-current-main Android and emulator evidence; keep release mutations internal-only and signer checks fail-closed.

**Tech Stack:** Existing Kotlin/Compose/Gradle, GitHub Actions, Python standard library, Play Android Publisher API, WIF.

**Spec:** `docs/play-store/automation-spec.md`

## Global Constraints

- Package `com.haebi.focuslock`; preserve current main versionName `0.3.0`.
- Preserve UI, app permissions, feature code and instrumentation scenarios.
- Existing signer only; no keys, credentials or account changes in this work.
- Initial empty inventory code 1 internal draft; normal allocation exceeds all Play codes.
- No closed or production publish surface; no success claim for Console readiness.

## Review Focus

- Android succeeds before emulator: first completion must not release; final successful completion may release.
- A new failed or incomplete CI attempt on the same SHA must block an earlier successful run.
- Cross-app package/signer/source and stale allocation must fail before release commit.
- A draft-only or unassigned artifact inventory must prevent duplicate initialization.
- Current usage permissions and persistent local sessions/rewards and approximate goal-alert limitations must agree with privacy/listing.

### Task 1: Release guards and workflow

**Files:** Create `scripts/release_signing.py`, `scripts/test_release_signing.py`, `scripts/play/*.py`, `scripts/play/tests/test_*.py`, `.github/workflows/play-internal.yml`, `.github/workflows/play-gates.yml`; modify `app/build.gradle.kts` and emulator workflow trigger.

**Interfaces:** Source outputs immutable `sha` and `release_mode`; verified readiness emits runtime `initialized=true`; allocator emits version code; Gradle consumes signer environment and allocated code; publisher returns safe JSON reports.

- [ ] Add failing configuration/source tests for FocusLock identity, both exact-source jobs, stale and untrusted source, no closed CLI, and WIF-only authentication.
- [ ] Run tests and confirm missing or mismatched release configuration fails.
- [ ] Port tested signing, inventory, hash/readback, artifact and native checks; adapt exact-source gating, WIF and Gradle signing without feature edits.
- [ ] Run both full Python discovery commands and verify every release guard passes.
- [ ] Commit release automation with no private material.

### Task 2: Truthful registration material and PR

**Files:** Create `docs/privacy-policy.html`, `docs/play-store/listing-ko.json`, `docs/play-store/registration.md`, `.github/workflows/privacy-pages.yml`; append README release documentation.

**Interfaces:** Static privacy page can be published through GitHub Pages; registration guide lists exact environment/secret names and pending setup; listing stays local until Console registration.

- [ ] Compare metadata against current app source and merged usage QA notes.
- [ ] Add Korean privacy/listing and pending setup instructions; no claim of automatic blocking, exact real-time alerts or cloud synchronization.
- [ ] Re-run Python suites, syntax/JSON checks and diff validation; create branch and PR against unchanged current main.
- [ ] Obtain whole-branch review; address important findings with a failing regression test and green full suite.
- [ ] Report PR SHA, test evidence, CI and remaining account/Console readiness accurately.
