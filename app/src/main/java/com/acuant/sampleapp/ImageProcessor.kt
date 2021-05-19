package com.acuant.sampleapp

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.ExifInterface
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.lang.Exception
import java.net.URL
import java.time.Instant

class ImageProcessor {
    companion object {
        val MAX_IMAGE_SIZE = 4.0 * 1024.0 * 1024.0 //4194304 bytes
        val MIN_QUALITY = 40
        val ipEndpoint = "https://api.globaldatacompany.com/common/v1/ip-info"

        private fun getIpAddress(): String {
            try {
                val response = URL(ipEndpoint).readText()
                val obj = JSONObject(response)
                return obj.get("ipAddress") as String
            } catch (e: Exception) {
                return "UNAVAILABLE"
            }
        }

        fun ProcessImage(image: File, imageType: String, retries: Int) {
            val size = image.length() / 1024.0 / 1024.0

            val exifInfo = ExifInterface(image.path)
            val messageObject = JSONObject()

            val ipAddress = getIpAddress()

            messageObject.put("V", Constants.TRULIOO_VERSION)
            messageObject.put("SYSTEM", "ANDROID")
            messageObject.put("CAPTURESDK", Constants.ACUANT_SDK_VERSION)
            messageObject.put("TIMESTAMP", Instant.now().toString())
            messageObject.put("IPADDRESS", ipAddress)
            messageObject.put("RETRIES", retries)
            messageObject.put("ACUANTHORIZONTALRESOLUTION", TruliooInformationStorage.currentDPI)
            messageObject.put("ACUANTVERTICALRESOLUTION", TruliooInformationStorage.currentDPI)
            messageObject.put("GPSLATITUDE", TruliooInformationStorage.currentLat)
            messageObject.put("GPSLONGITUDE", TruliooInformationStorage.currentLng)

            if (imageType == "Selfie") {
                messageObject.put("TRULIOOSDK", "SELFIE")
                messageObject.put("MODE", "AUTO")
            } else {
                if (TruliooInformationStorage.isAutoCaptureEnabled) {
                    messageObject.put("MODE", "AUTO")
                } else {
                    messageObject.put("MODE", "MANUAL")
                }
                when (TruliooInformationStorage.cardType) {
                    "DrivingLicence" -> {
                        messageObject.put("TRULIOOSDK", "DOCUMENT")
                    }
                    "Passport" -> {
                        messageObject.put("TRULIOOSDK", "PASSPORT")
                    }
                }
            }

            exifInfo.setAttribute(ExifInterface.TAG_SOFTWARE, messageObject.toString())

            if (size > MAX_IMAGE_SIZE) {
                compressImage(image)
            }
            exifInfo.saveAttributes()
        }

        private fun compressImage(image: File) {
            // this process needs to be in thread or AsyncTask
            // compression could be slow for large images
            val imageBitmap = BitmapFactory.decodeByteArray(image.readBytes(), 0, image.readBytes().size)
            val byteArrayStream = ByteArrayOutputStream()
            var quality = 100
            do {
                byteArrayStream.reset()
                imageBitmap.compress(Bitmap.CompressFormat.JPEG, quality, byteArrayStream)
                quality -= 10
            } while (byteArrayStream.size() > MAX_IMAGE_SIZE && quality > MIN_QUALITY)
            image.writeBytes(byteArrayStream.toByteArray())
        }



    }
}