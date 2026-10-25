package com.yogistudio.molkkyboard.core.designsystem.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Couleurs brutes de la direction artistique « Nordic Bold ». Les écrans utilisent les tokens, jamais ces valeurs. */
internal object NordicPalette {
    val Birch = Color(0xFFF4EBDD)
    val BirchSurface = Color(0xFFFFFAF2)
    val Forest = Color(0xFF1F4D3A)
    val Saffron = Color(0xFFF2A93B)
    val Coral = Color(0xFFE8604C)

    /** Corail assombri pour le texte sur fond clair (4,7:1 sur bouleau, contre 2,9:1 pour le corail). */
    val CoralDeep = Color(0xFFB8402E)
    val Ink = Color(0xFF1B1B18)

    val NightBackground = Color(0xFF14201A)
    val NightSurface = Color(0xFF1E2B24)
    val NightInk = Color(0xFFEDE6D8)
    val ForestLight = Color(0xFF8CC7A6)
    val SaffronMuted = Color(0xFFE0A54A)
    val CoralMuted = Color(0xFFDB7462)
}

/** Couleur d'une équipe ou d'un joueur : un aplat et la couleur du texte posé dessus. */
@Immutable
data class TeamColor(
    val fill: Color,
    val onFill: Color,
)

/**
 * Les 8 teintes d'équipe. Clair : texte bouleau sur aplat foncé (≥ 4,9:1) ; sombre : variante claire et
 * texte encre (≥ 7:1). L'emblème double toujours la couleur (daltonisme).
 */
object TeamPalette {
    private val light =
        listOf(
            0xFF3949AB, // Myrtille
            0xFF00796B, // Fjord
            0xFF2E7D32, // Sapin
            0xFFA35200, // Ambre
            0xFFAD1457, // Framboise
            0xFF6A3FC4, // Lilas
            0xFF455A64, // Ardoise
            0xFFB3261E, // Brique
        ).map { TeamColor(Color(it), NordicPalette.BirchSurface) }

    private val dark =
        listOf(
            0xFF9FA8F0,
            0xFF5CC4B5,
            0xFF7CC584,
            0xFFF0A35E,
            0xFFF28AB5,
            0xFFBFA4F5,
            0xFFA7B6BF,
            0xFFF28B82,
        ).map { TeamColor(Color(it), NordicPalette.Ink) }

    const val SIZE = 8

    fun colors(dark: Boolean): List<TeamColor> = if (dark) this.dark else light
}

/** Tokens propres à l'application, en plus du `ColorScheme` Material. */
@Immutable
data class MBColors(
    /** Joueur actif, validation. */
    val saffron: Color,
    val onSaffron: Color,
    /** Raté, dépassement de 50, élimination. */
    val coral: Color,
    val onCoral: Color,
    /** Corail lisible en texte sur le fond. */
    val coralText: Color,
    val pinWood: Color,
    val pinWoodTop: Color,
    val pinWoodShade: Color,
    val pinNumber: Color,
    val pinKnocked: Color,
    val pinKnockedTop: Color,
    val ringTrack: Color,
    /** Contour des composants : discret en temps normal, franc en « Plein soleil ». */
    val outline: Color,
    val outlineWidth: Dp,
    val teams: List<TeamColor>,
)

internal val LightMBColors =
    MBColors(
        saffron = NordicPalette.Saffron,
        onSaffron = NordicPalette.Ink,
        coral = NordicPalette.Coral,
        onCoral = NordicPalette.Ink,
        coralText = NordicPalette.CoralDeep,
        pinWood = Color(0xFFE9D3AE),
        pinWoodTop = Color(0xFFF7E9CF),
        pinWoodShade = Color(0xFFD2B486),
        pinNumber = NordicPalette.Ink,
        pinKnocked = NordicPalette.Saffron,
        pinKnockedTop = Color(0xFFF9D18F),
        ringTrack = Color(0xFFE6DAC5),
        outline = Color(0x1F1B1B18),
        outlineWidth = 1.dp,
        teams = TeamPalette.colors(dark = false),
    )

internal val DarkMBColors =
    MBColors(
        saffron = NordicPalette.SaffronMuted,
        onSaffron = NordicPalette.Ink,
        coral = NordicPalette.CoralMuted,
        onCoral = NordicPalette.Ink,
        coralText = NordicPalette.CoralMuted,
        pinWood = Color(0xFFD9C29C),
        pinWoodTop = Color(0xFFEBDABB),
        pinWoodShade = Color(0xFFB89C70),
        pinNumber = NordicPalette.Ink,
        pinKnocked = NordicPalette.SaffronMuted,
        pinKnockedTop = Color(0xFFF0C987),
        ringTrack = Color(0xFF2C3B33),
        outline = Color(0x33EDE6D8),
        outlineWidth = 1.dp,
        teams = TeamPalette.colors(dark = true),
    )

