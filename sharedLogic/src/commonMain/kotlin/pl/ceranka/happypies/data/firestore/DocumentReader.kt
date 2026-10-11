package pl.ceranka.happypies.data.firestore

import pl.ceranka.happypies.data.DataException
import pl.ceranka.happypies.data.firestore.FirestoreValue.ArrayValue
import pl.ceranka.happypies.data.firestore.FirestoreValue.LongValue
import pl.ceranka.happypies.data.firestore.FirestoreValue.MapValue
import pl.ceranka.happypies.data.firestore.FirestoreValue.NullValue
import pl.ceranka.happypies.data.firestore.FirestoreValue.StringValue
import pl.ceranka.happypies.data.firestore.FirestoreValue.TimestampValue

/**
 * Reads typed fields of a document (or of a nested map) and throws [DataException.MalformedData]
 * on a missing required field or a field of the wrong type. [path] names the place in errors,
 * e.g. `reports/abc.comment`.
 */
internal class DocumentReader(private val path: String, private val fields: Map<String, FirestoreValue>) {

    constructor(collection: String, document: FirestoreDocument) : this("$collection/${document.id}", document.fields)

    fun requireString(name: String): String =
        (fields[name] as? StringValue)?.value ?: throw malformed(name)

    /** Missing or null means "not set"; any other type is malformed. */
    fun optionalString(name: String): String? = when (val value = fields[name]) {
        null, NullValue -> null
        is StringValue -> value.value
        else -> throw malformed(name)
    }

    fun requireLong(name: String): Long =
        (fields[name] as? LongValue)?.value ?: throw malformed(name)

    fun requireMillis(name: String): Long =
        (fields[name] as? TimestampValue)?.epochMillis ?: throw malformed(name)

    fun <T : Enum<T>> requireEnum(name: String, entries: List<T>): T {
        val raw = requireString(name)
        return entries.firstOrNull { it.name == raw } ?: throw malformed(name)
    }

    /** Missing or null means "not set"; any other type than a map is malformed. */
    fun optionalMap(name: String): DocumentReader? = when (val value = fields[name]) {
        null, NullValue -> null
        is MapValue -> DocumentReader("$path.$name", value.fields)
        else -> throw malformed(name)
    }

    /** A required array whose every element is a map. */
    fun requireMapArray(name: String): List<DocumentReader> {
        val array = fields[name] as? ArrayValue ?: throw malformed(name)
        return array.values.mapIndexed { index, element ->
            val map = element as? MapValue ?: throw malformed("$name[$index]")
            DocumentReader("$path.$name[$index]", map.fields)
        }
    }

    private fun malformed(name: String): DataException.MalformedData {
        val found = fields[name]?.let { it::class.simpleName } ?: "nothing"
        return DataException.MalformedData("Missing or invalid field '$name' in $path (found $found)")
    }
}
