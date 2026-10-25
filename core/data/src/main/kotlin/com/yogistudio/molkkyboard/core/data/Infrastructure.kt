package com.yogistudio.molkkyboard.core.data

/** Horloge injectable (millisecondes depuis l'époque Unix). */
fun interface TimeSource {
    fun now(): Long
}

/** Générateur d'identifiants (UUID) injectable. */
fun interface IdGenerator {
    fun newId(): String
}
