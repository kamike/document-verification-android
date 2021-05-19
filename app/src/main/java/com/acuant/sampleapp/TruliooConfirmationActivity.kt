package com.acuant.sampleapp

import android.app.AlertDialog
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.*

class TruliooConfirmationActivity : AppCompatActivity() {

    var firstNameBox:EditText? = null
    var lastNameBox:EditText? = null
    var countryDropDown:Spinner? = null
    var documentTypeTextBox:TextView? = null
    var cancelButton: Button? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_trulioo_confirmation)

        var frontImage = TruliooInformationStorage.frontImageFile
        var backImage = TruliooInformationStorage.backImageFile
        var selfieImage = TruliooInformationStorage.selfieImageFile

        firstNameBox = findViewById<EditText>(R.id.firstNameBox)
        lastNameBox = findViewById<EditText>(R.id.lastNameBox)
        cancelButton = findViewById(R.id.cancelButton)
        cancelButton?.setOnClickListener {
            cancelClicked()
        }

        // supported countryCodes/ documentTypes can be fetch from GlobalGateway server
        // please see the details on the documentation page
        // https://developer.trulioo.com/docs/document-verification-step-3-get-country-codes
        // https://developer.trulioo.com/docs/document-verification-step-4-get-document-types
        countryDropDown = findViewById<Spinner>(R.id.countryDropDown)
        documentTypeTextBox = findViewById<TextView>(R.id.documentTypeBox)
        documentTypeTextBox?.text = TruliooInformationStorage.cardType

        val frontImageBox = findViewById<ImageView>(R.id.frontImage)
        if(frontImage != null){
            frontImageBox.setImageBitmap(BitmapFactory.decodeFile(frontImage.path))
            frontImageBox.scaleType = ImageView.ScaleType.FIT_CENTER
        }

        val backImageBox = findViewById<ImageView>(R.id.backImage)
        if(backImage != null){
            backImageBox.setImageBitmap(BitmapFactory.decodeFile(backImage.path))
            backImageBox.scaleType = ImageView.ScaleType.FIT_CENTER
        }

        val selfieImageBox = findViewById<ImageView>(R.id.selfieImage)
        if(selfieImage != null){
            selfieImageBox.setImageBitmap(BitmapFactory.decodeFile(selfieImage.path))
            selfieImageBox.scaleType = ImageView.ScaleType.FIT_CENTER
        }
    }

    fun confirmClicked(view: View) {
        var firstName = firstNameBox?.text.toString().trim()
        var lastName = lastNameBox?.text.toString().trim()
        TruliooInformationStorage.firstName = firstName
        TruliooInformationStorage.lastName = lastName
        TruliooInformationStorage.countryCode = countryDropDown?.getSelectedItem().toString()

        if(firstName.isNullOrBlank() || lastName.isNullOrBlank()){
            val alert = AlertDialog.Builder(this@TruliooConfirmationActivity)
            alert.setTitle("Error")
            alert.setMessage("Please Enter Both First And Last Name")
            alert.setPositiveButton("OK") { dialog, whichButton ->
                dialog.dismiss()

            }
            alert.show()
        }
        else{
            val alert = AlertDialog.Builder(this@TruliooConfirmationActivity)
            alert.setTitle("Message")
            alert.setMessage("Verify: "+ firstName + " " + lastName)
            alert.setPositiveButton("OK") { dialog, whichButton ->
                dialog.dismiss()
                sendRequest()
            }
            alert.setNegativeButton("Cancel"){dialog, whichButton ->
                dialog.dismiss()
            }
            alert.show()

        }
    }

    fun sendRequest(){
        val result = Intent(
                this,
                TruliooResultActivity::class.java
        )
        startActivity(result)
    }

    fun cancelClicked() {
        returnToMainPage()
    }

    fun returnToMainPage(){
        val result = Intent(
                this,
                MainActivity::class.java
        )
        startActivity(result)
    }

    override fun onBackPressed() {
        TruliooInformationStorage.cleanup()
        returnToMainPage()
    }
}
