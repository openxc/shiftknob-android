package com.example.shiftindicator;

import com.openxc.units.Boolean;
import com.openxc.measurements.BaseMeasurement;

public class ShiftRecommendation extends BaseMeasurement<Boolean> {

    public final static String ID = "shift_indication";

    public ShiftRecommendation(Boolean value) {
        super(value);
    }

    public ShiftRecommendation(java.lang.Boolean value) {
        this(new Boolean(value));
    }

    @Override
    public String getGenericName() {
        return ID;
    }
}
