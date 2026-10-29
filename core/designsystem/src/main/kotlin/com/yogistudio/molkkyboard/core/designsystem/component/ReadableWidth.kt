package com.yogistudio.molkkyboard.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.yogistudio.molkkyboard.core.designsystem.theme.MBDimens

/**
 * Contenu centré, limité à [MBDimens.MaxContentWidth] : sur tablette ou pliable ouvert, les listes et formulaires ne
 * s'étirent plus sur toute la largeur. Plus étroit qu'un téléphone, il n'y change rien.
 */
@Composable
fun ReadableWidth(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Box(Modifier.widthIn(max = MBDimens.MaxContentWidth).fillMaxWidth().fillMaxSize()) { content() }
    }
}
