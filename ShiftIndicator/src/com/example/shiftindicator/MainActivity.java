package com.example.shiftindicator;


import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;

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
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.text.Layout;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.TextView;

public class MainActivity extends Activity {

	private VehicleManager mVehicleManager;
	private MeasurementUpdater mUpdater;
	private TextView mVehicleSpeedView;
	private TextView mEngineSpeedView;
	private TextView mShiftIndicator;
	private TextView mShiftCalc;
	private TextView mPedalView;
	private View mLayout;
	private TraceVehicleDataSource mTraceSource;
	private int engine_speed;
	private double vehicle_speed;
	private double pedal_pos;
	
	private int currentGear;
	private double base_pedal_position = 15.0;
	private int min_rpm = 1200;
	
//	FIGO RATIOS rpm/speed
//
//	private double ratio1 = 139.9;
//	private double ratio2 = 75.2;
//	private double ratio3 = 50.0;
//	private double ratio4 = 37.2;
//	private double ratio5 = 29.5;
	
//	Focus ST RATIOS rpm/speed:
	private int ratio1 = 114;
	private int ratio2 = 69;
	private int ratio3 = 46;
	private int ratio4 = 36;
	private int ratio5 = 28;
	private int ratio6 = 23;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		Intent intent = new Intent(this, VehicleManager.class);
	    bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
	    mVehicleSpeedView = (TextView) findViewById(R.id.vehicle_speed);
	    mEngineSpeedView = (TextView) findViewById(R.id.engine_speed);
	    mShiftIndicator = (TextView) findViewById(R.id.shift_indicator);
	    mShiftCalc = (TextView) findViewById(R.id.shift_calculated);
	    mPedalView = (TextView) findViewById(R.id.pedal_position);
	    mLayout = findViewById(R.id.layout);
	    mLayout.setBackgroundColor(Color.BLACK);
	    
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

	public void onPause() {
	    super.onPause();
	    Log.i("openxc", "Unbinding from vehicle service");
	    unbindService(mConnection);
	    mVehicleManager.removeSource(mTraceSource);
	}

	private ServiceConnection mConnection = new ServiceConnection() {
	    // Called when the connection with the service is established
	    public void onServiceConnected(ComponentName className,
	            IBinder service) {
	        Log.i("openxc", "Bound to VehicleManager");
	        mVehicleManager = ((VehicleManager.VehicleBinder)service).getService();
	        try {
				mVehicleManager.addListener(VehicleSpeed.class, mSpeedListener);
				mVehicleManager.addListener(EngineSpeed.class, mEngineListener);
				mVehicleManager.addListener(AcceleratorPedalPosition.class, mPedalListener);
				mVehicleManager.addListener(ShiftRecommendation.class, mShiftRecommendation);
			} catch (VehicleServiceException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (UnrecognizedMeasurementTypeException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	        
	        URI mTraceFile;
			try {
				mTraceFile = new URI("file:///sdcard/com.openxc/shiftIndicateTraceboolean.json");
				mTraceSource = new TraceVehicleDataSource(MainActivity.this, mTraceFile);
			} catch (URISyntaxException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (DataSourceException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	        mVehicleManager.addSource(mTraceSource);
	        mUpdater = new MeasurementUpdater();
	        mUpdater.start();
	    }

	    // Called when the connection with the service disconnects unexpectedly
	    public void onServiceDisconnected(ComponentName className) {
	        Log.w("openxc", "VehicleService disconnected unexpectedly");
	        mVehicleManager = null;
	        mUpdater = null;
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
		    
		    // Gear position calculation
		    
		    // First calculate gear based on ratio of rpm to speed
		    if(vehicle_speed==0) vehicle_speed = 1;
		    double ratio = engine_speed/vehicle_speed;
		    int next_ratio=1;
		    
		    if((ratio1*1.03) > ratio && (ratio1*.97) < ratio){
		    	next_ratio=ratio2;
		    }
		    else if((ratio2*1.03) > ratio && (ratio2*.97) < ratio){
		    	next_ratio=ratio3;
		    }
		    else if((ratio3*1.03) > ratio && (ratio3*.97) < ratio){
		    	next_ratio=ratio4;
		    }
		    else if((ratio4*1.03) > ratio && (ratio4*.97) < ratio){
		    	next_ratio=ratio5;
		    }
		    else if((ratio5*1.03) > ratio && (ratio5*.97) < ratio){
		    	next_ratio=ratio6;
		    }
		    else {
		    	MainActivity.this.runOnUiThread(new Runnable() {
			        public void run() {
			            mShiftCalc.setText("");
			        }
			    }); 
		    	return;
		    }
		    
		    if (pedal_pos < 10) {
		    	MainActivity.this.runOnUiThread(new Runnable() {
			        public void run() {
			            mShiftCalc.setText("");
			        }
			    });
		    	return;
		    }
		    
		    double next_rpm;
		    if (pedal_pos >= base_pedal_position){
		    	next_rpm = 1.2*(pedal_pos)*(pedal_pos)-35*pedal_pos+1457;
		    }
		    else next_rpm=min_rpm;
		    
		    if (vehicle_speed > next_rpm/next_ratio){
		    	MainActivity.this.runOnUiThread(new Runnable() {
			        public void run() {
			            mShiftCalc.setText("Shift!!");
			        }
			    }); 
		    }
		    
		    else MainActivity.this.runOnUiThread(new Runnable() {
		        public void run() {
		            mShiftCalc.setText("");
		        }
		    }); 
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
		        		//mLayout.setBackgroundColor(Color.WHITE); // flash the background when the driver should shift
		        	}
		        	else {
		        		//mLayout.setBackgroundColor(Color.BLACK);
		        		mShiftIndicator.setText("");
		        	}
		        }
		    });
		}
	};
	
	private class MeasurementUpdater extends Thread {
        private boolean mRunning = true;

        public void done() {
            mRunning = false;
        }

        public void run() {
            while(mRunning) {
            	
            	
            	
            	
            	
            	
            	
            	
                int pollFrequency = 100;
                try {
                    Thread.sleep(pollFrequency);
                } catch (InterruptedException e) {
                	e.printStackTrace();
                }
            }
        }
    }
}
