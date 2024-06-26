package com.github.lamba92.kotlin.db

@JvmInline
value class FileSize internal constructor(val size: Long)

val Number.bytes get() = FileSize(this.toLong())
val Number.kilobytes get() = FileSize(this.toLong() * 1024)
val Number.megabytes get() = FileSize(this.toLong() * 1024 * 1024)
val Number.gigabytes get() = FileSize(this.toLong() * 1024 * 1024 * 1024)
val FileSize.bytes
    get() = size
val FileSize.kilobytes
    get() = size.toDouble() / 1024
val FileSize.megabytes
    get() = size.toDouble() / 1024 / 1024
val FileSize.gigabytes
    get() = size.toDouble() / 1024 / 1024 / 1024