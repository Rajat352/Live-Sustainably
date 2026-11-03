package com.livesustainably.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack

// UI owns navigation, tab selection never appends another root
class AppNavigationState(
    private val selection: MutableState<String>,
    val backStacks: Map<TopLevelDestinations, NavBackStack<NavKey>>,
) {
    val selectedTab: TopLevelDestinations get() = TopLevelDestinations.valueOf(selection.value)
    val currentStack: NavBackStack<NavKey> get() = backStacks.getValue(selectedTab)
    val canGoBack: Boolean get() = currentStack.size > 1 || selectedTab != TopLevelDestinations.Feed

    fun selectTab(tab: TopLevelDestinations) { selection.value = tab.name }

    fun navigate(route: AppRoute) {
        TopLevelDestinations.entries.firstOrNull { it.route == route }?.let { selectTab(it); return }
        require(route is AppRoute.Comments || route == AppRoute.Chat || route == AppRoute.Leaderboard)
        // Keys identify entries, bring an existing destination forward without duplicate ViewModels
        val existingIndex = currentStack.indexOf(route)
        if (existingIndex >= 0) {
            while (currentStack.lastIndex > existingIndex) currentStack.removeAt(currentStack.lastIndex)
        } else currentStack.add(route)
    }

    fun goBack() {
        if (currentStack.size > 1) currentStack.removeAt(currentStack.lastIndex)
        else selectTab(TopLevelDestinations.Feed)
    }
}

@Composable
fun rememberAppNavigationState(): AppNavigationState {
    val selection = rememberSaveable { mutableStateOf(TopLevelDestinations.Feed.name) }
    val stacks = TopLevelDestinations.entries.associateWith { tab -> rememberNavBackStack(tab.route) }
    return remember { AppNavigationState(selection, stacks) }
}
