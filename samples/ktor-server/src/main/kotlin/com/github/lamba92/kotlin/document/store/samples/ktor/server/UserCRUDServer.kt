@file:OptIn(ExperimentalSerializationApi::class)

package com.github.lamba92.kotlin.document.store.samples.ktor.server

import com.github.lamba92.kotlin.document.store.core.ObjectCollection
import com.github.lamba92.kotlin.document.store.core.find
import com.github.lamba92.kotlin.document.store.samples.Page
import com.github.lamba92.kotlin.document.store.samples.User
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

@Suppress("FunctionName")
fun Application.UserCRUDServer(userCollection: ObjectCollection<User>) {
    install(ContentNegotiation) {
        json(
            Json {
                prettyPrint = true
            },
        )
    }

    routing {
        route("users") {
            get("all") {
                val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 0
                val pageSize = call.request.queryParameters["pageSize"]?.toIntOrNull() ?: 10

                val totalElements = userCollection.size()
                val totalPages = (totalElements + pageSize - 1) / pageSize

                val users =
                    userCollection.iterateAll()
                        .drop(page * pageSize)
                        .take(pageSize)
                        .toList()

                call.respond(Page(users, page, pageSize, totalElements.toInt(), totalPages.toInt()))
            }
            get("{id}") {
                val id = call.parameters["id"]?.toLongOrNull()
                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest)
                    return@get
                }
                val user = userCollection.findById(id)
                if (user == null) {
                    call.respond(HttpStatusCode.NotFound)
                    return@get
                }
                call.respond(user)
            }
            put {
                val user = call.receive<User>()
                userCollection.insert(user)
                call.respond(status = HttpStatusCode.Created, user)
            }
            post {
                val user = call.receive<JsonObject>()
                val id = user["id"]?.jsonPrimitive?.longOrNull
                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest)
                    return@post
                }
                userCollection.jsonCollection.updateById(id) { user }
                call.respond(HttpStatusCode.OK)
            }
            get("search") {
                val name = call.request.queryParameters["name"]
                if (name == null) {
                    call.respond(HttpStatusCode.BadRequest)
                    return@get
                }
                val users = userCollection.find("name", name)
                call.respond(users)
            }
            get("insertTestUsers") {
                val users =
                    Thread.currentThread()
                        .contextClassLoader
                        .getResourceAsStream("testUsers.json")
                        ?.let { Json.decodeFromStream<List<User>>(it) }
                        ?.map { userCollection.insert(it) }

                when (users) {
                    null -> call.respond(HttpStatusCode.InternalServerError)
                    else -> call.respond(users)
                }
            }
        }
    }
}
