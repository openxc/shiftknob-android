package com.example.shiftindicator;

import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

public class JsonBuilder {
	private static final String TAG = "JsonBuilder";
	private static final String NAME_FIELD = "name";
    private static final String VALUE_FIELD = "value";
    
	public static String builder(String name, int value) {
		JSONObject message = new JSONObject();
		try {
			message.put(VALUE_FIELD, value);
			message.put(NAME_FIELD, name);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			Log.d(TAG, "Unable to create JSONObject");
		}
		return message.toString();
	}
}
