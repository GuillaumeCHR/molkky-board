package com.yogistudio.molkkyboard.feature.setup

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.yogistudio.molkkyboard.core.data.PlayerProfile
import com.yogistudio.molkkyboard.core.designsystem.component.BigActionButton
import com.yogistudio.molkkyboard.core.designsystem.component.BigActionKind
import com.yogistudio.molkkyboard.core.designsystem.component.GuideBubble
import com.yogistudio.molkkyboard.core.designsystem.component.MBAvatar
import com.yogistudio.molkkyboard.core.designsystem.component.MBDragHandle
import com.yogistudio.molkkyboard.core.designsystem.component.MBTopBar
import com.yogistudio.molkkyboard.core.designsystem.component.guideAnchor
import com.yogistudio.molkkyboard.core.designsystem.component.rememberGuideAnchors
import com.yogistudio.molkkyboard.core.designsystem.icon.PLAYER_AVATARS
import com.yogistudio.molkkyboard.core.designsystem.icon.TEAM_EMBLEMS
import com.yogistudio.molkkyboard.core.designsystem.icon.emblemLabel
import com.yogistudio.molkkyboard.core.designsystem.theme.MBDimens
import com.yogistudio.molkkyboard.core.designsystem.theme.MolkkyBoardTheme
import com.yogistudio.molkkyboard.core.designsystem.theme.TeamColor
import com.yogistudio.molkkyboard.core.model.GameConfig
import com.yogistudio.molkkyboard.core.model.PlayerId
import com.yogistudio.molkkyboard.core.model.RulesMode
import com.yogistudio.molkkyboard.core.model.TeamId
import com.yogistudio.molkkyboard.core.model.TeamOverride
import com.yogistudio.molkkyboard.core.model.withoutUnsafeChars

/** Actions de l'écran de création (l'écran reste sans état, pour les aperçus et les captures). */
data class SetupActions(
    val onBack: () -> Unit = {},
    val onNameChange: (String) -> Unit = {},
    val onAdd: () -> Unit = {},
    val onAddProfile: (PlayerProfile) -> Unit = {},
    val onRemove: (PlayerId) -> Unit = {},
    val onMovePlayer: (Int, Int) -> Unit = { _, _ -> },
    val onAvatar: (PlayerId, String) -> Unit = { _, _ -> },
    val onPhoto: (PlayerId, Uri) -> Unit = { _, _ -> },
    val onRemovePhoto: (PlayerId) -> Unit = {},
    val captureUri: () -> Uri? = { null },
    val onShuffle: () -> Unit = {},
    val onPlayMode: (PlayMode) -> Unit = {},
    val onAddTeam: () -> Unit = {},
    val onRemoveTeam: (TeamId) -> Unit = {},
    val onRenameTeam: (TeamId, String) -> Unit = { _, _ -> },
    val onRandomName: (TeamId) -> Unit = {},
    val onTeamColor: (TeamId, Int) -> Unit = { _, _ -> },
    val onTeamEmblem: (TeamId, String) -> Unit = { _, _ -> },
    val onMoveTeam: (Int, Int) -> Unit = { _, _ -> },
    val onMoveMember: (TeamId, Int, Int) -> Unit = { _, _, _ -> },
    val onSwitchTeam: (PlayerId) -> Unit = {},
    val onAddToTeam: (TeamId, String) -> Unit = { _, _ -> },
    val onAddProfileToTeam: (TeamId, PlayerProfile) -> Unit = { _, _ -> },
    val onAcceptKnown: (TeamId) -> Unit = {},
    val onDismissKnown: (TeamId) -> Unit = {},
    val onMode: (RulesMode) -> Unit = {},
    val onImo: (Boolean) -> Unit = {},
    val onSetsToWin: (Int) -> Unit = {},
    val onSkipGuide: () -> Unit = {},
    val onPlayful: (GameConfig) -> Unit = {},
    val onFamily: (TeamId, TeamOverride) -> Unit = { _, _ -> },
    val onStart: () -> Unit = {},
)

