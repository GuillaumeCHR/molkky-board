package com.yogistudio.molkkyboard.core.designsystem.catalog

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.yogistudio.molkkyboard.core.designsystem.component.BigActionButton
import com.yogistudio.molkkyboard.core.designsystem.component.BigActionKind
import com.yogistudio.molkkyboard.core.designsystem.component.FitRollingNumber
import com.yogistudio.molkkyboard.core.designsystem.component.MissIndicator
import com.yogistudio.molkkyboard.core.designsystem.component.MolkkyBoardLogotype
import com.yogistudio.molkkyboard.core.designsystem.component.PinView
import com.yogistudio.molkkyboard.core.designsystem.component.PlayerChip
import com.yogistudio.molkkyboard.core.designsystem.component.QuickPad
import com.yogistudio.molkkyboard.core.designsystem.component.QuickPadMode
import com.yogistudio.molkkyboard.core.designsystem.component.ScoreRing
import com.yogistudio.molkkyboard.core.designsystem.icon.MBIcons
import com.yogistudio.molkkyboard.core.designsystem.theme.MBDimens
import com.yogistudio.molkkyboard.core.designsystem.theme.MolkkyBoardTheme

// Catalogue de développement (source set debug, jamais livré) : textes d'exemple en dur, assumés.

/** Formation de départ officielle, vue du lanceur (rangée du fond en haut). */
internal val PIN_FORMATION = listOf(listOf(7, 9, 8), listOf(5, 11, 12, 6), listOf(3, 10, 4), listOf(1, 2))

@Composable
internal fun PinFormation(
    knocked: Set<Int>,
    onToggle: (Int, Boolean) -> Unit,
    pinSize: androidx.compose.ui.unit.Dp = 64.dp,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy((-14).dp)) {
        PIN_FORMATION.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                row.forEach { pin -> PinView(pin, pin in knocked, onToggle = { onToggle(pin, it) }, size = pinSize) }
            }
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(MBDimens.Gap)) {
        Text(
            title.uppercase(),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        content()
    }
}

