package com.openxc.shiftindicator;

import android.app.Activity;
import android.content.Intent;
import android.widget.Toast;

import com.getpebble.android.kit.Constants;
import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.util.PebbleDictionary;

import java.util.UUID;

/**
 * Created by DMELCHE6 on 10/29/2015.
 */
public class PebbleHardware implements HardwareInterface {
    private Activity mActivity = null;
    private static final int KEY_GEAR = 1;
    private static final int KEY_SHIFT = 2;
    private static final UUID mPebbleApp = UUID.fromString("5d1e7ed5-3906-4cda-9be6-d6bb01ceebc4");

    public PebbleHardware (Activity sourceActivity) {
        mActivity = sourceActivity;
    }

    @Override
    public void turnOnShiftIndication() {
        // Send a shift notification to the Pebble
        PebbleDictionary outgoing = new PebbleDictionary();
        outgoing.addInt32(KEY_SHIFT, 0);
        PebbleKit.sendDataToPebble(mActivity.getBaseContext(), mPebbleApp, outgoing);
    }

    @Override
    public void turnOffShiftIndication() {

    }

    @Override
    public void sendDigit(int i) {
        // Send an updated gear to the Pebble
        PebbleDictionary outgoing = new PebbleDictionary();
        outgoing.addString(KEY_GEAR, Integer.toString(i));
        PebbleKit.sendDataToPebble(mActivity.getBaseContext(), mPebbleApp, outgoing);
    }

    @Override
    public void sendColor(int i) {

    }

    @Override
    public void connect() {
        boolean isConnected = PebbleKit.isWatchConnected(mActivity);
        Toast.makeText(mActivity, "Pebble " + (isConnected ? "is" : "is not") + " connected!", Toast.LENGTH_LONG).show();

        // Launch the Pebble app
        PebbleKit.startAppOnPebble(mActivity.getBaseContext(), mPebbleApp);
    }

    @Override
    public void usbDetached(Intent intent) {

    }

    @Override
    public void end() {

    }
}
