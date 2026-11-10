import com.haebi.focuslock.data.DemoData

fun main() {
    check(DemoData.todayTotalMinutes == 135) { "Today's app totals must equal 135 min" }
    check(DemoData.hourlyUsageMinutes.sum() == DemoData.todayTotalMinutes) { "Hourly chart must match today total" }
    check(DemoData.weeklyTotalMinutes == 860) { "Weekly chart must equal 14h20m" }
    check(DemoData.monthlyUsageMinutes.size == 30) { "Monthly chart must have 30 days" }
    check(DemoData.monthlyTotalMinutes == DemoData.monthlyUsageMinutes.sum())
    check(DemoData.apps.map { it.id }.distinct().size == DemoData.apps.size)
    check(DemoData.apps.groupBy { it.category }.values.sumOf { apps -> apps.sumOf { it.minutes } } == DemoData.todayTotalMinutes)
    check(DemoData.minutesLabel(135) == "2시간 15분")
    check(DemoData.minutesLabel(60) == "1시간")
    check(DemoData.minutesLabel(58) == "58분")
    check(DemoData.sharePercent(58, 135) == 43)
    check(DemoData.sharePercent(30, 0) == 0)
    for (period in listOf("오늘", "7일", "30일")) {
        val usage = DemoData.usageByPeriod(period)
        val expected = when (period) {
            "오늘" -> DemoData.todayTotalMinutes
            "7일" -> DemoData.weeklyTotalMinutes
            else -> DemoData.monthlyTotalMinutes
        }
        check(usage.sumOf { it.minutes } == expected) { "Period $period total inconsistent" }
    }
    check(DemoData.apps.sumOf { DemoData.sharePercent(it.minutes, DemoData.todayTotalMinutes) } == 100)
    println("PASS: 15 fixture and formatting invariants")
}
