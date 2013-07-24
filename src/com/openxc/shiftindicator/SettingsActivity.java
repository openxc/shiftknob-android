package com.openxc.shiftindicator;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;

public class SettingsActivity extends PreferenceActivity implements 
            SharedPreferences.OnSharedPreferenceChangeListener{

    public static final String KEY_PREF_CALCULATION_MODE = "pref_operation_mode";
    public static final String KEY_PREF_SHIFT_POINT = "pref_shift_point";
    public static final String KEY_PREF_CALCULATION = "pref_calculation_mode";
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPref, String key) {
        // TODO Auto-generated method stub
        if (key.equals(KEY_PREF_CALCULATION_MODE)) {
            Preference shiftPointPref = findPreference(KEY_PREF_SHIFT_POINT);
            Preference algorithmPref = findPreference(KEY_PREF_CALCULATION);
            if (sharedPref.getBoolean(key, false)) {
                shiftPointPref.setEnabled(true);
                algorithmPref.setEnabled(false);
            } else {
                shiftPointPref.setEnabled(false);
                algorithmPref.setEnabled(true);
            }
        }
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