/** Création de partie : joueurs ou équipes, ordre de passage par glisser-déposer, règles. */
@Composable
fun SetupScreen(
    onBack: () -> Unit,
    onGameCreated: (String) -> Unit,
    viewModel: SetupViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val names = stringArrayResource(R.array.team_names).toList()
    SetupContent(
        state = state,
        actions =
            SetupActions(
                onBack = onBack,
                onNameChange = viewModel::onNameChange,
                onAdd = { viewModel.addPlayer(names) },
                onAddProfile = { viewModel.addProfile(it, names) },
                onRemove = viewModel::removePlayer,
                onMovePlayer = viewModel::movePlayer,
                onAvatar = viewModel::setAvatar,
                onPhoto = viewModel::setPhoto,
                onRemovePhoto = viewModel::removePhoto,
                captureUri = viewModel::captureUri,
                onShuffle = viewModel::shuffle,
                onPlayMode = { viewModel.setPlayMode(it, names) },
                onAddTeam = { viewModel.addTeam(names) },
                onRemoveTeam = viewModel::removeTeam,
                onRenameTeam = viewModel::renameTeam,
                onRandomName = { viewModel.randomTeamName(it, names) },
                onTeamColor = viewModel::setTeamColor,
                onTeamEmblem = viewModel::setTeamEmblem,
                onMoveTeam = viewModel::moveTeam,
                onMoveMember = viewModel::moveMember,
                onSwitchTeam = viewModel::switchTeam,
                onAddToTeam = viewModel::addPlayerToTeam,
                onAddProfileToTeam = viewModel::addProfileToTeam,
                onAcceptKnown = viewModel::acceptKnownTeam,
                onDismissKnown = viewModel::dismissKnownTeam,
                onMode = viewModel::setMode,
                onImo = viewModel::setImo,
                onSetsToWin = viewModel::setSetsToWin,
                onSkipGuide = viewModel::skipGuide,
                onPlayful = viewModel::setPlayful,
                onFamily = viewModel::setFamily,
                onStart = { viewModel.start(onGameCreated) },
            ),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupContent(state: SetupUiState, actions: SetupActions) {
    var editingPlayful by rememberSaveable { mutableStateOf(false) }
    var avatarFor by remember { mutableStateOf<PlayerId?>(null) }
    val anchors = rememberGuideAnchors()
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().imePadding()) {
            MBTopBar(stringResource(R.string.setup_title), actions.onBack)
            Column(
                modifier =
                    Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = MBDimens.ScreenPadding),
                verticalArrangement = Arrangement.spacedBy(MBDimens.Gap),
            ) {
                Label(R.string.setup_players)
                PlayModeSelector(state.playMode, actions.onPlayMode)
                // En équipes, on ajoute les joueurs directement dans chaque équipe (retour du test sur téléphone).
                if (state.playMode == PlayMode.SOLO) {
                    AddPlayerRow(state, actions, Modifier.guideAnchor(anchors, GUIDE_ADD))
                    if (state.profiles.isNotEmpty()) KnownProfiles(state.profiles, actions.onAddProfile)
                } else {
                    Hint(R.string.setup_teams_hint)
                }
                when (state.playMode) {
                    PlayMode.SOLO -> SoloOrder(state, actions, onAvatar = { avatarFor = it })
                    PlayMode.TEAMS -> TeamsOrder(state, actions, onAvatar = { avatarFor = it })
                }
                if (state.units.size >= 2) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Hint(R.string.setup_order_hint, Modifier.weight(1f))
                        TextButton(onClick = actions.onShuffle, modifier = Modifier.heightIn(min = MBDimens.MinTouch)) {
                            Text(stringResource(R.string.setup_shuffle))
                        }
                    }
                }
                RulesSection(state, actions, onEditPlayful = { editingPlayful = true })
            }
            // Le bouton de lancement reste visible en bas et devient évident dès deux joueurs.
            Column(
                modifier = Modifier.navigationBarsPadding().padding(MBDimens.ScreenPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (!state.canStart) {
                    Hint(
                        if (state.playMode ==
                            PlayMode.SOLO
                        ) {
                            R.string.setup_need_two
                        } else {
                            R.string.setup_need_two_teams
                        },
                    )
                }
                BigActionButton(
                    text = stringResource(R.string.setup_start),
                    onClick = actions.onStart,
                    kind = BigActionKind.Primary,
                    enabled = state.canStart,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        // Prise en main, étape 1 sur 4 : tant qu'il manque des joueurs.
        if (state.guide && !state.canStart) {
            GuideBubble(
                text = stringResource(R.string.setup_guide),
                step = 1,
                steps = GUIDE_STEPS,
                anchor = anchors.of(GUIDE_ADD).takeIf { state.playMode == PlayMode.SOLO },
                onSkip = actions.onSkipGuide,
            )
        }
    }
    if (editingPlayful) {
        ModalBottomSheet(onDismissRequest = { editingPlayful = false }, dragHandle = { MBDragHandle() }) {
            Column(
                modifier =
                    Modifier
                        .verticalScroll(rememberScrollState())
                        .navigationBarsPadding()
                        .padding(horizontal = MBDimens.ScreenPadding)
                        .padding(bottom = MBDimens.ScreenPadding),
                verticalArrangement = Arrangement.spacedBy(MBDimens.Gap),
            ) {
                Text(stringResource(R.string.setup_rules_playful_sheet), style = MaterialTheme.typography.titleLarge)
                Text(playfulSummary(state.playful), style = MaterialTheme.typography.titleSmall)
                PlayfulRulesEditor(state.playful, actions.onPlayful)
            }
        }
    }
    avatarFor?.let { id -> state.player(id)?.let { AvatarSheet(it, actions, onDismiss = { avatarFor = null }) } }
}

private const val GUIDE_ADD = "add_player"
private const val GUIDE_STEPS = 4

@Composable
private fun PlayModeSelector(mode: PlayMode, onChange: (PlayMode) -> Unit) {
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        PlayMode.entries.forEachIndexed { index, entry ->
            SegmentedButton(
                selected = mode == entry,
                onClick = { onChange(entry) },
                shape = SegmentedButtonDefaults.itemShape(index, PlayMode.entries.size),
                modifier = Modifier.heightIn(min = MBDimens.MinTouch),
                icon = {},
            ) {
                Text(stringResource(if (entry == PlayMode.SOLO) R.string.setup_solo else R.string.setup_teams))
            }
        }
    }
}

