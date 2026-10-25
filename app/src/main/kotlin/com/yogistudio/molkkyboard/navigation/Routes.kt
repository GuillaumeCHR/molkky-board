package com.yogistudio.molkkyboard.navigation

import kotlinx.serialization.Serializable

// Routes typées de Navigation Compose.

/** Intro animée (destination de départ, retirée de la pile une fois finie). */
@Serializable
data object IntroRoute

@Serializable
data object HomeRoute

@Serializable
data object HistoryRoute

@Serializable
data object HallOfFameRoute

@Serializable
data object SettingsRoute

@Serializable
data object AboutRoute

@Serializable
data object JoinRoute

@Serializable
data object GameRoute

/** Vitrine du design system (build debug uniquement). */
@Serializable
data object ShowcaseRoute
