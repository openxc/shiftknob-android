package com.openxc.shiftindicator;

import android.content.Intent;

/**
 * Created by DMELCHE6 on 10/29/2015.
 */
public interface HardwareInterface {
    public void turnOnShiftIndication();
    public void turnOffShiftIndication();
    public void sendDigit(int i);
    public void sendColor(int i);
    public void connect();
    public void usbDetached(Intent intent);
    public void end();
}
