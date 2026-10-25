package com.yogistudio.molkkyboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yogistudio.molkkyboard.core.designsystem.catalog.DesignSystemCatalog
import com.yogistudio.molkkyboard.core.designsystem.catalog.GameScreenMockup
import com.yogistudio.molkkyboard.core.designsystem.component.MolkkyBoardLogotype

// Build debug uniquement : vitrine du design system (J2) en attendant les vrais écrans.
// Textes de démonstration en dur, assumés : ce fichier n'est jamais livré.
private val TABS = listOf("Jeu", "Saisie rapide", "Catalogue")

@Composable
internal fun StartScreen() {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.safeDrawingPadding()) {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                MolkkyBoardLogotype()
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    TABS.forEachIndexed { index, label ->
                        SegmentedButton(
                            selected = tab == index,
                            onClick = { tab = index },
                            shape = SegmentedButtonDefaults.itemShape(index, TABS.size),
                            icon = {},
                        ) { Text(label, maxLines = 1) }
                    }
                }
            }
            Column(Modifier.verticalScroll(rememberScrollState())) {
                when (tab) {
                    0 -> GameScreenMockup()
                    1 -> GameScreenMockup(quickMode = true)
                    else -> DesignSystemCatalog()
                }
            }
        }
    }
}
