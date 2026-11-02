package com.yogistudio.molkkyboard.core.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView

/**
 * Protection contre le tapjacking (MASVS-PLATFORM) : tant que ce composable est affiché, la fenêtre ignore les touchers
 * reçus alors qu'une autre app la recouvre (`filterTouchesWhenObscured`). À placer sur les actions sensibles :
 * accepter un téléphone dans la partie, supprimer toutes les données. Dans une boîte de dialogue, il protège la
 * fenêtre de la boîte elle-même.
 */
@Composable
fun ObscuredTouchGuard() {
    val view = LocalView.current
    DisposableEffect(view) {
        val before = view.filterTouchesWhenObscured
        view.filterTouchesWhenObscured = true
        onDispose { view.filterTouchesWhenObscured = before }
    }
}
