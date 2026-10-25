package fr.molkky.core.scoring

import fr.molkky.core.model.GameConfig
import fr.molkky.core.model.MissPenalty
import fr.molkky.core.model.Overshoot
import fr.molkky.core.model.TeamOverride
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
                Throw(EventId("1"), A, alice, listOf(12, 4)),
                Fault(EventId("2"), B, bruno, FaultKind.TRESPASS),
                WrongTeam(EventId("3"), A, alice, emptyList()),
                Abandon(EventId("4")),
                Undo(EventId("5"), EventId("4")),
                Correct(EventId("6"), EventId("1"), Throw(EventId("7"), A, alice, listOf(3))),
            )
        val encoded = json.encodeToString(events)
        assertEquals(events, json.decodeFromString<List<SetEvent>>(encoded))
        assertEquals(
            """{"type":"throw","id":"1","team":"A","player":"Alice","pins":[12,4]}""",
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
