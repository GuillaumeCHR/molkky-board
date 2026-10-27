package com.yogistudio.molkkyboard.ui

import androidx.annotation.StringRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yogistudio.molkkyboard.BuildConfig
import com.yogistudio.molkkyboard.R
import com.yogistudio.molkkyboard.core.designsystem.component.MBTopBar
import com.yogistudio.molkkyboard.core.designsystem.theme.MBDimens
import com.yogistudio.molkkyboard.core.designsystem.theme.MolkkyBoardTheme
import kotlin.math.PI
import kotlin.math.sin

/**
 * Crédits, façon générique de film : défilement automatique lent, arrêté au premier tap ou glissement ; liste
 * statique avec « Réduire les animations ». Ton léger, mais chaque affirmation est vraie (comme l'encart « Vie
 * privée ») ; la version vient de `BuildConfig`. Scène post-générique : le scintillement de l'intro, une seule fois.
 */
@Composable
internal fun CreditsScreen(onBack: () -> Unit) {
    val scroll = rememberScrollState()
    val reduced = MolkkyBoardTheme.reducedMotion
    var rolling by rememberSaveable { mutableStateOf(!reduced) }
    LaunchedEffect(rolling, scroll.maxValue) {
        if (!rolling || scroll.maxValue == 0) return@LaunchedEffect
        val remaining = scroll.maxValue - scroll.value
        scroll.animateScrollTo(scroll.maxValue, tween((remaining / ROLL_PX_PER_MS).toInt(), easing = LinearEasing))
        rolling = false
    }
    val atEnd by remember { derivedStateOf { scroll.maxValue > 0 && scroll.value >= scroll.maxValue - END_SLACK } }
    Column(Modifier.fillMaxSize()) {
        MBTopBar(stringResource(R.string.credits_title), onBack)
        Column(
            Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        rolling = false
                    }
                }.verticalScroll(scroll)
                .navigationBarsPadding()
                .padding(MBDimens.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(MBDimens.Gap),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Terminal()
            Title(stringResource(R.string.app_name))
            Text(
                stringResource(R.string.credits_version, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE),
                style = MaterialTheme.typography.titleMedium,
            )
            Note(
                stringResource(
                    R.string.credits_compiled,
                    if (BuildConfig.DEBUG) "assembleDebug" else "assembleRelease",
                ),
            )
            CREDITS.forEach { Credit(it) }
            Heading(R.string.credits_game_title)
            Body(R.string.credits_game)
            Credit(
                CreditBlock(R.string.credits_hardware_title, R.string.credits_hardware, R.string.credits_hardware_note),
            )
            Heading(R.string.credits_stats_title)
            ProductionStats()
            Heading(R.string.credits_thanks_title)
            listOf(R.string.credits_thanks_players, R.string.credits_thanks_pin12).forEach { Body(it) }
            Spacer(Modifier.height(24.dp))
            Note(stringResource(R.string.credits_outro))
            Note(stringResource(R.string.credits_post_credits))
            Sparkle(play = atEnd && !reduced)
            Text("exit 0", style = MolkkyBoardTheme.typography.scoreSmall, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(48.dp))
        }
    }
}

/** Un rôle du générique : intitulé, nom (en gras), note en italique. */
private class CreditBlock(
    @param:StringRes val role: Int,
    @param:StringRes val name: Int,
    @param:StringRes val note: Int?,
)

private val CREDITS =
    listOf(
        CreditBlock(R.string.credits_production, R.string.credits_production_name, R.string.credits_production_note),
        CreditBlock(R.string.credits_dev, R.string.credits_dev_name, R.string.credits_dev_note),
        CreditBlock(R.string.credits_pair, R.string.credits_pair_name, R.string.credits_pair_note),
        CreditBlock(R.string.credits_engine, R.string.credits_engine_name, R.string.credits_engine_note),
        CreditBlock(R.string.credits_physics, R.string.credits_physics_name, R.string.credits_physics_note),
        CreditBlock(R.string.credits_art, R.string.credits_art_name, R.string.credits_art_note),
        CreditBlock(R.string.credits_fonts, R.string.credits_fonts_names, R.string.credits_fonts_note),
    )

@Composable
private fun Credit(block: CreditBlock) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Heading(block.role)
        Text(
            stringResource(block.name),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
        )
        block.note?.let { Note(stringResource(it)) }
    }
}

@Composable
private fun Title(text: String) {
    Text(text, style = MaterialTheme.typography.headlineLarge, modifier = Modifier.semantics { heading() })
}

