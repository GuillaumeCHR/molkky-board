package com.yogistudio.molkkyboard.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.yogistudio.molkkyboard.core.designsystem.icon.emblemIcon
import com.yogistudio.molkkyboard.core.designsystem.theme.TeamColor

/**
 * Avatar d'un joueur ou d'une équipe : l'emblème au trait, dessiné dans la couleur de contraste de [color], sur un
 * disque de cette couleur ; l'initiale de [name] si la clé est inconnue. Décoratif pour TalkBack (le nom est lu à
 * côté).
 */
@Composable
fun MBAvatar(
    key: String?,
    name: String,
    color: TeamColor,
    size: Dp,
    modifier: Modifier = Modifier,
    background: Boolean = true,
) {
    Box(
        modifier.size(size).background(if (background) color.fill else Color.Transparent, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        AvatarGlyph(key, name, if (background) color.onFill else color.fill, size)
    }
}

/** Le seul dessin de l'avatar (emblème ou initiale), pour l'insérer dans une pastille existante. */
@Composable
fun AvatarGlyph(key: String?, name: String, tint: Color, size: Dp) {
    val icon = emblemIcon(key)
    if (icon != null) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(size * ICON_RATIO))
    } else {
        Text(
            name.take(1).uppercase(),
            style = if (size > LARGE) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleMedium,
            color = tint,
        )
    }
}

private const val ICON_RATIO = 0.6f
private val LARGE = 40.dp
