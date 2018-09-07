package com.yugensoft.simplesleepjournal;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

public class AdFunctions {
    /**
     * Function to load an ad request into an adview
     * @param adView Which adView to load it into
     */
    public static void loadAdIntoAdView(AdView adView){
        AdRequest adRequest = new AdRequest.Builder()
                .addTestDevice(AdRequest.DEVICE_ID_EMULATOR)
                .addTestDevice("F961E2E362704F9592CC2F9CC025A1BF")
                .addKeyword("sleep")
                .addKeyword("rest")
                .addKeyword("tiredness")
                .addKeyword("insomnia")
                .addKeyword("well-rested")
                .addKeyword("lethargic")
                .addKeyword("bedtime")
                .addKeyword("exhausted")
                .addKeyword("exhaustion")
                .addKeyword("bed")
                .build();
        adView.loadAd(adRequest);
    }
}
