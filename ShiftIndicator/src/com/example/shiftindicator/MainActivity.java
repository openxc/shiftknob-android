package com.example.shiftindicator;


import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;

import jp.ksksue.driver.serial.FTDriver;

import com.openxc.VehicleManager;
import com.openxc.measurements.AcceleratorPedalPosition;
import com.openxc.measurements.EngineSpeed;
import com.openxc.measurements.Measurement;
import com.openxc.measurements.UnrecognizedMeasurementTypeException;
import com.openxc.measurements.VehicleSpeed;
import com.openxc.remote.VehicleServiceException;
import com.openxc.sources.DataSourceException;
import com.openxc.sources.trace.TraceVehicleDataSource;

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
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

public class MainActivity extends Activity {

	private static String TAG = "ShiftIndicator";
	private VehicleManager mVehicleManager;
	private boolean mIsBound;
	
	private SharedPreferences sharedPrefs;
	
	//USB setup:
    public static final String ACTION_USB_PERMISSION =
            "com.ford.openxc.USB_PERMISSION";
    static boolean mSerialStarted = false;
    static FTDriver mSerialPort = null;
    
    private PendingIntent mPermissionIntent;
    UsbManager mUsbManager = null;
    UsbDevice mGaugeDevice = null;
    UsbDeviceConnection mGaugeConnection = null;
    UsbEndpoint mEndpointIn = null;
    UsbEndpoint mEndpointOut = null;
    UsbInterface mGaugeInterface = null;
    
	private TextView mVehicleSpeedView;
	private TextView mEngineSpeedView;
	private TextView mShiftIndicator;
	private TextView mShiftCalc;
	private TextView mPedalView;
	private TextView mGearPosition;
	private SeekBar mLEDbar;
	private View mLayout;
	private int engine_speed;
	private double vehicle_speed;
	private double pedal_pos;
	private long shiftTime;
	
	private int currentGear;
	boolean justShifted;
	int next_ratio=1;
	
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
	private int[] gearRatios = {
		0,		// Neutral
		100,	// 1st
		66,		// 2nd
		46,		// 3rd
		35,		// 4th
		27,		// 5th
		18 		// 6th
	};
	private double base_pedal_position = 10.0;
	private int min_rpm = 1600;
	
//	Focus ST RATIOS rpm/speed:
//	private int[] gearRatios = {
//		0,		// Neutral
//		114,	// 1st
//		69,		// 2nd
//		46,		// 3rd
//		36,		// 4th
//		28,		// 5th
//		23 		// 6th
//	};
//	private double base_pedal_position = 15.0;
//	private int min_rpm = 1300;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		Log.i(TAG, "Shift Indicator created");
		
		sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		
		Intent intent = new Intent(this, VehicleManager.class);
	    bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
	    mVehicleSpeedView = (TextView) findViewById(R.id.vehicle_speed);
	    mEngineSpeedView = (TextView) findViewById(R.id.engine_speed);
	    mShiftIndicator = (TextView) findViewById(R.id.shift_indicator);
	    mShiftCalc = (TextView) findViewById(R.id.shift_calculated);
	    mPedalView = (TextView) findViewById(R.id.pedal_position);
	    mGearPosition = (TextView) findViewById(R.id.gear_position);
	    mLayout = findViewById(R.id.layout);
	    mLayout.setBackgroundColor(Color.BLACK);
	    
