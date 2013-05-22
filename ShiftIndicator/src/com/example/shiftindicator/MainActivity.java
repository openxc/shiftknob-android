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
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Switch;
import android.widget.TextView;

public class MainActivity extends Activity {

	private static String TAG = "ShiftIndicator";
	private VehicleManager mVehicleManager;
	private boolean mIsBound;
	
	private SharedPreferences sharedPrefs;
	private MediaPlayer mediaPlayer;
	
	//USB setup:
    public static final String ACTION_USB_PERMISSION =
            "com.ford.openxc.USB_PERMISSION";
    static boolean mSerialStarted = false;
    static FTDriver mSerialPort = null;
    
    UsbManager mUsbManager = null;
    UsbDevice mGaugeDevice = null;
    UsbDeviceConnection mGaugeConnection = null;
    UsbEndpoint mEndpointIn = null;
    UsbEndpoint mEndpointOut = null;
    UsbInterface mGaugeInterface = null;
    
	private TextView mVehicleSpeedView;
	private TextView mEngineSpeedView;
	private TextView mPedalView;
	private TextView mGearPosition;
	private Switch mPowerSwitch;
	private boolean power_status = true;
	private SeekBar mLEDbar;
	private View mLayout;
	private int engine_speed;
	private double vehicle_speed;
	private double pedal_pos;
	private long shiftTime;
	
	private int currentGear;
	boolean justShifted;
	int next_ratio=1;
	
////* VEHICLE SPECIFIC DATA *////
	
//	FIGO RATIOS rpm/speed
//	private int[] gearRatios = {
//		0,		// Neutral
//		140,	// 1st
//		75,		// 2nd
//		50,		// 3rd
//		37,		// 4th
//		30,		// 5th
//	};
//	private double base_pedal_position = 15.0;
//	private int min_rpm = 1300;
	
//	Mustang GT RATIOS rpm/speed
//	private int[] gearRatios = {
//		0,		// Neutral
//		100,	// 1st
//		66,		// 2nd
//		46,		// 3rd
//		35,		// 4th
//		27,		// 5th
//		18 		// 6th
//	};
//	private double base_pedal_position = 10.0;
//	private int min_rpm = 1600;
	
//	Focus ST RATIOS rpm/speed:
	private int[] gearRatios = {
		0,		// Neutral
		114,	// 1st
		69,		// 2nd
		46,		// 3rd
		36,		// 4th
		28,		// 5th
		23 		// 6th
	};
	private double base_pedal_position = 15.0;
	private int min_rpm = 1300;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		Log.i(TAG, "Shift Indicator created");
		
		sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		
		mediaPlayer = MediaPlayer.create(this, R.raw.chime);
		
		Intent intent = new Intent(this, VehicleManager.class);
	    bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
	    mVehicleSpeedView = (TextView) findViewById(R.id.vehicle_speed);
	    mEngineSpeedView = (TextView) findViewById(R.id.engine_speed);
	    mPedalView = (TextView) findViewById(R.id.pedal_position);
	    mGearPosition = (TextView) findViewById(R.id.gear_position);
	    mLayout = findViewById(R.id.layout);
	    mLayout.setBackgroundColor(Color.BLACK);
	    