@Composable
private fun AddPlayerRow(state: SetupUiState, actions: SetupActions, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MBDimens.SmallGap),
    ) {
        OutlinedTextField(
            value = state.draftName,
            onValueChange = actions.onNameChange,
            label = { Text(stringResource(R.string.setup_player_name)) },
            singleLine = true,
            keyboardOptions =
                KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Done,
                ),
            keyboardActions = KeyboardActions(onDone = { actions.onAdd() }),
            modifier = Modifier.weight(1f),
        )
        TextButton(
            onClick = actions.onAdd,
            enabled = state.canAdd,
            modifier = Modifier.heightIn(min = MBDimens.MinTouch),
        ) {
            Text(stringResource(R.string.setup_add))
        }
    }
}

/** Joueurs habituels (profils locaux), ajoutés d'un tap. */
@Composable
private fun KnownProfiles(profiles: List<PlayerProfile>, onAdd: (PlayerProfile) -> Unit) {
    Hint(R.string.setup_known_players)
    val colors = MolkkyBoardTheme.colors.teams
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(MBDimens.SmallGap),
        verticalArrangement = Arrangement.spacedBy(MBDimens.SmallGap),
    ) {
        profiles.forEach { profile ->
            val label = stringResource(R.string.setup_add_known, profile.name)
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.clickable(onClickLabel = label) { onAdd(profile) },
            ) {
                Row(
                    Modifier.heightIn(min = 48.dp).padding(start = 6.dp, end = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Avatar(
                        profile.avatar ?: PLAYER_AVATARS.first(),
                        profile.photoUri,
                        colors[
                            profile.colorIndex %
                                colors.size,
                        ],
                        36.dp,
                    )
                    Text("+ ${profile.name}", style = MaterialTheme.typography.titleSmall)
                }
            }
        }
    }
}

@Composable
private fun SoloOrder(state: SetupUiState, actions: SetupActions, onAvatar: (PlayerId) -> Unit) {
    ReorderableColumn(
        items = state.players,
        key = { it.id.value },
        label = { it.name },
        onMove = actions.onMovePlayer,
    ) { index, player, handle ->
        PlayerRow(
            player = player,
            position = index + 1,
            handle = handle,
            onAvatar = { onAvatar(player.id) },
            onRemove = { actions.onRemove(player.id) },
        )
    }
}

