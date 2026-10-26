package com.yogistudio.molkkyboard.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yogistudio.molkkyboard.core.designsystem.R
import com.yogistudio.molkkyboard.core.designsystem.theme.MBDimens
import com.yogistudio.molkkyboard.core.designsystem.theme.MolkkyBoardTheme
import com.yogistudio.molkkyboard.core.designsystem.theme.TeamColor

/**
 * Une ligne du tableau des scores : rang, médaillon, nom, points restants et ratés, score en grands chiffres
 * tabulaires (jamais tronqué : c'est le nom qui passe à la ligne). Le joueur actif est surligné en safran,
 * une équipe hors jeu est grisée et barrée. En équipe, [members] se déplient sous la ligne.
 *
 * @param rank rang affiché, ou `null` pour une équipe hors jeu.
 * @param status mention d'une équipe hors jeu (« Éliminé », « Parti »), ou `null`.
 */
@Composable
fun ScoreboardRow(
    rank: Int?,
    name: String,
    color: TeamColor,
    score: Int,
    remaining: Int,
    misses: Int,
    missLimit: Int,
    modifier: Modifier = Modifier,
    active: Boolean = false,
    status: String? = null,
    members: List<String> = emptyList(),
) {
    var expanded by rememberSaveable(name) { mutableStateOf(false) }
    val out = status != null
    val background = if (active) MolkkyBoardTheme.colors.saffron.copy(alpha = ACTIVE_ALPHA) else Color.Transparent
    val description =
        listOfNotNull(
            rank?.let { stringResource(R.string.ds_rank, it) },
            stringResource(R.string.ds_player_score, name, score),
            if (out) status else stringResource(R.string.ds_remaining, remaining),
            stringResource(R.string.ds_misses, misses, missLimit),
            if (active) stringResource(R.string.ds_player_active, name) else null,
        ).joinToString(", ")
    Column(
        modifier
            .fillMaxWidth()
            .background(background, RoundedCornerShape(20.dp))
            .alpha(if (out) OUT_ALPHA else 1f),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .then(if (members.size > 1) Modifier.clickable { expanded = !expanded } else Modifier)
                    .heightIn(min = MBDimens.MinTouch)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .clearAndSetSemantics { contentDescription = description },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MBDimens.Gap),
        ) {
            Text(
                rank?.toString() ?: "–",
                style = MolkkyBoardTheme.typography.scoreSmall,
                modifier = Modifier.widthIn(min = 28.dp),
            )
            Box(Modifier.size(36.dp).background(color.fill, CircleShape), contentAlignment = Alignment.Center) {
                Text(name.take(1).uppercase(), style = MaterialTheme.typography.titleMedium, color = color.onFill)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    name,
                    style = MaterialTheme.typography.titleMedium,
                    textDecoration = if (out) TextDecoration.LineThrough else null,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    status ?: stringResource(R.string.ds_remaining, remaining),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (out) MolkkyBoardTheme.colors.coralText else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                MissIndicator(misses.coerceAtMost(missLimit), limit = missLimit, dotSize = 10.dp)
            }
            Text(score.toString(), style = MolkkyBoardTheme.typography.scoreLarge, maxLines = 1, softWrap = false)
        }
        if (expanded) {
            members.forEach { member ->
                Text(
                    member,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 88.dp, bottom = 6.dp).semantics { contentDescription = member },
                )
            }
        }
    }
}

private const val ACTIVE_ALPHA = 0.32f
private const val OUT_ALPHA = 0.55f
