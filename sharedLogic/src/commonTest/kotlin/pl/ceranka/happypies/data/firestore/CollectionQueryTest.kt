package pl.ceranka.happypies.data.firestore

import pl.ceranka.happypies.data.firestore.FirestoreValue.ArrayValue
import pl.ceranka.happypies.data.firestore.FirestoreValue.MapValue
import pl.ceranka.happypies.data.firestore.FirestoreValue.ServerTimestamp
import pl.ceranka.happypies.data.firestore.FirestoreValue.StringValue
import kotlin.test.Test
import kotlin.test.assertFailsWith

class CollectionQueryTest {

    @Test
    fun acceptsAValidQuery() {
        CollectionQuery(
            collection = "reports",
            whereEqualTo = mapOf("clientId" to StringValue("c1")),
            orderBy = listOf(OrderBy("createdAt", descending = true)),
            limit = 20,
        )
    }

    @Test
    fun rejectsANonPositiveLimit() {
        assertFailsWith<IllegalArgumentException> { CollectionQuery("reports", limit = 0) }
    }

    @Test
    fun rejectsAWriteOnlyValueInAFilter() {
        assertFailsWith<IllegalArgumentException> {
            CollectionQuery("reports", whereEqualTo = mapOf("createdAt" to ServerTimestamp))
        }
    }

    @Test
    fun rejectsAWriteOnlyValueNestedInAFilter() {
        assertFailsWith<IllegalArgumentException> {
            CollectionQuery("reports", whereEqualTo = mapOf("tags" to ArrayValue(listOf(MapValue(mapOf("at" to ServerTimestamp))))))
        }
    }

    @Test
    fun rejectsAnUnsupportedValueInAFilter() {
        assertFailsWith<IllegalArgumentException> {
            CollectionQuery("reports", whereEqualTo = mapOf("place" to FirestoreValue.Unsupported("GeoPoint")))
        }
    }
}