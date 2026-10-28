package com.yogistudio.molkkyboard.feature.share

import androidx.compose.ui.graphics.ImageBitmap
import com.yogistudio.molkkyboard.core.data.GameSnapshot
import com.yogistudio.molkkyboard.core.model.TeamId
import com.yogistudio.molkkyboard.core.model.targetFor
import com.yogistudio.molkkyboard.core.stats.Moment

/** Formats de vignette : stories (par défaut), fil Instagram, carré. */
enum class ShareFormat(
    val width: Int,
    val height: Int,
) {
    STORY(SIDE, STORY_HEIGHT),
    PORTRAIT(SIDE, PORTRAIT_HEIGHT),
    SQUARE(SIDE, SIDE),
}

private const val SIDE = 1_080
private const val STORY_HEIGHT = 1_920
private const val PORTRAIT_HEIGHT = 1_350

/**
 * Modèles graphiques « Nordic Bold » ; le moment choisit le premier proposé. [PHOTO] n'apparaît qu'une fois une
 * photo du moment choisie.
 */
enum class ShareTemplate { PODIUM, SCOREBOARD, HIGHLIGHT, OUCH, PHOTO }

/** Une équipe sur la vignette : pseudo, couleur, emblème au trait (clé de `LineEmblems`). */
data class ShareTeam(
    val id: TeamId,
    val name: String,
    val colorIndex: Int,
    val avatar: String?,
    val score: Int,
    val out: Boolean = false,
)

/**
 * Tout ce qu'une vignette affiche. Aucune photo par défaut ; aucune donnée de localisation. [photo] : la photo du
 * moment prise ou choisie par l'auteur (en mémoire seulement, jamais enregistrée par l'app).
 */
data class ShareContent(
    val moment: Moment,
    val teams: List<ShareTeam>,
    /** Équipe mise en avant (vainqueur, ou équipe du moment). */
    val hero: ShareTeam?,
    val target: Int,
    val throws: Int,
    val round: Int,
    val playedAt: Long,
    val place: String?,
    val photo: ImageBitmap? = null,
)

/** Le modèle qui convient le mieux à un moment. */
fun Moment.defaultTemplate(): ShareTemplate =
    when (this) {
        is Moment.Record, is Moment.Perfect, is Moment.Twelve -> ShareTemplate.HIGHLIGHT
        is Moment.ResetFrom49, is Moment.Eliminated -> ShareTemplate.OUCH
        is Moment.Comeback, is Moment.Victory -> ShareTemplate.PODIUM
    }

/** Équipe concernée par un moment. */
val Moment.team: TeamId
    get() =
        when (this) {
            is Moment.Victory -> team
            is Moment.Perfect -> team
            is Moment.Record -> team
            is Moment.Comeback -> team
            is Moment.Twelve -> team
            is Moment.ResetFrom49 -> team
            is Moment.Eliminated -> team
        }

/**
 * Contenu d'une vignette pour [moment]. [victory] donne le chiffre clé (lancers, tour).
 * [avatars] : emoji des profils locaux.
 */
fun GameSnapshot.shareContent(
    moment: Moment,
    victory: Moment.Victory?,
    avatars: Map<String, String> = emptyMap(),
): ShareContent {
    val state = currentSet.replay.state
    val order = state.ranking.ifEmpty { state.teams.sortedByDescending { it.score }.map { it.id } }
    val teams =
        order.mapIndexedNotNull { index, id ->
            val team = state.team(id) ?: return@mapIndexedNotNull null
            val info = team(id)
            val avatar =
                if (team.players.size == 1) {
                    players.firstOrNull { it.id == team.players.first() }?.profileId?.let(avatars::get)
                } else {
                    info?.emblem
                }
            ShareTeam(
                id = id,
                name = info?.name ?: id.value,
                colorIndex = info?.colorIndex ?: 0,
                avatar = avatar,
                score = team.score,
                out = team.eliminated || team.left,
            )
        }
    return ShareContent(
        moment = moment,
        teams = teams,
        hero = teams.firstOrNull { it.id == moment.team },
        target = config.targetFor(moment.team),
        throws = victory?.throws ?: 0,
        round = victory?.round ?: state.round,
        playedAt = createdAt,
        place = place,
    )
}
