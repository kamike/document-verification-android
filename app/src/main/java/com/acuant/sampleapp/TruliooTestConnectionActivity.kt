package com.acuant.sampleapp

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.TextView
import android.widget.ProgressBar

import com.trulioo.normalizedapi.*
import com.trulioo.normalizedapi.api.ConnectionApi
import com.trulioo.normalizedapi.ApiCallback
import com.trulioo.normalizedapi.ApiClient

class TruliooTestConnectionActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_trulioo_test_connection)

        var progressBar: ProgressBar = findViewById<ProgressBar>(R.id.testProgressBar)
        progressBar.visibility = View.VISIBLE

        val truliooHelper = TruliooVerificationHelper()
        truliooHelper.init()
        val connectionClient = truliooHelper.getConnectionClient()

        connectionClient.testAuthenticationAsync(object : ApiCallback<String> {
            override fun onFailure(e: ApiException, statusCode: Int, responseHeaders: Map<String, List<String>>?) {
                setResultBox("failed\n"+e.message)
            }

            override fun onSuccess(result: String, statusCode: Int, responseHeaders: Map<String, List<String>>) {
                setResultBox(result+"\n connected to: "+ truliooHelper.getBasePath())
            }

            override fun onUploadProgress(bytesWritten: Long, contentLength: Long, done: Boolean) {
                //To change body of generated methods, choose Tools | Templates.
            }

            override fun onDownloadProgress(bytesRead: Long, contentLength: Long, done: Boolean) {
                //To change body of generated methods, choose Tools | Templates.
            }
        })
    }

    fun setResultBox(text:String){
        runOnUiThread(object : Runnable {
            override fun run(){
                var progressBar: ProgressBar = findViewById<ProgressBar>(R.id.testProgressBar)
                progressBar.visibility = View.GONE
                val resultBox = findViewById<TextView>(R.id.resultBox)
                resultBox.setText(text)
            }
        })
    }

    fun exit(view: View) {
        val result = Intent(
                this,
                MainActivity::class.java
        )
        startActivity(result)
    }

}
