package kotlinx.document.store.core

import kotlinx.coroutines.NonCancellable

/**
 * Represents a closable resource that implements a suspendable `close` operation.
 * This interface is useful for managing resources that need to perform cleanup
 * or release operations in a coroutine context.
 */
public fun interface SuspendCloseable {
    /**
     * Closes this resource.
     *
     * This function may throw, thus it is strongly recommended to use the [use] function instead,
     * which closes this resource correctly whether an exception is thrown or not.
     *
     * Implementers of this interface should pay increased attention to cases where the close operation may fail.
     * It is recommended that all underlying resources are closed and the resource internally is marked as closed
     * before throwing an exception. Such a strategy ensures that the resources are released in a timely manner,
     * and avoids many problems that could come up when the resource wraps, or is wrapped, by another resource.
     *
     * Note that calling this function more than once may have some visible side effect.
     * However, implementers of this interface are strongly recommended to make this function idempotent.
     *
     *
     * Consider also using [NonCancellable] to avoid cancellation of the close operation:
     * ```kotlin
     * try {
     *    // Perform operations
     * } finally {
     *    withContext(NonCancellable) {
     *        closeable.close()
     *    }
     * }
     * ```
     */
    public suspend fun close()
}

/**
 * Executes the given [block] function on this resource and ensures that it is
 * properly closed after execution. This is useful for working with resources
 * that need to be closed after use, such as files or network connections,
 * in a coroutine context.
 *
 * @param T The return type of the block function.
 * @param R The type of the resource implementing [SuspendCloseable].
 * @param block The function to be executed with this resource.
 *
 * @return The result of [block] function.
 *
 * @throws Exception if an exception occurs during resource usage or closing.
 */
public suspend fun <T, R : SuspendCloseable> R.use(block: suspend (R) -> T): T {
    return try {
        block(this)
    } finally {
        close()
    }
}
