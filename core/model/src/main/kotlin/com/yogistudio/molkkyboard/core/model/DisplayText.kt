package com.yogistudio.molkkyboard.core.model

/**
 * Textes saisis par les joueurs (pseudos, noms d'équipe, lieu) : on refuse les caractères invisibles qui changent
 * l'affichage, comme l'inversion du sens de lecture (U+202E), les isolats bidirectionnels, les espaces de largeur
 * nulle ou les séparateurs de ligne, ainsi que les caractères de contrôle. Le liant sans chasse (U+200D) reste
 * permis : il compose les emoji (👨‍👩‍👧).
 */
private const val ZERO_WIDTH_JOINER = 0x200D

private fun isUnsafe(codePoint: Int): Boolean =
    when (Character.getType(codePoint).toByte()) {
        Character.CONTROL,
        Character.LINE_SEPARATOR,
        Character.PARAGRAPH_SEPARATOR,
        -> true

        Character.FORMAT -> codePoint != ZERO_WIDTH_JOINER

        else -> false
    }

/** Vrai si le texte contient un caractère de contrôle ou de mise en forme invisible. */
fun String.hasUnsafeChars(): Boolean = codePoints().anyMatch(::isUnsafe)

/**
 * Le texte sans ses caractères de contrôle ou de mise en forme invisibles (filtre à la saisie). [keepNewlines] :
 * garde les retours à la ligne d'un texte libre sur plusieurs lignes.
 */
fun String.withoutUnsafeChars(keepNewlines: Boolean = false): String {
    val keep = { cp: Int -> !isUnsafe(cp) || (keepNewlines && cp == '\n'.code) }
    if (codePoints().allMatch(keep)) return this
    return buildString { this@withoutUnsafeChars.codePoints().filter(keep).forEach(::appendCodePoint) }
}
