package com.haebi.focuslock.ui

internal object SettingsDisclosurePolicy {
    const val subtitle = "스마트폰 사용 분석·선택형 목표·집중 설정"
    const val permissionSubtitle = "Android 사용 정보 접근과 알림 권한을 확인합니다."
    const val alertNotice =
        "앱별 목표의 80%·100% 경고는 선택한 요일에만 확인합니다. " +
        "WorkManager는 최소 약 15분 간격으로 동작하며 절전 정책 때문에 알림이 늦어질 수 있습니다."
    const val localDataNotice =
        "앱 사용 시간은 Android 시스템 통계에서 조회합니다. 목표 설정과 정원 배치, " +
        "집중 세션·보상 기록은 이 기기에 저장됩니다. 다른 앱의 메시지나 화면 내용은 읽지 않습니다."
    const val deleteNotice =
        "앱 내 전체 데이터 삭제 기능은 제공하지 않습니다. " +
        "Android 앱 정보에서 저장공간을 초기화하면 FocusLock의 목표·집중·보상·정원 기록과 설정이 삭제됩니다. " +
        "Android 시스템이 보관하는 다른 앱 사용 기록은 별도로 관리됩니다."
    const val limitsNotice =
        "FocusLock은 앱 사용 시간을 분석하고 선택한 목표에 알림을 제공하지만, " +
        "다른 앱을 자동 차단하거나 실시간 사용 감지를 보장하지 않습니다."
    const val privacyNotice =
        "FocusLock은 Android가 제공하는 사용 통계를 기기 내부에서 분석하며 " +
        "현재 구현된 기능에서 사용 기록을 외부 서버로 업로드하지 않습니다."
}
