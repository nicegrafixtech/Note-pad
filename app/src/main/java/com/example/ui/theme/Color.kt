package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Primary Branding Palettes
val IndigoPrimary = Color(0xFF4F46E5)
val IndigoOnPrimary = Color(0xFFFFFFFF)
val IndigoPrimaryContainer = Color(0xFFEEF2FF)
val IndigoOnPrimaryContainer = Color(0xFF312E81)

val EmeraldPrimary = Color(0xFF059669)
val EmeraldOnPrimary = Color(0xFFFFFFFF)
val EmeraldPrimaryContainer = Color(0xFFD1FAE5)
val EmeraldOnPrimaryContainer = Color(0xFF064E3B)

val RosePrimary = Color(0xFFE11D48)
val RoseOnPrimary = Color(0xFFFFFFFF)
val RosePrimaryContainer = Color(0xFFFFE4E6)
val RoseOnPrimaryContainer = Color(0xFF881337)

val AmberPrimary = Color(0xFFD97706)
val AmberOnPrimary = Color(0xFFFFFFFF)
val AmberPrimaryContainer = Color(0xFFFEF3C7)
val AmberOnPrimaryContainer = Color(0xFF78350F)

val VioletPrimary = Color(0xFF7C3AED)
val VioletOnPrimary = Color(0xFFFFFFFF)
val VioletPrimaryContainer = Color(0xFFEDE9FE)
val VioletOnPrimaryContainer = Color(0xFF4C1D95)

val OceanPrimary = Color(0xFF0284C7)
val OceanOnPrimary = Color(0xFFFFFFFF)
val OceanPrimaryContainer = Color(0xFFE0F2FE)
val OceanOnPrimaryContainer = Color(0xFF0C4A6E)

// Neutral & Surface Colors
val Gray50 = Color(0xFFF8FAFC)
val Gray100 = Color(0xFFF1F5F9)
val Gray200 = Color(0xFFE2E8F0)
val Gray300 = Color(0xFFCBD5E1)
val Gray400 = Color(0xFF94A3B8)
val Gray500 = Color(0xFF64748B)
val Gray600 = Color(0xFF475569)
val Gray700 = Color(0xFF334155)
val Gray800 = Color(0xFF1E293B)
val Gray900 = Color(0xFF0F172A)
val Gray950 = Color(0xFF020617)

// Note Background Pastel Colors (Light mode)
val NoteColorDefaultLight = Color(0xFFFFFFFF)
val NoteColorCoralLight = Color(0xFFFFEDEC)
val NoteColorAmberLight = Color(0xFFFEF3C7)
val NoteColorMintLight = Color(0xFFD1FAE5)
val NoteColorSkyLight = Color(0xFFE0F2FE)
val NoteColorLavenderLight = Color(0xFFEDE9FE)
val NoteColorRoseLight = Color(0xFFFCE7F3)
val NoteColorSlateLight = Color(0xFFF1F5F9)

// Note Background Pastel Colors (Dark mode)
val NoteColorDefaultDark = Color(0xFF1E293B)
val NoteColorCoralDark = Color(0xFF3C1F24)
val NoteColorAmberDark = Color(0xFF3B2E15)
val NoteColorMintDark = Color(0xFF123829)
val NoteColorSkyDark = Color(0xFF132F45)
val NoteColorLavenderDark = Color(0xFF29204A)
val NoteColorRoseDark = Color(0xFF3B1B30)
val NoteColorSlateDark = Color(0xFF273549)

data class NoteColorOption(
    val id: String,
    val name: String,
    val lightColor: Color,
    val darkColor: Color
)

val AvailableNoteColors = listOf(
    NoteColorOption("default", "Default", NoteColorDefaultLight, NoteColorDefaultDark),
    NoteColorOption("coral", "Coral", NoteColorCoralLight, NoteColorCoralDark),
    NoteColorOption("amber", "Amber", NoteColorAmberLight, NoteColorAmberDark),
    NoteColorOption("mint", "Mint", NoteColorMintLight, NoteColorMintDark),
    NoteColorOption("sky", "Sky", NoteColorSkyLight, NoteColorSkyDark),
    NoteColorOption("lavender", "Lavender", NoteColorLavenderLight, NoteColorLavenderDark),
    NoteColorOption("rose", "Rose", NoteColorRoseLight, NoteColorRoseDark),
    NoteColorOption("slate", "Slate", NoteColorSlateLight, NoteColorSlateDark)
)
