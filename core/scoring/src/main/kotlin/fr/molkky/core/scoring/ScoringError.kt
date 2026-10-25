package fr.molkky.core.scoring

enum class ScoringError {
    // Création d'une manche
    NOT_ENOUGH_TEAMS,
    EMPTY_TEAM,
    PLAYER_IN_MULTIPLE_TEAMS,
    DUPLICATE_TEAM,
    INVALID_CONFIG,

    // Événements
    SET_FINISHED,
    NOT_YOUR_TURN,
    UNKNOWN_TEAM,
    UNKNOWN_PLAYER,
    INVALID_PIN,
    DUPLICATE_PIN,
    INVALID_EVENT,
    NOTHING_TO_UNDO,
    UNKNOWN_EVENT,
    CORRECTION_INVALIDATES_LOG,

    // Match
    LINEUP_LOCKED,
}
