package kotlinx.document.store

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

public fun <T> Flow<T>.drop(count: Long): Flow<T> {
    require(count >= 0) { "Drop count should be non-negative, but had $count" }
    return flow {
        var skipped = 0L
        collect { value ->
            if (skipped >= count) emit(value) else ++skipped
        }
    }
}

public fun <T> Sequence<T>.drop(count: Long): Sequence<T> {
    require(count >= 0) { "Drop count should be non-negative, but had $count" }
    return sequence {
        var skipped = 0L
        forEach { value ->
            if (skipped >= count) yield(value) else ++skipped
        }
    }
}

// available in 1.9.0 but not in the version bundled inside intellijIdea
public fun <T> Flow<T>.chunked(size: Int): Flow<List<T>> {
    require(size >= 1) { "Expected positive chunk size, but got $size" }
    return flow {
        var result: ArrayList<T>? = null // Do not preallocate anything
        collect { value ->
            // Allocate if needed
            val acc = result ?: ArrayList<T>(size).also { result = it }
            acc.add(value)
            if (acc.size == size) {
                emit(acc)
                // Cleanup, but don't allocate -- it might've been the case this is the last element
                result = null
            }
        }
        result?.let { emit(it) }
    }
}
