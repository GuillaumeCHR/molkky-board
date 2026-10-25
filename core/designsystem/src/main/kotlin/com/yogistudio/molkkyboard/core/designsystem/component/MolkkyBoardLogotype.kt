package com.yogistudio.molkkyboard.core.designsystem.component

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.yogistudio.molkkyboard.core.designsystem.theme.BricolageGrotesque
import com.yogistudio.molkkyboard.core.designsystem.theme.MolkkyBoardTheme

/**
 * Logotype « Molkky Board » : Bricolage Grotesque 800, « Molkky » en vert forêt et « Board » en safran,
 * sur la même ligne de base. Sert de titre à l'intro et à l'en-tête de l'accueil (élément partagé).
 * Nom de marque : non traduit.
 */
@Composable
fun MolkkyBoardLogotype(
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 40.sp,
) {
    val forest = MaterialTheme.colorScheme.primary
    val saffron = MolkkyBoardTheme.colors.saffron
    Text(
        text =
            buildAnnotatedString {
                withStyle(SpanStyle(color = forest)) { append("Molkky") }
                append(" ")
                withStyle(SpanStyle(color = saffron)) { append("Board") }
            },
        modifier = modifier,
        fontFamily = BricolageGrotesque,
        fontWeight = FontWeight.ExtraBold,
        fontSize = fontSize,
        letterSpacing = (-0.03).em,
        maxLines = 1,
    )
}
