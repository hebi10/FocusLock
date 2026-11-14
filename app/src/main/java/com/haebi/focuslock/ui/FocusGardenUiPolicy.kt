package com.haebi.focuslock.ui

internal object FocusGardenUiPolicy {
    fun timerLabel(seconds: Int): String {
        val safe = seconds.coerceAtLeast(0)
        return "${(safe / 60).toString().padStart(2, '0')}:${(safe % 60).toString().padStart(2, '0')}"
    }

    fun elapsedLabel(seconds: Int): String {
        val safe = seconds.coerceAtLeast(0)
        return "${safe / 60}분 ${(safe % 60).toString().padStart(2, '0')}초"
    }

    fun restorationNotice(restarted: Boolean, paused: Boolean): String = when {
        restarted -> "기기 재시작으로 일시정지되었습니다. 계속 집중을 누르면 이어집니다."
        paused -> "집중 시간을 일시정지했습니다."
        else -> "진행 상태는 기기 안에 저장되며 앱을 다시 열면 복구합니다."
    }

    fun resultNotice(completed: Boolean): String =
        if (completed) "설정한 집중 시간을 달성했습니다."
        else "보상 조건을 충족하지 않고 종료한 집중 기록입니다."

    fun rewardLine(experience: Int, points: Int): String =
        if (experience > 0 || points > 0)
            "경험치 +${experience.coerceAtLeast(0)} · 성장 포인트 +${points.coerceAtLeast(0)}"
        else "완료 조건 미충족 · 지급된 보상 없음"
}
