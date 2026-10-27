package com.yogistudio.molkkyboard.feature.game

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.github.takahirom.roborazzi.captureScreenRoboImage
import com.yogistudio.molkkyboard.core.designsystem.theme.MolkkyBoardTheme
import com.yogistudio.molkkyboard.core.model.InputMode
import com.yogistudio.molkkyboard.core.model.PlayerId
import com.yogistudio.molkkyboard.core.model.TeamId
import com.yogistudio.molkkyboard.core.scoring.EventId
import com.yogistudio.molkkyboard.core.scoring.PointsKind
import com.yogistudio.molkkyboard.core.scoring.ThrowResult
import com.yogistudio.molkkyboard.core.scoring.points
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Captures du vrai écran de jeu, en clair et en sombre, et aux tailles de police 1,0 / 1,5 / 2,0 :
 * aucun score ne doit être tronqué et tout tient sur un écran.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "fr-w411dp-h891dp-xxhdpi")
class GameScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    private val teams =
        listOf(
            TeamUi(TeamId("A"), "Alice", 0, 38, 1, active = true, out = false, remaining = 12, avatar = "🦊"),
            TeamUi(TeamId("B"), "Bruno", 1, 27, 0, active = false, out = false, remaining = 23, avatar = "🐻"),
            TeamUi(TeamId("C"), "Chloé", 2, 12, 2, active = false, out = false, remaining = 38),
        )
    private val playing =
        GameUiState(
            loading = false,
            teams = teams,
            currentTeam = teams.first(),
            currentPlayerName = "Alice",
            selection = Selection.Pins(setOf(12, 4)),
            preview = Preview(PreviewKind.POINTS, 2, 40),
        )

    private val eliminated =
        TeamUi(TeamId("D"), "Denis", 3, 21, 3, active = false, out = true, eliminated = true, remaining = 29)

    private fun capture(
        name: String,
        state: GameUiState,
        dark: Boolean = false,
        fontScale: Float = 1f,
        overlay: (@Composable () -> Unit)? = null,
    ) {
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                MolkkyBoardTheme(darkTheme = dark, reducedMotion = true) { overlay?.invoke() ?: Screen(state) }
            }
        }
        compose.onRoot().captureRoboImage("src/test/screenshots/$name.png")
    }

    @Composable
    private fun Screen(state: GameUiState) = GameContent(state, GameActions())

    @Test
    fun jeuClair() = capture("jeu_clair", playing)

    @Test
    fun jeuSombre() = capture("jeu_sombre", playing, dark = true)

    @Test
    fun jeuPolice150() = capture("jeu_police_150", playing, fontScale = 1.5f)

    @Test
    fun jeuPolice200() = capture("jeu_police_200", playing, fontScale = 2f)

    @Test
    fun jeuRapide() =
        capture(
            "jeu_rapide",
            playing.copy(
                inputMode = InputMode.QUICK,
                selection = Selection.Quick(7),
                preview = Preview(PreviewKind.OVERSHOOT, 7, 25),
            ),
        )

    @Test
    fun jeuRapidePolice200() =
        capture(
            "jeu_rapide_police_200",
            playing.copy(inputMode = InputMode.QUICK, selection = Selection.None, preview = Preview()),
            fontScale = 2f,
        )

    // Traduction anglaise : aucun libellé coupé, y compris en grande police.
    @Test
    @Config(qualifiers = "en-w411dp-h891dp-xxhdpi")
    fun jeuAnglais() = capture("jeu_en", playing)

    @Test
    @Config(qualifiers = "en-w411dp-h891dp-xxhdpi")
    fun jeuAnglaisPolice200() = capture("jeu_en_police_200", playing, fontScale = 2f)

    @Test
    @Config(qualifiers = "en-w411dp-h2600dp-xxhdpi")
    fun reglesAnglais() {
        compose.setContent { MolkkyBoardTheme(reducedMotion = true) { RulesScreen(onBack = {}) } }
        compose.onRoot().captureRoboImage("src/test/screenshots/regles_en.png")
    }

    @Test
    @Config(qualifiers = "fr-w411dp-h1100dp-xxhdpi")
    fun placementPasAPas() {
        compose.setContent { MolkkyBoardTheme(reducedMotion = true) { PlacementScreen(onBack = {}) } }
        repeat(4) { compose.onNodeWithText("Suivante").performClick() }
        compose.onRoot().captureRoboImage("src/test/screenshots/placement_pas_a_pas.png")
        compose.onNodeWithText("Vue de côté").performClick()
        compose.onRoot().captureRoboImage("src/test/screenshots/placement_vue_de_cote.png")
    }

    @Test
    fun cartePretsAJouer() = capture("prets_a_jouer", playing.copy(readyCard = true))

    @Test
    fun jeuTourEtAvertissement() = capture("jeu_tour_avertissement", playing.copy(round = 4, missWarning = true))

    @Test
    fun tableauDesScores() {
        compose.setContent {
            MolkkyBoardTheme(reducedMotion = true) {
                GameContent(
                    playing.copy(round = 3, teams = teams + eliminated),
                    GameActions(),
                    scoreboardOpen = true,
                )
            }
        }
        compose.waitForIdle()
        captureScreenRoboImage("src/test/screenshots/tableau_des_scores.png")
    }

    @Test
    fun douzeStrikeEnVol() {
        compose.mainClock.autoAdvance = false
        compose.setContent {
            MolkkyBoardTheme(reducedMotion = false) {
                GameContent(playing, GameActions(), twelve = TwelveShow(strike = true, key = 1))
            }
        }
        compose.mainClock.advanceTimeBy(STRIKE_FRAME_MS)
        compose.onRoot().captureRoboImage("src/test/screenshots/douze_strike_en_vol.png")
    }

    @Test
    fun douzeQuilleSeule() =
        capture(
            "douze_quille_seule",
            playing,
            overlay = { GameContent(playing, GameActions(), twelve = TwelveShow(false, 1)) },
        )

    @Test
    fun elimination() =
        capture(
            "elimination",
            playing,
            overlay = {
                GameContent(
                    playing,
                    GameActions(),
                    elimination = EliminationShow(teams[2], 3, reverse = false, key = 1),
                )
            },
        )

    private fun item(n: Int, round: Int, team: TeamUi, result: ThrowResult, before: Int, after: Int) =
        LogItemUi(
            id = EventId("e$n"),
            round = round,
            kind = LogKind.THROW,
            teamName = team.name,
            colorIndex = team.colorIndex,
            playerName = team.name,
            result = result,
            points = result.points,
            scoreBefore = before,
            scoreAfter = after,
        )

    private val log =
        listOf(
            LogRound(
                1,
                listOf(
                    item(1, 1, teams[0], ThrowResult.Pins(listOf(12)), 0, 12),
                    item(2, 1, teams[1], ThrowResult.Pins(listOf(3, 5, 9)), 0, 3),
                    item(3, 1, teams[2], ThrowResult.Pins(emptyList()), 0, 0),
                ),
            ),
            LogRound(
                2,
                listOf(
                    item(4, 2, teams[0], ThrowResult.Points(7, PointsKind.PIN_COUNT), 12, 19),
                    item(5, 2, teams[1], ThrowResult.Pins(listOf(11)), 3, 14),
                    LogItemUi(EventId("e6"), 2, LogKind.JOINED, playerName = "Denis"),
                    item(7, 2, teams[2], ThrowResult.Pins(listOf(10)), 45, 25),
                ),
            ),
        )

    private fun captureLog(name: String, editing: LogItemUi?, fontScale: Float = 1f) {
        // La feuille vit dans sa propre fenêtre : la taille de police se règle au niveau du système simulé.
        RuntimeEnvironment.setFontScale(fontScale)
        compose.setContent {
            MolkkyBoardTheme(reducedMotion = true) {
                LogSheet(
                    playing.copy(log = log),
                    GameActions(),
                    message = null,
                    onDismiss = {},
                    initialEditing = editing,
                )
            }
        }
        compose.waitForIdle()
        captureScreenRoboImage("src/test/screenshots/$name.png")
    }

    @Test
    fun joueurs() {
        compose.setContent {
            MolkkyBoardTheme(reducedMotion = true) {
                PlayersSheet(
                    playing.copy(
                        teams =
                            (teams + eliminated).map {
                                it.copy(
                                    members = listOf(it.name),
                                    memberIds = listOf(PlayerId(it.name)),
                                )
                            },
                        betweenRounds = false,
                        round = 3,
                    ),
                    GameActions(),
                    null,
                    {},
                )
            }
        }
        compose.waitForIdle()
        captureScreenRoboImage("src/test/screenshots/joueurs.png")
    }

    @Test
    @Config(qualifiers = "fr-w411dp-h2600dp-xxhdpi")
    fun regles() {
        compose.setContent { MolkkyBoardTheme(reducedMotion = true) { RulesScreen(onBack = {}) } }
        compose.onRoot().captureRoboImage("src/test/screenshots/regles.png")
    }

    @Test
    fun deroule() = captureLog("deroule", editing = null)

    @Test
    fun deroulePolice200() = captureLog("deroule_police_200", editing = null, fontScale = 2f)

    @Test
    fun derouleEditeur() = captureLog("deroule_editeur", editing = log[1].items[3])

    private val winner = teams.first().copy(score = 50, remaining = 0, active = false)
    private val ranked = listOf(winner, teams[1].copy(active = false), teams[2])
    private val end =
        EndUi(
            winner = winner,
            winnerThrows = 7,
            newRecord = true,
            teamStats =
                listOf(
                    TeamStatsUi(ranked[0], 7, 0, 7.1f),
                    TeamStatsUi(ranked[1], 7, 1, 3.9f),
                    TeamStatsUi(ranked[2], 6, 2, 2f),
                ),
            series =
                SeriesUi(
                    sets = 3,
                    rows =
                        listOf(
                            SeriesRowUi("Alice", 0, 2, listOf(50, 31, 50), 131),
                            SeriesRowUi("Bruno", 1, 1, listOf(22, 50, 27), 99),
                            SeriesRowUi("Chloé", 2, 0, listOf(18, 40, 12), 70),
                        ),
                ),
        )
    private val finished =
        playing.copy(finished = true, winnerName = "Alice", ranking = ranked, teams = ranked, round = 7, end = end)

    @Test
    fun finDePartie() = capture("fin_de_partie", finished)

    @Test
    fun finDePartieSombre() = capture("fin_de_partie_sombre", finished, dark = true)

    @Test
    fun finDePartiePolice200() = capture("fin_de_partie_police_200", finished, fontScale = 2f)

    @Test
    fun finDeMancheMolkkout() {
        val duo = ranked.take(2)
        val match =
            MatchUi(
                setsToWin = 2,
                wins = duo.map { it to 1 },
                winner = null,
                nextSet = null,
                molkkout = MolkkoutUi(duo.map { it to 18 }, "Bruno", "Bruno", null, 3),
            )
        capture(
            "fin_de_manche_molkkout",
            finished.copy(ranking = duo, teams = duo, end = end.copy(match = match, series = null, newRecord = false)),
        )
    }

    /** Image de l'animation de victoire à [frameMs] : le « 50 » qui explose, puis la pluie de quilles et le nom. */
    private fun captureVictory(name: String, frameMs: Long) {
        compose.mainClock.autoAdvance = false
        compose.setContent {
            MolkkyBoardTheme(reducedMotion = false) {
                GameContent(
                    finished,
                    GameActions(),
                    victory = VictoryShow("Alice", 0, 50, newRecord = true, perfect = false, key = 1),
                )
            }
        }
        compose.mainClock.advanceTimeBy(frameMs)
        compose.onRoot().captureRoboImage("src/test/screenshots/$name.png")
    }

    private val idle = playing.copy(selection = Selection.None, preview = Preview())

    /** Bulles de la première partie guidée, mises en page après la mesure des cibles. */
    private fun captureGuide(name: String, state: GameUiState) {
        compose.setContent { MolkkyBoardTheme(reducedMotion = true) { GameContent(state, GameActions()) } }
        compose.waitForIdle()
        compose.onRoot().captureRoboImage("src/test/screenshots/$name.png")
    }

    @Test
    fun guideQuilles() = captureGuide("guide_quilles", idle.copy(guide = GuideStep.PINS))

    @Test
    fun guideValider() = captureGuide("guide_valider", playing.copy(guide = GuideStep.VALIDATE))

    @Test
    fun guideAnnuler() = captureGuide("guide_annuler", idle.copy(guide = GuideStep.UNDO, canUndo = true))

    @Test
    fun coach() =
        capture(
            "coach_zone_de_danger",
            idle.copy(
                teams = listOf(teams[0].copy(score = 47, remaining = 3)) + teams.drop(1),
                currentTeam = teams[0].copy(score = 47, remaining = 3),
                coach = Coach(needed = 3, danger = true, resetTo = 25),
            ),
        )

    @Test
    fun victoireExplosion() = captureVictory("victoire_explosion", VICTORY_BURST_MS)

    @Test
    fun victoireConfettis() = captureVictory("victoire_confettis", VICTORY_NAME_MS)
}

private const val VICTORY_BURST_MS = 350L
private const val VICTORY_NAME_MS = 1_600L

private const val STRIKE_FRAME_MS = 260L
