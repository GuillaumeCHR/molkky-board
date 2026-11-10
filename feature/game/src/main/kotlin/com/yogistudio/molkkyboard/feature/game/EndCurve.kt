package com.yogistudio.molkkyboard.feature.game

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.yogistudio.molkkyboard.core.designsystem.component.CurveDot
import com.yogistudio.molkkyboard.core.designsystem.component.CurveDotMark
import com.yogistudio.molkkyboard.core.designsystem.component.GameCurve
import com.yogistudio.molkkyboard.core.designsystem.component.GameCurveHighlight
import com.yogistudio.molkkyboard.core.designsystem.component.GameCurveLine
import com.yogistudio.molkkyboard.core.designsystem.component.GameCurveTimeline
import com.yogistudio.molkkyboard.core.designsystem.icon.MBIcons
import com.yogistudio.molkkyboard.core.designsystem.icon.emblemIcon
import com.yogistudio.molkkyboard.core.designsystem.sound.Sound
import com.yogistudio.molkkyboard.core.designsystem.sound.rememberSoundEffects
import com.yogistudio.molkkyboard.core.designsystem.theme.MBDimens
import com.yogistudio.molkkyboard.core.designsystem.theme.MolkkyBoardTheme
import com.yogistudio.molkkyboard.core.scoring.SeriesMark
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * « Courbe de la partie » sur l'écran de fin (spécification « Courbe de la partie »). L'animation (3 s au plus) part
 * quand la séquence de victoire est finie ([celebrating] faux) et que la carte est à l'écran, une seule fois ; un tap
 * la passe. Vibration brève à l'impact, son optionnel (réglage « Sons »). « Réduire les animations » : la courbe
 * finale apparaît en fondu. Une
 * fois la courbe posée, un tap sur un tour, ou les flèches « Tour précédent / suivant », donnent le score exact de
 * chacun à ce moment ; TalkBack lit le résumé de la partie. Une correction de lancer redessine la courbe sans
 * rejouer l'animation.
 */
@Composable
internal fun CurveCard(curve: CurveUi, celebrating: Boolean, sounds: Boolean) {
    val reduced = MolkkyBoardTheme.reducedMotion
    // L'animation attend que la carte soit à l'écran : sur l'écran de fin, elle est souvent plus bas et se jouerait
    // hors de vue pendant qu'on fait défiler.
    var onScreen by remember { mutableStateOf(false) }
    val animation = rememberCurveAnimation(reduced, waiting = celebrating || !onScreen)
    var selected by rememberSaveable { mutableStateOf<Int?>(null) }
    ImpactFeedback(animation.time, sounds)
    val finished = animation.finished
    val lines = curveLines(curve)
    val summary = curveSummary(curve)
    val skip = stringResource(R.string.end_curve_skip)
    val skipNow = animation::skip
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        modifier =
            Modifier.fillMaxWidth().onGloballyPositioned { coordinates ->
                // Partie visible, rognée par le défilement et la barre du bas.
                val visible = coordinates.boundsInWindow().height
                onScreen = coordinates.size.height > 0 && visible >= coordinates.size.height * VISIBLE_SHARE
            },
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            CurveTitle(showReplay = !reduced && finished) {
                selected = null
                animation.replay()
            }
            GameCurve(
                lines = lines,
                rounds = curve.rounds,
                target = curve.target,
                timeMs = { animation.time.value },
                highlight = curve.highlight?.let { (line, round) -> GameCurveHighlight(line, round) },
                selectedRound = selected,
                roundsLabel = pluralStringResource(R.plurals.end_curve_rounds, curve.rounds, curve.rounds),
                onTap = { round -> if (finished) selected = round.coerceAtLeast(1) else skipNow() },
                modifier =
                    Modifier.graphicsLayer { alpha = animation.fade.value }.semantics {
                        contentDescription = summary
                        if (!finished) {
                            onClick(skip) {
                                skipNow()
                                true
                            }
                        }
                    },
            )
            if (finished) RoundScores(curve, selected ?: curve.rounds) { selected = it }
        }
    }
}

/** Horloge de l'animation (0 à 3 s) et fondu de « Réduire les animations », joués une seule fois. */
private class CurveAnimation(
    val time: Animatable<Float, *>,
    val fade: Animatable<Float, *>,
    private val scope: CoroutineScope,
) {
    /** Lu par la composition : ne change qu'à la fin (ou au début d'un « Rejouer »), pas à chaque image. */
    val finished: Boolean by derivedStateOf { time.value >= GameCurveTimeline.END && !time.isRunning }

    /** Passée d'un tap : définitif, elle ne repart pas d'elle-même. */
    var skipped = false
        private set

    fun skip() {
        skipped = true
        scope.launch { time.snapTo(GameCurveTimeline.END) }
    }

    /** Retour au tout début, avant le tracé (ou avant le fondu). */
    suspend fun restart(reduced: Boolean) {
        if (reduced) fade.snapTo(0f) else time.snapTo(0f)
    }

    fun replay() {
        scope.launch {
            time.snapTo(0f)
            time.play()
        }
    }
}

/**
 * L'animation part quand plus rien ne la retient ([waiting] faux : séquence de victoire finie et carte à l'écran), une
 * seule fois (même après rotation). Interrompue avant la fin (l'écran de fin s'affiche un instant avant la séquence de
 * victoire, qui la recouvre aussitôt), elle revient au départ et repartira : elle n'est « jouée » qu'une fois finie
 * ou passée d'un tap.
 */
