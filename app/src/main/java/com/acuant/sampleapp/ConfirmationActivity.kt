package com.acuant.sampleapp

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Paint.ANTI_ALIAS_FLAG
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.DisplayMetrics
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView


class ConfirmationActivity : AppCompatActivity() {

    var IsFrontImage: Boolean = true
    var IsBarcode: Boolean = false
    var isHealthCard: Boolean = false

    private var barcodeString: String? = null
    private var image: Bitmap? = null
    private var sharpness = -1
    private var glare = -1
    private var dpi = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_confirmation)
        IsFrontImage = intent.getBooleanExtra("IsFrontImage", true)
        isHealthCard = intent.getBooleanExtra("IsHealthCard", false)
        IsBarcode = intent.getBooleanExtra("IsBarcode", false)
        barcodeString = intent.getStringExtra("barcode")
        sharpness = intent.getIntExtra("sharpness", -1)
        glare = intent.getIntExtra("glare", -1)
        dpi = intent.getIntExtra("dpi", -1)
        image = MainActivity.image

        if(barcodeString != null){
            val barcodeText = findViewById<TextView>(R.id.barcodeText)
            barcodeText.text = "Barcode :" + barcodeString!!.substring(0,(barcodeString!!.length*0.2).toInt())+"..."
        }

        if (image != null) {

            val generalMessageText = findViewById<TextView>(R.id.generalMessageText)
            if (generalMessageText != null && image != null) {
                generalMessageText.text = "Ensure all texts are visible."
            } else if (image == null) {
                generalMessageText.text = "Could not crop image."
            }

            val sharpnessText = findViewById<TextView>(R.id.sharpnessText)
            val isBlurry = sharpness < SHARPNESS_THRESHOLD
            if (sharpnessText != null) {
                if (isBlurry) {
                    sharpnessText.text = "It is a blurry image. Sharpness Garde : $sharpness"
                } else {
                    sharpnessText.text = "It is a sharp image. Sharpness Garde : $sharpness"
                }
            }

            val glareText = findViewById<TextView>(R.id.glareText)
            val hasGlare = glare < GLARE_THRESHOLD
            if (glareText != null) {
                if (hasGlare) {
                    glareText.text = "Image has glare. Glare Garde : $glare"
                } else {
                    glareText.text = "Image doesn't have glare. Glare Garde : $glare"
                }
            }

            val dpiText = findViewById<TextView>(R.id.dpiText)
            if (dpiText != null) {
                TruliooInformationStorage.currentDPI = dpi
                when {
                    dpi < 550 -> {
                        dpiText.text = "DPI is low: $dpi"
                    }
                    dpi < 600 -> {
                        dpiText.text = "DPI is slightly low: $dpi"
                    }
                    else -> {
                        dpiText.text = "DPI: $dpi"
                    }
                }
            }

            val confrimationImage = findViewById<ImageView>(R.id.confrimationImage)
            if (confrimationImage != null ) {
                confrimationImage.setImageBitmap(image)
                confrimationImage.scaleType = ImageView.ScaleType.FIT_CENTER

                val displayMetrics = DisplayMetrics()
                windowManager.defaultDisplay.getMetrics(displayMetrics)
                val height = displayMetrics.heightPixels

                val lp = confrimationImage.layoutParams
                lp.height = (height * 0.4).toInt()
                confrimationImage.layoutParams = lp

            }
        } else {
            val confirmButton = findViewById<Button>(R.id.confirmButton)
            confirmButton.visibility = View.GONE
            val generalMessageText = findViewById<TextView>(R.id.generalMessageText)
            generalMessageText.text = "Could not crop image."

            val confrimationImage = findViewById<ImageView>(R.id.confrimationImage)
            if (confrimationImage != null) {
                confrimationImage.scaleType = ImageView.ScaleType.FIT_CENTER
                val displayMetrics = DisplayMetrics()
                windowManager.defaultDisplay.getMetrics(displayMetrics)
                val height = displayMetrics.heightPixels

                val lp = confrimationImage.layoutParams
                lp.height = (height * 0.4).toInt()
                confrimationImage.layoutParams = lp
                confrimationImage.setImageBitmap(textAsBitmap("Could not crop",200.0f,Color.RED))
                confrimationImage.visibility = View.VISIBLE
            }

        }

    }


    fun confirmClicked(view: View) {
        val result = Intent()
        result.putExtra("Confirmed", true)
        result.putExtra("IsFrontImage", IsFrontImage)
        this@ConfirmationActivity.setResult(Constants.REQUEST_CONFIRMATION, result)
        this@ConfirmationActivity.finish()

    }

    fun retryClicked(view: View) {
        val result = Intent()
        result.putExtra("Confirmed", false)
        result.putExtra("IsFrontImage", IsFrontImage)
        result.putExtra("IsBarcode", IsBarcode)
        this@ConfirmationActivity.setResult(Constants.REQUEST_RETRY, result)
        this@ConfirmationActivity.finish()
    }

    fun textAsBitmap(text: String, textSize: Float, textColor: Int): Bitmap {
        val paint = Paint(ANTI_ALIAS_FLAG)
        paint.textSize = textSize
        paint.color = textColor
        paint.textAlign = Paint.Align.LEFT
        val baseline = -paint.ascent() // ascent() is negative
        val width = (paint.measureText(text) + 0.5f).toInt() // round
        val height = (baseline + paint.descent() + 0.5f).toInt()
        val image = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(image)
        canvas.drawText(text, 0.0f, baseline, paint)
        return image
    }

    companion object {
        const val SHARPNESS_THRESHOLD = 50
        const val GLARE_THRESHOLD = 50
    }
}
