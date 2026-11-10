# FocusLock Google Cloud WIF 복구 및 Play 아이콘 등록

검토일: 2026-11-10. **이 문서는 계정 변경 완료 증명이 아닙니다.** GitHub 코드·공개 공식 문서에 근거한 복구 가이드입니다.

## 1. 확인된 오류와 원인 가설

- 실패 실행: https://github.com/hebi10/FocusLock/actions/runs/42782819755
- Google Cloud 반환: `unauthorized_client` / `The given credential is rejected by the attribute condition.`
- Android CI·에뮬레이터 QA·Source/Gates는 성공. 인증 실패로 AAB 업로드·Play edit·아이콘 등록 미실행.
- 저장소 생성일: 2026-11-10, GitHub 저장소 ID `1452185615`, 소유자 ID `80847404`.
- 2026-07-15 이후 생성된 GitHub 저장소의 기본 OIDC `sub`는 이름과 **불변 ID**를 포함함. 기존 구성 문서의 `repo:hebi10/FocusLock:environment:google-play`는 구형 형식이므로, 실제 Provider에도 그대로 사용 중이면 새 토큰을 거절함.
- 새 기본 형식의 예상 주체:

```text
repo:hebi10@80847404/FocusLock@1452185615:environment:google-play
```

**확인 필요:** 실제 Google Cloud WIF Provider의 현행 속성 조건 및 GitHub 저장소 OIDC 커스터마이징. 현재 연결로 Cloud IAM 설정을 직접 읽거나 변경하지 못했으므로, 이 값은 검증 전 예상 값이다. 기존 값을 삭제하거나 제한을 무조건 완화하지 않는다.

근거: https://docs.github.com/en/actions/reference/security/oidc#immutable-subject-claims
및 https://docs.cloud.google.com/iam/docs/workload-identity-federation-with-deployment-pipelines

## 2. Google Cloud에서 확인·수정할 설정

1. Google Cloud Console → IAM 및 관리자 → Workload Identity Federation → 기존 풀 `focuslock-github` → Provider `github-main`의 **현재 Attribute condition / Attribute mapping / OIDC issuer**를 읽는다.
2. 로그·현재 조건과 GitHub OIDC의 실제 `sub`를 비교한다. 예전의 문자열 정확 일치 조건을 유지하고 있다면 새 형식으로 교체한다.
3. 아래 CEL은 **해당 저장소 ID와 소유자 ID, main ref, Play 전용 환경을 동시에 제한**하는 권장 조건이며, 실제 `sub` 확인 후 적용한다.

```cel
assertion.repository_id == '1452185615' &&
assertion.repository_owner_id == '80847404' &&
assertion.ref == 'refs/heads/main' &&
assertion.sub == 'repo:hebi10@80847404/FocusLock@1452185615:environment:google-play'
```

4. Attribute mapping은 최소 `google.subject=assertion.sub`를 포함해야 한다. repository_id/owner_id를 IAM principalSet에 사용할 경우 별도로 `attribute.repository_id=assertion.repository_id`, `attribute.repository_owner_id=assertion.repository_owner_id`를 구성한다. 기존 mapping을 불필요하게 지우지 않는다.
5. 서비스 계정의 **roles/iam.workloadIdentityUser** 바인딩이 옛 주체에만 연결돼 있으면 새 불변 주체로 바꾸거나, 정확한 repo ID로 매핑된 principalSet에 한정한다. 전체 풀 모든 신원을 허용하지 않는다.
6. Provider의 ID·Audience·GitHub Environment `google-play` 및 GitHub Actions `permissions: id-token: write`를 대조한다.
7. 권한 수정 후 GitHub Actions `Play internal release`를 `check` 모드로 실행하여 **실제 WIF 교환과 Play API 읽기 성공**을 확인한다. 코드 측 검증 통과만으로 인증 성공을 주장하지 않는다.

## 3. Google Play 스토어 아이콘

- 선택 확정 원본: `assets/store/FocusLock_Play_Icon_512.png` (512×512 PNG).
- 스토어 아이콘은 앱 런처 리소스와 별도다. AndroidManifest의 런처 아이콘 반영만으로 Play Console 스토어 아이콘은 바뀌지 않는다.
- Google Play Developer API에서는 `edits.images.upload`를 사용하며 `language=ko-KR`, `imageType=icon`이다.
- API를 쓰려면 Play의 **스토어 등록정보 수정 권한** 및 해당 로캘의 스토어 등록정보가 필요하다. 이미지 등록은 새 edit commit 후 다른 새 edit에서 목록 readback을 통해 확인한다.
- 현재 Drive에 스토어용 PNG 업로드는 완료됐지만, Play Console에는 아직 업로드·등록됐다고 확인할 증거가 없다.
- 복구 후 `Play internal release`의 수동 `icon` 모드를 사용할 수 있다. 이 모드는 해당 앱·ko-KR 아이콘만 교체하며 앱 바이너리 출시를 수행하지 않는다.

공식 API: https://developers.google.com/android-publisher/api-ref/rest/v3/edits.images/upload

## 4. 내부 테스트 순서

1. `check`: WIF 인증·Play API 조회 및 현재 등록된 AAB·APK 인벤토리 확인.
2. 등록된 바이너리가 0개이면 현재 Play 개발자 계정의 필수 등록·App Signing 상태를 확인한 뒤, **기존 FocusLock 서명 키로** `initialize` (내부 draft) 실행. 동일 코드 재업로드 금지.
3. 이미 업로드된 번들이 있으면 `deploy` (내부 테스트) 실행. 최신 main의 Android CI와 Emulator QA가 동일 SHA에서 모두 성공해야 하고, 버전 코드는 인벤토리보다 커야 한다.
4. 최초 업로드와 이후 업데이트는 별개. 검토 제출·테스터 사용 가능 여부·스토어 이미지 게시 상태는 Play Console에서 재확인한다.
5. 생산/비공개 테스트 승격, 서명키 회전, 자격 증명 출력, 퍼블릭 게시 등은 하지 않는다.

**외부 환경 제한:** 현재 연결에서는 Google Cloud IAM 수정과 Play Console 화면 조작이 불가능했다. 실제 변경 성공을 기록하려면 성공한 WIF `check` 실행과 Play 측 새 Edit readback 증거가 필요하다.