/** « Plein soleil » : fond blanc, texte noir, accents plus profonds et contours épais (≥ 7:1 partout). */
internal val SunlightMBColors =
    LightMBColors.copy(
        saffron = Color(0xFFF5A623),
        onSaffron = Color.Black,
        coral = Color(0xFFA3301F),
        onCoral = Color.White,
        coralText = Color(0xFFA3301F),
        pinNumber = Color.Black,
        pinWoodShade = Color(0xFFB8955E),
        ringTrack = Color(0xFFD6CCBB),
        outline = Color.Black,
        outlineWidth = 2.dp,
    )

internal val LightColorScheme: ColorScheme =
    lightColorScheme(
        primary = NordicPalette.Forest,
        onPrimary = NordicPalette.BirchSurface,
        primaryContainer = Color(0xFFCFE3D6),
        onPrimaryContainer = Color(0xFF0B2A1E),
        secondary = NordicPalette.Saffron,
        onSecondary = NordicPalette.Ink,
        secondaryContainer = Color(0xFFFBE3BC),
        onSecondaryContainer = Color(0xFF3D2A00),
        tertiary = NordicPalette.Coral,
        onTertiary = NordicPalette.Ink,
        tertiaryContainer = Color(0xFFFAD3CC),
        onTertiaryContainer = Color(0xFF4A120A),
        error = NordicPalette.CoralDeep,
        onError = NordicPalette.BirchSurface,
        background = NordicPalette.Birch,
        onBackground = NordicPalette.Ink,
        surface = NordicPalette.BirchSurface,
        onSurface = NordicPalette.Ink,
        surfaceVariant = Color(0xFFEADFCC),
        onSurfaceVariant = Color(0xFF4A463E),
        surfaceContainerLowest = Color.White,
        surfaceContainerLow = NordicPalette.BirchSurface,
        surfaceContainer = Color(0xFFF9F1E4),
        surfaceContainerHigh = Color(0xFFEFE4D2),
        surfaceContainerHighest = Color(0xFFE8DCC7),
        outline = Color(0xFF8A8172),
        outlineVariant = Color(0xFFD9CDB8),
    )

internal val DarkColorScheme: ColorScheme =
    darkColorScheme(
        primary = NordicPalette.ForestLight,
        onPrimary = Color(0xFF0B2A1E),
        primaryContainer = NordicPalette.Forest,
        onPrimaryContainer = Color(0xFFCFE3D6),
        secondary = NordicPalette.SaffronMuted,
        onSecondary = NordicPalette.Ink,
        secondaryContainer = Color(0xFF5A4010),
        onSecondaryContainer = Color(0xFFFBE3BC),
        tertiary = NordicPalette.CoralMuted,
        onTertiary = NordicPalette.Ink,
        tertiaryContainer = Color(0xFF6B2418),
        onTertiaryContainer = Color(0xFFFAD3CC),
        error = Color(0xFFF28B82),
        onError = NordicPalette.Ink,
        background = NordicPalette.NightBackground,
        onBackground = NordicPalette.NightInk,
        surface = NordicPalette.NightSurface,
        onSurface = NordicPalette.NightInk,
        surfaceVariant = Color(0xFF2A3A31),
        onSurfaceVariant = Color(0xFFC2BBAD),
        surfaceContainerLowest = Color(0xFF0F1913),
        surfaceContainerLow = Color(0xFF19251F),
        surfaceContainer = NordicPalette.NightSurface,
        surfaceContainerHigh = Color(0xFF26352C),
        surfaceContainerHighest = Color(0xFF2F3F36),
        outline = Color(0xFF8D978F),
        outlineVariant = Color(0xFF3A4A41),
    )

internal val SunlightColorScheme: ColorScheme =
    LightColorScheme.copy(
        primary = Color(0xFF123426),
        onPrimary = Color.White,
        background = Color(0xFFFFFDF7),
        onBackground = Color.Black,
        surface = Color.White,
        onSurface = Color.Black,
        onSurfaceVariant = Color(0xFF26231E),
        tertiary = Color(0xFFA3301F),
        onTertiary = Color.White,
        error = Color(0xFFA3301F),
        outline = Color.Black,
    )
