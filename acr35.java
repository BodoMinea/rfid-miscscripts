package me.stuartphillips.plugins;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.acs.audiojack.AudioJackReader;
import com.acs.audiojack.ReaderException;

import android.media.AudioManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;

import java.lang.Override;
import java.lang.Runnable;
import java.lang.System;
import java.lang.Thread;
import java.util.Locale;

/**
 * Controleaza un cititor ACS ACR35 (FloJack FLOMIO) conectat pe port audio. Necesita modificari, fiind initial gandita pentru Cordova
 */
public class acr35 {

    private Transmitter transmitter;
    private AudioManager mAudioManager;
    private AudioJackReader mReader;

    private boolean firstReset = true;  /** prima resetare */

    /** comanda pentru citit UID de catre ACR35 fara a specifica tipul de card (GENERIC) */
    private final byte[] apdu = { (byte) 0xFF, (byte) 0xCA, (byte) 0x00, (byte) 0x00, (byte) 0x00 };
    /** timeout raspuns cititor in SECUNDE */
    private final int timeout = 1;

    /**
     * Raw to HEX
     *
     * @param buffer: raw data in the form of a byte array
     * @return a string containing the data in hexidecimal form
     */
    private String bytesToHex(byte[] buffer) {
        String bufferString = "";
        if (buffer != null) {
            for(int i = 0; i < buffer.length; i++) {
                String hexChar = Integer.toHexString(buffer[i] & 0xFF);
                if (hexChar.length() == 1) {
                    hexChar = "0" + hexChar;
                }
                bufferString += hexChar.toUpperCase(Locale.US) + " ";
            }
        }
        return bufferString;
    }

    /**
     * Verifica daca volumul audio este la maxim
     *
     * @return true daca volumul este la 100%
     */
    private boolean maxVolume() {
        int currentVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        int maxVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);

        if (currentVolume < maxVolume) {
            return false;
        }
        else{
            return true;
        }
    }

    /**
     * Seteaza cititorul ACR35 sa caute incontinuu semnal de la un card, returneaza in callback
     * UID-ul cand este gasit
     *
     * @param callbackContext: context de callback, initial pentru Cordova
     * @param cardType: tip card, optionak
     */
    private void read(final CallbackContext callbackContext, final int cardType){
        System.out.println("setting up for reading...");
        firstReset = true;

        /* niciun device sau volum audio dat incet */
        if(!mAudioManager.isWiredHeadsetOn()){
            /* niciun device audio conectat */
            return "unplugged";
        } else if(!maxVolume()) {
            /* volumul nu este la maxim */
            return "low_volume";
        }

        /* raspuns apropiere card */
        mReader.setOnPiccResponseApduAvailableListener
                (new AudioJackReader.OnPiccResponseApduAvailableListener() {
                    @Override
                    public void onPiccResponseApduAvailable(AudioJackReader reader,
                                                            byte[] responseApdu) {
                        transmitter.updateStatus(true);
                        System.out.println(bytesToHex(responseApdu));
                        return bytesToHex(responseApdu);
                    }
                });

        /* callback resetare */
        mReader.setOnResetCompleteListener(new AudioJackReader.OnResetCompleteListener() {
            @Override
            public void onResetComplete(AudioJackReader reader) {
                System.out.println("reset complete");

                /* prima resetare, pentru a evita bug cititor se da restart */
                if(firstReset){
                    cordova.getThreadPool().execute(new Runnable() {
                        public void run() {
                            try{
                                /* setare stand-by */
                                mReader.sleep();
                                /* delay */
                                Thread.sleep(1000);
                                /* reset */
                                mReader.reset();
                                firstReset = false;
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                } else {
                    /* creaza un nou transmitator pentru dupa resetare */
                    transmitter = new Transmitter(mReader, mAudioManager, callbackContext, timeout,
                            apdu, cardType);
                    /* comanda de thread se folosea initial din stack-ul Cordova, trebuie rescrisa cu thread normal de sistem */
                    //cordova.getThreadPool().execute(transmitter);
                    Thread t = new Thread(new Runnable() {
				    public void run() {
				        // transmitter init....
					    }
					});
					t.start();
                }
            }
        });

        mReader.start();
        mReader.reset();
        System.out.println("setup complete");
    }

    /**
     * metoda de comunicare ca plugin cordova, golita pentru ca nu mai are sens
     */
    @Override
    public boolean execute(String action, JSONArray args, final CallbackContext callbackContext)
            throws JSONException {

    }

}