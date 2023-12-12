package com.braineer.nuresult

data class DashboardItem(
    var icon: Int,
    var title: String,
    val type: DashboardItemType,
)

enum class DashboardItemType {
    PSC,SSC,NU,ABOUT,OPEN
}
val dashboardItemList = listOf(
    DashboardItem(icon = R.drawable.psc, title = "PSC Result", type = DashboardItemType.PSC),
    DashboardItem(icon = R.drawable.ssc, title = "JSC/SSC/HSC Result", type = DashboardItemType.SSC),
    DashboardItem(icon = R.drawable.nu, title = "National University Result", type = DashboardItemType.NU),
    DashboardItem(icon = R.drawable.open, title = "Open University Result", type = DashboardItemType.OPEN),
    DashboardItem(icon = R.drawable.about, title = "About", type = DashboardItemType.ABOUT)
)
