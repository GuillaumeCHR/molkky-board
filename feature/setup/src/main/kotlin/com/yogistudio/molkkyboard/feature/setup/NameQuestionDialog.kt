package com.yogistudio.molkkyboard.feature.setup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.yogistudio.molkkyboard.core.model.withoutUnsafeChars

/** Deux joueurs ne portent jamais le même nom : même joueur habituel, ou un autre nom (« Léa 2 », « Léa B. »). */
@Composable
internal fun NameQuestionDialog(question: NameQuestion, actions: SetupActions) {
    when (question) {
        is NameQuestion.SameProfile -> {
            val name = question.profile.name
            AlertDialog(
                onDismissRequest = actions.onDismissName,
                title = { Text(stringResource(R.string.setup_same_player_title, name)) },
                text = { Text(stringResource(R.string.setup_same_player_text, name)) },
                confirmButton = {
                    TextButton(onClick = { actions.onSameProfile(true) }) {
                        Text(stringResource(R.string.setup_same_player_yes, name))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { actions.onSameProfile(false) }) {
                        Text(stringResource(R.string.setup_same_player_no))
                    }
                },
            )
        }

        is NameQuestion.Rename -> {
            var name by rememberSaveable(question.taken) { mutableStateOf(question.suggestion) }
            AlertDialog(
                onDismissRequest = actions.onDismissName,
                title = {
                    Text(
                        stringResource(
                            if (question.inGame) R.string.setup_name_in_game else R.string.setup_name_other_player,
                            question.taken,
                        ),
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.setup_name_hint))
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it.withoutUnsafeChars().take(SetupUiState.MAX_NAME) },
                            singleLine = true,
                            isError = question.stillTaken,
                            supportingText =
                                if (question.stillTaken) {
                                    { Text(stringResource(R.string.setup_name_still_taken)) }
                                } else {
                                    null
                                },
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { actions.onConfirmName(name) }, enabled = name.isNotBlank()) {
                        Text(stringResource(R.string.setup_name_add))
                    }
                },
                dismissButton = {
                    TextButton(onClick = actions.onDismissName) { Text(stringResource(R.string.setup_name_cancel)) }
                },
            )
        }
    }
}
