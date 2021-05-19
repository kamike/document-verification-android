package com.acuant.sampleapp;

import java.io.File;

public class TruliooInformationStorage {
    public static File frontImageFile = null;
    public static File backImageFile = null;
    public static File selfieImageFile = null;

    public static String firstName = null;
    public static String lastName = null;

    public static String countryCode = "CA";
    public static String cardType = "DrivingLicence";
    public static Boolean isAutoCaptureEnabled = true;

    public static Integer currentDPI = -1;
    public static String currentLat = "";
    public static String currentLng = "";

    public static void cleanup(){
        frontImageFile = null;
        backImageFile = null;
        selfieImageFile = null;
        firstName = null;
        lastName = null;
        currentDPI = -1;
        currentLat = "";
        currentLng = "";
    }
}
