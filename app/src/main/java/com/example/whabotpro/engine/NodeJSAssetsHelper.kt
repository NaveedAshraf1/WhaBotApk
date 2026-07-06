package com.example.whabotpro.engine

import android.content.Context
import android.content.res.AssetManager
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * Extracts the bundled Node.js project from assets to the app's files directory.
 */
object NodeJSAssetsHelper {

    fun extractProject(context: Context): File {
        val destDir = File(context.filesDir, "nodejs-project")
        val assetManager = context.assets
        copyAssetsRecursively(assetManager, "nodejs-project", destDir)
        return destDir
    }

    private fun copyAssetsRecursively(assetManager: AssetManager, assetPath: String, destDir: File) {
        val assets = assetManager.list(assetPath) ?: return
        if (assets.isEmpty()) {
            // It's a file
            destDir.parentFile?.mkdirs()
            assetManager.open(assetPath).use { input ->
                FileOutputStream(destDir).use { output ->
                    input.copyTo(output)
                }
            }
            return
        }
        destDir.mkdirs()
        for (asset in assets) {
            copyAssetsRecursively(assetManager, "$assetPath/$asset", File(destDir, asset))
        }
    }
}