@Composable
private fun PlayerRow(
    player: SetupPlayer,
    position: Int?,
    handle: Modifier,
    onAvatar: () -> Unit,
    onRemove: () -> Unit,
    extra: @Composable () -> Unit = {},
) {
    val colors = MolkkyBoardTheme.colors.teams
    Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surface) {
        Row(Modifier.padding(end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            DragHandle(handle)
            if (position !=
                null
            ) {
                Text(
                    "$position.",
                    style = MolkkyBoardTheme.typography.scoreSmall,
                    modifier = Modifier.padding(end = 8.dp),
                )
            }
            val avatarLabel = stringResource(R.string.setup_change_avatar, player.name)
            Box(
                Modifier
                    .clip(CircleShape)
                    .clickable(onClickLabel = avatarLabel, onClick = onAvatar)
                    .semantics { contentDescription = avatarLabel },
            ) {
                Avatar(player.avatar, player.photoUri, colors[player.colorIndex % colors.size], 44.dp)
            }
            Text(
                player.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
            )
            extra()
            val removeLabel = stringResource(R.string.setup_remove, player.name)
            IconButton(
                onClick = onRemove,
                modifier =
                    Modifier.size(MBDimens.MinTouch).semantics {
                        contentDescription =
                            removeLabel
                    },
            ) {
                Text(
                    "✕",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** Avatar d'un joueur : sa photo si elle existe, sinon son emblème au trait sur un disque à sa couleur. */
@Composable
fun Avatar(emblem: String, photoUri: String?, color: TeamColor, size: Dp) {
    if (photoUri != null) {
        AsyncImage(
            model = photoUri,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(size).clip(CircleShape),
        )
    } else {
        MBAvatar(emblem, name = "", color = color, size = size)
    }
}

@Composable
private fun TeamsOrder(state: SetupUiState, actions: SetupActions, onAvatar: (PlayerId) -> Unit) {
    ReorderableColumn(
        items = state.teams,
        key = { it.id.value },
        label = { it.name },
        onMove = actions.onMoveTeam,
        spacing = MBDimens.Gap,
    ) { index, team, handle ->
        TeamCard(
            team = team,
            position = index + 1,
            state = state,
            actions = actions,
            handle = handle,
            canRemove = state.teams.size > 2,
            onAvatar = onAvatar,
        )
    }
    TextButton(onClick = actions.onAddTeam, modifier = Modifier.heightIn(min = MBDimens.MinTouch)) {
        Text(stringResource(R.string.setup_add_team))
    }
}

@Composable
private fun TeamCard(
    team: SetupTeam,
    position: Int,
    state: SetupUiState,
    actions: SetupActions,
    handle: Modifier,
    canRemove: Boolean,
    onAvatar: (PlayerId) -> Unit,
) {
    val colors = MolkkyBoardTheme.colors.teams
    val color = colors[team.colorIndex % colors.size]
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(3.dp, color.fill),
    ) {
        Column(Modifier.padding(bottom = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.padding(end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                DragHandle(handle)
                Text("$position.", style = MolkkyBoardTheme.typography.scoreSmall)
                val emblemLabel = stringResource(R.string.setup_change_emblem, team.name)
                val keys = TEAM_EMBLEMS
                IconButton(
                    onClick = { actions.onTeamEmblem(team.id, keys[(keys.indexOf(team.emblem) + 1) % keys.size]) },
                    modifier = Modifier.size(MBDimens.MinTouch).semantics { contentDescription = emblemLabel },
                ) {
                    val teamColors = MolkkyBoardTheme.colors.teams
                    MBAvatar(team.emblem, team.name, teamColors[team.colorIndex % teamColors.size], EMBLEM_SIZE)
                }
                OutlinedTextField(
                    value = team.name,
                    onValueChange = { actions.onRenameTeam(team.id, it) },
                    label = { Text(stringResource(R.string.setup_team_name)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    modifier = Modifier.weight(1f),
                )
                val diceLabel = stringResource(R.string.setup_random_name)
                IconButton(
                    onClick = { actions.onRandomName(team.id) },
                    modifier =
                        Modifier.size(MBDimens.MinTouch).semantics {
                            contentDescription =
                                diceLabel
                        },
                ) {
                    Text("🎲", style = MaterialTheme.typography.headlineSmall)
                }
            }
            ColorPicker(team.colorIndex) { actions.onTeamColor(team.id, it) }
            team.known?.let { known ->
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MolkkyBoardTheme.colors.saffron.copy(alpha = KNOWN_ALPHA),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                ) {
                    Column(Modifier.padding(12.dp)) {
                        val names = team.members.mapNotNull { state.player(it)?.name }
                        Text(
                            stringResource(
                                R.string.setup_known_team,
                                namesList(names),
                                known.name,
                            ),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Row {
                            TextButton(
                                onClick = { actions.onAcceptKnown(team.id) },
                            ) { Text(stringResource(R.string.setup_known_accept, known.name)) }
                            TextButton(
                                onClick = { actions.onDismissKnown(team.id) },
                            ) { Text(stringResource(R.string.setup_known_dismiss, team.name)) }
                        }
                    }
                }
            }
            val members = team.members.mapNotNull(state::player)
            if (members.isEmpty()) {
                Hint(R.string.setup_team_empty, Modifier.padding(horizontal = 16.dp))
            }
            ReorderableColumn(
                items = members,
                key = { it.id.value },
                label = { it.name },
                onMove = { from, to -> actions.onMoveMember(team.id, from, to) },
                modifier = Modifier.padding(horizontal = 8.dp),
            ) { _, player, memberHandle ->
                PlayerRow(
                    player = player,
                    position = null,
                    handle = memberHandle,
                    onAvatar = { onAvatar(player.id) },
                    onRemove = { actions.onRemove(player.id) },
                ) {
                    if (state.teams.size > 1) {
                        val label = stringResource(R.string.setup_switch_team, player.name)
                        IconButton(
                            onClick = { actions.onSwitchTeam(player.id) },
                            modifier = Modifier.size(MBDimens.MinTouch).semantics { contentDescription = label },
                        ) {
                            Text("⇄", style = MaterialTheme.typography.titleLarge)
                        }
                    }
                }
            }
            TeamAddRow(team, state, actions)
            if (canRemove) {
                TextButton(
                    onClick = { actions.onRemoveTeam(team.id) },
                    modifier = Modifier.padding(horizontal = 8.dp),
                ) {
                    Text(stringResource(R.string.setup_remove_team), color = MolkkyBoardTheme.colors.coralText)
                }
            }
        }
    }
}

/** « Alice, Bruno et Chloé ». */
@Composable
private fun namesList(names: List<String>): String =
    if (names.size <
        2
    ) {
        names.joinToString()
    } else {
        names.dropLast(1).joinToString(", ") + stringResource(R.string.setup_and) +
            names.last()
    }

/** Ajout d'un joueur directement dans cette équipe : nouveau nom, ou joueur habituel en un tap. */
@Composable
private fun TeamAddRow(team: SetupTeam, state: SetupUiState, actions: SetupActions) {
    var name by rememberSaveable(team.id.value) { mutableStateOf("") }
    val add = {
        actions.onAddToTeam(team.id, name)
        name = ""
    }
    Row(
        Modifier.padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MBDimens.SmallGap),
    ) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it.withoutUnsafeChars().take(SetupUiState.MAX_NAME) },
            label = { Text(stringResource(R.string.setup_team_add_player)) },
            singleLine = true,
            keyboardOptions =
                KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Done,
                ),
            keyboardActions = KeyboardActions(onDone = { add() }),
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = add, enabled = name.isNotBlank(), modifier = Modifier.heightIn(min = MBDimens.MinTouch)) {
            Text(stringResource(R.string.setup_add))
        }
    }
    if (state.profiles.isNotEmpty()) {
        val colors = MolkkyBoardTheme.colors.teams
        FlowRow(
            Modifier.padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            state.profiles.forEach { profile ->
                val label = stringResource(R.string.setup_add_known_to_team, profile.name, team.name)
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier =
                        Modifier.clickable(
                            onClickLabel = label,
                        ) { actions.onAddProfileToTeam(team.id, profile) },
                ) {
                    Row(
                        Modifier.heightIn(min = 44.dp).padding(start = 4.dp, end = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Avatar(
                            profile.avatar ?: PLAYER_AVATARS.first(),
                            profile.photoUri,
                            colors[
                                profile.colorIndex %
                                    colors.size,
                            ],
                            32.dp,
                        )
                        Text("+ ${profile.name}", style = MaterialTheme.typography.titleSmall)
                    }
                }
            }
        }
    }
}

/** Les 8 couleurs d'équipe, en pastilles (un choix unique, lu comme des boutons radio). */
@Composable
private fun ColorPicker(current: Int, onSelect: (Int) -> Unit) {
    val colors = MolkkyBoardTheme.colors.teams
    // Cibles de 56 dp (zones tactiles de la spec) : les pastilles passent à la ligne si la carte est étroite.
    FlowRow(Modifier.padding(horizontal = 8.dp)) {
        colors.forEachIndexed { index, color ->
            val label = stringResource(R.string.setup_color, index + 1)
            Box(
                Modifier
                    .size(MBDimens.MinTouch)
                    .clip(CircleShape)
                    .selectable(selected = index == current, role = Role.RadioButton) { onSelect(index) }
                    .semantics { contentDescription = label },
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier
                        .size(if (index == current) 30.dp else 24.dp)
                        .background(color.fill, CircleShape)
                        .then(
                            if (index == current) {
                                Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                            } else {
                                Modifier
                            },
                        ),
                )
            }
        }
    }
}

@Composable
private fun RulesSection(state: SetupUiState, actions: SetupActions, onEditPlayful: () -> Unit) {
    Label(R.string.setup_rules)
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        RulesMode.entries.forEachIndexed { index, mode ->
            SegmentedButton(
                selected = state.mode == mode,
                onClick = { actions.onMode(mode) },
                shape = SegmentedButtonDefaults.itemShape(index, RulesMode.entries.size),
                modifier = Modifier.heightIn(min = MBDimens.MinTouch),
                icon = {},
            ) {
                Text(
                    stringResource(
                        if (mode ==
                            RulesMode.OFFICIAL
                        ) {
                            R.string.setup_rules_official
                        } else {
                            R.string.setup_rules_playful
                        },
                    ),
                )
            }
        }
    }
    if (state.mode == RulesMode.OFFICIAL) {
        Hint(R.string.setup_rules_official_hint)
        SwitchCard(
            title = stringResource(R.string.setup_rules_imo),
            hint = stringResource(R.string.setup_rules_imo_hint),
            checked = state.imo,
            onChange = actions.onImo,
        )
    } else {
        Hint(R.string.setup_rules_playful_hint)
        Text(playfulSummary(state.playful), style = MaterialTheme.typography.titleSmall)
        TextButton(onClick = onEditPlayful, modifier = Modifier.heightIn(min = MBDimens.MinTouch)) {
            Text(stringResource(R.string.setup_rules_playful_edit))
        }
        FamilySection(state, actions)
    }
    FormatSection(state, actions)
}

