package com.livesustainably.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.livesustainably.R

private val Rounded = FontFamily(
    Font(R.font.nunito, FontWeight.Normal, variationSettings = FontVariation.Settings(FontVariation.weight(400))),
    Font(R.font.nunito, FontWeight.SemiBold, variationSettings = FontVariation.Settings(FontVariation.weight(600))),
    Font(R.font.nunito, FontWeight.Bold, variationSettings = FontVariation.Settings(FontVariation.weight(700))),
    Font(R.font.nunito, FontWeight.ExtraBold, variationSettings = FontVariation.Settings(FontVariation.weight(800))),
    Font(R.font.nunito, FontWeight.Black, variationSettings = FontVariation.Settings(FontVariation.weight(900))),
)
private fun rounded(size: Int, height: Int, weight: FontWeight = FontWeight.Normal) = TextStyle(
    fontFamily = Rounded, fontSize = size.sp, lineHeight = height.sp, fontWeight = weight, letterSpacing = 0.sp,
)

val Typography = Typography(
    displayLarge = rounded(48, 54, FontWeight.Black),
    displayMedium = rounded(40, 48, FontWeight.Black),
    displaySmall = rounded(36, 42, FontWeight.ExtraBold),
    headlineLarge = rounded(30, 36, FontWeight.ExtraBold),
    headlineMedium = rounded(26, 32, FontWeight.ExtraBold),
    headlineSmall = rounded(23, 29, FontWeight.ExtraBold),
    titleLarge = rounded(21, 28, FontWeight.ExtraBold),
    titleMedium = rounded(17, 24, FontWeight.Bold),
    titleSmall = rounded(15, 22, FontWeight.Bold),
    bodyLarge = rounded(16, 24, FontWeight.SemiBold),
    bodyMedium = rounded(14, 21, FontWeight.SemiBold),
    bodySmall = rounded(12, 18, FontWeight.Normal),
    labelLarge = rounded(14, 20, FontWeight.ExtraBold),
    labelMedium = rounded(12, 18, FontWeight.Bold),
    labelSmall = rounded(11, 16, FontWeight.Bold),
)
