package com.yogistudio.molkkyboard.core.model

import kotlinx.serialization.Serializable

/** Une équipe et l'ordre de passage de ses joueurs pour une manche. Un joueur seul = une équipe d'un joueur. */
@Serializable
data class TeamLineup(
    val id: TeamId,
    val players: List<PlayerId>,
)
