package kotlinx.document.database

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