/** Une manche, ou un match en 2 manches gagnantes : la belle se départage aux points (Mölkkout si égalité). */
@Composable
private fun FormatSection(state: SetupUiState, actions: SetupActions) {
    Label(R.string.setup_format)
    val options = listOf(1 to R.string.setup_format_single, 2 to R.string.setup_format_match)
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        options.forEachIndexed { index, (sets, label) ->
            SegmentedButton(
                selected = state.setsToWin == sets,
                onClick = { actions.onSetsToWin(sets) },
                shape = SegmentedButtonDefaults.itemShape(index, options.size),
                modifier = Modifier.heightIn(min = MBDimens.MinTouch),
                icon = {},
            ) { Text(stringResource(label)) }
        }
    }
    if (state.setsToWin >
        1
    ) {
        Hint(
            if (state.mode == RulesMode.OFFICIAL &&
                state.imo
            ) {
                R.string.setup_format_match_imo_hint
            } else {
                R.string.setup_format_match_hint
            },
        )
    }
}

/** Mode famille (Ludique) : score de départ, objectif réduit et exemption d'élimination par joueur ou équipe. */
@Composable
private fun FamilySection(state: SetupUiState, actions: SetupActions) {
    var open by rememberSaveable { mutableStateOf(state.family.isNotEmpty()) }
    SwitchCard(
        title = stringResource(R.string.setup_family),
        hint = stringResource(R.string.setup_family_hint),
        checked = open,
        onChange = { open = it },
    )
    if (!open) return
    Hint(R.string.setup_family_distance)
    val target = state.playful.targetScore
    state.units.forEach { unit ->
        val override = state.family[unit.id] ?: TeamOverride()
        val unitTarget = override.targetScore ?: target
        Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surface) {
            Column(Modifier.padding(vertical = 8.dp)) {
                Text(
                    unit.name,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
                Stepper(
                    label = R.string.setup_family_start,
                    value = override.startScore ?: 0,
                    canDecrease = (override.startScore ?: 0) > 0,
                    canIncrease = (override.startScore ?: 0) + FAMILY_STEP < unitTarget,
                    onDecrease = {
                        actions.onFamily(
                            unit.id,
                            override.withStartScore((override.startScore ?: 0) - FAMILY_STEP, target),
                        )
                    },
                    onIncrease = {
                        actions.onFamily(
                            unit.id,
                            override.withStartScore((override.startScore ?: 0) + FAMILY_STEP, target),
                        )
                    },
                )
                Stepper(
                    label = R.string.setup_family_target,
                    value = unitTarget,
                    canDecrease = unitTarget > PlayfulRules.MIN_TARGET,
                    canIncrease = unitTarget < PlayfulRules.MAX_TARGET,
                    onDecrease = { actions.onFamily(unit.id, override.withTarget(unitTarget - FAMILY_STEP, target)) },
                    onIncrease = { actions.onFamily(unit.id, override.withTarget(unitTarget + FAMILY_STEP, target)) },
                )
                SwitchCard(
                    title = stringResource(R.string.setup_family_exempt),
                    hint = null,
                    checked = override.exemptFromMissLimit,
                    onChange = { actions.onFamily(unit.id, override.copy(exemptFromMissLimit = it)) },
                )
            }
        }
    }
}

