@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)

package product.lifecycle.windenergy

import app.App
import androidx.compose.ui.window.ComposeUIViewController
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import app.core.ui.theme.WindklarTheme
import app.data.local.source.SourceDatabase
import app.data.local.user.UserDatabase
import platform.Foundation.NSBundle
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask
import platform.UIKit.UIViewController
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create

fun MainViewController(): UIViewController {
    try {
        ensureSourceDatabaseFromBundle(SOURCE_DATABASE_NAME)
    } catch (error: Throwable) {
        return ComposeUIViewController {
            WindklarTheme {
                FatalStartupError(error.message ?: error.toString())
            }
        }
    }

    val sourceDriver = NativeSqliteDriver(SourceDatabase.Schema, SOURCE_DATABASE_NAME)
    applyPragmas(sourceDriver, "source")
    val userDriver = NativeSqliteDriver(UserDatabase.Schema, USER_DATABASE_NAME)
    applyPragmas(userDriver, "user")
    val sourceDatabase = SourceDatabase(sourceDriver)
    val userDatabase = UserDatabase(userDriver)
    val locationProvider = app.core.location.IosLocationProvider()
    return ComposeUIViewController { App(sourceDatabase, userDatabase, locationProvider) }
}

private fun ensureSourceDatabaseFromBundle(databaseName: String) {
    val bundledChecksum = readBundledSnapshotChecksum()
        ?: error("Gebündelte Stammdatenbank enthält keine Snapshot-Metadaten.")
    val targetPath = databasePath(databaseName)
    val installedChecksum = if (NSFileManager.defaultManager.fileExistsAtPath(targetPath)) {
        readSourceChecksum(databaseName)
    } else {
        null
    }

    if (installedChecksum == bundledChecksum) {
        println("MainViewController: Source database is current ($installedChecksum).")
        return
    }

    println("MainViewController: Replacing source database ($installedChecksum -> $bundledChecksum).")
    removeDatabaseFiles(databaseName)
    val bundlePath = NSBundle.mainBundle.pathForResource("windklar_source_seed", "db")
        ?: error("windklar_source_seed.db fehlt im iOS-App-Bundle.")
    val copied = NSFileManager.defaultManager.copyItemAtPath(bundlePath, targetPath, null)
    check(copied) { "Stammdatenbank konnte nicht aus dem App-Bundle kopiert werden." }

    val copiedChecksum = readSourceChecksum(databaseName)
    check(copiedChecksum == bundledChecksum) {
        "Kopierte Stammdatenbank ist unvollständig oder beschädigt."
    }
}

private fun readBundledSnapshotChecksum(): String? {
    val bundlePath = NSBundle.mainBundle.pathForResource("windklar_source_seed", "sha256")
        ?: return null
    return NSString.create(contentsOfFile = bundlePath, encoding = NSUTF8StringEncoding, error = null)?.toString()?.trim()
}

private fun readSourceChecksum(databaseName: String): String? = runCatching {
    val driver = NativeSqliteDriver(SourceDatabase.Schema, databaseName)
    try {
        SourceDatabase(driver)
            .snapshotMetadataQueries
            .selectLatestSnapshot()
            .executeAsOneOrNull()
            ?.checksum_sha256
    } finally {
        driver.close()
    }
}.getOrNull()

private fun applyPragmas(driver: NativeSqliteDriver, label: String) {
    try {
        driver.execute(null, "PRAGMA foreign_keys = ON;", 0)
        driver.execute(null, "PRAGMA journal_mode = WAL;", 0)
        driver.execute(null, "PRAGMA synchronous = NORMAL;", 0)
        driver.execute(null, "PRAGMA temp_store = MEMORY;", 0)
        driver.execute(null, "PRAGMA cache_size = -64000;", 0)
        println("MainViewController: Successfully applied SQLite PRAGMAs on iOS ($label).")
    } catch (e: Exception) {
        println("MainViewController ERROR: Failed to apply PRAGMAs on iOS ($label): ${e.message}")
    }
}

private fun databasePath(databaseName: String): String {
    val documentsDirectory = NSSearchPathForDirectoriesInDomains(
        NSDocumentDirectory,
        NSUserDomainMask,
        true,
    ).first() as String
    return "$documentsDirectory/$databaseName"
}

private fun removeDatabaseFiles(databaseName: String) {
    val fileManager = NSFileManager.defaultManager
    listOf(
        databasePath(databaseName),
        databasePath("$databaseName-wal"),
        databasePath("$databaseName-shm"),
    ).forEach { path ->
        if (fileManager.fileExistsAtPath(path)) {
            fileManager.removeItemAtPath(path, null)
        }
    }
}

@Composable
private fun FatalStartupError(message: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "WindKlar konnte die lokalen Stammdaten nicht laden.\n\n$message",
            color = WindklarTheme.colors.errorDarkRed,
            textAlign = TextAlign.Center,
        )
    }
}

private const val SOURCE_DATABASE_NAME = "windklar_source.db"
private const val USER_DATABASE_NAME = "windklar_user.db"
