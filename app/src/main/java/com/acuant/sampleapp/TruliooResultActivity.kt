package com.acuant.sampleapp

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Base64
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import com.trulioo.normalizedapi.ApiCallback
import com.trulioo.normalizedapi.ApiException
import com.trulioo.normalizedapi.model.*
import org.json.JSONArray
import java.io.ByteArrayOutputStream
import kotlin.concurrent.thread


class TruliooResultActivity : AppCompatActivity() {

    var resultTextView:TextView? = null
    var progressBar: ProgressBar? = null
    val truliooImageSizeLimit = 4*1024*1024 //4MB limit for image size

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_trulioo_result)

        resultTextView = findViewById<TextView>(R.id.resultTextBox) as TextView
        progressBar = findViewById<ProgressBar>(R.id.progressBar) as ProgressBar
        progressBar?.visibility = View.VISIBLE

        if(TruliooInformationStorage.firstName.isNullOrEmpty() || TruliooInformationStorage.lastName.isNullOrEmpty()||
                TruliooInformationStorage.frontImageFile == null || (TruliooInformationStorage.cardType == "DrivingLicence" && TruliooInformationStorage.backImageFile == null)){
            processFinish()
            val alert = AlertDialog.Builder(this@TruliooResultActivity)
            alert.setTitle("Error")
            alert.setMessage("Missing fields")
            alert.setPositiveButton("OK") { dialog, whichButton ->
                dialog.dismiss()
            }
            alert.show()
        }
        else{
            thread {
                val truliooHelper = TruliooVerificationHelper()
                truliooHelper.init()
                val verificationClient = truliooHelper.getVerificationClient()

                var request = buildRequest()

                verificationClient.verifyAsync(request, object : ApiCallback<VerifyResult> {
                    override fun onFailure(e: ApiException, statusCode: Int, responseHeaders: Map<String, List<String>>?) {
                        appendResultTextBox("Failed, status code: " + statusCode)
                        appendResultTextBox(e.message)
                        appendResultTextBox(e.responseBody)
                        if(responseHeaders!=null){
                            for ((k, v) in responseHeaders){
                                appendResultTextBox(k + ": " + v.toString())
                            }
                        }
                        processFinish()
                    }

                    override fun onSuccess(result: VerifyResult, statusCode: Int, responseHeaders: Map<String, List<String>>) {
                        appendResultTextBox("Success, status code: " + statusCode)
                        appendResultTextBox("Transaction ID: " + result.transactionID)
                        var record = result.record
                        var datasourceResult = record.datasourceResults.firstOrNull()
                        if(datasourceResult != null){
                            //output fields
                            appendResultTextBox("\nOutput Results:")
                            var outputFields = datasourceResult.datasourceFields
                            for(fieldRecord in outputFields){
                                appendResultTextBox(fieldRecord.fieldName +": " + fieldRecord.status)
                            }

                            appendResultTextBox("\nAppended Fields:")
                            var appendedFields = datasourceResult.appendedFields
                            var authenticityDetailString = ""
                            for(fieldRecord in appendedFields){
                                if(!fieldRecord.fieldName.equals("AuthenticityDetails")){
                                    appendResultTextBox(fieldRecord.fieldName + ": " +fieldRecord.data)
                                }
                                else{
                                    var arrayString = (fieldRecord.data as String)
                                    var jsonArray = arrayStringToJsonArray(arrayString)
                                    for (i in 0 until jsonArray.length()) {
                                        authenticityDetailString += toPrettyFormat(jsonArray.getJSONObject(i).toString()) + "\n"
                                    }
                                }
                            }
                            if(!authenticityDetailString.isNullOrEmpty()){
                                appendResultTextBox("\nAuthenticityDetails:")
                                appendResultTextBox(authenticityDetailString)
                            }
                        }
                        processFinish()
                    }

                    override fun onUploadProgress(bytesWritten: Long, contentLength: Long, done: Boolean) {
                    }

                    override fun onDownloadProgress(bytesRead: Long, contentLength: Long, done: Boolean) {
                    }
                })
            }
        }
    }

    fun buildRequest(): VerifyRequest{
        val document = Document()

        document.documentFrontImage(TruliooInformationStorage.frontImageFile.readBytes())
        document.documentType(TruliooInformationStorage.cardType)

        if(TruliooInformationStorage.backImageFile != null){
            document.documentBackImage(TruliooInformationStorage.backImageFile.readBytes())
        }

        if(TruliooInformationStorage.selfieImageFile != null){
            document.livePhoto(TruliooInformationStorage.selfieImageFile.readBytes())
        }

        return VerifyRequest()
                .acceptTruliooTermsAndConditions(true)
                .configurationName("Identity Verification")
                .countryCode(TruliooInformationStorage.countryCode)
                .dataFields(DataFields()
                        .personInfo(PersonInfo()
                                .firstGivenName(TruliooInformationStorage.firstName)
                                .firstSurName(TruliooInformationStorage.lastName))
                        .document(document))
    }



    fun processFinish(){
        runOnUiThread(object : Runnable {
            override fun run(){
                progressBar?.visibility = View.GONE
                resultTextView?.visibility = View.VISIBLE
            }
        })
    }

    fun onDone(view: View){
        cleanup()
        exit()
    }

    fun appendResultTextBox(text:String?){
        runOnUiThread(object : Runnable {
            override fun run(){
                if(!text.isNullOrEmpty()){
                    resultTextView?.append(text + "\n")
                }
            }
        })
    }

    fun exit() {
        val result = Intent(
                this,
                MainActivity::class.java
        )
        startActivity(result)
    }

    fun cleanup(){
        TruliooInformationStorage.cleanup()
    }

    fun bitmapTobase64(image: Bitmap): ByteArray {
        // this process needs to be in thread or AsyncTask
        // compression could be slow for large images
        val byteArrayStream = ByteArrayOutputStream()
        var quality = 100
        do {
            byteArrayStream.reset()
            image.compress(Bitmap.CompressFormat.JPEG, quality, byteArrayStream)
            quality -= 10
        } while(byteArrayStream.size() > truliooImageSizeLimit)
        return byteArrayStream.toByteArray()
    }

    fun arrayStringToJsonArray(arrayString: String): JSONArray{
        var array = JSONArray(arrayString)
        return array
    }

    fun toPrettyFormat(jsonString: String): String {
        val parser = JsonParser()
        val json = parser.parse(jsonString).asJsonObject
        val gson = GsonBuilder().setPrettyPrinting().create()
        return gson.toJson(json)
    }

    override fun onBackPressed() {
        cleanup()
        exit()
    }
}
