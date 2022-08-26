package com.acuant.sampleapp

import com.trulioo.normalizedapi.ApiClient
import com.trulioo.normalizedapi.api.ConnectionApi
import com.trulioo.normalizedapi.api.VerificationsApi

public class TruliooVerificationHelper {
        //Enter Trulioo credentials
        private val username = "dev_Trulioo_fRhwv"
        private val password = "82FGr^^KB3hreygC"
        private var apiClient:ApiClient? = null

        fun init(){
                apiClient = ApiClient()
                apiClient?.setUsername(username)
                apiClient?.setPassword(password)
                // Set server address
                // GG20
                // apiClient?.setBasePath("https://api.globaldatacompany.com/")
                // GG25
                apiClient?.setBasePath("https://api.globalgateway.io/")
        }

        fun getConnectionClient():ConnectionApi{
                if(apiClient != null)
                {
                        return ConnectionApi(apiClient)
                }
                return ConnectionApi(ApiClient())
        }

        fun getVerificationClient(): VerificationsApi {
                if(apiClient != null)
                {
                        return VerificationsApi(apiClient)
                }
                return VerificationsApi(ApiClient())
        }

        fun getBasePath():String?{
                if(apiClient != null){
                        return apiClient?.getBasePath()
                }
                return "not initialized"
        }
}