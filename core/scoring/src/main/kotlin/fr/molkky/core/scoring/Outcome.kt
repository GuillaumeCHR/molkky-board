package fr.molkky.core.scoring

/** Résultat d'une opération du moteur : un état valide ou une erreur métier, jamais une exception. */
sealed interface Outcome<out T> {
    data class Ok<out T>(
        val value: T,
    ) : Outcome<T>

    data class Err(
        val error: ScoringError,
    ) : Outcome<Nothing>
}

internal inline fun <T, R> Outcome<T>.flatMap(transform: (T) -> Outcome<R>): Outcome<R> =
    when (this) {
        is Outcome.Ok -> transform(value)
        is Outcome.Err -> this
    }

fun <T> Outcome<T>.getOrNull(): T? = (this as? Outcome.Ok)?.value

fun <T> Outcome<T>.errorOrNull(): ScoringError? = (this as? Outcome.Err)?.error

fun <T> Outcome<T>.getOrThrow(): T =
    when (this) {
        is Outcome.Ok -> value
        is Outcome.Err -> error("Erreur de score : $error")
    }
