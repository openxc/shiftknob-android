package com.openxc.shiftindicator;

import java.util.Date;

import jp.ksksue.driver.serial.FTDriver;

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
    private long mShiftCommandTime;
    private long mNewGearTime;
    
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
            
            // Only calculate the vehicle state if the operation mode is set
            // to "Performance" OR if the efficiency shift point algorithm is
            // set to "Calculate."
            if (mSharedPrefs.getBoolean("pref_calculation_mode", false) || 
                    mSharedPrefs.getBoolean("pref_operation_mode", false)) {
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

            if (!mSharedPrefs.getBoolean("pref_calculation_mode", true)) {
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

            if (!mSharedPrefs.getBoolean("pref_calculation_mode", true)) {
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
                if (selectedVehicle.equals("Fiesta ST")) {
                    mGearRatios = new int[]{
                            0, // Neutral
                            133, // 1st
                            72, // 2nd
                            47, // 3rd
                            36, // 4th
                            29, // 5th
                            24 // 6th  
                    };
                    mBasePedalPosition = 15.0;
                    mMinRPM = 1300;
                    mScaler = .685;
                    mCurvature = -15;
                    mRpmOffset = 1380;
                }
                
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
                
                if (selectedVehicle.equals("2013 Mustang GT500")) {
                    mGearRatios = new int[] {
                            0, // Neutral
                            67, // 1st
                            46, // 2nd
                            33, // 3rd
                            25, // 4th
                            19, // 5th
                            12 // 6th
                      };
                      mBasePedalPosition = 12.0;
                      mMinRPM = 1700;
                      mScaler = 1.3;
                      mCurvature = -20;
                      mRpmOffset = 1750;
                } 
                
                if (selectedVehicle.equals("2012 Mustang GT500")) {
                    mGearRatios = new int[] {
                            0, // Neutral
                            86, // 1st
                            51, // 2nd
                            37, // 3rd
                            29, // 4th
                            21, // 5th
                            14 // 6th
                      };
                      mBasePedalPosition = 12.0;
                      mMinRPM = 1500;
                      mScaler = 1.2;
                      mCurvature = -20;
                      mRpmOffset = 1570;
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
        
        handleGearPosition(ratio, currentTime);

        /**
         * SHIFT CALCULATION:
         * 
         * If the powerStatus switch is off, then don't do the calculation.
         */

        if (!mPowerStatus) {
            return;
        }

        shouldDriverShift(currentTime);
    }
    
    /**
     * SHOULD DRIVER SHIFT:
     * 
     * 2 Operation Modes. 
     * 
     * MODE 1: Efficiency Mode
     *  
     *  Values A, B, and C of the algorithm below must be optimized for each
     *  specific vehicle. These values can be changed in the Vehicle-Specific
     *  Section above.
     * 
     *  TEMPLATE:
     *    nextRPM = A*mPedalPos*mPedalPos - B*mPedalPos + C 
     * 
     *  If the calculated nextRPM is less than RPM the vehicle would be in if
     *  the transmission were shifted to the next gear, then the shift signal 
     *  is sent to the shift knob.
     * 
     *  Additional criteria:
     *   1. Do not send another "shift signal" if a signal was just sent (mJustShifted)
     *   2. Wait until after the driver has been in a gear for 1 second before sending
     *   another shift signal.
     *   
     * MODE 2: Performance Mode
     * 
     *   This operation mode is simple. Tell the driver to shift once the
     *   engine speed is equal to or exceeds the user-defined shift point.
     *   Only send the shift signal if the pedal position is greater than 
     *   or equal to 10% (which we assume means the driver is accelerating).
     */
    
    public void shouldDriverShift(long t) {
        if (!mSharedPrefs.getBoolean("pref_operation_mode", false)) {
            double nextRPM;
            if (mPedalPos >= mBasePedalPosition) {
                nextRPM = mScaler * mPedalPos * mPedalPos + mCurvature * mPedalPos + mRpmOffset;
            } else if (mBasePedalPosition <= 10) {
                return;
            } else {
                nextRPM = mMinRPM;
            }
    
            if (nextRPM < mVehicleSpeed * mNextRatio && !mJustShifted && (t-mNewGearTime > 1000)) {
                shift();
            }
        } else {
            int userShiftPoint = Integer.parseInt(mSharedPrefs.getString("pref_shift_point", "5000"));
            if (mPedalPos >= 10 && mEngineSpeed >= userShiftPoint) {
                shift();
            }
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
    public void handleGearPosition(double r, long t) {
        for (int i = 1; i < mGearRatios.length; i++) {
            if (mGearRatios[i] * .9 < r && mGearRatios[i] * 1.1 > r) {
                
                // if the vehicle is in a new gear, then no longer need the mJustShifted command
                if (i != mCurrentGear) {
                    mJustShifted = false;
                    mNewGearTime = t;
                    mCurrentGear = i;
                }
                
                // if the vehicle has been in the same gear for more than 750 milliseconds
                // then we can update the UI with the new gear position. This reduces noise in 
                // the gear estimation process.
                else if (t - mNewGearTime > 750) {
                    updateGear(i);
                }
                
                if (i == mGearRatios.length - 1) {
                    // if the vehicle is in the last gear, then set the next ratio to 1
                    // to nullify shift calculation
                    mNextRatio = 1;
                } else {
                    mNextRatio = mGearRatios[i+1];
                }
                break;
            }

            if (i == mGearRatios.length - 1) {
                // if the loop gets to here, then the vehicle is thought to be
                // in Neutral
                mJustShifted = false;
                if (mCurrentGear != 0) {
                    mNewGearTime = t;
                    mCurrentGear = 0;
                }

                else if (t - mNewGearTime > 750){
                    updateGear(0);
                }
                
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

        mArduinoHardware.sendDigit(g);
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
            MainActivity.this.runOnUiThread(new Runnable() {
                public void run() {
                    mMediaPlayer.start();
                }
            });
        }

        if (mSharedPrefs.getBoolean("pref_visual_feedback", false)) {
            MainActivity.this.runOnUiThread(new Runnable() {
                public void run() {
                    mLayout.setBackgroundColor(Color.WHITE);
                }
            });
        }

        mJustShifted = true;
        mShiftCommandTime = new Date().getTime();
    }

    /**
     * cancelShift* removes the "upshift message" from the UI screen after 
     * 500 milliseconds.
     */
    private void cancelShiftTime(long t) {
        if (t - mShiftCommandTime > 500) {
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
