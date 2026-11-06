package pl.ceranka.happypies.ui.common

import pl.ceranka.happypies.data.DataException

fun Throwable.toUserMessage(): String = when (this) {
    is DataException.Unavailable -> "Brak połączenia z internetem. Sprawdź sieć i spróbuj ponownie."
    is DataException.PermissionDenied -> "Nie masz dostępu do tych danych."
    is DataException.NotFound -> "Nie znaleziono danych. Mogły zostać usunięte."
    is DataException.MalformedData -> "Dane w bazie są niekompletne. Skontaktuj się z administratorem."
    else -> "Coś poszło nie tak. Spróbuj ponownie później."
}