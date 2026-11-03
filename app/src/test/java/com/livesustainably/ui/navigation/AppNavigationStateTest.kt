package com.livesustainably.ui.navigation

import androidx.compose.runtime.mutableStateOf
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppNavigationStateTest {
    private fun navigation() = AppNavigationState(
        mutableStateOf(TopLevelDestinations.Feed.name),
        TopLevelDestinations.entries.associateWith { NavBackStack<NavKey>(it.route) },
    )

    @Test
    fun tabSwitchAndReselectionRetainHistoryWithoutAppendingRoots() {
        val navigation = navigation()
        val comments = AppRoute.Comments("author", "post")
        navigation.navigate(comments)
        navigation.selectTab(TopLevelDestinations.Reports)
        navigation.navigate(AppRoute.Leaderboard)
        navigation.selectTab(TopLevelDestinations.Feed)
        navigation.selectTab(TopLevelDestinations.Feed)
        assertEquals(listOf(AppRoute.TopLevel.Feed, comments), navigation.currentStack.toList())
        navigation.selectTab(TopLevelDestinations.Reports)
        assertEquals(listOf(AppRoute.TopLevel.Reports, AppRoute.Leaderboard), navigation.currentStack.toList())
    }

    @Test
    fun backPopsDetailsThenReturnsToFeedWithoutRemovingRoot() {
        val navigation = navigation()
        navigation.selectTab(TopLevelDestinations.Reports)
        assertTrue(navigation.currentStack.last() is AppRoute.TopLevel)
        navigation.navigate(AppRoute.Leaderboard)
        assertFalse(navigation.currentStack.last() is AppRoute.TopLevel)
        navigation.goBack()
        assertTrue(navigation.currentStack.last() is AppRoute.TopLevel)
        assertEquals(listOf(AppRoute.TopLevel.Reports), navigation.currentStack.toList())
        assertTrue(navigation.canGoBack)
        navigation.goBack()
        assertEquals(TopLevelDestinations.Feed, navigation.selectedTab)
        assertFalse(navigation.canGoBack)
        repeat(3) { navigation.goBack() }
        assertEquals(listOf(AppRoute.TopLevel.Feed), navigation.currentStack.toList())
    }

    @Test
    fun repeatedDestinationDoesNotCreateDuplicateEntryKeys() {
        val navigation = navigation()
        val comments = AppRoute.Comments("author", "post")
        navigation.navigate(comments)
        navigation.navigate(comments)
        navigation.navigate(AppRoute.Chat)
        navigation.navigate(comments)
        assertEquals(listOf(AppRoute.TopLevel.Feed, comments), navigation.currentStack.toList())
    }

    @Test
    fun commentKeyKeepsAuthorAndPostIdsThroughSerialization() {
        val first: AppRoute = AppRoute.Comments("author-1", "shared-post-id")
        val second: AppRoute = AppRoute.Comments("author-2", "shared-post-id")
        val routes: List<AppRoute> = listOf(
            AppRoute.Login, AppRoute.Signup, AppRoute.TopLevel.Feed, AppRoute.TopLevel.Reports,
            AppRoute.TopLevel.FriendsMap, AppRoute.Chat, AppRoute.Leaderboard, AppRoute.TopLevel.Profile, first, second,
        )
        assertEquals(routes, Json.decodeFromString<List<AppRoute>>(Json.encodeToString(routes)))
        assertTrue(first != second)
    }
}
