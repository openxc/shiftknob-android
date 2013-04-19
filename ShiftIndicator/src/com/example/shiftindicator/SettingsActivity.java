package com.example.shiftindicator;

import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.Switch;

public class SettingsActivity extends PreferenceActivity implements
				OnClickListener {
	
	Switch modeSwitch;
	CheckBox audioCB;
	CheckBox vibrateCB;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings);
 
        modeSwitch = (Switch) findViewById(R.id.pref_operation_mode_key);
    }
	
    private void loadPrefs() {
    	
    }

    private void savePrefs(String key, boolean value) {

    }

    private void savePrefs(String key, String value) {

    }

	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		
	}
}
