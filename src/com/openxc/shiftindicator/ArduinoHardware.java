package com.openxc.shiftindicator;

import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.util.Log;
import jp.ksksue.driver.serial.FTDriver;

public class ArduinoHardware {
    
    private static final String TAG = "ArduinoHardware";
    
    private static final String SHIFT_NAME = "shift"; 
    private static final String COLOR_NAME = "color";
    private static final String DIGIT_NAME = "digit";
    
    private static final int UPSHIFT_VALUE = 1;
    private static final int NO_SHIFT_VALUE = 0;
    
    private static FTDriver mFTDriver;
    private int baudRate = FTDriver.BAUD115200;
    private UsbManager mUsbManager = null;
    
    /**
     * This class handles all communication and device setup for the 
     * connection between Shift Indicator app and the Shift Knob 
     * hardware. It can serve as a template for future Arduino and/or
     * serial devices attached to an Android device.
     */
    public ArduinoHardware(UsbManager u) {
        mUsbManager = u;
    }
    
    public void turnOnShiftIndication() {
        String outString = JsonBuilder.builder(SHIFT_NAME, UPSHIFT_VALUE);
        sendOut(outString);
    }
    
    public void turnOffShiftIndication() {
        String outString = JsonBuilder.builder(SHIFT_NAME, NO_SHIFT_VALUE);
        sendOut(outString);
    }
    
    public void sendDigit(int i) {
        String outString = JsonBuilder.builder(DIGIT_NAME, i);
        sendOut(outString);
    }
    
    public void sendColor(int i) {
        String outString = JsonBuilder.builder(COLOR_NAME, i);
        sendOut(outString);
    }
    
    private void sendOut(String s) {
        try {
            mFTDriver.write(s);
        } catch (Exception e) {
            Log.d(TAG, "FTDriver.write() just threw an exception.  Is the cable plugged in?");
        }
    }
    
    public void connect() {
        if (mFTDriver == null) {
            mFTDriver = new FTDriver(mUsbManager);
        }

        if (mFTDriver.isConnected()) {
            return;
        }

        mFTDriver.begin(baudRate);
        if (!mFTDriver.isConnected()) {
            Log.d(TAG, "mSerialPort.begin() failed.");
        } else {
            Log.d(TAG, "mSerialPort.begin() success!.");
            sendDigit(0);
        }
    }
    
    public void end() {
        mFTDriver.end();
    }
    
    public UsbDevice getDevice() {
        return mFTDriver.getDevice();
    }

}
