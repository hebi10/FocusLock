# STORE-01 — FocusLock Google Play 한국어 설명 이미지

대상: com.haebi.focuslock, ko-KR, 휴대전화 스크린샷 **8장**과 대표 그래픽 **1장**.

## 디자인 및 출처

- 실제 Android API 36 에뮬레이터 UI 계측 캡처만 사용합니다. 설명 문구는 앱 이미지 외부에 표시하고, 내부 UI를 조작하거나 가짜 사용 기록을 만들지 않습니다.
- 휴대전화: RGB PNG 1350×2400(9:16) × 8장.
- 대표 그래픽: RGB PNG 1024×500 × 1장.
- 기존 앱 아이콘은 유지하며 스토어 이미지 작업에서 수정하지 않습니다.
- 대표 화면: 홈, 사용 분석, 앱별 상세, 사용 목표, 집중 진행, 집중 결과, 정원, 리포트.
- 자동 원본: 최신 성공한 Android full-screen emulator QA의 focuslock-android-fullscreen-captures artifact (API 36). ZIP에는 실제 PNG 23개가 있어야 합니다.
- 각 PNG의 SHA-256, 크기, 소스 파일, QA 실행/커밋은 assets/play-store/ko-KR/manifest.json에 기록됩니다.

## 등록 자동화

[STORE-01 workflow](../../.github/workflows/store-01-images.yml)는 다음 중 하나로 실행됩니다.

1. PR의 렌더링 스크립트 수정: 실제 캡처 다운로드 → 8+1 PNG 생성·검증 → artifact 저장, **Play 변경 없음**.
2. owner의 Issue #14 명령 /store-01-upload: 위 검증 완료 후 이미지를 GitHub assets/play-store/ko-KR에 [skip ci] 커밋하고 기존 Google Play WIF로 한국어 phoneScreenshots와 featureGraphic만 교체합니다.
3. workflow_dispatch mode=check는 검증만, mode=upload는 업로드까지 진행합니다.

Google Play API는 한 edit에서 이전 이미지 제거 → 8장 순서대로 등록 → 대표 그래픽 등록 → 이미지 SHA-256 검증 → edit validate/commit → **새 edit 재조회**를 수행합니다. 기존 ko-KR 등록정보가 없거나, 등록 검증·인증이 실패하면 commit하지 않습니다. Play에 이미 동일한 이미지가 있다면 변경하지 않습니다.

**하지 않는 작업:** AAB/APK 업로드, 버전 코드 변경, 내부/비공개/프로덕션 트랙 변경, 앱 아이콘 변경, 다른 언어 등록정보 변경, 정책 설문 변경. 이미지 자료 자동 커밋은 [skip ci]를 사용해 앱 자동 재배포가 실행되지 않도록 합니다.

## 재실행과 검증

- GitHub Actions artifact는 만료되므로 워크플로는 실행 당시 최신 성공한 main 계측 테스트 artifact를 조회합니다.
- 재실행 시 기존 스토어 이미지와 SHA-256을 비교합니다. 등록이 완료된 이미지는 중복 업로드하지 않습니다.
- 편집이 심사 중이거나 API가 거절하면 반복해서 강행하지 않으며 Play Console 확인이 필요합니다.
- 검증 명령: python3 scripts/store/render_images.py --check --output assets/play-store/ko-KR
- 업로드는 owner 전용 명령 또는 main의 명시적 upload 모드에서만 실행합니다.

관련 자료: [Google Play 이미지 규격](https://support.google.com/googleplay/android-developer/answer/9866151), [STORE-01 Issue #14](https://github.com/hebi10/FocusLock/issues/14).
