package com.yogistudio.molkkyboard.core.sync

import com.yogistudio.molkkyboard.core.model.PlayerId
import com.yogistudio.molkkyboard.core.model.TeamId
import com.yogistudio.molkkyboard.core.scoring.EventId
import com.yogistudio.molkkyboard.core.scoring.MolkkoutThrow
import com.yogistudio.molkkyboard.core.scoring.NextStarter
import com.yogistudio.molkkyboard.core.scoring.Throw
import com.yogistudio.molkkyboard.core.scoring.ThrowResult
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GameCommandTest {
    private val alice = PlayerId("alice")
    private val aliceDevice = Author(DeviceId("tel-alice"), Role.PLAYER, alice)
    private val organizer = Author(DeviceId("hote"), Role.ORGANIZER)
    private val molkkout = GameCommand.MolkkoutPlay(1, MolkkoutThrow(TeamId("A"), alice, listOf(12)))

    private val commands =
        listOf(
            GameCommand.Play(Throw(EventId("e1"), ThrowResult.Pins(listOf(7)), alice), baseSeq = 0),
            GameCommand.NextSet(1),
            molkkout,
            GameCommand.MolkkoutUndo(1),
            GameCommand.Rematch("partie-2", NextStarter.Rotate),
        )

    @Test
    fun `l'organisateur peut tout demander`() {
        commands.forEach { assertTrue(it.allowedFor(organizer), "$it") }
    }

    @Test
    fun `un joueur saisit ses lancers, y compris au Molkkout, mais ne gere pas le match`() {
        assertTrue(commands[0].allowedFor(aliceDevice))
        assertTrue(molkkout.allowedFor(aliceDevice))
        assertFalse(
            molkkout.copy(throwEvent = molkkout.throwEvent.copy(player = PlayerId("bruno"))).allowedFor(aliceDevice),
        )
        assertFalse(molkkout.allowedFor(Author(DeviceId("x"), Role.PLAYER)))
        listOf(commands[1], commands[3], commands[4]).forEach { assertFalse(it.allowedFor(aliceDevice), "$it") }
    }

    @Test
    fun `les commandes passent en JSON et reviennent a l'identique (contenu des propositions)`() {
        val json = Json
        commands.forEach { command ->
            val text = json.encodeToString(GameCommand.serializer(), command)
            assertEquals(command, json.decodeFromString(GameCommand.serializer(), text))
        }
        assertTrue(json.encodeToString(GameCommand.serializer(), commands[1]).contains("\"next_set\""))
    }

    @Test
    fun `resultat d'une commande - applique, ou refuse avec son motif`() {
        val outcomes: List<CommandOutcome> = listOf(CommandOutcome.Applied, CommandOutcome.Rejected(RejectCode.STALE))
        assertEquals("Applied", outcomes[0].toString())
        assertEquals(CommandOutcome.Rejected(RejectCode.STALE, null), outcomes[1])
    }
}
