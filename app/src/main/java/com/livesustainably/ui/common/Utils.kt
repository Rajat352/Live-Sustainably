package com.livesustainably.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.livesustainably.R
import com.livesustainably.domain.model.DataException
import com.livesustainably.domain.model.DataFailure
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

internal fun dataFailure(error: Throwable): DataFailure =
    (error as? DataException)?.reason ?: DataFailure.Unknown

internal val DataFailure.isAccessDenied: Boolean
    get() = this == DataFailure.PermissionDenied || this == DataFailure.SignedOut

@Composable
internal fun dataErrorMessage(reason: DataFailure): String = stringResource(when (reason) {
    DataFailure.SignedOut -> R.string.data_error_signed_out
    DataFailure.PermissionDenied -> R.string.data_error_permission
    DataFailure.Network -> R.string.data_error_network
    DataFailure.InvalidData -> R.string.data_error_invalid
    DataFailure.DemoNotConfigured -> R.string.data_error_demo
    DataFailure.Unknown -> R.string.data_error_unknown
})

internal fun formatPostDate(millis: Long): String = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
    .format(Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()))

internal fun formatSaving(value: Double): String = NumberFormat.getNumberInstance().apply {
    maximumFractionDigits = 2
}.format(value)
