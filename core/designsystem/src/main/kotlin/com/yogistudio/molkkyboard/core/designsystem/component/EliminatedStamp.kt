package com.yogistudio.molkkyboard.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.yogistudio.molkkyboard.core.designsystem.theme.BricolageGrotesque
import com.yogistudio.molkkyboard.core.designsystem.theme.MolkkyBoardTheme

/** Tampon corail posé en biais (« Éliminé ») : ton humoristique, jamais moqueur. */
@Composable
fun EliminatedStamp(
    text: String,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 28.sp,
) {
    val colors = MolkkyBoardTheme.colors
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = colors.coral.copy(alpha = STAMP_FILL_ALPHA),
        contentColor = colors.coralText,
        border = BorderStroke(3.dp, colors.coralText),
        modifier = modifier.rotate(STAMP_ANGLE),
    ) {
        Text(
            text.uppercase(),
            fontFamily = BricolageGrotesque,
            fontWeight = FontWeight.ExtraBold,
            fontSize = fontSize,
            letterSpacing = 0.04.em,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
        )
    }
}

private const val STAMP_ANGLE = -12f
private const val STAMP_FILL_ALPHA = 0.12f
