package com.example.shiftindicator;

import java.util.Locale;

import com.openxc.measurements.BaseMeasurement;
import com.openxc.units.State;

/**
 * The TransmissionGearPosition is the actual current gear of the transmission.
 *
 * This measurement is the current actual gear, not the selected or desired gear
 * by the driver or computer.
 */
public class ShiftRecommendation
        extends BaseMeasurement<State<ShiftRecommendation.ShiftSignal>> {
    public final static String ID = "shift_recommendation";

    public enum ShiftSignal {
        UPSHIFT,
        NONE,
        DOWNSHIFT
    }

    public ShiftRecommendation(State<ShiftSignal> value) {
        super(value);
    }

    public ShiftRecommendation(ShiftSignal value) {
        this(new State<ShiftSignal>(value));
    }

    public ShiftRecommendation(String value) {
        this(ShiftSignal.valueOf(value.toUpperCase(Locale.US)));
    }

    @Override
    public String getGenericName() {
        return ID;
    }
}
