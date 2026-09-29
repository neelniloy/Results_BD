package com.braineer.nuresult

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

data class DashboardItem(
    @DrawableRes val icon: Int,
    @StringRes val title: Int,
    @StringRes val subtitle: Int,
    val type: DashboardItemType,
)

enum class DashboardItemType {
    PSC, SSC, NU, OPEN
}

val dashboardItemList = listOf(
    DashboardItem(R.drawable.ssc, R.string.exam_ssc, R.string.exam_ssc_sub, DashboardItemType.SSC),
    DashboardItem(R.drawable.nu, R.string.exam_nu, R.string.exam_nu_sub, DashboardItemType.NU),
    DashboardItem(R.drawable.open, R.string.exam_open, R.string.exam_open_sub, DashboardItemType.OPEN),
    DashboardItem(R.drawable.psc, R.string.exam_psc, R.string.exam_psc_sub, DashboardItemType.PSC)
)