@Composable
private fun Heading(
    @StringRes text: Int,
) {
    Text(
        stringResource(text).uppercase(),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(top = 16.dp).semantics { heading() },
    )
}

@Composable
private fun Body(
    @StringRes text: Int,
) {
    Text(stringResource(text), style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
}

@Composable
private fun Note(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        fontStyle = FontStyle.Italic,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
}

/**
 * Le bloc « terminal » sur fond vert forêt, en Inter à chiffres tabulaires (pas de police monospace en plus). TalkBack
 * le lit comme une phrase simple, pas caractère par caractère. La ligne des traceurs repose sur le contrôle des
 * dépendances de la CI (`scripts/check-trackers.sh`).
 */
@Composable
private fun Terminal() {
    val summary = stringResource(R.string.credits_terminal_description)
    Surface(
        shape = MaterialTheme.shapes.large,
        // Vert forêt et texte bouleau dans les deux thèmes (le primaire du thème sombre est un vert clair).
        color = TERMINAL_FOREST,
        contentColor = TERMINAL_BIRCH,
        modifier = Modifier.fillMaxWidth().clearAndSetSemantics { contentDescription = summary },
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                "$ molkky-board --credits",
                style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "tnum"),
            )
            listOf(
                R.string.credits_terminal_pins to "[12/12] OK",
                R.string.credits_terminal_molkky to "OK",
                R.string.credits_terminal_trackers to stringResource(R.string.credits_terminal_none),
                R.string.credits_terminal_loading to "OK",
            ).forEach { (label, value) -> TerminalLine(stringResource(label), value) }
        }
    }
}

@Composable
private fun TerminalLine(label: String, value: String) {
    val style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "tnum")
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("> $label", style = style)
        Text(DOTS, style = style, maxLines = 1, overflow = TextOverflow.Clip, modifier = Modifier.weight(1f))
        Text(" $value", style = style, maxLines = 1)
    }
}

private val DOTS = "…".repeat(40)
private val TERMINAL_FOREST = Color(0xFF1F4D3A)
private val TERMINAL_BIRCH = Color(0xFFF4EBDD)

/** Les chiffres qui comptent. Les zéros sont vrais : aucune pub, aucun traceur, aucun SDK d'analytics, aucun cookie. */
@Composable
private fun ProductionStats() {
    val rows =
        listOf(
            stringResource(R.string.credits_stat_ads) to "0",
            stringResource(R.string.credits_stat_trackers) to "0",
            stringResource(R.string.credits_stat_analytics) to "0",
            stringResource(R.string.credits_stat_cookies) to stringResource(R.string.credits_stat_cookies_value),
            stringResource(R.string.credits_stat_sold) to "0",
            stringResource(R.string.credits_stat_pins) to "12",
            stringResource(R.string.credits_stat_target) to stringResource(R.string.credits_stat_target_value),
        )
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            rows.forEach { (label, value) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    Text(value, style = MolkkyBoardTheme.typography.scoreSmall, textAlign = TextAlign.End)
                }
            }
        }
    }
}

/**
 * Scène post-générique : la petite étoile safran de l'intro, qui scintille **une seule fois** (un flash, conforme à la
 * contrainte photosensible) quand on arrive au bout du générique.
 */
@Composable
private fun Sparkle(play: Boolean) {
    val progress = remember { Animatable(0f) }
    var played by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(play) {
        if (!play || played) return@LaunchedEffect
        played = true
        progress.animateTo(1f, tween(SPARKLE_MS, easing = LinearEasing))
    }
    val saffron = MolkkyBoardTheme.colors.saffron
    Canvas(Modifier.size(48.dp).clearAndSetSemantics {}) {
        val p = progress.value
        if (p <= 0f || p >= 1f) return@Canvas
        val half = size.minDimension / 2 * sin(p * PI.toFloat())
        val c = center
        drawLine(saffron, Offset(c.x - half, c.y), Offset(c.x + half, c.y), 4.dp.toPx(), StrokeCap.Round)
        drawLine(saffron, Offset(c.x, c.y - half), Offset(c.x, c.y + half), 4.dp.toPx(), StrokeCap.Round)
    }
}

/** Vitesse du générique : environ 40 dp par seconde sur un téléphone courant. */
private const val ROLL_PX_PER_MS = 0.11f
private const val END_SLACK = 24
private const val SPARKLE_MS = 700
