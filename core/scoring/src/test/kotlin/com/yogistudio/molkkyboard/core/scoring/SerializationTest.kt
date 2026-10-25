package com.yogistudio.molkkyboard.core.scoring

import com.yogistudio.molkkyboard.core.model.GameConfig
import com.yogistudio.molkkyboard.core.model.MissPenalty
import com.yogistudio.molkkyboard.core.model.Overshoot
import com.yogistudio.molkkyboard.core.model.TeamOverride
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/** Les événements et la configuration sont stockés en JSON (Room puis Supabase) : l'aller-retour doit être exact. */
class SerializationTest {
    private val json = Json

    @Test
    fun `aller-retour des evenements`() {
        val events: List<SetEvent> =
            listOf(
                Throw(EventId("1"), ThrowResult.Pins(listOf(12, 4)), by = alice),
                Fault(EventId("2"), FaultKind.TRESPASS),
                WrongTeam(EventId("3"), A, alice, ThrowResult.Points(0, PointsKind.PIN_COUNT)),
                AddPlayer(EventId("8"), chloe, C),
                RemovePlayer(EventId("9"), bruno),
                Reorder(EventId("10"), listOf(B, A), mapOf(A to listOf(alice))),
                Delete(EventId("11"), EventId("2")),
                Abandon(EventId("4")),
                Undo(EventId("5"), EventId("4")),
                Correct(EventId("6"), EventId("1"), Throw(EventId("7"), ThrowResult.Points(3, PointsKind.SINGLE_PIN))),
            )
        val encoded = json.encodeToString(events)
        assertEquals(events, json.decodeFromString<List<SetEvent>>(encoded))
        assertEquals(
            """{"type":"throw","id":"1","result":{"type":"pins","pins":[12,4]},"by":"Alice"}""",
            json.encodeToString<SetEvent>(events.first()),
        )
    }

    @Test
    fun `aller-retour de la configuration`() {
        val configs =
            listOf(
                GameConfig.Classic,
                GameConfig.ImoTournament,
                GameConfig(
                    overshoot = Overshoot.Bounce,
                    onMissLimit = MissPenalty.ResetScore(0),
                    teamOverrides = mapOf(A to TeamOverride(startScore = 10, exemptFromMissLimit = true)),
                ),
                GameConfig(overshoot = Overshoot.Stay, onMissLimit = MissPenalty.Nothing),
            )
        configs.forEach { assertEquals(it, json.decodeFromString<GameConfig>(json.encodeToString(it))) }
        assertEquals(MatchConfig.Classic, json.decodeFromString<MatchConfig>(json.encodeToString(MatchConfig.Classic)))
    }
}
