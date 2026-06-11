package com.wergogo.travelnote.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Using system fonts (Manrope/Bricolage not available as bundled fonts without download).
// The design system specifies Bricolage Grotesque for headers and Manrope for body.
// We approximate with the default sans-serif until custom fonts are added.
val Typography = Typography(
    displayLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 24.sp),
    titleLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 24.sp),   // screen title
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 18.sp),  // section header
    titleSmall = TextStyle(fontWeight = FontWeight.Medium, fontSize = 16.sp),     // card title
    bodyLarge = TextStyle(fontWeight = FontWeight.Normal, fontSize = 14.sp),      // body
    bodyMedium = TextStyle(fontWeight = FontWeight.Normal, fontSize = 14.sp),
    bodySmall = TextStyle(fontWeight = FontWeight.Normal, fontSize = 12.sp),      // caption / meta
    labelSmall = TextStyle(fontWeight = FontWeight.Normal, fontSize = 12.sp)
)
