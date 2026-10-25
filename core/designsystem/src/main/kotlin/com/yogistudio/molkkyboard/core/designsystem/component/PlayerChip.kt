package com.yogistudio.molkkyboard.core.designsystem.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yogistudio.molkkyboard.core.designsystem.R
import com.yogistudio.molkkyboard.core.designsystem.icon.MBIcons
import com.yogistudio.molkkyboard.core.designsystem.theme.MBDimens
import com.yogistudio.molkkyboard.core.designsystem.theme.MBMotion
import com.yogistudio.molkkyboard.core.designsystem.theme.MolkkyBoardTheme
import com.yogistudio.molkkyboard.core.designsystem.theme.TeamColor

/**
 * Pastille d'un joueur ou d'une équipe : un médaillon à sa couleur (initiale ou emblème) et son nom.
 * Le joueur actif est entouré de safran et légèrement grossi.
 *
 * @param score score affiché après le nom : jamais tronqué, c'est le nom qui s'abrège si la place manque.
 * @param badge contenu du médaillon ; par défaut l'initiale du nom.
 */
@Composable
fun PlayerChip(
    name: String,
    color: TeamColor,
    modifier: Modifier = Modifier,
    active: Boolean = false,
    score: Int? = null,
    onClick: (() -> Unit)? = null,
    onRemove: (() -> Unit)? = null,
    badge: @Composable () -> Unit = {
        Text(name.take(1).uppercase(), style = MaterialTheme.typography.titleMedium, color = color.onFill)
    },
) {
    val colors = MolkkyBoardTheme.colors
    val scheme = MaterialTheme.colorScheme
    val scale by animateFloatAsState(
        targetValue = if (active) 1.04f else 1f,
        animationSpec = if (MolkkyBoardTheme.reducedMotion) snap() else MBMotion.bouncy(),
        label = "active",
    )
    val borderColor by animateColorAsState(
        if (active) colors.saffron else colors.outline,
        MBMotion.gentle(),
        label = "border",
    )
    val borderWidth = if (active) 3.dp else colors.outlineWidth
    val activeState = if (active) stringResource(R.string.ds_player_active, name) else ""
    val scoreDescription = score?.let { stringResource(R.string.ds_player_score, name, it) }
    val content: @Composable () -> Unit = {
        Row(
            modifier =
                Modifier
                    .heightIn(min = MBDimens.MinTouch)
                    .padding(start = 8.dp, end = if (onRemove != null) 4.dp else 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                Modifier.size(40.dp).background(color.fill, CircleShape),
                contentAlignment = Alignment.Center,
            ) { badge() }
            Text(
                name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            if (score != null) {
                Text(score.toString(), style = MolkkyBoardTheme.typography.scoreSmall, maxLines = 1, softWrap = false)
            }
            if (onRemove != null) {
                val label = stringResource(R.string.ds_remove, name)
                IconButton(onClick = onRemove, modifier = Modifier.size(MBDimens.MinTouch)) {
                    Icon(MBIcons.Cross, contentDescription = label, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
    val chipModifier =
        modifier
            .scale(scale)
            .semantics {
                if (active) stateDescription = activeState
                if (scoreDescription != null) contentDescription = scoreDescription
            }
    val border = BorderStroke(borderWidth, borderColor)
    if (onClick != null) {
        Surface(
            onClick = onClick,
            modifier = chipModifier,
            shape = CircleShape,
            color = scheme.surface,
            border = border,
            content = content,
        )
    } else {
        Surface(
            modifier = chipModifier,
            shape = CircleShape,
            color = scheme.surface,
            border = border,
            content = content,
        )
    }
}
