package kotlinx.document.database.tests

import kotlin.time.Duration.Companion.days
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class TestUser(
    val name: String,
    val age: Int,
    val isAdult: Boolean = true,
    val birthDate: Instant = if (isAdult) Clock.System.now() - (365 * 20).days else Clock.System.now(),
    val addresses: List<Address>? = null,
) {
    companion object {
        val Mario = TestUser("mario", 20)
        val Luigi = TestUser("luigi", 20)
    }
}

@Serializable
data class Address(
    val street: String,
    val number: Int
)