	    mLEDbar = (SeekBar) findViewById(R.id.led_bar);
	    mLEDbar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				String s = Integer.toString(progress*255/100);
				send2Arduino('('+s+')');
			}

			public void onStartTrackingTouch(SeekBar seekBar) {
			}

			public void onStopTrackingTouch(SeekBar seekBar) {			
			}

	    });
	    
	    mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
	    
	    mPermissionIntent = PendingIntent.getBroadcast(this, 0,
                new Intent(ACTION_USB_PERMISSION), 0);
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        this.registerReceiver(mBroadcastReceiver, filter);
        
        if(mSerialPort == null){
            mSerialPort = new FTDriver(mUsbManager);
            mSerialPort.setPermissionIntent(mPermissionIntent);
            mSerialStarted = mSerialPort.begin(115200);
            if (!mSerialStarted)
            {
                Log.d(TAG, "mSerialPort.begin() failed.");
            } else{
                Log.d(TAG, "mSerialPort.begin() success!.");
                String s = '<'+"0"+'>';
				send2Arduino(s);
            }
        }
	}

	public void send2Arduino(String outString){
		char[] outMessage = outString.toCharArray();
        byte outBuffer[] = new byte[128];
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
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		getMenuInflater().inflate(R.menu.settings, menu);
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
    
	public void onPause() {
	    super.onPause();
	    Log.i("openxc", "Unbinding from vehicle service");
	    unbindService(mConnection);
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
		    shiftCalculation();
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
	
	ShiftRecommendation.Listener mShiftRecommendation = new ShiftRecommendation.Listener() {
		public void receive(Measurement measurement) {
		    final ShiftRecommendation updated_value = (ShiftRecommendation) measurement;
		    MainActivity.this.runOnUiThread(new Runnable() {
		        public void run() {
		        	if (updated_value.getValue().booleanValue() == true) {
		        		mShiftIndicator.setText("SHIFT!");
		        		mLayout.setBackgroundColor(Color.WHITE);
		        		if (!justShifted){
		        			send2Arduino('['+"1"+']');
		        		}
		        		justShifted = true;
		        	}
		        	
		        	else {
		        		mShiftIndicator.setText("");
		        		mLayout.setBackgroundColor(Color.WHITE);
		        		justShifted = false;
		        	}
		        }
		    });
		}
	};
	
	/* shiftCalculation is the main function of this class. In the event 
	 * that a vehicle is not equipped with a built-in "ShiftRecommendation" 
	 * signal on CAN, this function will calculate the upshift point locally. 
	 * The gear position and shift point are then sent to the shift knob. 
	 */
	public void shiftCalculation() {
		
		/* GEAR POSITION CALCULATION:
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
	    
	    /* SHIFT CALCULATION:
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
	    
	    /* If the pedal position is above the minimum threshold, then the driver
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
	    	next_rpm = 1.3*(pedal_pos)*(pedal_pos)-20*pedal_pos+1680; //GT Mustang
	    	// next_rpm = 1.2*(pedal_pos)*(pedal_pos)-30*pedal_pos+1300; //Figo/Focus
	    }
	    
	    else next_rpm=min_rpm;
	    
	    if (next_rpm < vehicle_speed*next_ratio){
	    	
	    	if (!justShifted){
    			send2Arduino('['+"1"+']');
    			MainActivity.this.runOnUiThread(new Runnable() {
			        public void run() {
			            mShiftCalc.setText("Shift!!");
			            mLayout.setBackgroundColor(Color.WHITE);
			        }
			    });
    			justShifted = true;
    			shiftTime = new Date().getTime();
    		}
    		cancelShift(currentTime);
	    }
	    
	    else cancelShift(currentTime);
	}

	/* updateGear takes the calculated gear position and sends that value
	 * to the shift knob. The gear position is enclosed in '<' ___ '>'
	 */
	private void updateGear(final int g) {
		MainActivity.this.runOnUiThread(new Runnable() {
			public void run() {
				mGearPosition.setText(Integer.toString(g));
			}
		});
		
		if (g != currentGear){
			String s = '<'+Integer.toString(g)+'>';
			send2Arduino(s);
		}
		currentGear = g;
	}
	
	/* cancelShift removes the "upshift message" from the UI screen after a given
	 * amount of time.
	 */
	private void cancelShift(long t) {
		if (t-shiftTime>500){
			MainActivity.this.runOnUiThread(new Runnable() {
				public void run() {
					mShiftCalc.setText("");
					mLayout.setBackgroundColor(Color.BLACK);
				}
			});
		}
		return;
	}
	
	private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action)) {
                UsbDevice device = (UsbDevice) intent.getParcelableExtra(
                        UsbManager.EXTRA_DEVICE);

                if(intent.getBooleanExtra(
                            UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                    mSerialStarted = mSerialPort.begin(9600);
                } 
                
                else {
                    Log.i(TAG, "User declined permission for device " + device);
                }
            }
        }
    };
    
    public void onExit(View view){

        if (mSerialPort != null){
            mSerialPort.end();
        }
        if(mIsBound) {
            Log.i(TAG, "Unbinding from vehicle service before exit");
            unbindService(mConnection);
            mIsBound = false;
        }
        finish();
        System.exit(0);
    }
}
