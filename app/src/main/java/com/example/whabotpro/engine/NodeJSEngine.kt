package com.example.whabotpro.engine

import android.content.Context
import android.util.Log
import java.io.File
import java.util.concurrent.Executors

/**
 * Embedded Node.js engine using nodejs-mobile.
 */
object NodeJSEngine {

    private val executor = Executors.newSingleThreadExecutor()

    init {
        System.loadLibrary("nodejs-bridge")
    }

    /**
     * Start the embedded Node.js process with the given script.
     */
    fun start(context: Context) {
        executor.execute {
            try {
                val projectDir = NodeJSAssetsHelper.extractProject(context)
                val scriptFile = File(projectDir, "bootstrap.mjs")
                val args = arrayOf(
                    "node",
                    scriptFile.absolutePath,
                    projectDir.absolutePath
                )
                val exitCode = startNodeWithArguments(args)
                Log.d("NodeJSEngine", "Node.js exited with code $exitCode")
            } catch (e: Exception) {
                Log.e("NodeJSEngine", "Node.js failed: ${e.message}", e)
            }
        }
    }

    private external fun startNodeWithArguments(arguments: Array<String>): Int
}