/** Planche de tous les composants du design system, dans le thème courant. */
@Composable
fun DesignSystemCatalog(modifier: Modifier = Modifier) {
    val teams = MolkkyBoardTheme.colors.teams
    Surface(modifier, color = MaterialTheme.colorScheme.background) {
        Column(Modifier.padding(MBDimens.ScreenPadding), verticalArrangement = Arrangement.spacedBy(28.dp)) {
            MolkkyBoardLogotype()
            Section("Typographie") {
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("38", style = MolkkyBoardTheme.typography.scoreHero)
                    Column {
                        Text("Titre de section", style = MaterialTheme.typography.headlineSmall)
                        Text("Texte courant en Inter, lisible au soleil.", style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
            Section("Quilles : debout, tombée, suggérée, inactive") {
                Row(horizontalArrangement = Arrangement.spacedBy(MBDimens.Gap)) {
                    PinView(7, knocked = false, onToggle = {})
                    PinView(12, knocked = true, onToggle = {})
                    PinView(3, knocked = false, onToggle = {}, highlighted = true)
                    PinView(5, knocked = false, onToggle = {}, enabled = false)
                }
            }
            Section("Saisie rapide") {
                var value by remember { mutableStateOf<Int?>(7) }
                var mode by remember { mutableStateOf(QuickPadMode.SinglePin) }
                QuickPad(value, { value = it }, mode, { mode = it })
            }
            Section("Jauges : 0, 24, 40 (un lancer de la victoire), 50") {
                Row(horizontalArrangement = Arrangement.spacedBy(MBDimens.Gap)) {
                    listOf(0, 24, 40, 50).forEachIndexed { i, score ->
                        ScoreRing(score, teams[i].fill, size = 82.dp, strokeWidth = 9.dp) {
                            Text(score.toString(), style = MolkkyBoardTheme.typography.scoreSmall)
                        }
                    }
                }
            }
            Section("Ratés consécutifs") {
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) { (0..3).forEach { MissIndicator(it) } }
            }
            Section("Joueurs et équipes") {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(MBDimens.SmallGap),
                    verticalArrangement = Arrangement.spacedBy(MBDimens.SmallGap),
                ) {
                    PlayerChip("Alice", teams[0], active = true)
                    PlayerChip("Bruno", teams[1])
                    PlayerChip("Les Bouleaux", teams[4], onRemove = {})
                }
            }
            Section("Actions") {
                Row(horizontalArrangement = Arrangement.spacedBy(MBDimens.Gap)) {
                    BigActionButton("Raté", {}, BigActionKind.Miss, Modifier.weight(1f), icon = MBIcons.Cross)
                    BigActionButton("Valider", {}, BigActionKind.Validate, Modifier.weight(1f), icon = MBIcons.Check)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(MBDimens.Gap)) {
                    BigActionButton("Nouvelle partie", {}, BigActionKind.Primary, Modifier.weight(1f))
                    BigActionButton("Rejoindre", {}, BigActionKind.Secondary, Modifier.weight(1f))
                }
            }
            Section("Couleurs d'équipe") {
                Row(horizontalArrangement = Arrangement.spacedBy(MBDimens.SmallGap)) {
                    teams.forEach { Box(Modifier.size(36.dp).background(it.fill, CircleShape)) }
                }
            }
        }
    }
}

/**
 * Maquette de l'écran de jeu assemblée avec les composants, pour juger l'ensemble avant le J4.
 * Ce n'est pas l'écran définitif (pas d'état, pas de navigation).
 */
@Composable
fun GameScreenMockup(modifier: Modifier = Modifier, quickMode: Boolean = false) {
    val teams = MolkkyBoardTheme.colors.teams
    var knocked by remember { mutableStateOf(setOf(12, 4)) }
    val score = 38
    val gain = if (knocked.size == 1) knocked.first() else knocked.size
    Surface(modifier, color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier.padding(MBDimens.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(MBDimens.SmallGap)) {
                PlayerChip("Bruno", teams[1], score = 27, modifier = Modifier.weight(1f, fill = false))
                PlayerChip("Chloé", teams[2], score = 12, modifier = Modifier.weight(1f, fill = false))
            }
            Surface(
                shape = RoundedCornerShape(32.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    ScoreRing(score, teams[0].fill, size = 196.dp, strokeWidth = 14.dp) {
                        FitRollingNumber(score, MolkkyBoardTheme.typography.scoreHero)
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(MBDimens.SmallGap)) {
                        Text("Alice", style = MaterialTheme.typography.headlineMedium, maxLines = 1)
                        Text(
                            "Les Bouleaux",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        MissIndicator(1)
                    }
                }
            }
            Text(
                text = if (knocked.isEmpty()) "Touchez les quilles tombées" else "+$gain → ${score + gain}",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
            )
            if (quickMode) {
                QuickPad(gain, {}, QuickPadMode.PinCount, {})
            } else {
                PinFormation(knocked, onToggle = { pin, down -> knocked = if (down) knocked + pin else knocked - pin })
            }
            Spacer(Modifier.height(4.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(MBDimens.Gap)) {
                BigActionButton("Raté", {}, BigActionKind.Miss, Modifier.weight(1f), icon = MBIcons.Cross)
                BigActionButton("Valider", {}, BigActionKind.Validate, Modifier.weight(1f), icon = MBIcons.Check)
            }
        }
    }
}

@Preview(name = "Clair", widthDp = 420, heightDp = 1500)
@Preview(name = "Sombre", widthDp = 420, heightDp = 1500, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CatalogPreview() = MolkkyBoardTheme { DesignSystemCatalog() }

@Preview(name = "Plein soleil", widthDp = 420, heightDp = 1500)
@Composable
private fun CatalogSunPreview() = MolkkyBoardTheme(sunlight = true) { DesignSystemCatalog() }

@Preview(name = "Jeu clair", widthDp = 411, heightDp = 891)
@Preview(name = "Jeu sombre", widthDp = 411, heightDp = 891, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun GamePreview() = MolkkyBoardTheme { GameScreenMockup() }
