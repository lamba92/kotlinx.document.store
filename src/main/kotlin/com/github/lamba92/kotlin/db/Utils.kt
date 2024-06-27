package com.github.lamba92.kotlin.db

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

val JsonObject.id
    get() = get(KotlinxDb.ID_PROPERTY_NAME)?.jsonPrimitive?.contentOrNull?.toLong()
