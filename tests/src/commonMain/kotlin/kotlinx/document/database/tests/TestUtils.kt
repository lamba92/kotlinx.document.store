package kotlinx.document.database.tests

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Duration.Companion.days

@Serializable
data class TestUser(
    val name: String,
    val age: Int,
    val isAdult: Boolean = true,
    val birthDate: Instant = if (isAdult) Clock.System.now() - (365 * 20).days else Clock.System.now(),
    val addresses: List<Address> = emptyList(),
    @SerialName("_id") val id: Long? = null,
) {
    companion object {
        val Mario =
            TestUser(
                name = "mario",
                age = 20,
                addresses =
                    listOf(
                        Address("Mushroom Kingdom", 1),
                        Address("Peach's Castle", 2),
                        Address("New York", 3),
                    ),
            )
        val Luigi =
            TestUser(
                name = "luigi",
                age = 20,
                addresses =
                    listOf(
                        Address("Sarasaland", 1),
                        Address("Daisy's Castle", 2),
                        Address("New York", 3),
                    ),
            )
    }
}

@Serializable
data class Address(
    val street: String,
    val number: Int,
)
