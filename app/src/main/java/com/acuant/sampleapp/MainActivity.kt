package com.acuant.sampleapp

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.location.LocationManager
import android.net.ConnectivityManager
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.util.Base64
import android.util.Log
import android.view.Surface
import android.view.View
import android.view.WindowManager
import android.widget.*
import com.acuant.acuantcamera.camera.AcuantCameraActivity
import com.acuant.acuantcamera.camera.AcuantCameraOptions
import com.acuant.acuantcamera.constant.*
import com.acuant.acuantcommon.exception.AcuantException
import com.acuant.acuantcommon.initializer.AcuantInitializer
import com.acuant.acuantcommon.initializer.IAcuantPackageCallback
import com.acuant.acuantcommon.model.*
import com.acuant.acuantfacematchsdk.AcuantFaceMatch
import com.acuant.acuantfacematchsdk.model.FacialMatchData
import com.acuant.acuantfacematchsdk.service.FacialMatchListener
import com.acuant.acuanthgliveness.model.FaceCapturedImage
import com.acuant.acuantimagepreparation.AcuantImagePreparation
import com.acuant.acuantimagepreparation.background.EvaluateImageListener
import com.acuant.acuantimagepreparation.initializer.ImageProcessorInitializer
import com.acuant.acuantimagepreparation.model.AcuantImage
import com.acuant.acuantimagepreparation.model.CroppingData
import com.acuant.acuantipliveness.AcuantIPLiveness
import com.acuant.acuantipliveness.constant.FacialCaptureConstant
import com.acuant.acuantipliveness.facialcapture.model.FacialCaptureResult
import com.acuant.acuantipliveness.facialcapture.model.FacialSetupResult
import com.acuant.acuantipliveness.facialcapture.service.FacialCaptureCredentialListener
import com.acuant.acuantipliveness.facialcapture.service.FacialCaptureLisenter
import com.acuant.acuantipliveness.facialcapture.service.FacialSetupLisenter
import com.acuant.sampleapp.utils.CommonUtils
import com.blankj.utilcode.util.GsonUtils
import com.blankj.utilcode.util.LogUtils
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.joda.time.DateTime
import java.io.*
import kotlin.concurrent.thread


class MainActivity : AppCompatActivity() {
    private var progressDialog: LinearLayout? = null
    private var progressText: TextView? = null
    private var capturedSelfieImage: Bitmap? = null
    private var capturedFaceImage: Bitmap? = null
    private var capturedBarcodeString: String? = null
    private var frontCaptured: Boolean = false
    private var isRetrying: Boolean = false
    private var currentRetries: Int = 0
    private var idButton: Button? = null
    private var capturingImageData: Boolean = true
    private var capturingSelfieImage: Boolean = false
    private var facialResultString: String? = null
    private var facialLivelinessResultString: String? = null
    private var documentInstanceID: String? = null
    private var autoCaptureEnabled: Boolean = true
    private var numerOfClassificationAttempts: Int = 0
    private var isInitialized = false
    private var isIPLivenessEnabled = false
    private var documentTypeDropDown:Spinner? = null
    private var recentImage: AcuantImage? = null

    private val shouldUseLocation : Boolean = true

