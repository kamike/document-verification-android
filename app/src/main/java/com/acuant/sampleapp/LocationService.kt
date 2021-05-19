package com.acuant.sampleapp

import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle

class LocationService {
    companion object {

        fun getLocation(locationManager: LocationManager, mainClass: MainActivity?) {

            val locationListener = object : LocationListener {
                override fun onLocationChanged(location: Location?) {
                    if (location != null) {
                        TruliooInformationStorage.currentLat = "${location.latitude}"
                        TruliooInformationStorage.currentLng = "${location.longitude}"
                    }
                    locationManager.removeUpdates(this)
                    mainClass?.requestLocationCallback()
                }

                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
                }

                override fun onProviderEnabled(provider: String?) {
                }

                override fun onProviderDisabled(provider: String?) {
                }
            }

            try {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 1f, locationListener)
                locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            } catch (ex:SecurityException) {
                mainClass?.locationFailureCallback()
            }
        }
    }
}