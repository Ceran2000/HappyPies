package pl.ceranka.happypies.data

/** Errors of the data layer, the same on both platforms. ViewModels decide what to show per type. */
sealed class DataException(message: String) : Exception(message) {
    class NotFound(message: String) : DataException(message)

    /** Also covers an expired or missing sign-in. */
    class PermissionDenied(message: String) : DataException(message)

    /** No network, offline or timeout. */
    class Unavailable(message: String) : DataException(message)

    /** A document is missing a required field or has one of the wrong type. */
    class MalformedData(message: String) : DataException(message)

    /** Anything else, e.g. a missing Firestore index (the message holds the link to create it). */
    class Unknown(message: String) : DataException(message)
}