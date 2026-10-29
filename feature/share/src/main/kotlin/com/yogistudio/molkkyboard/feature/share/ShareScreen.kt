package com.yogistudio.molkkyboard.feature.share

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yogistudio.molkkyboard.core.designsystem.component.BigActionButton
import com.yogistudio.molkkyboard.core.designsystem.component.BigActionKind
import com.yogistudio.molkkyboard.core.designsystem.component.MBTopBar
import com.yogistudio.molkkyboard.core.designsystem.theme.MBDimens
import com.yogistudio.molkkyboard.core.stats.Moment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Actions de l'écran de partage (l'écran reste sans état pour les captures). */
data class ShareActions(
    val onBack: () -> Unit = {},
    val onMoment: (Int) -> Unit = {},
    val onTemplate: (ShareTemplate) -> Unit = {},
    val onFormat: (ShareFormat) -> Unit = {},
    val onText: (String) -> Unit = {},
    val onPhoto: (ImageBitmap) -> Unit = {},
    val onRemovePhoto: () -> Unit = {},
)

@Composable
fun ShareScreen(onBack: () -> Unit, viewModel: ShareViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ShareContentScreen(
        state = state,
        actions =
            ShareActions(
                onBack = onBack,
                onMoment = viewModel::selectMoment,
                onTemplate = viewModel::selectTemplate,
                onFormat = viewModel::selectFormat,
                onText = viewModel::editText,
                onPhoto = viewModel::setPhoto,
                onRemovePhoto = viewModel::removePhoto,
            ),
    )
}

@Composable
fun ShareContentScreen(state: ShareUiState, actions: ShareActions) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val layer = rememberGraphicsLayer()
    val snapshot = state.snapshot
    val moment = state.moment
    Column(Modifier.fillMaxSize()) {
        MBTopBar(stringResource(R.string.share_title), actions.onBack)
        if (snapshot == null || moment == null) {
            if (!state.loading) Text(stringResource(R.string.share_nothing), Modifier.padding(MBDimens.ScreenPadding))
            return@Column
        }
        val content = snapshot.shareContent(moment, state.victory, state.avatars).copy(photo = state.photo)
        val text = state.choices.text ?: defaultText(context, content)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = MBDimens.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(MBDimens.SmallGap),
        ) {
            ChipRow {
                state.moments.forEachIndexed { index, m ->
                    FilterChip(
                        index == state.choices.momentIndex,
                        { actions.onMoment(index) },
                        label = { Text(momentLabel(m)) },
                    )
                }
            }
            TemplatePager(state, content, layer, actions)
            PhotoButtons(state.photo != null, actions)
            ChipRow {
                ShareFormat.entries.forEach { format ->
                    FilterChip(state.choices.format == format, {
                        actions.onFormat(format)
                    }, label = { Text(stringResource(format.label())) })
                }
            }
            OutlinedTextField(
                value = text,
                onValueChange = actions.onText,
                label = { Text(stringResource(R.string.share_text_label)) },
                modifier = Modifier.fillMaxWidth(),
            )
            val title = momentLabel(moment)
            val key = keyFigure(content)
            val resources = LocalResources.current
            val reel =
                remember(content, title, key, resources) {
                    snapshot.reelData(
                        content,
                        title,
                        key,
                        ReelLabels(
                            round = { resources.getString(R.string.reel_round, it) },
                            reset = resources.getString(R.string.reel_reset),
                            eliminated = resources.getString(R.string.reel_eliminated),
                            scan = resources.getString(R.string.card_scan),
                            hashtag = resources.getString(R.string.card_hashtag),
                        ),
                    )
                }
            ReelSection(reel, text)
        }
        ShareButtons(context, layer, text, onSaved = {})
    }
}

/**
 * Photo du moment (N1) : appareil photo du système ou sélecteur de photos, sans permission. La photo reste en mémoire,
 * le fichier de prise de vue est effacé dès qu'il est lu.
 */