        mLEDbar = (SeekBar) findViewById(R.id.led_bar);
	    mLEDbar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				send2Arduino("color", progress*255/100);
			}
			public void onStartTrackingTouch(SeekBar seekBar) {}
			public void onStopTrackingTouch(SeekBar seekBar) {}
	    });
	    
	    mPowerSwitch = (Switch) findViewById(R.id.power_switch);
	    mPowerSwitch.setChecked(true);
	    mPowerSwitch.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				power_status = isChecked;
			}
	    });
	    
	    mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
	    IntentFilter filter = new IntentFilter();
	    filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
	    this.registerReceiver(mBroadcastReceiver, filter);
	    
	    filter = new IntentFilter();
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
        Log.i(TAG, "Option Selected "+item.getItemId());
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
	    public void onServiceConnected(ComponentName className,
	            IBinder service) {
	    	Log.i(TAG, "Bound to VehicleManager");
	        mVehicleManager = ((VehicleManager.VehicleBinder)service).getService();
	        try {
				mVehicleManager.addListener(VehicleSpeed.class, mSpeedListener);
				mVehicleManager.addListener(EngineSpeed.class, mEngineListener);
				mVehicleManager.addListener(AcceleratorPedalPosition.class, mPedalListener);
				mVehicleManager.addListener(ShiftRecommendation.class, mShiftRecommendation);
				mVehicleManager.addListener(TransmissionGearPosition.class, mTransmissionGearPosition);
			} catch(VehicleServiceException e) {
           	 	Log.w(TAG, "Couldn't add listeners for measurements", e);
			} catch(UnrecognizedMeasurementTypeException e) {
           	 	Log.w(TAG, "Couldn't add listeners for measurements", e);
			}
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
		    vehicle_speed = updated_value.getValue().doubleValue();
		    MainActivity.this.runOnUiThread(new Runnable() {
		        public void run() {
		        	// send vehicle speed with 1 decimal point
		            mVehicleSpeedView.setText(""+Math.round(vehicle_speed*10)/10);
		        }
		    });
		}
	};
	
	EngineSpeed.Listener mEngineListener = new EngineSpeed.Listener() {
		public void receive(Measurement measurement) {
		    final EngineSpeed updated_value = (EngineSpeed) measurement;
		    engine_speed = updated_value.getValue().intValue();
		    MainActivity.this.runOnUiThread(new Runnable() {
		        public void run() {
		            mEngineSpeedView.setText(""+engine_speed);
		        }
		    });
		    
		    if (!sharedPrefs.getBoolean("pref_calculation_mode", false)) {
		    	shiftCalculation();
		    }
		}
	};		
	
	AcceleratorPedalPosition.Listener mPedalListener = new AcceleratorPedalPosition.Listener() {
		public void receive(Measurement measurement) {
		    final AcceleratorPedalPosition updated_value = (AcceleratorPedalPosition) measurement;
		    pedal_pos = updated_value.getValue().doubleValue();
		    MainActivity.this.runOnUiThread(new Runnable() {
		        public void run() {
		            mPedalView.setText(""+(int)pedal_pos);
		        }
		    });
		}
	};
	
	TransmissionGearPosition.Listener mTransmissionGearPosition = 
			new TransmissionGearPosition.Listener() {
		public void receive(Measurement measurement) {
			final TransmissionGearPosition status = (TransmissionGearPosition) measurement;
			
			if (sharedPrefs.getBoolean("pref_calculation_mode", false)) {
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
		        case REVERSE:;
		            break;
		        }
			}
		}
	};
    
	ShiftRecommendation.Listener mShiftRecommendation = new ShiftRecommendation.Listener() {
		public void receive(Measurement measurement) {
		    final ShiftRecommendation updated_value = (ShiftRecommendation) measurement;
        	
		    if (sharedPrefs.getBoolean("pref_calculation_mode", false)) {
				if (updated_value.getValue().enumValue() == ShiftRecommendation.ShiftSignal.UPSHIFT 
						&& power_status) {
	        		shift();
	        	}
	        	
	        	else {
	        		cancelShift(shiftTime+600);
	        	}
		    }
		}
	};
	
	/** shiftCalculation is the main function of this class. In the event 
	 * that a vehicle is not equipped with a built-in "ShiftRecommendation" 
	 * signal on CAN, this function will calculate the upshift point locally. 
	 * The gear position and shift point are then sent to the shift knob. 
	 */
	public void shiftCalculation() {
		
		/** GEAR POSITION CALCULATION:
		 * First calculate gear based on ratio of rpm to speed.
		 * The for loop compares known gear ratios with the 
		 * calculated ratio. 
		 */
	    if(vehicle_speed==0) vehicle_speed = 1;
	    double ratio = engine_speed/vehicle_speed;
	    long currentTime = new Date().getTime();
	    
	    for (int i = 1; i < gearRatios.length; i++) {
	    	if (gearRatios[i]*.9 < ratio && gearRatios[i]*1.1 > ratio) {
	    		if (next_ratio != gearRatios[i]) justShifted = false;
	    		next_ratio=gearRatios[i];
	    		updateGear(i);
	    		break;
	    	}
	    	
	    	if (i == gearRatios.length-1) {
	    		//if the loop gets to here, then the vehicle is thought to be in Neutral
	    		justShifted = false;
	    		updateGear(0);
	    		cancelShift(currentTime);
	    		return;
	    	}
	    }
	    
	    if (!power_status) return;
	    
	    /** SHIFT CALCULATION:
	     * The upshift signal is based on throttle position and the rpm
	     * of the engine in the NEXT gear. The higher the throttle position,
	     * the higher the rpm in the next gear (quick acceleration). 
	     * 
	     * First, if the pedal position is less than 10, then the driver is 
	     * probably shifting or slowing down, so no shift signal is needed.
	     */
	    
	    if (pedal_pos < 10) {
	    	cancelShift(currentTime);
	    	return;
	    }
	    
	    /** If the pedal position is above the minimum threshold, then the driver
	     * is thought to be holding a constant speed or accelerating and thus 
	     * the shift signal point should be calculated.
	     * 
	     * Values A, B, and C of the algorithm below must be optimized for each
	     * specific vehicle.
	     * 
	     * next_rpm = A*(pedal_pos)*(pedal_pos)-B*(pedal_pos)+C   TEMPLATE
	     * 
	     * If the calculated next_rpm is less than rpm the vehicle would be if shifted
	     * to the next gear, the shift signal is sent to the shift knob.
	     */

	    double next_rpm;
	    if (pedal_pos >= base_pedal_position){
	    	// next_rpm = 1.3*(pedal_pos)*(pedal_pos)-20*pedal_pos+1680; //GT Mustang
	    	next_rpm = 1.2*(pedal_pos)*(pedal_pos)-30*pedal_pos+1300; //Figo/Focus
	    }
	    else next_rpm=min_rpm;
	    
	    if (next_rpm < vehicle_speed*next_ratio){
	    	
	    	if (!justShifted){
	    		shift();
    		}
    		cancelShift(currentTime);
	    }
	    else cancelShift(currentTime);
	}

	/** updateGear takes the calculated gear position and sends that value
	 * to the shift knob. The gear position is enclosed in '<' ___ '>'
	 */
	private void updateGear(final int g) {
		MainActivity.this.runOnUiThread(new Runnable() {
			public void run() {
				mGearPosition.setText(Integer.toString(g));
			}
		});
		
		if (g != currentGear){
			send2Arduino("gear", g);
		}
		currentGear = g;
	}
	
	/** shift() handles all UI and shift knob functions for sending
	 * shift indication messages to the driver. It checks the settings 
	 * to see which signals to send, and then send the corresponding
	 * signals to the proper places. 
	 */
	private void shift() {
		if (sharedPrefs.getBoolean("pref_haptic_feedback", false)) {
			send2Arduino("shift", 1);
		}
		
		if (sharedPrefs.getBoolean("pref_audio_feedback", false)) {
			mediaPlayer.start();
		}
		
		if (sharedPrefs.getBoolean("pref_visual_feedback", false)) {
			MainActivity.this.runOnUiThread(new Runnable() {
		        public void run() {
		            mLayout.setBackgroundColor(Color.WHITE);
		        }
		    });
		}
					
		justShifted = true;
		shiftTime = new Date().getTime();
	}
	
	/** cancelShift removes the "upshift message" from the UI screen after a given
	 * amount of time.
	 */
	private void cancelShift(long t) {
		if (t-shiftTime>500){
			MainActivity.this.runOnUiThread(new Runnable() {
				public void run() {
					mLayout.setBackgroundColor(Color.BLACK);
				}
			});
		}
	}
	
	public void send2Arduino(String signal, int value){
		
		String outString = null;
		if (signal.equals("shift")) {
			outString = '['+Integer.toString(value)+']';
		}
		
		if (signal.equals("gear")) {
			outString = '<'+Integer.toString(value)+'>';
		}
		
		if (signal.equals("color")) {
			outString = '('+Integer.toString(value)+')';
		}
		
		if (signal.equals("connect")) {
			outString = '{'+Integer.toString(value)+'}';
		}
		
		char[] outMessage = outString.toCharArray();
        byte outBuffer[] = new byte[20];
        for(int i=0; i<outString.length(); i++)
        {
            outBuffer[i] = (byte)outMessage[i];
        }
        try {
        	mSerialPort.write(outBuffer, outString.length());
        } catch (Exception e) {
            Log.d(TAG, "mSerialPort.write() just threw an exception.  Is the cable plugged in?");
        }
	}
	
	private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
            	Log.d(TAG, "Device detached.");
            	mSerialPort = null;
            }
        }
    };
    
    private void connectToDevice() {
    	if (mSerialPort != null) {
    		mSerialPort.end();
    	}
    	mSerialPort = new FTDriver(mUsbManager);
    	mSerialPort.setPermissionIntent(PendingIntent.getBroadcast(this, 0, 
    			new Intent(ACTION_USB_PERMISSION), 0));
    	mSerialStarted = mSerialPort.begin(FTDriver.BAUD115200);
    	if (!mSerialStarted) {
    		Log.d(TAG, "mSerialPort.begin() failed.");
    	} else {
    		Log.d(TAG, "mSerialPort.begin() success!");
    		send2Arduino("connect", 1);
    		byte[] readBuffer = new byte[10];
    		int dataIn = 0;
    		boolean readSuccess = false;
			try {
				for (int i=0; i<25; i++) {
					dataIn = mSerialPort.read(readBuffer);
					if (dataIn > 0) {
						readSuccess = true;
						Log.d(TAG, "Read from serial: "+dataIn+" @ "+i);
						send2Arduino("gear", 0);
						break;
					}
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				Log.d(TAG, "couldn't read Serial.");
			}
    	}
    }

    @Override
    public void onResume() {
        super.onResume();
        connectToDevice();
    }
    
    public void onExit(View view){

        if (mSerialPort != null){
            mSerialPort.end();
        } 
        if(mIsBound) {
            Log.i(TAG, "Unbinding from vehicle service before exit");
            unbindService(mConnection);
            mIsBound = false;
        }
        Log.d(TAG,"Closing");
        finish();
        System.exit(0);
    }
}
