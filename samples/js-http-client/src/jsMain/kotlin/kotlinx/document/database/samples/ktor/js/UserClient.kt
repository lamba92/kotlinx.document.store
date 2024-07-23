@file:OptIn(ExperimentalJsExport::class, DelicateCoroutinesApi::class)

package kotlinx.document.database.samples.ktor.js

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.js.Js
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.serialization.kotlinx.json.json
import kotlin.js.Promise
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.promise
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.document.database.KotlinxDocumentDatabase
import kotlinx.document.database.browser.IndexedDBStore
import kotlinx.document.database.samples.User
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class CacheEntry<K, V>(
    val cacheKey: K,
    val data: V,
    val lastUpdate: Instant = Clock.System.now()
)

@JsExport
class UserClient(
    val protocol: String = "http",
    val host: String = "localhost",
    val port: Int = 8080,
    cacheDurationInSeconds: Int = 1.days.inWholeSeconds.toInt()
) : AutoCloseable {

    private val cacheDuration = cacheDurationInSeconds.seconds

    private val httpClient = HttpClient(Js) {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
            })
        }
    }

    private val cache = KotlinxDocumentDatabase(IndexedDBStore)

    @Serializable
    data class GetAllUsersRequest(val page: Int, val pageSize: Int)

//    fun getAllUsers(request: GetAllUsersRequest): Promise<Page<User>> = GlobalScope.promise {
//        val collection: ObjectCollection<CacheEntry<GetAllUsersRequest, Page<User>>> =
//            cache.getObjectCollection("getAllUsers")
//
//        val cacheHit = collection.iterateAll()
//            .filter { it.cacheKey == request && it.lastUpdate > Clock.System.now() - cacheDuration }
//            .firstOrNull()
//            ?.data
//
//        if (cacheHit != null) {
//            return@promise cacheHit
//        }
//
////        val result = httpClient.get("$protocol://$host:$port/users/all?page=$page&pageSize=$pageSize")
//            .body<Page<User>>()
//
////        collection.updateWhere()
//
//        result
//
//    }

    fun getUser(id: Int): Promise<User> = GlobalScope.promise {
        httpClient.get("$protocol://$host:$port/users/$id")
            .body<User>()
    }

    fun insertUser(user: User): Promise<User> = GlobalScope.promise {
        httpClient.post("$protocol://$host:$port/users") {
            setBody(user)
        }.body<User>()
    }

    fun updateUser(user: User): Promise<User> = GlobalScope.promise {
        httpClient.put("$protocol://$host:$port/users") {
            setBody(user)
        }.body<User>()
    }

    fun searchUsers(name: String): Promise<List<User>> = GlobalScope.promise {
        httpClient.get("$protocol://$host:$port/users/search?name=$name")
            .body<List<User>>()
    }

    fun loadTestUsers() = GlobalScope.promise {
        httpClient.get("$protocol://$host:$port/insertTestUsers")
            .status.value
    }

    override fun close() {
        httpClient.close()
    }
}
