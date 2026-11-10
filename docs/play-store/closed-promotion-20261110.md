# FocusLock — Google Play 비공개 테스트 승격 (2026-11-10)

## 승인 및 대상
- 사용자 요청: FocusLock 비공개 테스트 승격.
- GitHub: hebi10/FocusLock
- Play 패키지: com.haebi.focuslock
- 기존 API 배포: 2026-11-10 내부 테스트 0.3.0 / versionCode 2, Actions #42789707695 성공.
- 비공개 대상 트랙: 기본 closed testing 트랙 alpha.
- 신규 AAB 업로드·versionCode 할당 없이 기존 내부 테스트 AAB versionCode 2를 alpha completed에 연결한다.

## 안전 조건
- 정확히 검증된 현재 main SHA의 Android CI, API36 에뮬레이터 QA, release guard 모두 성공해야 한다.
- 명시적 main 병합 커밋 제목의 독립 마커 [promote-play-closed] 로만 자동 실행한다. 다른 main 커밋은 기존 internal deploy 정책을 유지한다. 충돌하는 play marker는 차단한다.
- 기존 WIF Provider, 서비스 계정 및 google-play 환경의 main 브랜치 제한을 유지한다. JSON 인증키, 서명키, 새로운 앱 패키지·빌드·배포 트랙 권한은 필요하지 않다.
- Google Play API를 조회하여 bundle versionCode 2 등록, internal completed versionCode 2, alpha 트랙 존재를 확인한다.
- alpha가 비었거나 같은 versionCode 2 draft, 또는 하나의 이전 versionCode 1 draft/completed 출시만 있을 경우 현재 versionCode 2 completed로 교체한다. 이전 versionCode 1 테스트는 새 버전 2로 업데이트하며 기존 테스터 그룹은 변경하지 않는다. 여러 릴리스, versionCode 3 이상, 중단된 halted/inProgress 상태는 변경 없이 차단한다.
- alpha가 이미 같은 completed versionCode 2이면 아무것도 변경하지 않고 확인 결과만 기록한다.
- production / beta / internal 또는 사용자 정의 다른 트랙에 대한 PUT/POST 절대 금지.
- 앱 상태 변경 전후를 다른 Play edit에서 재조회한다. 커밋 결과가 모호하면 Console 확인 전 재시도하지 않는다.
- 서명된 AAB/APK 신규 업로드, 키 교체, 내부 테스트 버전 추가 생성 없음.

## 검증 절차
1. PR CI 테스트와 워크플로 문법 검증.
2. PR을 검증 성공 시 squash로 main 병합하면서 승인 마커 [promote-play-closed]를 단일 커밋 제목에 적용.
3. Android CI + API36 에뮬레이터 QA 동일 SHA 전부 성공.
4. Play internal release 워크플로의 mode=closed 선택; 일반 release/checked는 실행하지 않음.
5. WIF 인증 → Read-only preflight → 현재 main 및 QA 재검증 → alpha track PUT → edit commit → 새 edit readback.
6. JSON 보고서 보존, Play Console 별도 확인.

## 필수 Play Console 후속 확인
- 테스트 및 출시 > 테스트 > 비공개 테스트에서 alpha 출시 상태와 심사/게시/검토 대기를 확인.
- 비공개 테스트 참여자 명단 또는 Google 그룹 지정, 피드백 이메일 지정.
- IARC 콘텐츠 등급, 데이터 보안, 앱 액세스, 광고, 정책 및 미국 수출 관련 필수 항목 충족.
- 검토를 위해 변경사항 전송 필요 여부 및 심사 완료 후 테스터 링크 노출 여부 확인.
- 해당 계정이 개인 개발자 대상 비공개 테스트 요건 적용 시 12명 이상의 연속 14일 참여 여부는 별도 집계.
- Google Play API에서 completed로 커밋해도 Console의 심사/게시/테스터 설치를 보장하지 않는다.

## 공식 문서
- https://developers.google.com/android-publisher/tracks
- https://developers.google.com/android-publisher/api-ref/rest/v3/edits.tracks/list
- https://developers.google.com/android-publisher/api-ref/rest/v3/edits.tracks/update
- https://support.google.com/googleplay/android-developer/answer/9845334

## 산출물
- scripts/play/closed_promotion.py
- scripts/play/tests/test_closed_promotion.py
- scripts/play/source.py
- .github/workflows/play-internal.yml

실제 승격 실행 결과 및 API readback / Play Console 심사 여부는 후속 기록으로 추가한다.
