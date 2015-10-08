package com.openxc.shiftindicator;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.util.Log;

public class SettingsActivity extends PreferenceActivity implements 
            SharedPreferences.OnSharedPreferenceChangeListener{

    public static final String KEY_PREF_OPERATION_MODE = "pref_operation_mode";
    public static final String KEY_PREF_SHIFT_POINT = "pref_shift_point";
    public static final String KEY_PREF_CALCULATION = "pref_calculation_mode";
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings);
        updatePreferenceScreen();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPref, String key) {
        // TODO Auto-generated method stub
        if (key.equals(KEY_PREF_OPERATION_MODE)) {
            Preference shiftPointPref = findPreference(KEY_PREF_SHIFT_POINT);
            Preference algorithmPref = findPreference(KEY_PREF_CALCULATION);
            if (sharedPref.getBoolean(key, false)) {
                shiftPointPref.setEnabled(true);
                algorithmPref.setEnabled(false);
            } else {
                shiftPointPref.setEnabled(false);
                algorithmPref.setEnabled(true);
            }
           
        } else if (key.equals(KEY_PREF_SHIFT_POINT)) {
            Preference shiftPointPref = findPreference(KEY_PREF_SHIFT_POINT);
            shiftPointPref.setSummary(sharedPref.getString(KEY_PREF_SHIFT_POINT, "")+" RPM");
            int shiftPoint = Integer.parseInt(sharedPref.getString(KEY_PREF_SHIFT_POINT, ""));
            MainActivity.setShiftPoint(shiftPoint);
        }
    }
    
    public void updatePreferenceScreen() {
        SharedPreferences operationPref = findPreference(KEY_PREF_OPERATION_MODE).getSharedPreferences();
        Preference shiftPointPref = findPreference(KEY_PREF_SHIFT_POINT);
        Preference algorithmPref = findPreference(KEY_PREF_CALCULATION);
        if (operationPref.getBoolean(KEY_PREF_OPERATION_MODE, false)) {
            shiftPointPref.setEnabled(true);
            algorithmPref.setEnabled(false);
        } else {
            shiftPointPref.setEnabled(false);
            algorithmPref.setEnabled(true);
        }
        shiftPointPref.setSummary(operationPref.getString(KEY_PREF_SHIFT_POINT, "")+" RPM");
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        getPreferenceScreen().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
    }
}
