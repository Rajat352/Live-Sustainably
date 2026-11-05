package com.livesustainably.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.livesustainably.R
import com.livesustainably.ui.navigation.TopLevelDestinations
import com.livesustainably.ui.theme.HeroBlue
import com.livesustainably.ui.theme.HeroCyan

@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.surface,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large,
        color = color, border = BorderStroke(3.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
}

@Composable
fun AppButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val shape = RoundedCornerShape(24.dp)
    Surface(
        onClick = onClick, enabled = enabled, shape = shape, color = Color.Transparent,
        modifier = modifier.padding(bottom = 4.dp).shadow(if (enabled) 3.dp else 0.dp, shape)
            .semantics { role = Role.Button },
    ) {
        Box(
            Modifier.background(if (enabled) Brush.horizontalGradient(listOf(HeroBlue, HeroCyan))
                else Brush.horizontalGradient(listOf(MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.colorScheme.surfaceContainerHigh)))
                .heightIn(min = 50.dp).padding(horizontal = 22.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(text, style = MaterialTheme.typography.labelLarge,
                color = if (enabled) Color.White else MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun ChoicePill(
    selected: Boolean,
    onClick: () -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Surface(
        modifier.padding(vertical = 3.dp).clip(CircleShape)
            .selectable(selected, enabled = enabled, role = Role.RadioButton, onClick = onClick),
        shape = CircleShape,
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        border = BorderStroke(2.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
    ) {
        Box(Modifier.heightIn(min = 48.dp).padding(horizontal = 16.dp, vertical = 10.dp), contentAlignment = Alignment.Center) {
            ProvideTextStyle(MaterialTheme.typography.labelLarge, label)
        }
    }
}

@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: @Composable (() -> Unit)? = null,
    enabled: Boolean = true,
    isError: Boolean = false,
    supportingText: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    singleLine: Boolean = false,
    minLines: Int = 1,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
) {
    OutlinedTextField(
        value, onValueChange, modifier, enabled = enabled, label = label, isError = isError,
        supportingText = supportingText, trailingIcon = trailingIcon, singleLine = singleLine,
        minLines = minLines, maxLines = maxLines, keyboardOptions = keyboardOptions, keyboardActions = keyboardActions,
        visualTransformation = visualTransformation, shape = RoundedCornerShape(22.dp),
        textStyle = MaterialTheme.typography.bodyLarge,
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            focusedBorderColor = MaterialTheme.colorScheme.primary,
        ),
    )
}

@Composable
fun ScreenHeading(
    title: String,
    subtitle: String? = null,
    eyebrow: String? = null
) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        eyebrow?.let { Text(it.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary) }
        Text(title, style = MaterialTheme.typography.headlineLarge, modifier = Modifier.semantics { heading() })
        subtitle?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@Composable
fun HeroCard(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit = {},
) {
    Surface(modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large, color = Color.Transparent) {
        Column(
            Modifier.background(Brush.linearGradient(listOf(HeroBlue, HeroCyan))).padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text(title, style = MaterialTheme.typography.headlineSmall, color = Color.White)
                    Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.92f))
                }
                SproutMascot(Modifier.size(88.dp))
            }
            content()
        }
    }
}

@Composable
fun AvatarBadge(
    avatarId: String?,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp
) {
    val symbol = when (avatarId) { "tree" -> "🌳"; "sun" -> "☀️"; "earth" -> "🌍"; else -> "🌱" }
    Surface(modifier.size(size), shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Box(contentAlignment = Alignment.Center) {
            Text(symbol, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.clearAndSetSemantics {})
        }
    }
}

@Composable
fun LoadingContent(
    message: String,
    modifier: Modifier = Modifier
) {
    AppCard(modifier.semantics { liveRegion = LiveRegionMode.Polite }) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            CircularProgressIndicator(Modifier.size(28.dp), strokeWidth = 3.dp)
            Text(message, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun EmptyContent(
    title: String,
    message: String,
    modifier: Modifier = Modifier
) {
    AppCard(modifier) {
        SproutMascot(Modifier.size(76.dp).align(Alignment.CenterHorizontally))
        Text(title, style = MaterialTheme.typography.titleLarge)
        Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun ErrorContent(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    AppCard(modifier.semantics { liveRegion = LiveRegionMode.Polite }) {
        Text("Let’s try that again", style = MaterialTheme.typography.titleLarge)
        Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        AppButton("Try again", onRetry)
    }
}

@Composable
fun MetricTile(
    symbol: String,
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    onHero: Boolean = false
) {
    Surface(
        modifier, shape = RoundedCornerShape(22.dp),
        color = if (onHero) Color.White.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surface,
        contentColor = if (onHero) Color.White else MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(2.dp, if (onHero) Color.White.copy(alpha = 0.22f) else MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(symbol, style = MaterialTheme.typography.titleLarge, modifier = Modifier.clearAndSetSemantics {})
            Text(value, style = MaterialTheme.typography.headlineSmall)
            Text(label, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
fun AppToolbar(canGoBack: Boolean, title: String, onBack: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Row(Modifier.fillMaxWidth().heightIn(min = 58.dp).padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (canGoBack) IconButton(onClick = onBack) {
                Icon(painterResource(R.drawable.ic_arrow_back), "Back", tint = MaterialTheme.colorScheme.onSurface)
            } else SproutMascot(Modifier.size(38.dp))
            Text(title, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
fun AppTabBar(selected: TopLevelDestinations, onSelect: (TopLevelDestinations) -> Unit) {
    Surface(
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp).fillMaxWidth(),
        shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(3.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(Modifier.selectableGroup().padding(horizontal = 6.dp, vertical = 8.dp)) {
            TopLevelDestinations.entries.forEach { tab ->
                val active = tab == selected
                Column(
                    Modifier.weight(1f).clip(RoundedCornerShape(20.dp))
                        .selectable(active, role = Role.Tab, onClick = { onSelect(tab) })
                        .padding(vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Box(Modifier.clip(RoundedCornerShape(14.dp))
                        .background(if (active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 15.dp, vertical = 5.dp)) {
                        Icon(painterResource(tab.icon), null, Modifier.size(24.dp),
                            tint = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    BasicText(
                        tab.label, style = MaterialTheme.typography.labelMedium.copy(
                            color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant),
                        maxLines = 1, autoSize = TextAutoSize.StepBased(minFontSize = 8.sp, maxFontSize = 12.sp),
                    )
                }
            }
        }
    }
}