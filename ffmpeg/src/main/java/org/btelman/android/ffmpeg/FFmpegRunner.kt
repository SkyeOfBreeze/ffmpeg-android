package org.btelman.android.ffmpeg

import android.content.Context
import org.btelman.android.shellutil.Executor
import org.btelman.android.shellutil.BinaryUpdateChecker
import java.io.File
import java.io.InputStream
import java.lang.IllegalArgumentException
import java.lang.IllegalStateException
import java.util.*
import kotlin.collections.HashMap

object FFmpegRunner{
    private val runningProcesses = HashMap<UUID, Executor>()

    /**
     * Check to see if the binary installed to local context.filesDir is outdated
     */
    fun checkIfUpToDate(context: Context, binaryPath : File = File(context.filesDir, "ffmpeg")) : Boolean{
        if(!binaryPath.exists()) return false
        return BinaryUpdateChecker.CheckBinaryCorrectVersion(context.assets, "ffmpeg", binaryPath)
    }

    /**
     * Synchronous copying of binary. This will copy the ffmpeg binary to the executable location,
     * and make it executable
     */
    fun update(context: Context, inputStream: InputStream? = null, binaryPath : File = File(context.filesDir, "ffmpeg")){
        @Suppress("NAME_SHADOWING") var inputStream = inputStream //we want to be able to write to it
        if(inputStream == null){
            val assetsManager = context.assets
            val asset = BinaryUpdateChecker.GetPreferredBinaryLocation(assetsManager, "ffmpeg")
            asset?:throw IllegalAccessException("No acceptable binary found! Device may not support ffmpeg!")
            inputStream = assetsManager.open(asset)
        }
        BinaryUpdateChecker.copyAsset(inputStream!!, binaryPath)
    }

    @Synchronized
    fun startProcess(builder : Builder, uuid: UUID = UUID.randomUUID()) : UUID{
        if(runningProcesses[uuid] != null)
            throw IllegalStateException("Process with $uuid is already running!")

        builder.onCompleteInternal = {
            if(runningProcesses[uuid] != null)
                runningProcesses.remove(uuid)
        }
        val executor = builder.build()
        runningProcesses[uuid] = executor.also {
            val filePath = builder.binaryPath.absolutePath
            executor.execute("$filePath ${builder.command}")
        }
        return uuid
    }

    @Synchronized
    fun checkIfRunning(uuid: UUID) : Boolean{
        return runningProcesses[uuid] != null
    }

    @Synchronized
    fun killAll(){
        runningProcesses.forEach {
            it.value.killWithGarbageData()
        }
    }

    @Synchronized
    fun kill(uuid: UUID){
        runningProcesses[uuid]?.killWithGarbageData()
    }

    class Builder(context: Context){
        var OnStart : () -> Unit = {}
        var OnProcess : (Process) -> Unit = {}
        var OnProgress : (String) -> Unit = {}
        var OnError : (String) -> Unit = {}
        var OnComplete : (Int?)->Unit = {}
        internal var onCompleteInternal : (Int?)->Unit = {}
        var command : String? = null
        var binaryPath : File = File(context.filesDir, "ffmpeg")

        private val onCompleteDispatcher = fun(statusCode : Int?){
            onCompleteInternal(statusCode)
            OnComplete(statusCode)
        }
        /**
         * Set event thread to thread that calls build(). Otherwise, events will happen on the main thread
         */
        var runEventThreadOnCurrent = true

        internal fun build() : Executor{
            if(command.isNullOrEmpty())
                throw IllegalArgumentException("command must not null or empty!")
            return Executor(OnStart, OnProcess, OnProgress, OnError, onCompleteDispatcher).also {
                if(runEventThreadOnCurrent)
                    it.setEventThread()
            }
        }
    }
}