@Composable
private fun PhotoButtons(hasPhoto: Boolean, actions: ShareActions) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val load: (Uri?) -> Unit = { uri ->
        if (uri != null) scope.launch { MomentPhoto.load(context, uri)?.let(actions.onPhoto) }
    }
    var capture by remember { mutableStateOf<Uri?>(null) }
    val camera =
        rememberTakePicture { taken ->
            if (taken) load(capture)
        }
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia(), load)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(
            onClick = {
                capture = MomentPhoto.captureUri(context).also(camera)
            },
            modifier = Modifier.heightIn(min = MBDimens.MinTouch),
        ) { Text(stringResource(R.string.share_photo_camera)) }
        OutlinedButton(
            onClick = { gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
            modifier = Modifier.heightIn(min = MBDimens.MinTouch),
        ) { Text(stringResource(R.string.share_photo_gallery)) }
        if (hasPhoto) {
            TextButton(onClick = actions.onRemovePhoto, modifier = Modifier.heightIn(min = MBDimens.MinTouch)) {
                Text(stringResource(R.string.share_photo_remove))
            }
        }
    }
    Text(
        stringResource(R.string.share_photo_hint),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun ChipRow(content: @Composable () -> Unit) {
    Row(
        Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) { content() }
}

/** Aperçu : un modèle par page, d'un glissement ; la vignette visible est enregistrée dans [layer] pour le partage. */
@Composable
private fun TemplatePager(state: ShareUiState, content: ShareContent, layer: GraphicsLayer, actions: ShareActions) {
    // Le modèle « Photo » n'existe qu'une fois la photo du moment choisie.
    val templates = ShareTemplate.entries.filter { it != ShareTemplate.PHOTO || state.photo != null }
    val selected = templates.indexOf(state.template).coerceAtLeast(0)
    val pager = rememberPagerState(initialPage = selected) { templates.size }
    LaunchedEffect(selected, templates.size) {
        if (pager.currentPage != selected) pager.scrollToPage(selected)
    }
    LaunchedEffect(pager.currentPage) {
        if (pager.currentPage != selected) actions.onTemplate(templates[pager.currentPage])
    }
    val format = state.choices.format
    val description = stringResource(R.string.share_preview_description, keyFigure(content))
    HorizontalPager(
        pager,
        Modifier.fillMaxWidth().height(PREVIEW_HEIGHT.dp).semantics {
            contentDescription =
                description
        },
    ) { page ->
        BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            val density = LocalDensity.current
            val scale =
                minOf(
                    with(density) { maxWidth.toPx() } / format.width,
                    with(density) { maxHeight.toPx() } / format.height,
                )
            Box(
                Modifier.graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                },
            ) {
                ShareCard(
                    content = content,
                    template = templates[page],
                    format = format,
                    modifier =
                        if (page == pager.currentPage) {
                            Modifier.drawWithContent {
                                layer.record { this@drawWithContent.drawContent() }
                                drawLayer(layer)
                            }
                        } else {
                            Modifier
                        },
                )
            }
        }
    }
    Text(
        stringResource(R.string.share_template_hint),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun ShareButtons(context: Context, layer: GraphicsLayer, text: String, onSaved: () -> Unit) {
    val scope = rememberCoroutineScope()
    val title = stringResource(R.string.share_chooser)
    val whatsapp = ShareFiles.isInstalled(context, ShareFiles.WHATSAPP)

    fun share(target: String?) {
        scope.launch {
            val bitmap = layer.toImageBitmap().asAndroidBitmap()
            val file = withContext(Dispatchers.IO) { ShareFiles.writePng(context, bitmap, fileName()) }
            context.startActivity(
                ShareFiles.shareIntent(ShareFiles.uriFor(context, file), "image/png", text, title, target),
            )
        }
    }
    Column(
        Modifier.fillMaxWidth().navigationBarsPadding().padding(MBDimens.ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(MBDimens.SmallGap),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BigActionButton(
            stringResource(R.string.share_button),
            { share(null) },
            BigActionKind.Primary,
            Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (whatsapp) {
                TextButton(
                    onClick = { share(ShareFiles.WHATSAPP) },
                ) { Text(stringResource(R.string.share_whatsapp)) }
            }
            if (ShareFiles.canSaveToGallery) {
                TextButton(onClick = {
                    scope.launch {
                        val bitmap = layer.toImageBitmap().asAndroidBitmap()
                        val ok = withContext(Dispatchers.IO) { ShareFiles.saveToGallery(context, bitmap, fileName()) }
                        Toast
                            .makeText(
                                context,
                                if (ok) R.string.share_saved else R.string.share_save_failed,
                                Toast.LENGTH_SHORT,
                            ).show()
                        if (ok) onSaved()
                    }
                }) { Text(stringResource(R.string.share_save)) }
            }
        }
    }
}

private fun fileName(): String = "molkky-board-${System.currentTimeMillis()}"

@Composable
private fun momentLabel(moment: Moment): String =
    stringResource(
        when (moment) {
            is Moment.Victory -> R.string.moment_victory
            is Moment.Perfect -> R.string.moment_perfect
            is Moment.Record -> R.string.moment_record
            is Moment.Comeback -> R.string.moment_comeback
            is Moment.Twelve -> if (moment.strike) R.string.moment_strike else R.string.moment_twelve
            is Moment.ResetFrom49 -> R.string.card_key_reset
            is Moment.Eliminated -> R.string.card_key_eliminated
        },
    )

/** Texte d'accompagnement proposé, modifiable. */
internal fun defaultText(context: Context, content: ShareContent): String {
    val name = content.hero?.name.orEmpty()
    return when (val moment = content.moment) {
        is Moment.Victory -> {
            context.getString(R.string.share_text_victory, name, content.throws)
        }

        is Moment.Perfect -> {
            context.getString(R.string.share_text_perfect, name)
        }

        is Moment.Record -> {
            context.getString(R.string.share_text_record, name, moment.throws)
        }

        is Moment.Comeback -> {
            context.getString(R.string.share_text_comeback, name, moment.deficit)
        }

        is Moment.Twelve -> {
            context.getString(
                if (moment.strike) R.string.share_text_strike else R.string.share_text_twelve,
                name,
            )
        }

        is Moment.ResetFrom49 -> {
            context.getString(R.string.share_text_reset, name)
        }

        is Moment.Eliminated -> {
            context.getString(R.string.share_text_eliminated, name)
        }
    }
}

private fun ShareFormat.label(): Int =
    when (this) {
        ShareFormat.STORY -> R.string.share_format_story
        ShareFormat.PORTRAIT -> R.string.share_format_portrait
        ShareFormat.SQUARE -> R.string.share_format_square
    }

private const val PREVIEW_HEIGHT = 420
