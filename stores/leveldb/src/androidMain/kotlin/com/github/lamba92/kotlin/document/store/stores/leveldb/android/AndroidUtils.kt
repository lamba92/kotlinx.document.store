package com.github.lamba92.kotlin.document.store.stores.leveldb.android

import android.content.Context
import com.github.lamba92.kotlin.document.store.stores.leveldb.LevelDBStore
import com.github.lamba92.leveldb.LevelDBOptions
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.createDirectories

/**
 * Opens a new [LevelDBStore] instance in the storage directory of the app.
 *
 * Intermediate directories are created if they do not exist.
 *
 * @param options Optional [LevelDBOptions] to configure LevelDB (default is [LevelDBOptions.DEFAULT]).
 * @param name The name of the database directory (default is `"leveldb"`).
 * @return A new [LevelDBStore] instance backed by the LevelDB database at the specified path.
 */
public fun Context.openLevelDBStore(
    options: LevelDBOptions = LevelDBOptions.DEFAULT,
    name: String = "leveldb",
): LevelDBStore =
    Path(getDatabasePath(name).path)
        .createDirectories()
        .let { LevelDBStore.open(it.absolutePathString(), options) }