    fun cleanUpTransaction() {
        facialResultString = null
        capturedSelfieImage = null
        capturedFaceImage = null
        capturedBarcodeString = null
        isRetrying = false
        currentRetries = 0
        capturingImageData = true
        documentInstanceID = null
        numerOfClassificationAttempts = 0
        TruliooInformationStorage.cleanup()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
        idButton = findViewById(R.id.main_id_passport)

        val autoCaptureSwitch = findViewById<Switch>(R.id.autoCaptureSwitch)
        autoCaptureSwitch.setOnCheckedChangeListener { _, isChecked ->
            autoCaptureEnabled = isChecked
            TruliooInformationStorage.isAutoCaptureEnabled = isChecked
        }

        documentTypeDropDown = findViewById<Spinner>(R.id.docTypeDropDown)

        progressDialog = findViewById(R.id.main_progress_layout)
        progressText = findViewById(R.id.pbText)

    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            1 -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if ((ContextCompat.checkSelfPermission(this@MainActivity,
                                    Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)) {
                        getLocation()
                        return
                    }
                }
            }
        }
        locationFailureCallback("Location Permission Denied")
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        initializeSDK()
    }

    private fun initializeSDK() {
        setProgress(true, "Initializing...")
        initializeAcuantSdk(object: IAcuantPackageCallback{
            override fun onInitializeSuccess() {
                this@MainActivity.runOnUiThread {
                    isInitialized = true
                    setProgress(false)
                }
            }

            override fun onInitializeFailed(error: List<Error>) {
                LogUtils.i("===onInitializeFailed:", GsonUtils.toJson(error))
                this@MainActivity.runOnUiThread {
                    setProgress(false)
                    val alert = AlertDialog.Builder(this@MainActivity)
                    alert.setTitle("Error")
                    alert.setMessage("Could not initialize")
                    alert.setPositiveButton("OK") { dialog, _ ->
                        dialog.dismiss()
                    }
                    alert.show()
                }
            }

        })
    }

    fun requestLocationCallback() {
        setProgress(false)
        showDocumentCaptureCamera()
    }

    fun locationFailureCallback(message: String) {
        val alert = AlertDialog.Builder(this@MainActivity)
        alert.setTitle("Location Error")
        alert.setMessage(message)
        alert.setPositiveButton("OK") { dialog, _ ->
            dialog.dismiss()
            requestLocationCallback()
        }
        alert.show()
    }

    private fun getLocation() {
        val context = this@MainActivity
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (locationManager.isLocationEnabled) {
            LocationService.getLocation(locationManager, context)
        } else {
            locationFailureCallback("System Location is disabled")
        }
    }

    private fun requestLocation() {
        setProgress(true, "Processing...")
        val context = this@MainActivity
        if (ContextCompat.checkSelfPermission(context,
            Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this@MainActivity,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
        } else {
            getLocation()
        }
    }

    private fun setProgress(visible : Boolean, text : String = "") {
        if(visible) {
            progressDialog?.visibility = View.VISIBLE
            window.setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
        } else {
            progressDialog?.visibility = View.GONE
            window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
        }
        progressText?.text = text
    }

    private fun initializeAcuantSdk(callback:IAcuantPackageCallback){
        try{
            AcuantInitializer.initialize("acuant.config.xml", this, listOf(ImageProcessorInitializer()),
                    object: IAcuantPackageCallback{
                        override fun onInitializeSuccess() {
                            if(Credential.get().subscription == null || Credential.get().subscription.isEmpty()){
                                isIPLivenessEnabled = false
                                callback.onInitializeSuccess()
                            } else{
                                getFacialLivenessCredentials(callback)
                            }
                        }

                        override fun onInitializeFailed(error: List<Error>) {
                            LogUtils.i("===AcuantInitializer:", GsonUtils.toJson(error))

                            callback.onInitializeFailed(error)
                        }

                    })
        }
        catch(e: AcuantException){
            LogUtils.e("Acuant Error", e.toString())
            setProgress(false)
            val alert = AlertDialog.Builder(this@MainActivity)
            alert.setTitle("Error")
            alert.setMessage(e.toString())
            alert.setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
            alert.show()

        }
    }

    private fun getFacialLivenessCredentials(callback: IAcuantPackageCallback){
        AcuantIPLiveness.getFacialCaptureCredential(object:FacialCaptureCredentialListener{
            override fun onDataReceived(result: Boolean) {
                if(result){
                    runOnUiThread{
                        val isIPEnabledSwitch = findViewById<Switch>(R.id.isIPLivenessEnabled)
                        isIPEnabledSwitch.setOnCheckedChangeListener { _, isChecked ->
                            isIPLivenessEnabled = isChecked
                        }
                        isIPEnabledSwitch.visibility = View.VISIBLE
                    }
                }
                isIPLivenessEnabled = result
                callback.onInitializeSuccess()
            }

            override fun onError(errorCode: Int, description: String) {
                callback.onInitializeFailed(listOf())
            }
        })
    }

    private fun readFromFile(fileUri: String?): ByteArray{
        val file = File(fileUri)
        val bytes = ByteArray(file.length().toInt())
        try {
            val buf = BufferedInputStream(FileInputStream(file))
            buf.read(bytes, 0, bytes.size)
            buf.close()
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: Exception){
            e.printStackTrace()
        }
        return bytes
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == Constants.REQUEST_CAMERA_PHOTO && resultCode == AcuantCameraActivity.RESULT_SUCCESS_CODE) {
            val url = data?.getStringExtra(ACUANT_EXTRA_IMAGE_URL)
            capturedBarcodeString = data?.getStringExtra(ACUANT_EXTRA_PDF417_BARCODE)
            if (url != null) {
                setProgress(true, "Cropping...")
                AcuantImagePreparation.evaluateImage(this, CroppingData(url), object :
                    EvaluateImageListener {

                    override fun onSuccess(image: AcuantImage) {
                        setProgress(false)
                        recentImage = image
                        showConfirmation(!frontCaptured, false)
                    }

                    override fun onError(error: Error) {
                        showDialog(error.errorDescription)
                    }
                })
            } else {
                showDialog("Camera failed to return valid image path")
            }
        } else if (resultCode == Constants.REQUEST_CONFIRMATION) {
            if (recentImage == null) {
                handleImageError()
                return
            }
            val isFront = data!!.getBooleanExtra("IsFrontImage", true)
            val isConfirmed = data.getBooleanExtra("Confirmed", true)
            if (isConfirmed) {
                if (isFront) {
                    processFrontOfDocument(recentImage!!.image)
                } else {
                   processBackOfDocument(recentImage!!.image)
                }
            } else {
                showDocumentCaptureCamera()
            }
        } else if (resultCode == Constants.REQUEST_RETRY) {
            isRetrying = true
            currentRetries += 1
            showDocumentCaptureCamera()

        } else if (requestCode == Constants.REQUEST_CAMERA_IP_SELFIE) {
            when (resultCode) {
                ErrorCodes.ERROR_CAPTURING_FACIAL -> showFaceCaptureError()
                ErrorCodes.USER_CANCELED_FACIAL -> {
                    setProgress(true, "Getting Data...")
                    capturingSelfieImage = false
                    facialLivelinessResultString = "Facial Liveliness Failed"
                }
                else -> {
                    val userId = data?.getStringExtra(FacialCaptureConstant.ACUANT_USERID_KEY)!!
                    val token = data.getStringExtra(FacialCaptureConstant.ACUANT_TOKEN_KEY)!!
                    startFacialLivelinessRequest(token, userId)
                }
            }
        } else if (requestCode == Constants.REQUEST_CAMERA_HG_SELFIE){
            if (FaceCapturedImage.bitmapImage == null) {
                handleImageError()
                return
            }
            if(resultCode == FacialLivenessActivity.RESPONSE_SUCCESS_CODE){
                processSelfie(FaceCapturedImage.bitmapImage!!)
            }
            else{
                showFaceCaptureError()
            }
        }
    }

    private fun showFaceCaptureError(){
        val alert = AlertDialog.Builder(this@MainActivity)
        alert.setTitle("Error Capturing Face")
        alert.setMessage("Would you like to retry?")
        alert.setPositiveButton("YES") { dialog, _ ->
            dialog.dismiss()
            showFrontCamera()
        }
        alert.setNegativeButton("NO") { dialog, _ ->
            dialog.dismiss()
            showTruliooConfirm()
            capturingSelfieImage = false
            facialLivelinessResultString = "Facial Liveliness Failed"
        }
        alert.show()
    }

    private fun startFacialLivelinessRequest(token: String, userId:String){
        setProgress(true, "Getting Data...")
        AcuantIPLiveness.getFacialLiveness(
                token,
                userId,
                object: FacialCaptureLisenter {
                    override fun onDataReceived(result: FacialCaptureResult) {
                        facialLivelinessResultString = "Facial Liveliness: " + result.isPassed
                        val decodedString = Base64.decode(result.frame, Base64.DEFAULT)
                        capturedSelfieImage = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
                        setProgress(false)
                        processFacialMatch()
                    }

                    override fun onError(errorCode:Int, errorDescription: String) {
                        capturingSelfieImage = false
                        setProgress(false)
                        facialLivelinessResultString = "Facial Liveliness Failed"
                        val alert = AlertDialog.Builder(this@MainActivity)
                        alert.setTitle("Error Retreiving Facial Data")
                        alert.setMessage(errorDescription)
                        alert.setPositiveButton("OK") { dialog, _ ->
                            dialog.dismiss()
                        }
                        alert.show()
                    }
                }
        )
    }

    private fun hasInternetConnection():Boolean{
        val connectivityManager= this.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo=connectivityManager.activeNetworkInfo
        return networkInfo!=null && networkInfo.isConnected
    }

    // ID/Passport Clicked
    fun idPassPortClicked(@Suppress("UNUSED_PARAMETER") view: View) {
        if(!hasInternetConnection()){
            val alert = AlertDialog.Builder(this@MainActivity)
            alert.setTitle("Error")
            alert.setMessage("No internet connection available.")
            alert.setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
            alert.show()
        } else {
            cleanUpTransaction()
            TruliooInformationStorage.cardType = documentTypeDropDown?.getSelectedItem().toString()
            if(isInitialized){
                frontCaptured = false

                if (shouldUseLocation) {
                    requestLocation()
                } else {
                    showDocumentCaptureCamera()
                }
            } else {
                initializeSDK()
            }
        }
    }

    //Show Rear Camera to Capture Image of ID or Passport
    fun showDocumentCaptureCamera() {
        capturedBarcodeString = null
        val cameraIntent = Intent(
                this@MainActivity,
                AcuantCameraActivity::class.java
        )
        cameraIntent.putExtra(ACUANT_EXTRA_CAMERA_OPTIONS,
            AcuantCameraOptions.DocumentCameraOptionsBuilder()
                .setAutoCapture(autoCaptureEnabled)
                .build()
        )
        startActivityForResult(cameraIntent, Constants.REQUEST_CAMERA_PHOTO)
    }

    //Show Front Camera to Capture Live Selfie
    fun showFrontCamera() {
        try{
            capturingSelfieImage = true

            if(isIPLivenessEnabled){
                showIPLiveness()
            }
            else{
                showHGLiveness()
            }

        }
        catch(e: Exception){
            e.printStackTrace()
        }
    }

    private fun showIPLiveness(){
        setProgress(true, "Loading...")
        AcuantIPLiveness.getFacialSetup(object :FacialSetupLisenter{
            override fun onDataReceived(result: FacialSetupResult?) {
                setProgress(false)
                if(result != null){
                    val facialIntent = AcuantIPLiveness.getFacialCaptureIntent(this@MainActivity, result)
                    startActivityForResult(facialIntent, Constants.REQUEST_CAMERA_IP_SELFIE)
                }
                else{
                    handleInternalError()
                }
            }

            override fun onError(errorCode: Int, description: String?) {
                setProgress(false)
                handleInternalError()
            }
        })
    }
    private fun showHGLiveness(){
        val cameraIntent = Intent(
                this@MainActivity,
                FacialLivenessActivity::class.java
        )
        startActivityForResult(cameraIntent, Constants.REQUEST_CAMERA_HG_SELFIE)
    }

    fun handleInternalError(){
        runOnUiThread{
            val alert = AlertDialog.Builder(this@MainActivity)
            alert.setTitle("Internal Error")
            alert.setMessage("Would you like to retry?")
            alert.setNegativeButton("Proceed") { dialog, _ ->
                dialog.dismiss()
                capturingSelfieImage = false
                facialLivelinessResultString = "Facial Liveliness Failed"
            }
            alert.setPositiveButton("Retry") { dialog, _ ->
                dialog.dismiss()
                showFrontCamera()
            }
            alert.show()
        }
    }

    private fun handleImageError() {
        val alert = AlertDialog.Builder(this@MainActivity)
        alert.setTitle("Image Error")
        alert.setMessage("No image received from capture")
        alert.setPositiveButton("OK") { dialog, _ ->
            dialog.dismiss()
        }
        alert.show()
    }

    private fun showDialog(message: String, title: String = "Error",
                              yesOnClick: DialogInterface.OnClickListener? = null,
                              noOnClick: DialogInterface.OnClickListener? = null) {

        setProgress(false)
        val alert = android.support.v7.app.AlertDialog.Builder(this@MainActivity)
        alert.setTitle(title)
        alert.setMessage(message)
        if (yesOnClick != null) {
            alert.setPositiveButton("YES", yesOnClick)
            if (noOnClick != null) {
                alert.setNegativeButton("NO", noOnClick)
            }
        } else {
            alert.setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
        }
        alert.show()
    }

    private fun saveImage(image: Bitmap, type: String, retries: Int) {
        val file = saveRawImageToFile(bitmapToByteArray(image), type)
        ImageProcessor.ProcessImage(file, type, retries)
        when (type) {
            Constants.FRONT_SIDE -> {
                TruliooInformationStorage.frontImageFile = file
            }
            Constants.BACK_SIDE -> {
                TruliooInformationStorage.backImageFile = file
            }
            Constants.SELFIE -> {
                TruliooInformationStorage.selfieImageFile = file
            }
        }
    }

    private fun saveRawImageToFile(rawImage: ByteArray, type: String): File {
        val filesDir = this.filesDir
        val imageFile = File(filesDir, type + DateTime() + ".jpg")
        imageFile.writeBytes(rawImage)
        return imageFile
    }

    private fun bitmapToByteArray(image: Bitmap): ByteArray {
        val bos = ByteArrayOutputStream()
        image.compress(Bitmap.CompressFormat.JPEG, 100, bos)
        return bos.toByteArray()
    }

    // Process Front image
    private fun processFrontOfDocument(image: Bitmap) {
        val numberOfRetries = currentRetries
        GlobalScope.launch {
            saveImage(image, Constants.FRONT_SIDE, numberOfRetries)
        }
        frontCaptured = true
        currentRetries = 0
        if (isBackSideRequired()) {
            this@MainActivity.runOnUiThread {
                val alert = AlertDialog.Builder(this@MainActivity)
                alert.setTitle("Message")
                alert.setMessage(R.string.scan_back_side_id)
                alert.setPositiveButton("OK") { dialog, _ ->
                    dialog.dismiss()
                    showDocumentCaptureCamera()
                }
                alert.show()
            }
        } else {
            val alert = AlertDialog.Builder(this@MainActivity)
            alert.setTitle("Message")
            alert.setMessage("Capture Selfie Image")
            alert.setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
                showFrontCamera()
                setProgress(true, "Getting Live Photo...")
            }
            alert.setNegativeButton("CANCEL") { dialog, _ ->
                facialLivelinessResultString = "Facial Liveliness Skipped"
                showTruliooConfirm()
                dialog.dismiss()
            }
            alert.show()
        }
    }

    private fun processBackOfDocument(image: Bitmap) {
        val numberOfRetries = currentRetries
        GlobalScope.launch {
            saveImage(image, Constants.BACK_SIDE, numberOfRetries)
        }
        currentRetries = 0
        val alert = AlertDialog.Builder(this@MainActivity)
        alert.setTitle("Message")
        if (capturedBarcodeString != null && capturedBarcodeString!!.trim().isNotEmpty()) {
            alert.setMessage("Following barcode is captured.\n\n"
                    + "Barcode String :\n\n"
                    + capturedBarcodeString!!.subSequence(0, (capturedBarcodeString!!.length * 0.25).toInt())
                    + "...\n\n"
                    + "Capture Selfie Image now.")
        }
        else{
            alert.setMessage("Capture Selfie Image now.")
        }
        alert.setPositiveButton("OK") { dialog, _ ->
            setProgress(true, "Processing")
            showFrontCamera()
            dialog.dismiss()
        }
        alert.setNegativeButton("SKIP") { dialog, _ ->
            facialLivelinessResultString = "Facial Liveliness Failed"
            capturingSelfieImage = false
            showTruliooConfirm()
            dialog.dismiss()
        }
        alert.show()
    }

    private fun processSelfie(image: Bitmap) {
        val numberOfRetries = currentRetries
        saveImage(image, Constants.SELFIE, numberOfRetries)
        facialLivelinessResultString = "Facial Liveliness: true"
        currentRetries = 0
        showTruliooConfirm()
    }

    //process Facial Match
    fun processFacialMatch() {
        thread {
            while (capturingImageData) {
                Thread.sleep(100)
            }
            this@MainActivity.runOnUiThread {
                val facialMatchData = FacialMatchData()
                facialMatchData.faceImageOne = capturedFaceImage
                facialMatchData.faceImageTwo = capturedSelfieImage

                if(facialMatchData.faceImageOne != null && facialMatchData.faceImageTwo != null){
                    setProgress(true, "Processing selfie...")
                    AcuantFaceMatch.processFacialMatch(facialMatchData, FacialMatchListener { result ->
                        this@MainActivity.runOnUiThread {
                            setProgress(false)
                            if (result!!.error == null) {
                                val resultStr = CommonUtils.stringFromFacialMatchResult(result)
                                facialResultString = resultStr
                            } else {
                                val alert = AlertDialog.Builder(this@MainActivity)
                                alert.setTitle("Error")
                                alert.setMessage(result.error.errorDescription)
                                alert.setPositiveButton("OK") { dialog, _ ->
                                    dialog.dismiss()
                                }
                                alert.show()
                            }
                        }
                        capturingSelfieImage = false
                    })
                }
                else{
                    capturingSelfieImage = false
                }
            }
        }
    }

    //Show Confirmation UI
    fun showConfirmation(isFrontImage: Boolean, isBarcode: Boolean) {
        val confirmationIntent = Intent(
                this@MainActivity,
                ConfirmationActivity::class.java
        )
        confirmationIntent.putExtra("IsFrontImage", isFrontImage)
        confirmationIntent.putExtra("IsBarcode", isBarcode)
        if (recentImage != null) {
            image = recentImage!!.image
            confirmationIntent.putExtra("sharpness", recentImage!!.sharpness)
            confirmationIntent.putExtra("glare", recentImage!!.glare)
            confirmationIntent.putExtra("dpi", recentImage!!.dpi)
            confirmationIntent.putExtra("barcode", capturedBarcodeString)
        }
        startActivityForResult(confirmationIntent, Constants.REQUEST_CONFIRMATION)
    }

    fun showTruliooConfirm() {
        setProgress(false)
        val resultIntent = Intent(
                this@MainActivity,
                TruliooConfirmationActivity::class.java
        )
        startActivity(resultIntent)
    }

    //Correct orientation
    fun correctBitmapOrientation(image: Image?): Image? {
        if (image?.image != null && image.image.height > image.image.width) {
            val mWindowManager = getSystemService(WINDOW_SERVICE) as WindowManager
            val display = mWindowManager.defaultDisplay
            val angle = when (display.rotation) {
                Surface.ROTATION_0 // This is display orientation
                -> 270 // This is camera orientation
                Surface.ROTATION_90 -> 180
                Surface.ROTATION_180 -> 90
                Surface.ROTATION_270 -> 0
                else -> 180
            }

            val matrix = Matrix()
            matrix.postRotate(angle.toFloat())
            image.image = Bitmap.createBitmap(image.image, 0, 0, image.image.width, image.image.height, matrix, true)
            return image
        }
        return image
    }

    fun isBackSideRequired():Boolean{
        return !(TruliooInformationStorage.cardType != null && TruliooInformationStorage.cardType.equals("Passport"))
    }

    fun testTruliooConnection(view: View){
        val test = Intent(
                this@MainActivity,
                TruliooTestConnectionActivity::class.java
        )
        startActivity(test)
    }

    companion object {
        var image: Bitmap? = null
    }
}