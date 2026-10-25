package com.yogistudio.molkkyboard.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.yogistudio.molkkyboard.core.designsystem.R

/** Titres et scores. Graisses figées (700, 800) extraites de la police variable, sous-ensemble latin. */
val BricolageGrotesque =
    FontFamily(
        Font(R.font.bricolage_grotesque_bold, FontWeight.Bold),
        Font(R.font.bricolage_grotesque_extrabold, FontWeight.ExtraBold),
    )

/** Texte courant. */
val Inter =
    FontFamily(
        Font(R.font.inter_regular, FontWeight.Normal),
        Font(R.font.inter_medium, FontWeight.Medium),
        Font(R.font.inter_semibold, FontWeight.SemiBold),
        Font(R.font.inter_bold, FontWeight.Bold),
    )

/** Chiffres tabulaires : un score qui change ne fait pas « sauter » la mise en page. */
private const val TABULAR = "tnum"

private fun display(size: Int, lineHeight: Int, weight: FontWeight = FontWeight.ExtraBold) =
    TextStyle(
        fontFamily = BricolageGrotesque,
        fontWeight = weight,
        fontSize = size.sp,
        lineHeight = lineHeight.sp,
        letterSpacing = (-0.02).em,
        fontFeatureSettings = TABULAR,
    )

private fun text(size: Int, lineHeight: Int, weight: FontWeight, letterSpacing: Double = 0.0) =
    TextStyle(
        fontFamily = Inter,
        fontWeight = weight,
        fontSize = size.sp,
        lineHeight = lineHeight.sp,
        letterSpacing = letterSpacing.em,
    )

internal val MBMaterialTypography =
    Typography(
        displayLarge = display(57, 60),
        displayMedium = display(45, 50),
        displaySmall = display(36, 42),
        headlineLarge = display(32, 38, FontWeight.Bold),
        headlineMedium = display(28, 34, FontWeight.Bold),
        headlineSmall = display(24, 30, FontWeight.Bold),
        titleLarge = text(22, 28, FontWeight.SemiBold),
        titleMedium = text(16, 24, FontWeight.SemiBold),
        titleSmall = text(14, 20, FontWeight.SemiBold),
        bodyLarge = text(16, 24, FontWeight.Normal),
        bodyMedium = text(14, 20, FontWeight.Normal),
        bodySmall = text(12, 16, FontWeight.Normal),
        labelLarge = text(14, 20, FontWeight.SemiBold, letterSpacing = 0.01),
        labelMedium = text(12, 16, FontWeight.SemiBold, letterSpacing = 0.02),
        labelSmall = text(11, 16, FontWeight.Medium, letterSpacing = 0.03),
    )

/** Styles propres à l'application. */
@Immutable
data class MBTypography(
    /** Score du joueur actif : 104 sp (minimum demandé : 96 sp). */
    val scoreHero: TextStyle = display(104, 104),
    val scoreLarge: TextStyle = display(48, 52),
    val scoreSmall: TextStyle = display(24, 28),
    /** Libellés des gros boutons d'action. */
    val action: TextStyle = display(22, 26, FontWeight.Bold).copy(letterSpacing = 0.em),
    /** Numéro gravé sur une quille. */
    val pinNumber: TextStyle = display(20, 22, FontWeight.ExtraBold).copy(letterSpacing = 0.em),
)
