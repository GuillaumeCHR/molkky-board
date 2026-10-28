package com.yogistudio.molkkyboard.feature.game

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.yogistudio.molkkyboard.core.designsystem.theme.MBDimens
import com.yogistudio.molkkyboard.core.designsystem.theme.MolkkyBoardTheme

/**
 * Partie à côté : « Inviter des téléphones » (ou « Téléphones de la partie » une fois le partage lancé) chez
 * l'organisateur, « Quitter la partie partagée » chez un invité connecté.
 */
@Composable
internal fun SharingItems(
    state: GameUiState,
    actions: GameActions,
    connection: ConnectionBadge?,
    onLeave: () -> Unit,
    close: () -> Unit,
) {
    val invite = actions.onInvite
    when {
        state.joined && connection != null -> {
            DropdownMenuItem(text = { Text(stringResource(R.string.game_menu_leave)) }, onClick = {
                close()
                onLeave()
            })
        }

        !state.joined && invite != null -> {
            DropdownMenuItem(text = { Text(stringResource(R.string.game_menu_phones)) }, onClick = {
                close()
                invite()
            })
        }
    }
}

@Composable
internal fun LeaveDialog(onLeave: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.game_leave_title)) },
        text = { Text(stringResource(R.string.game_leave_text)) },
        confirmButton = {
            TextButton(onClick = {
                onDismiss()
                onLeave()
            }) { Text(stringResource(R.string.game_leave_confirm)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.log_cancel)) } },
    )
}

/** Partie à côté : nombre de téléphones connectés (hôte) ou état de la connexion (invité). */
data class ConnectionBadge(
    /** Téléphones connectés (connu de l'hôte seulement). */
    val phones: Int?,
    val online: Boolean,
    /** Invité : joueur dont ce téléphone saisit les lancers (`null` : il ne fait que suivre). */
    val player: String? = null,
)

/**
 * Invité : il saisit les lancers de tout le monde (joueur ou arbitre), sauf quand la partie n'est plus partagée
 * (lecture seule) ou que la connexion est perdue.
 */
internal fun GameUiState.forGuest(connection: ConnectionBadge?): GameUiState =
    if (!joined) {
        this
    } else {
        copy(inputLocked = connection == null || !connection.online)
    }

/** Invité : qui saisit quoi, dit en clair sous la barre de titre. */
@Composable
internal fun GuestBanner(state: GameUiState, connection: ConnectionBadge?, onLeave: () -> Unit) {
    if (!state.joined) return
    var leaving by remember { mutableStateOf(false) }
    val name = state.currentPlayerName.orEmpty()
    val text =
        when {
            connection == null -> stringResource(R.string.guest_read_only)
            state.finished -> stringResource(R.string.guest_finished)
            !connection.online -> stringResource(R.string.guest_reconnecting)
            connection.player == null -> stringResource(R.string.guest_watching)
            connection.player == state.currentPlayerId -> stringResource(R.string.guest_your_turn, name)
            else -> stringResource(R.string.guest_other_turn, name)
        }
    val mine = connection?.online == true && connection.player == state.currentPlayerId
    Surface(
        color = if (mine) MolkkyBoardTheme.colors.saffron else MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(16.dp),
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = MBDimens.ScreenPadding)
                .semantics { liveRegion = LiveRegionMode.Polite },
    ) {
        Column(Modifier.padding(start = 12.dp, top = 12.dp, end = 4.dp)) {
            Text(text, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(end = 8.dp))
            // On peut quitter à tout moment ; la partie reste consultable sur ce téléphone.
            if (connection != null) {
                TextButton(
                    onClick = { leaving = true },
                    modifier = Modifier.align(Alignment.End).heightIn(min = MBDimens.MinTouch),
                ) { Text(stringResource(R.string.game_menu_leave)) }
            }
        }
    }
    if (leaving) LeaveDialog(onLeave = onLeave, onDismiss = { leaving = false })
}

/** « 3 téléphones », ou « Hors ligne » en corail quand un invité a perdu l'hôte. */
@Composable
internal fun ConnectionPill(badge: ConnectionBadge, onClick: (() -> Unit)?) {
    val phones = badge.phones
    val text =
        when {
            !badge.online -> stringResource(R.string.game_offline)
            phones != null -> pluralStringResource(R.plurals.game_phones, phones, phones)
            else -> stringResource(R.string.game_connected)
        }
    val phonesLabel = stringResource(R.string.game_menu_phones)
    Surface(
        shape = RoundedCornerShape(50),
        color =
            if (badge.online) {
                MolkkyBoardTheme.colors.saffron.copy(
                    alpha = PILL_ALPHA,
                )
            } else {
                MolkkyBoardTheme.colors.coral
            },
        contentColor = if (badge.online) MaterialTheme.colorScheme.onSurface else MolkkyBoardTheme.colors.onCoral,
        modifier =
            Modifier
                .then(
                    if (onClick !=
                        null
                    ) {
                        Modifier.clickable(onClickLabel = phonesLabel, onClick = onClick)
                    } else {
                        Modifier
                    },
                ).semantics { liveRegion = LiveRegionMode.Polite },
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

private const val PILL_ALPHA = 0.45f
