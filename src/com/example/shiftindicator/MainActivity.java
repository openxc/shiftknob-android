package com.example.shiftindicator;

import java.util.Date;

import jp.ksksue.driver.serial.FTDriver;

import com.example.shiftindicator.ShiftRecommendation.ShiftSignal;
import com.openxc.VehicleManager;
import com.openxc.measurements.AcceleratorPedalPosition;
import com.openxc.measurements.EngineSpeed;
import com.openxc.measurements.Measurement;
import com.openxc.measurements.TransmissionGearPosition;
import com.openxc.measurements.UnrecognizedMeasurementTypeException;
import com.openxc.measurements.VehicleSpeed;
import com.openxc.remote.VehicleServiceException;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    private static String TAG = "ShiftIndicator";
    private VehicleManager mVehicleManager;
    private boolean mIsBound;

    private SharedPreferences mSharedPrefs;
    private MediaPlayer mMediaPlayer;

    // USB setup:
    public static final String ACTION_USB_PERMISSION = "com.ford.openxc.USB_PERMISSION";
    
    static ArduinoHardware mArduinoHardware = null;

    UsbManager mUsbManager = null;

    private TextView mVehicleSpeedView;
    private TextView mEngineSpeedView;
    private TextView mPedalView;
    private TextView mGearPosition;
    private Spinner mVehicleSpinner;
    private Switch mPowerSwitch;
    private boolean mPowerStatus = true;
    private SeekBar mLEDbar;
    private View mLayout;
    private int mEngineSpeed;
    private double mVehicleSpeed;
    private double mPedalPos;
    private long mShiftTime;

    private int mCurrentGear;
    boolean mJustShifted;
    int mNextRatio = 1;

    // Use the Figo by Default
    private int[] mGearRatios = new int[]{
            0, // Neutral
            140, // 1st
            75, // 2nd
            50, // 3rd
            37, // 4th
            30, // 5th
    };
    private double mBasePedalPosition = 15.0;
    private int mMinRPM = 1300;
    private double mScaler = 1.2;
    private double mCurvature = -30;
    private double mRpmOffset = 1300;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.i(TAG, "Shift Indicator created");

        mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        mMediaPlayer = MediaPlayer.create(this, R.raw.chime);

        Intent intent = new Intent(this, VehicleManager.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        mVehicleSpeedView = (TextView) findViewById(R.id.vehicle_speed);
        mEngineSpeedView = (TextView) findViewById(R.id.engine_speed);
        mPedalView = (TextView) findViewById(R.id.pedal_position);
        mGearPosition = (TextView) findViewById(R.id.gear_position);
        addVehicleSpinnerListener();
        
        mLayout = findViewById(R.id.layout);
        mLayout.setBackgroundColor(Color.BLACK);

        mLEDbar = (SeekBar) findViewById(R.id.led_bar);
        mLEDbar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

            public void onProgressChanged(SeekBar seekBar, int progress,
                    boolean fromUser) {
                mArduinoHardware.sendColor(progress * 255 / 100);
            }

            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        mPowerSwitch = (Switch) findViewById(R.id.power_switch);
        mPowerSwitch.setChecked(true);
        mPowerSwitch.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView,
                    boolean isChecked) {
                mPowerStatus = isChecked;
            }
        });

        mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        IntentFilter filter = new IntentFilter();
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        this.registerReceiver(mBroadcastReceiver, filter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.settings, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.i(TAG, "Option Selected " + item.getItemId());
        switch (item.getItemId()) {
        case R.id.settings:
            startActivity(new Intent(this, SettingsActivity.class));
            break;
        case R.id.close:
            System.exit(0);
            break;
        }
        return super.onOptionsItemSelected(item);
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        // Called when the connection with the service is established
        public void onServiceConnected(ComponentName className, IBinder service) {
            Log.i(TAG, "Bound to VehicleManager");
            mVehicleManager = ((VehicleManager.VehicleBinder) service)
                    .getService();
            try {
                mVehicleManager.addListener(VehicleSpeed.class, mSpeedListener);
                mVehicleManager.addListener(EngineSpeed.class, mEngineListener);
                mVehicleManager.addListener(AcceleratorPedalPosition.class,
                        mPedalListener);
                mVehicleManager.addListener(ShiftRecommendation.class,
                        mShiftRecommendation);
                mVehicleManager.addListener(TransmissionGearPosition.class,
                        mTransmissionGearPosition);
            } catch (VehicleServiceException e) {
                Log.w(TAG, "Couldn't add listeners for measurements", e);
            } catch (UnrecognizedMeasurementTypeException e) {
                Log.w(TAG, "Couldn't add listeners for measurements", e);
            }
            mIsBound = true;
        }

        // Called when the connection with the service disconnects unexpectedly
        public void onServiceDisconnected(ComponentName className) {
            Log.w(TAG, "VehicleService disconnected unexpectedly");
            mVehicleManager = null;
            mIsBound = false;
        }
    };

    VehicleSpeed.Listener mSpeedListener = new VehicleSpeed.Listener() {
        public void receive(Measurement measurement) {
            final VehicleSpeed updated_value = (VehicleSpeed) measurement;
            mVehicleSpeed = updated_value.getValue().doubleValue();
            MainActivity.this.runOnUiThread(new Runnable() {
                public void run() {
                    // send vehicle speed with 1 decimal point
                    mVehicleSpeedView.setText(""
                            + Math.round(mVehicleSpeed * 10) / 10);
                }
            });
        }
    };

    EngineSpeed.Listener mEngineListener = new EngineSpeed.Listener() {
        public void receive(Measurement measurement) {
            final EngineSpeed updated_value = (EngineSpeed) measurement;
            mEngineSpeed = updated_value.getValue().intValue();
            MainActivity.this.runOnUiThread(new Runnable() {
                public void run() {
                    mEngineSpeedView.setText("" + mEngineSpeed);
                }
            });

            if (!mSharedPrefs.getBoolean("pref_calculation_mode", false)) {
                vehicleStateCalculation();
            }
        }
    };

    AcceleratorPedalPosition.Listener mPedalListener = new AcceleratorPedalPosition.Listener() {
        public void receive(Measurement measurement) {
            final AcceleratorPedalPosition updated_value = (AcceleratorPedalPosition) measurement;
            mPedalPos = updated_value.getValue().doubleValue();
            MainActivity.this.runOnUiThread(new Runnable() {
                public void run() {
                    mPedalView.setText("" + (int) mPedalPos);
                }
            });
        }
    };

    TransmissionGearPosition.Listener mTransmissionGearPosition = new TransmissionGearPosition.Listener() {
        public void receive(Measurement measurement) {
            final TransmissionGearPosition status = (TransmissionGearPosition) measurement;

            if (mSharedPrefs.getBoolean("pref_calculation_mode", false)) {
                switch (status.getValue().enumValue()) {
                case FIRST:
                    updateGear(1);
                    break;
                case SECOND:
                    updateGear(2);
                    break;
                case THIRD:
                    updateGear(3);
                    break;
                case FOURTH:
                    updateGear(4);
                    break;
                case FIFTH:
                    updateGear(5);
                    break;
                case SIXTH:
                    updateGear(6);
                    break;
                case NEUTRAL:
                    updateGear(0);
                    break;
                case REVERSE:
                    ;
                    break;
                }
            }
        }
    };

    ShiftRecommendation.Listener mShiftRecommendation = new ShiftRecommendation.Listener() {
        public void receive(Measurement measurement) {
            final ShiftRecommendation updated_value = (ShiftRecommendation) measurement;

            if (mSharedPrefs.getBoolean("pref_calculation_mode", false)) {
                if (updated_value.getValue().enumValue() == ShiftRecommendation.ShiftSignal.UPSHIFT
                        && mPowerStatus) {
                    shift();
                }

                else {
                    cancelShiftBoolean(true);
                }
            }
        }
    };

    public void addVehicleSpinnerListener() {
        mVehicleSpinner = (Spinner) findViewById(R.id.vehicle_selector);
        mVehicleSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                    int pos, long id) {
                String selectedVehicle = parent.getItemAtPosition(pos).toString();
                Toast.makeText(parent.getContext(), 
                        "Selected Vehicle: "+selectedVehicle, Toast.LENGTH_SHORT).show();
                // Load Vehicle-Specific Data: //
                if (selectedVehicle.equals("Figo")) {
                    mGearRatios = new int[]{
                          0, // Neutral
                          140, // 1st
                          75, // 2nd
                          50, // 3rd
                          37, // 4th
                          30, // 5th
                    };
                    mBasePedalPosition = 15.0;
                    mMinRPM = 1300;
                    mScaler = 1.2;
                    mCurvature = -30;
                    mRpmOffset = 1300;
                }
                
                if (selectedVehicle.equals("Focus ST")) { 
                    mGearRatios = new int[]{ 
                          0, // Neutral
                          114, // 1st
                          69, // 2nd
                          46, // 3rd
                          36, // 4th
                          28, // 5th
                          23 // 6th
                    };
                    mBasePedalPosition = 15.0;
                    mMinRPM = 1300;
                    mScaler = 1.2;
                    mCurvature = -30;
                    mRpmOffset = 1300;
                }
                
                if (selectedVehicle.equals("Mustang GT")) {
                    mGearRatios = new int[] {
                          0, // Neutral
                          100, // 1st
                          66, // 2nd
                          46, // 3rd
                          35, // 4th
                          27, // 5th
                          18 // 6th
                    };
                    mBasePedalPosition = 10.0;
                    mMinRPM = 1600;
                    mScaler = 1.3;
                    mCurvature = -20;
                    mRpmOffset = 1680;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) { 
            }
            
        });
    }
    
    /**
     * VEHICLE STATE CALCULATION:
     * 
     * This is the main function of this class. In the 
     * event that a vehicle is not equipped with a built-in "ShiftRecommendation" 
     * and "TransmissionGearPosition" signals on CAN, this function will 
     * calculate the gear position and shift point locally. These variables are
     * then sent to the shift knob.
     */
    public void vehicleStateCalculation() {

        /**
         * First, setup the variables that will be needed for the calculations. 
         * Also cancel the visual shift message if the allotted time has 
         * passed (cancelShiftTime function).
         */
        if (mVehicleSpeed == 0) {
            mVehicleSpeed = 1;
        }
        double ratio = mEngineSpeed / mVehicleSpeed;
        long currentTime = new Date().getTime();
        cancelShiftTime(currentTime);
        
        calculateGearPosition(ratio);

        /**
         * SHIFT CALCULATION: The upshift signal is based on throttle position
         * and the rpm of the engine in the NEXT gear. The higher the throttle
         * position, the higher the rpm in the next gear (quick acceleration).
         * 
         * First, if the powerStatus switch is off, then don't do the calculation.
         * Secondly, if the pedal position is less than 10, then the driver is
         * probably shifting or slowing down, so no shift signal is needed.
         */

        if (!mPowerStatus || mPedalPos < 10) {
            return;
        }

        /**
         * If the pedal position is above the minimum threshold, then the driver
         * is thought to be holding a constant speed or accelerating and thus
         * the shift signal point should be calculated.
         */
        shouldDriverShift();
    }
    
    /**
     * SHOULD DRIVER SHIFT:
     * 
     * Values A, B, and C of the algorithm below must be optimized for each
     * specific vehicle. These values can be changed in the Vehicle-Specific
     * Section above.
     * 
     * TEMPLATE:
     *   nextRPM = A*mPedalPos*mPedalPos - B*mPedalPos + C 
     * 
     * If the calculated nextRPM is less than RPM the vehicle would be in if
     * the transmission were shifted to the next gear, then the shift signal 
     * is sent to the shift knob.
     */
    
    public void shouldDriverShift() {
        double nextRPM;
        if (mPedalPos >= mBasePedalPosition) {
            nextRPM = mScaler * mPedalPos * mPedalPos + mCurvature * mPedalPos + mRpmOffset;
        } else {
            nextRPM = mMinRPM;
        }

        if (nextRPM < mVehicleSpeed * mNextRatio && !mJustShifted) {
            shift();
        }
    }
    
    /** 
     * CALCULATE GEAR POSITION:
     * 
     * This function iterates through the known gear ratios for a given vehicle
     * and compares those values to the current ratio which is computed by 
     * dividing mEngineSpeed by mVehicleSpeed. Once the computed ratio is
     * within 10% of a known ratio, the transmission is thought to be in a 
     * specific gear. IF there is no ratio match then the vehicle must be in
     * neutral or the clutch is depressed.
     */
    public void calculateGearPosition(double r) {
        for (int i = 1; i < mGearRatios.length; i++) {
            if (mGearRatios[i] * .9 < r && mGearRatios[i] * 1.1 > r) {
                if (mNextRatio != mGearRatios[i]) {
                    mJustShifted = false;
                }
                mNextRatio = mGearRatios[i];
                updateGear(i);
                break;
            }

            if (i == mGearRatios.length - 1) {
                // if the loop gets to here, then the vehicle is thought to be
                // in Neutral
                mJustShifted = false;
                updateGear(0);
                return;
            }
        }
    }
    /** updateGear takes the calculated gear position and sends that value
	 * to the shift knob.
	 */
    private void updateGear(final int g) {
        MainActivity.this.runOnUiThread(new Runnable() {
            public void run() {
                mGearPosition.setText(Integer.toString(g));
            }
        });

        if (g != mCurrentGear) {
            mArduinoHardware.sendDigit(g);
        }
        mCurrentGear = g;
    }

    /**
     * shift() handles all UI and shift knob functions for sending shift
     * indication messages to the driver. It checks the settings to see which
     * signals to send, and then send the corresponding signals to the proper
     * places.
     */
    private void shift() {
        if (mSharedPrefs.getBoolean("pref_haptic_feedback", false)) {
            mArduinoHardware.turnOnShiftIndication();
        }

        if (mSharedPrefs.getBoolean("pref_audio_feedback", false)) {
            mMediaPlayer.start();
        }

        if (mSharedPrefs.getBoolean("pref_visual_feedback", false)) {
            MainActivity.this.runOnUiThread(new Runnable() {
                public void run() {
                    mLayout.setBackgroundColor(Color.WHITE);
                }
            });
        }

        mJustShifted = true;
        mShiftTime = new Date().getTime();
    }

    /**
     * cancelShift* removes the "upshift message" from the UI screen after 
     * 500 milliseconds.
     */
    private void cancelShiftTime(long t) {
        if (t - mShiftTime > 500) {
            cancelShift();
        }
    }
    
    public void cancelShiftBoolean(boolean b) {
        if (b) {
            cancelShift();
        }
    }
    
    public void cancelShift() {
        MainActivity.this.runOnUiThread(new Runnable() {
            public void run() {
                mLayout.setBackgroundColor(Color.BLACK);
            }
        });
    }

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                Log.d(TAG, "Device detached");
                Bundle extras = intent.getExtras();
                UsbDevice lostDevice = (UsbDevice) extras.get("device");
                if (lostDevice.equals(mArduinoHardware.getDevice())) {
                    mArduinoHardware.end();
                }
            }
        }
    };

    @Override
    public void onResume() {
        super.onResume();
        mArduinoHardware = new ArduinoHardware(mUsbManager);
        mArduinoHardware.connect();
    }

    public void onExit(View view) {

        if (mArduinoHardware != null) {
            mArduinoHardware.end();
        }
        if (mIsBound) {
            Log.i(TAG, "Unbinding from vehicle service before exit");
            unbindService(mConnection);
            mIsBound = false;
        }
        Log.d(TAG, "Closing");
        finish();
        System.exit(0);
    }
}