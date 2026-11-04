package com.livesustainably.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.key
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.rememberViewModelStoreOwner
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.livesustainably.domain.model.UserSession
import com.livesustainably.ui.SessionUiState
import com.livesustainably.ui.screens.profile.ProfileViewModel
import com.livesustainably.ui.screens.auth.AuthScreen
import com.livesustainably.ui.components.LoadingContent
import com.livesustainably.ui.screens.chat.ChatScreen
import com.livesustainably.ui.screens.chat.ChatViewModel
import com.livesustainably.ui.screens.profile.ProfileScreen
import com.livesustainably.ui.screens.feed.FeedScreen
import com.livesustainably.ui.screens.feed.FeedViewModel
import com.livesustainably.ui.screens.comments.CommentsScreen
import com.livesustainably.ui.screens.comments.CommentsViewModel
import com.livesustainably.ui.screens.comments.CommentThread
import com.livesustainably.domain.model.ReportSelection
import com.livesustainably.ui.screens.reports.ReportsScreen
import com.livesustainably.ui.screens.reports.ReportsViewModel
import com.livesustainably.ui.screens.leaderboard.LeaderboardScreen
import com.livesustainably.ui.screens.leaderboard.LeaderboardViewModel
import com.livesustainably.ui.screens.friendsmap.FriendsMapScreen
import com.livesustainably.ui.screens.friendsmap.FriendsMapViewModel
import java.time.LocalDate

@Composable
fun RootNav(
    state: SessionUiState,
    modifier: Modifier = Modifier
) {
    Surface(modifier.fillMaxSize()) {
        when (state.session) {
            UserSession.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                LoadingContent("Restoring your session…")
            }
            else -> {
                key(state.navigationKey) {
                    val sessionOwner = rememberViewModelStoreOwner()
                    CompositionLocalProvider(LocalViewModelStoreOwner provides sessionOwner) {
                        Box(Modifier.fillMaxSize().safeDrawingPadding()) {
                            if (state.session is UserSession.SignedIn) {
                                AppNavDisplay()
                            } else {
                                AuthNavDisplay()
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AuthNavDisplay() {
    val stack = rememberNavBackStack(AppRoute.Login)

    NavDisplay(
        backStack = stack,
        onBack = { if (stack.size > 1) stack.removeAt(stack.lastIndex) },
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        entryProvider = entryProvider {
            entry<AppRoute.Login> { AuthScreen(isSignup = false, onSwitchForm = { stack.add(AppRoute.Signup) }) }
            entry<AppRoute.Signup> { AuthScreen(isSignup = true, onSwitchForm = { stack.removeAt(stack.lastIndex) }) }
        },
    )
}

@Composable
private fun AppNavDisplay() {

    val viewModel: ProfileViewModel = hiltViewModel()
    val accountState by viewModel.state.collectAsStateWithLifecycle()

    val navigation = rememberAppNavigationState()
    val currentKey = navigation.currentStack.lastOrNull()

    val provider = entryProvider<NavKey> {
        entry<AppRoute.TopLevel.Feed> {
            val feedViewModel: FeedViewModel = hiltViewModel()
            val feedState by feedViewModel.state.collectAsStateWithLifecycle()
            FeedScreen(feedState, feedViewModel::onAction, onChat = { navigation.navigate(AppRoute.Chat) }) { post ->
                navigation.navigate(AppRoute.Comments(post.authorId, post.postId))
            }
        }
        entry<AppRoute.TopLevel.Reports> {
            val reportsViewModel: ReportsViewModel = hiltViewModel()
            val reportsState by reportsViewModel.state.collectAsStateWithLifecycle()
            ReportsScreen(reportsState, reportsViewModel::onAction) { selection ->
                navigation.navigate(AppRoute.Leaderboard(selection.period, selection.anchorDate.toEpochDay()))
            }
        }
        entry<AppRoute.TopLevel.FriendsMap> {
            val mapViewModel: FriendsMapViewModel = hiltViewModel()
            val mapState by mapViewModel.state.collectAsStateWithLifecycle()
            FriendsMapScreen(mapState, mapViewModel::selectFriend, mapViewModel::retry)
        }
        entry<AppRoute.Chat> {
            val chatViewModel: ChatViewModel = hiltViewModel()
            val chatState by chatViewModel.state.collectAsStateWithLifecycle()
            ChatScreen(chatState, chatViewModel::onAction)
        }
        entry<AppRoute.Leaderboard> { route ->
            val leaderboardViewModel = hiltViewModel<LeaderboardViewModel, LeaderboardViewModel.Factory>(
                creationCallback = { it.create(ReportSelection(route.period, LocalDate.ofEpochDay(route.anchorEpochDay))) },
            )
            val leaderboardState by leaderboardViewModel.state.collectAsStateWithLifecycle()
            LeaderboardScreen(leaderboardState, leaderboardViewModel::retry)
        }
        entry<AppRoute.TopLevel.Profile> { ProfileScreen(accountState, viewModel::signOut, viewModel::retryProfile) }
        entry<AppRoute.Comments> { route ->
            val commentsViewModel = hiltViewModel<CommentsViewModel, CommentsViewModel.Factory>(
                creationCallback = { it.create(CommentThread(route.authorId, route.postId)) },
            )
            val commentsState by commentsViewModel.state.collectAsStateWithLifecycle()
            CommentsScreen(commentsState, commentsViewModel::updateDraft, commentsViewModel::send, commentsViewModel::retry)
        }
    }

    // Keep every tab's decorators in composition so hidden tabs retain entry state and ViewModels
    val entries = TopLevelDestinations.entries.associateWith { tab ->
        key(tab.name) {
            rememberDecoratedNavEntries(
                backStack = navigation.backStacks.getValue(tab),
                entryDecorators = listOf(
                    rememberSaveableStateHolderNavEntryDecorator(),
                    rememberViewModelStoreNavEntryDecorator(),
                ),
                entryProvider = provider,
            )
        }
    }
    BackHandler(enabled = navigation.currentStack.size == 1 && navigation.selectedTab != TopLevelDestinations.Feed) {
        navigation.goBack()
    }
    Scaffold(
        topBar = {
            if (navigation.canGoBack) {
                TextButton(onClick = navigation::goBack) { Text("Back") }
            }
        },
        bottomBar = {
            if (currentKey is AppRoute.TopLevel) {
                NavigationBar {
                    TopLevelDestinations.entries.forEach { tab ->
                        NavigationBarItem(
                            selected = currentKey == tab.route,
                            onClick = { navigation.selectTab(tab) },
                            icon = { Icon(painterResource(tab.icon), contentDescription = null) },
                            label = { Text(tab.label) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavDisplay(
            entries = entries.getValue(navigation.selectedTab),
            onBack = navigation::goBack,
            modifier = Modifier.padding(padding),
        )
    }
}
