package com.yogistudio.molkkyboard.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Poignée des feuilles du bas. Celle de Material 3 ne fait que 32 dp de large, alors que la feuille la rend
 * actionnable (TalkBack : agrandir, réduire, fermer) : on élargit la zone tactile à 64 dp sans changer le dessin.
 * À passer en `dragHandle` de chaque `ModalBottomSheet`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MBDragHandle() {
    Box(Modifier.width(TOUCH_WIDTH), contentAlignment = Alignment.Center) { BottomSheetDefaults.DragHandle() }
}

private val TOUCH_WIDTH = 64.dp
