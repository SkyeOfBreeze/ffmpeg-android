package org.btelman.android.ffmpeg.demo

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_main.*
import org.btelman.android.ffmpeg.FFmpegRunner
import java.lang.IllegalStateException
import java.util.*

class MainActivity : AppCompatActivity() {
    val ffmpegProcess = UUID.randomUUID()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setupUI()
        runUpdateFlow()
    }

    private fun setupUI() {
        executeButton.setOnClickListener{
            runFFmpegUsingEditText()
        }
        updateFileButton.setOnClickListener{
            runUpdateFlow()
        }
    }

    fun runUpdateFlow(popupInfoDialogIfSuccess : Boolean = false){
        val upToDate = FFmpegRunner.checkIfUpToDate(this)
        if(upToDate){
            if(popupInfoDialogIfSuccess){
                var alertDialog : AlertDialog? = null
                alertDialog = AlertDialog.Builder(this).also {
                    it.setTitle("FFmpeg up to date")
                    it.setMessage("FFmpeg binary is already up to date. Do you want to replace it with a custom ffmpeg binary?")
                    it.setNegativeButton("No"){ _, _ ->
                        alertDialog?.dismiss()
                    }
                    it.setPositiveButton("Yes"){ _, _ ->
                        alertDialog?.dismiss()
                        openFilePicker()
                    }
                    it.setCancelable(false)
                }.show()
            }
        }
        else if(!upToDate){
            var alertDialog : AlertDialog? = null
            alertDialog = AlertDialog.Builder(this).also {
                it.setTitle("Update FFMpeg?")
                it.setMessage("FFmpeg binary is outdated or does not exist")
                it.setNegativeButton("No"){ _, _ ->
                    alertDialog?.dismiss()
                }
                it.setPositiveButton("Yes"){ _, _ ->
                    alertDialog?.dismiss()
                    FFmpegRunner.update(this)
                }
                it.setCancelable(false)
            }.show()
        }
    }

    private fun openFilePicker() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun runFFmpegUsingEditText(){
        val builder = FFmpegRunner.Builder(this).also {
            it.OnComplete = {statusCode ->
                var string : String = ffmpegStatusText.text.toString()
                string+="\n"
                string+="Completed with status code "
                string+=statusCode.toString()
                ffmpegStatusText.text = string
            }
            it.OnProgress = {progressText ->
                var string : String = ffmpegStatusText.text.toString()
                string+="\n"
                string+=progressText
                ffmpegStatusText.text = string
            }
            it.OnError = {error ->
                var string : String = ffmpegStatusText.text.toString()
                string+="\n"
                string+=error
                ffmpegStatusText.text = string
            }
        }
        builder.command = ffmpegEditText.text.toString()
        if(builder.command.isNullOrEmpty()){
            Toast.makeText(this, "Command is empty! Cannot run!", Toast.LENGTH_SHORT).show()
            return
        }
        ffmpegStatusText.text = builder.command
        if(!FFmpegRunner.checkIfRunning(ffmpegProcess)){
            val returnedUUID = FFmpegRunner.startProcess(builder, ffmpegProcess)
            if(returnedUUID != ffmpegProcess)
                throw IllegalStateException("returnedUUID does not match ffmpegProcess uuid!")
        }
        else{
            Snackbar.make(window.decorView, "Already running!", Snackbar.LENGTH_LONG).also {
                it.setAction("Kill FFmpeg"){
                    FFmpegRunner.kill(ffmpegProcess)
                }
            }.show()
        }
    }
}
