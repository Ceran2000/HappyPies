package pl.ceranka.happypies.data.firestore

/**
 * A Firestore field value as it crosses the platform bridge. Typed on purpose:
 * values typed as `Any` (e.g. `NSNumber` from Swift) are not converted reliably by Kotlin/Native.
 */
sealed class FirestoreValue {
    data class StringValue(val value: String) : FirestoreValue()
    data class LongValue(val value: Long) : FirestoreValue()
    data class DoubleValue(val value: Double) : FirestoreValue()
    data class BooleanValue(val value: Boolean) : FirestoreValue()
    data class TimestampValue(val epochMillis: Long) : FirestoreValue()
    data class ArrayValue(val values: List<FirestoreValue>) : FirestoreValue()
    data class MapValue(val fields: Map<String, FirestoreValue>) : FirestoreValue()
    data object NullValue : FirestoreValue()

    /** Read only: a type the app does not use (GeoPoint, reference, bytes...). */
    data class Unsupported(val typeName: String) : FirestoreValue()

    /** Write only: replaced by the server's time. */
    data object ServerTimestamp : FirestoreValue()

    /** Write only: removes the field. */
    data object Delete : FirestoreValue()
}

internal fun String?.toFirestoreValue(): FirestoreValue =
    if (this == null) FirestoreValue.NullValue else FirestoreValue.StringValue(this)
