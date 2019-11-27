package com.acuant.sampleapp;

import android.graphics.Bitmap;

public class TruliooInformationStorage {
    public static Bitmap frontImage = null;
    public static Bitmap backImage = null;
    public static Bitmap selfieImage = null;

    public static String firstName = null;
    public static String lastName = null;

    public static String countryCode = "CA";
    public static String cardType = "DrivingLicence";

    public static void cleanup(){
        frontImage = null;
        backImage = null;
        selfieImage = null;
        firstName = null;
        lastName = null;
        cardType = "DrivingLicence";
        countryCode = "CA";
    }
}