@Composable
private fun rememberCurveAnimation(reduced: Boolean, waiting: Boolean): CurveAnimation {
    var played by rememberSaveable { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val animation =
        remember {
            CurveAnimation(
                time = Animatable(if (reduced || played) GameCurveTimeline.END else 0f),
                fade = Animatable(if (reduced && !played) 0f else 1f),
                scope = scope,
            )
        }
    LaunchedEffect(waiting) {
        if (waiting || played) return@LaunchedEffect
        try {
            if (reduced) animation.fade.animateTo(1f, tween(FADE_MS)) else animation.time.play()
            played = true
        } finally {
            if (animation.skipped) {
                played = true
            } else if (!played) {
                withContext(NonCancellable) { animation.restart(reduced) }
            }
        }
    }
    return animation
}

/** Joue l'animation complète (3 s), au rythme de l'horloge : chaque phase applique son propre amorti. */
private suspend fun Animatable<Float, *>.play() =
    animateTo(GameCurveTimeline.END, tween(GameCurveTimeline.END.toInt(), easing = LinearEasing))

/** Impact : une vibration brève, et le son s'il est activé (jamais si l'animation a été passée d'un tap). */
@Composable
private fun ImpactFeedback(time: Animatable<Float, *>, sounds: Boolean) {
    val haptic = LocalHapticFeedback.current
    val effects = rememberSoundEffects(sounds)
    LaunchedEffect(time) {
        snapshotFlow { time.isRunning && time.value >= GameCurveTimeline.IMPACT }
            .distinctUntilChanged()
            .filter { it }
            .collect {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                effects?.play(Sound.IMPACT)
            }
    }
}

/** Titre de la carte, avec « Rejouer » une fois la courbe posée. */
@Composable
private fun CurveTitle(showReplay: Boolean, onReplay: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            stringResource(R.string.end_curve_title),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.weight(1f).semantics { heading() },
        )
        if (showReplay) {
            TextButton(onClick = onReplay, modifier = Modifier.heightIn(min = MBDimens.MinTouch)) {
                Text(stringResource(R.string.end_curve_replay))
            }
        }
    }
}

/** Lignes du dessin : couleur et emblème de chaque équipe (jamais la couleur seule), points et marques. */
@Composable
private fun curveLines(curve: CurveUi): List<GameCurveLine> {
    val palette = MolkkyBoardTheme.colors.teams
    return curve.lines.map { line ->
        GameCurveLine(
            color = palette[line.team.colorIndex % palette.size].fill,
            emblem = emblemIcon(line.team.avatar),
            initial = line.team.name,
            dots = line.points.map { CurveDot(it.round, it.score, it.mark.toDotMark()) },
            winner = line.winner,
        )
    }
}

/** Score exact de chacun à la fin du tour [round], avec des flèches pour changer de tour (sans geste précis). */
@Composable
private fun RoundScores(curve: CurveUi, round: Int, onRound: (Int) -> Unit) {
    val palette = MolkkyBoardTheme.colors.teams
    Row(verticalAlignment = Alignment.CenterVertically) {
        val previous = stringResource(R.string.end_curve_previous)
        IconButton(
            onClick = { onRound(round - 1) },
            enabled = round > 1,
            modifier = Modifier.semantics { contentDescription = previous },
        ) { Icon(MBIcons.ChevronLeft, contentDescription = null) }
        Text(
            stringResource(R.string.end_curve_round, round),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f),
        )
        val next = stringResource(R.string.end_curve_next)
        IconButton(
            onClick = { onRound(round + 1) },
            enabled = round < curve.rounds,
            modifier = Modifier.semantics { contentDescription = next },
        ) { Icon(MBIcons.ChevronRight, contentDescription = null) }
    }
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(MBDimens.Gap),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
    ) {
        curve.lines.forEach { line ->
            val score = line.points.lastOrNull { it.round <= round }?.score
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.size(12.dp).background(palette[line.team.colorIndex % palette.size].fill, CircleShape))
                Text(
                    if (score == null) {
                        stringResource(R.string.end_curve_not_yet, line.team.name)
                    } else {
                        stringResource(R.string.end_curve_score, line.team.name, score)
                    },
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
    }
}

/** Résumé lu par TalkBack : « Léa gagne au tour 8, Tom retombe à 25 au tour 6… ». */
@Composable
private fun curveSummary(curve: CurveUi): String {
    val parts = mutableListOf<String>()
    curve.lines.firstOrNull { it.winner }?.let { line ->
        parts += stringResource(R.string.end_curve_summary_win, line.team.name, line.points.last().round)
    }
    curve.lines.forEach { line ->
        line.points.forEach { point ->
            when (point.mark) {
                SeriesMark.RESET -> {
                    parts +=
                        stringResource(R.string.end_curve_summary_reset, line.team.name, point.score, point.round)
                }

                SeriesMark.ELIMINATED -> {
                    parts +=
                        stringResource(R.string.end_curve_summary_out, line.team.name, point.round)
                }

                SeriesMark.LEFT -> {
                    parts += stringResource(R.string.end_curve_summary_left, line.team.name, point.round)
                }

                SeriesMark.NONE, SeriesMark.WIN -> {
                    Unit
                }
            }
        }
    }
    return stringResource(R.string.end_curve_summary, curve.rounds, parts.joinToString(", "))
}

private fun SeriesMark.toDotMark(): CurveDotMark =
    when (this) {
        SeriesMark.NONE -> CurveDotMark.NONE
        SeriesMark.RESET -> CurveDotMark.RESET
        SeriesMark.ELIMINATED -> CurveDotMark.ELIMINATED
        SeriesMark.LEFT -> CurveDotMark.LEFT
        SeriesMark.WIN -> CurveDotMark.WIN
    }

private const val FADE_MS = 600

/** Part de la carte qui doit être à l'écran pour lancer l'animation. */
private const val VISIBLE_SHARE = 0.6f
