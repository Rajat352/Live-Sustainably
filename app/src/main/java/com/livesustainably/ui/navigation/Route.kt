package com.livesustainably.ui.navigation

import androidx.annotation.DrawableRes
import androidx.navigation3.runtime.NavKey
import com.livesustainably.R
import com.livesustainably.domain.model.ReportPeriod
import kotlinx.serialization.Serializable

@Serializable
sealed interface AppRoute : NavKey {
    @Serializable data object Login : AppRoute
    @Serializable data object Signup : AppRoute
    @Serializable
    sealed interface TopLevel : AppRoute {
        @Serializable data object Feed : TopLevel
        @Serializable data object Reports : TopLevel
        @Serializable data object FriendsMap : TopLevel
        @Serializable data object Profile : TopLevel
    }

    @Serializable data object Chat : AppRoute
    @Serializable data class Leaderboard(val period: ReportPeriod, val anchorEpochDay: Long) : AppRoute
    @Serializable data class Comments(val authorId: String, val postId: String) : AppRoute
}

enum class TopLevelDestinations(
    val route: AppRoute.TopLevel,
    val label: String,
    @param:DrawableRes val icon: Int
) {
    Feed(AppRoute.TopLevel.Feed, "Feed", R.drawable.ic_tab_feed),
    Reports(AppRoute.TopLevel.Reports, "Reports", R.drawable.ic_tab_reports),
    Map(AppRoute.TopLevel.FriendsMap, "Map", R.drawable.ic_tab_map),
    Profile(AppRoute.TopLevel.Profile, "Profile", R.drawable.ic_tab_profile),
}
