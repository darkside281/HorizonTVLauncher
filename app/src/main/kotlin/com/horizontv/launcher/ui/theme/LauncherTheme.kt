package com.horizontv.launcher.ui.theme

import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.horizontv.launcher.model.AccentPreset
import com.horizontv.launcher.model.WallpaperTheme

object LauncherPalettes {
  val themes: List<WallpaperTheme> = listOf(
    WallpaperTheme(
      id = "midnight_horizon",
      name = "Midnight Horizon",
      baseColor = Color(0xFF070B14),
      midColor = Color(0xFF0F172A),
      topGlow = Color(0xFF1D4ED8),
      bottomGlow = Color(0xFF0EA5E9),
      cardSurface = Color(0xFF131C2E),
    ),
    WallpaperTheme(
      id = "aurora_borealis",
      name = "Aurora Borealis",
      baseColor = Color(0xFF041114),
      midColor = Color(0xFF0A2228),
      topGlow = Color(0xFF10B981),
      bottomGlow = Color(0xFF6366F1),
      cardSurface = Color(0xFF0F252B),
    ),
    WallpaperTheme(
      id = "obsidian_ember",
      name = "Obsidian Ember",
      baseColor = Color(0xFF0E0A08),
      midColor = Color(0xFF1C130E),
      topGlow = Color(0xFFEA580C),
      bottomGlow = Color(0xFFF59E0B),
      cardSurface = Color(0xFF211712),
    ),
    WallpaperTheme(
      id = "nordic_frost",
      name = "Nordic Frost",
      baseColor = Color(0xFF08101A),
      midColor = Color(0xFF112236),
      topGlow = Color(0xFF38BDF8),
      bottomGlow = Color(0xFF0284C7),
      cardSurface = Color(0xFF14263B),
    ),
    WallpaperTheme(
      id = "sunset_velvet",
      name = "Sunset Velvet",
      baseColor = Color(0xFF110815),
      midColor = Color(0xFF211028),
      topGlow = Color(0xFFEC4899),
      bottomGlow = Color(0xFF8B5CF6),
      cardSurface = Color(0xFF25152D),
    ),
    WallpaperTheme(
      id = "cinema_noir",
      name = "Cinema Noir",
      baseColor = Color(0xFF040507),
      midColor = Color(0xFF0C0E13),
      topGlow = Color(0xFF475569),
      bottomGlow = Color(0xFF1E293B),
      cardSurface = Color(0xFF14171F),
    ),
  )

  val accents: List<AccentPreset> = listOf(
    AccentPreset(
      id = "sky_cyan",
      name = "Sky Cyan",
      primary = Color(0xFF38BDF8),
      onPrimary = Color(0xFF061621),
      glow = Color(0xFF0EA5E9),
    ),
    AccentPreset(
      id = "electric_violet",
      name = "Electric Violet",
      primary = Color(0xFFA78BFA),
      onPrimary = Color(0xFF1B1033),
      glow = Color(0xFF8B5CF6),
    ),
    AccentPreset(
      id = "emerald_mint",
      name = "Emerald Mint",
      primary = Color(0xFF34D399),
      onPrimary = Color(0xFF052218),
      glow = Color(0xFF10B981),
    ),
    AccentPreset(
      id = "sunset_amber",
      name = "Sunset Amber",
      primary = Color(0xFFFBBF24),
      onPrimary = Color(0xFF261802),
      glow = Color(0xFFF59E0B),
    ),
    AccentPreset(
      id = "coral_rose",
      name = "Coral Rose",
      primary = Color(0xFFFB7185),
      onPrimary = Color(0xFF2C0811),
      glow = Color(0xFFF43F5E),
    ),
    AccentPreset(
      id = "pure_pearl",
      name = "Pure Pearl",
      primary = Color(0xFFF1F5F9),
      onPrimary = Color(0xFF0F172A),
      glow = Color(0xFFCBD5E1),
    ),
  )

  fun themeById(id: String): WallpaperTheme =
    themes.firstOrNull { it.id == id } ?: themes.first()

  fun accentById(id: String): AccentPreset =
    accents.firstOrNull { it.id == id } ?: accents.first()
}

private val TvTypography = Typography(
  displayLarge = TextStyle(
    fontSize = 36.sp,
    lineHeight = 40.sp,
    fontWeight = FontWeight.SemiBold,
    letterSpacing = (-0.5).sp,
  ),
  headlineMedium = TextStyle(
    fontSize = 24.sp,
    lineHeight = 30.sp,
    fontWeight = FontWeight.SemiBold,
  ),
  titleLarge = TextStyle(
    fontSize = 19.sp,
    lineHeight = 24.sp,
    fontWeight = FontWeight.SemiBold,
    letterSpacing = 0.1.sp,
  ),
  titleMedium = TextStyle(
    fontSize = 16.sp,
    lineHeight = 20.sp,
    fontWeight = FontWeight.Medium,
  ),
  bodyLarge = TextStyle(
    fontSize = 15.sp,
    lineHeight = 20.sp,
    fontWeight = FontWeight.Normal,
  ),
  bodyMedium = TextStyle(
    fontSize = 13.sp,
    lineHeight = 18.sp,
    fontWeight = FontWeight.Normal,
  ),
  labelLarge = TextStyle(
    fontSize = 14.sp,
    lineHeight = 18.sp,
    fontWeight = FontWeight.SemiBold,
    letterSpacing = 0.2.sp,
  ),
  labelMedium = TextStyle(
    fontSize = 12.sp,
    lineHeight = 16.sp,
    fontWeight = FontWeight.Medium,
    letterSpacing = 0.3.sp,
  ),
)

/**
 * Every TV button and card in Horizon renders its own rounded focus border, color fill,
 * and spring scale animation. Suppressing Material's default unclipped square focus/ripple
 * overlay prevents rectangular grey boxes from appearing behind circular icons and rounded pills.
 */
private object NoFocusBoxIndication : IndicationNodeFactory {
  override fun create(interactionSource: InteractionSource): DelegatableNode = object : Modifier.Node() {}
  override fun equals(other: Any?): Boolean = other === this
  override fun hashCode(): Int = 0
}

@Composable
fun HorizonTVLauncherTheme(
  wallpaperTheme: WallpaperTheme,
  accentPreset: AccentPreset,
  content: @Composable () -> Unit,
) {
  val colorScheme = darkColorScheme(
    primary = accentPreset.primary,
    onPrimary = accentPreset.onPrimary,
    secondary = accentPreset.glow,
    background = wallpaperTheme.baseColor,
    onBackground = Color(0xFFF8FAFC),
    surface = wallpaperTheme.cardSurface,
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = Color(0xFF1E293B),
    onSurfaceVariant = Color(0xFFCBD5E1),
    outline = Color(0xFF334155),
  )

  MaterialTheme(
    colorScheme = colorScheme,
    typography = TvTypography,
  ) {
    CompositionLocalProvider(
      LocalIndication provides NoFocusBoxIndication,
      content = content,
    )
  }
}
