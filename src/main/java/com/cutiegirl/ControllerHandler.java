package com.cutiegirl;

import com.bitwig.extension.controller.api.AbsoluteHardwareKnob;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.HardwareButton;
import com.bitwig.extension.controller.api.RelativeHardwareKnob;

import static com.cutiegirl.CringeScriptExtension.NUM_PADS;
import static com.cutiegirl.GESTURE.*;
import static com.cutiegirl.InputHandler.ACTIVE_LAYER;

public final class ControllerHandler {
    ControllerHandler(CringeScriptExtension driver, InputHandler handler) {
        this.driver = driver;
        this.host = driver.getHost();
        this.handler = handler;

        setKnobs();
        setPads();
        setAft();
    }
    private void setKnobs() {
        RelativeHardwareKnob mainEncoder = driver.mainEncoder;
        RelativeHardwareKnob shiftEncoder = driver.shiftEncoder;
        mainEncoder.setBinding(host.createRelativeHardwareControlStepTarget(
                host.createAction(() -> handler.handleEncoderMsg(new Msg(handler.buttonStatus(), ONN, ACTIVE_LAYER)), () -> ""),
                host.createAction(() -> handler.handleEncoderMsg(new Msg(handler.buttonStatus(), OFF, ACTIVE_LAYER)), () -> "")));
        shiftEncoder.setBinding(host.createRelativeHardwareControlStepTarget(
                host.createAction(() -> handler.handleEncoderMsg(new Msg(handler.buttonStatus(), ONN, ACTIVE_LAYER)), () -> ""),
                host.createAction(() -> handler.handleEncoderMsg(new Msg(handler.buttonStatus(), OFF, ACTIVE_LAYER)), () -> "")));

        mainEncoder.hardwareButton().pressedAction().setBinding(host.createAction(() -> handler.handleMsg(new Msg(17, ONN, ACTIVE_LAYER, 49, null)), () -> ""));
        mainEncoder.hardwareButton().releasedAction().setBinding(host.createAction(() -> handler.handleMsg(new Msg(17, OFF, ACTIVE_LAYER, 49, null)), () -> ""));
        shiftEncoder.hardwareButton().pressedAction().setBinding(host.createAction(() -> handler.handleMsg(new Msg(18, ONN, ACTIVE_LAYER, 50, null)), () -> ""));
        shiftEncoder.hardwareButton().releasedAction().setBinding(host.createAction(() -> handler.handleMsg(new Msg(18, OFF, ACTIVE_LAYER, 50, null)), () -> ""));
    }
    private void setPads() {
        HardwareButton[] pads = driver.getBankAPads();
        for (int i = 0; i < NUM_PADS; i++) {
            final int I = i;
            pads[I].pressedAction().setBinding(host.createAction(vel -> {
                if (vel > velThresh) {
                    if (I > 5) handler.handleEncoderMsg(new Msg(40, I == 6 ? OFF : ONN, ACTIVE_LAYER));
                    else handler.handleMetaMsg(new Msg(I, ONN, LAYER.META));
                } else handler.handleMsg(new Msg(I, ONN, ACTIVE_LAYER));
            }, () -> ""));
            pads[I].releasedAction().setBinding(host.createAction(vel -> handler.handleMsg(new Msg(I, OFF, ACTIVE_LAYER)), () -> ""));
        }
        HardwareButton shift = driver.shiftButton;
        shift.pressedAction().setBinding(host.createAction(() -> {
            handler.handleShiftMsg(true);
            handler.updateKnobSensitivity(true);
        }, () -> ""));
        shift.releasedAction().setBinding(host.createAction(() -> {
            handler.handleShiftMsg(false);
            handler.updateKnobSensitivity(false);
        }, () -> ""));
    }
    private void setAft() {
        AbsoluteHardwareKnob[] aft = driver.padAftertouch;
        for (int i = 0; i < NUM_PADS; i++) {
            final int I = i;
            aft[I].setBinding(host.createAbsoluteHardwareControlAdjustmentTarget(val -> {
                if (!aftEngaged[I]) {
                    aftEngaged[I] = true;
                    handler.handleMsg(new Msg(I, AFT, ACTIVE_LAYER));
                } else if (aftTokens[I] && val < aftLoThresh) {
                    aftTokens[I] = false;
                } else if (!aftTokens[I] && val > aftHiThresh) {
                    aftTokens[I] = true;
                    handler.handleMsg(new Msg(I, AFT, ACTIVE_LAYER));
                }
            }));
        }
    }
    private final double velThresh = 0.3;
    private final double aftLoThresh = 0.3;
    private final double aftHiThresh = 0.7;
    private final boolean[] aftEngaged = new boolean[8];
    private final boolean[] aftTokens = new boolean[8];
    private final CringeScriptExtension driver;
    private final ControllerHost host;
    private final InputHandler handler;
}