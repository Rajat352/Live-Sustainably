package com.livesustainably.ui.screens.friendsmap

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.TextButton
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.Surface
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.livesustainably.R
import com.livesustainably.domain.model.GeoPoint
import com.livesustainably.domain.model.distanceKmTo
import com.livesustainably.ui.common.dataErrorMessage
import com.livesustainably.ui.common.displayCommunityName
import com.livesustainably.ui.common.displayFriendStatus
import com.livesustainably.ui.common.formatSaving
import com.livesustainably.ui.components.*
import kotlin.math.cos
import kotlin.math.log2
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun FriendsMapScreen(
    state: FriendsMapUiState,
    onSelectFriend: (String?) -> Unit,
    onRetry: () -> Unit,
) {
    var areaRequests by rememberSaveable { mutableIntStateOf(0) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    LazyColumn(
        Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp),
        state = listState,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            ScreenHeading(stringResource(R.string.map_title), stringResource(R.string.map_demo_explanation), "A GREENER WORLD, TOGETHER")
        }

        when {
            state.isLoading -> item { LoadingContent(stringResource(R.string.map_loading)) }
            state.error != null -> item { ErrorContent(dataErrorMessage(state.error), onRetry) }
            state.configuration != null -> {
                item {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.map_area, formatSaving(state.configuration.radiusKm)),
                            Modifier.weight(1f), style = MaterialTheme.typography.labelLarge)
                        TextButton(onClick = { onSelectFriend(null); areaRequests++ }) { Text(stringResource(R.string.map_show_area)) }
                    }
                }

                item(key = "demoMap") { DemoFriendsMap(state, onSelectFriend, areaRequests) }

                state.selectedFriend?.let { friend ->
                    item(key = "selectedFriend") {
                        AppCard {
                            Text(stringResource(R.string.map_selected_friend), style = MaterialTheme.typography.labelLarge)
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                AvatarBadge(friend.profile.avatarId)
                                Text(displayCommunityName(friend.profile.displayName), style = MaterialTheme.typography.titleLarge)
                            }
                            Text(stringResource(R.string.map_demo_status, displayFriendStatus(friend.sampleStatus)))
                        }
                    }
                }

                if (state.friends.isEmpty()) item {
                    EmptyContent(stringResource(R.string.map_empty_title), stringResource(R.string.map_empty_message))
                } else {
                    item { Text(pluralStringResource(R.plurals.map_friends_count, state.friends.size, state.friends.size), style = MaterialTheme.typography.titleMedium) }
                    items(state.friends, key = { it.profile.userId }) { friend ->
                        AppCard {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                AvatarBadge(friend.profile.avatarId)
                                Column {
                                    Text(displayCommunityName(friend.profile.displayName), style = MaterialTheme.typography.titleMedium)
                                    Text(stringResource(R.string.map_distance, formatSaving(state.configuration.center.distanceKmTo(friend.position))),
                                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Text(stringResource(R.string.map_demo_status, displayFriendStatus(friend.sampleStatus)))
                            AppButton(
                                stringResource(if (state.selectedFriendId == friend.profile.userId) R.string.map_friend_selected else R.string.map_select_friend),
                                {
                                    onSelectFriend(friend.profile.userId)
                                    scope.launch { listState.animateScrollToItem(2) }
                                },
                                enabled = state.selectedFriendId != friend.profile.userId,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DemoFriendsMap(
    state: FriendsMapUiState,
    onSelectFriend: (String?) -> Unit,
    areaRequests: Int
) {
    var attempt by rememberSaveable { mutableIntStateOf(0) }
    key(attempt) {
        DemoFriendsMapContent(state, onSelectFriend, areaRequests, onReload = { attempt++ })
    }
}

@Composable
private fun DemoFriendsMapContent(
    state: FriendsMapUiState,
    onSelectFriend: (String?) -> Unit,
    areaRequests: Int,
    onReload: () -> Unit,
) {
    val configuration = requireNotNull(state.configuration)
    val center = configuration.center.toLatLng()
    val camera = rememberCameraPositionState { position = CameraPosition.fromLatLngZoom(center, 10f) }
    var mapLoaded by remember { mutableStateOf(false) }
    var loadTimedOut by remember { mutableStateOf(false) }
    var mapSize by remember { mutableStateOf(IntSize.Zero) }
    val density = LocalDensity.current.density
    val selected = state.selectedFriend

    LaunchedEffect(mapLoaded) {
        if (!mapLoaded) {
            delay(15_000)
            loadTimedOut = true
        }
    }

    LaunchedEffect(mapLoaded, mapSize, density, configuration, selected?.profile?.userId, selected?.position, areaRequests) {
        if (mapLoaded && mapSize.width > 0 && mapSize.height > 0) {
            val availablePixels = minOf(mapSize.width, mapSize.height) / density * 0.8
            val metersPerPixelAtZoomZero = 156543.03392 * cos(Math.toRadians(configuration.center.latitude)).coerceAtLeast(0.01)
            val areaZoom = log2(metersPerPixelAtZoomZero * availablePixels / (configuration.radiusKm * 2000))
                .toFloat().coerceIn(2f, 20f)
            withContext(Dispatchers.Main.immediate) {
                camera.animate(CameraUpdateFactory.newLatLngZoom(
                    selected?.position?.toLatLng() ?: center,
                    if (selected == null) areaZoom else maxOf(areaZoom, 14f),
                ))
            }
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        Surface(shape = RoundedCornerShape(28.dp), border = BorderStroke(3.dp, MaterialTheme.colorScheme.outlineVariant)) {
            GoogleMap(
                modifier = Modifier.fillMaxWidth().height(340.dp).clip(RoundedCornerShape(28.dp)).onSizeChanged { mapSize = it },
                cameraPositionState = camera,
                contentDescription = stringResource(R.string.map_description),
                properties = MapProperties(isMyLocationEnabled = false),
                uiSettings = MapUiSettings(myLocationButtonEnabled = false, mapToolbarEnabled = false),
                onMapLoaded = { mapLoaded = true },
                onMapClick = { onSelectFriend(null) },
            ) {
                Circle(
                    center = center, radius = configuration.radiusKm * 1000,
                    fillColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                    strokeColor = MaterialTheme.colorScheme.primary, strokeWidth = 3f,
                )
                state.friends.forEach { friend ->
                    key(friend.profile.userId) {
                        Marker(
                            state = rememberUpdatedMarkerState(position = friend.position.toLatLng()),
                            title = displayCommunityName(friend.profile.displayName),
                            icon = BitmapDescriptorFactory.defaultMarker(if (friend.profile.userId == state.selectedFriendId) BitmapDescriptorFactory.HUE_ORANGE else BitmapDescriptorFactory.HUE_AZURE),
                            snippet = stringResource(R.string.map_demo_status, displayFriendStatus(friend.sampleStatus)),
                            onClick = { onSelectFriend(friend.profile.userId); true },
                        )
                    }
                }
            }
        }

        if (!mapLoaded) {
            if (loadTimedOut) {
                ErrorContent(stringResource(R.string.map_tiles_unavailable), onReload)
            } else {
                Text(stringResource(R.string.map_tiles_loading))
            }
        }
    }
}

private fun GeoPoint.toLatLng() = LatLng(latitude, longitude)
