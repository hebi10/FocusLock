# FocusLock RENEW release freeze — 2026-11-12

## 사용자 요청
RENEW-02~RENEW-09 기능과 UI를 모두 구현하고 검증한 뒤, Google Play 스토어 이미지와 최신 서명 AAB를 **단 한 번의 최종 릴리스 작업**으로 등록한다. 각 PR마다 internal/closed 배포하지 않는다.

## 적용 정책
- `.github/workflows/play-internal.yml`: `workflow_run` 자동 트리거 제거. Android CI/main push 성공으로 Play에 자동 업로드되지 않는다.
- 수동 `workflow_dispatch` 경로는 기존 명시적 최종 배포 승인 시 사용하도록 남긴다.
- `.github/workflows/store-01-images.yml`: push 이벤트로 이미지 업로드하지 않으며, 이미지 업로드는 기존 승인된 소유자 명령 또는 수동 `mode=upload`만 가능하다.
- 기존 `ui-r09-full-release.yml`도 자동 push 트리거는 없고 명시적 저장소 소유자 Issue #14 명령으로만 실행된다. 리뉴얼 기간 중에는 이 명령을 실행하지 않는다.
- RENEW PR은 모두 CI·실제 Android 캡처 성공 후 `[skip ci]`로 squash 안전 병합한다. Google Play 등록 정보·AAB/SDK 서명·서비스 계정·테스트 트랙에 쓰지 않는다.
- API29/35/36 계측 QA, 160% 글꼴, 태블릿/폴더블 크기 모사 및 개인정보·기존 데이터 회귀는 배포 없이 수행한다.

## 최종 1회 배포 조건
1. RENEW-02~09 모든 PR 검증/안전 병합.
2. 최종 main SHA에서 Android CI·API36 전체 화면 및 필요한 실기기 검증 성공.
3. 새 main SHA의 실화면 8장/대표 그래픽 생성 및 사전 검수.
4. 스토어 ko-KR 등록 후 main SHA가 바뀌면 CI·캡처 다시 성공.
5. 서명 AAB 내부 테스트 등록 및 Play API readback 성공.
6. 동일 AAB를 비공개 alpha로 승격, Play API readback과 실제 Console/테스터 설치 검증.

## 상태
RENEW 개발 진행 중. Play 최종 출시 승인 전. 이 문서 및 워크플로 변경으로 신규 Play 업로드를 실행하지 않는다.
