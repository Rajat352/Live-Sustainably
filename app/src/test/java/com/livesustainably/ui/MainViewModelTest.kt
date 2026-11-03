package com.livesustainably.ui

import androidx.lifecycle.SavedStateHandle
import com.livesustainably.domain.model.UserSession
import com.livesustainably.domain.repository.SessionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {
    @Test
    fun sessionResetsAfterLogoutAndSameAccountLoginAndRestoresRevision() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val updates = MutableSharedFlow<UserSession>()
            val repository = object : SessionRepository { override val session = updates }
            val savedState = SavedStateHandle()
            val viewModel = MainViewModel(repository, savedState)
            runCurrent()
            updates.emit(UserSession.SignedIn("one"))
            runCurrent()
            val firstKey = viewModel.session.value.navigationKey
            updates.emit(UserSession.SignedOut)
            runCurrent()
            updates.emit(UserSession.SignedIn("one"))
            runCurrent()
            assertNotEquals(firstKey, viewModel.session.value.navigationKey)
            assertEquals(2, viewModel.session.value.revision)

            val restoredState = SavedStateHandle(mapOf(
                "sessionIdentity" to savedState.get<String>("sessionIdentity"),
                "sessionRevision" to savedState.get<Int>("sessionRevision"),
            ))
            val recreated = MainViewModel(repository, restoredState)
            runCurrent()
            updates.emit(UserSession.SignedIn("one"))
            runCurrent()
            assertEquals(viewModel.session.value, recreated.session.value)
            updates.emit(UserSession.SignedIn("two"))
            runCurrent()
            assertNotEquals(firstKey, recreated.session.value.navigationKey)
        } finally {
            Dispatchers.resetMain()
        }
    }
}
