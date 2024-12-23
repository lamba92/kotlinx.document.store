package com.github.lamba92.kotlin.document.store.leveldb

import com.github.lamba92.kotlin.document.store.stores.leveldb.LevelDBStore
import com.github.lamba92.leveldb.LevelDBOptions
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.createDirectories

/**
 * Opens a new [LevelDBStore] instance at the specified path with the given options.
 *
 * Intermediate directories are created if they do not exist. The database will be created
 * at the specified path as a directory.
 *
 * @param path The file system path where the LevelDB database will be created or accessed.
 *             Can be provided as a `String` or a `Path`.
 * @param options Optional [LevelDBOptions] to configure LevelDB (default is [LevelDBOptions.DEFAULT]).
 * @return A new `LevelDBStore` instance backed by the LevelDB database at the specified path.
 */
public fun LevelDBStore.Companion.open(
    path: Path,
    options: LevelDBOptions = LevelDBOptions.DEFAULT,
): LevelDBStore = open(path.createDirectories().absolutePathString(), options)