/** Choix de l'avatar : un emoji, ou une photo (appareil photo ou galerie), jamais obligatoire. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AvatarSheet(player: SetupPlayer, actions: SetupActions, onDismiss: () -> Unit) {
    var capture by remember { mutableStateOf<Uri?>(null) }
    val camera =
        rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
            capture?.takeIf { saved }?.let { actions.onPhoto(player.id, it) }
            onDismiss()
        }
    val gallery =
        rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            uri?.let { actions.onPhoto(player.id, it) }
            onDismiss()
        }
    ModalBottomSheet(onDismissRequest = onDismiss, dragHandle = { MBDragHandle() }) {
        Column(
            Modifier
                .navigationBarsPadding()
                .padding(
                    horizontal = MBDimens.ScreenPadding,
                ).padding(bottom = MBDimens.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(MBDimens.Gap),
        ) {
            Text(stringResource(R.string.setup_avatar_title, player.name), style = MaterialTheme.typography.titleLarge)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                val playerColor = MolkkyBoardTheme.colors.teams.let { it[player.colorIndex % it.size] }
                PLAYER_AVATARS.forEach { emblem ->
                    val chosen = emblem == player.avatar && player.photoUri == null
                    val label = emblemLabel(emblem)
                    Box(
                        Modifier
                            .size(MBDimens.MinTouch)
                            .clip(CircleShape)
                            .background(
                                if (chosen) {
                                    MolkkyBoardTheme.colors.saffron.copy(
                                        alpha = KNOWN_ALPHA,
                                    )
                                } else {
                                    MaterialTheme.colorScheme.surface
                                },
                            ).clickable(onClickLabel = label) {
                                actions.onAvatar(player.id, emblem)
                                if (player.photoUri != null) actions.onRemovePhoto(player.id)
                                onDismiss()
                            }.semantics {
                                selected = chosen
                                contentDescription = label
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        MBAvatar(emblem, player.name, playerColor, AVATAR_CHOICE)
                    }
                }
            }
            Hint(R.string.setup_photo_hint)
            Row(horizontalArrangement = Arrangement.spacedBy(MBDimens.SmallGap)) {
                TextButton(onClick = {
                    capture = actions.captureUri()
                    capture?.let(camera::launch)
                }) { Text(stringResource(R.string.setup_photo_camera)) }
                TextButton(onClick = {
                    gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }) { Text(stringResource(R.string.setup_photo_gallery)) }
            }
            if (player.photoUri != null) {
                TextButton(onClick = {
                    actions.onRemovePhoto(player.id)
                    onDismiss()
                }) { Text(stringResource(R.string.setup_photo_remove), color = MolkkyBoardTheme.colors.coralText) }
            }
        }
    }
}

@Composable
private fun Hint(text: Int, modifier: Modifier = Modifier) {
    Text(
        stringResource(text),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}

@Composable
private fun Label(text: Int) {
    Text(
        stringResource(text).uppercase(),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 8.dp),
    )
}

private const val KNOWN_ALPHA = 0.3f
private const val FAMILY_STEP = 5

private val EMBLEM_SIZE = 36.dp
private val AVATAR_CHOICE = 44.dp
