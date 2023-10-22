package com.cutiegirl;

import com.bitwig.extension.controller.api.ControllerHost;
import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.NativeHookException;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener;

import static com.cutiegirl.TEMPLATE.*;
import static com.github.kwhat.jnativehook.keyboard.NativeKeyEvent.*;
import static com.cutiegirl.GESTURE.*;
import static com.cutiegirl.GROUP.*;
import static com.cutiegirl.LAYER.*;

final class KeyHandler implements NativeKeyListener {
    KeyHandler(ControllerHost host, InputHandler handler) {
        this.host = host;
        this.handler = handler;
        isEnabled = true;
    }
    public void nativeKeyPressed(NativeKeyEvent e) {
        if (isEnabled) handleKey(e, true);
        else if (e.getKeyCode() == VC_ENTER && e.getKeyLocation() == KEY_LOCATION_NUMPAD) enable();
    }
    public void nativeKeyReleased(NativeKeyEvent e) {
        if (isEnabled) handleKey(e, false);
    }
    void handleKey(NativeKeyEvent e, boolean pressed) {
        GESTURE gesture = pressed ? ONN : OFF;
        int keyCode = e.getKeyCode();

        switch (e.getKeyLocation()) {
            case KEY_LOCATION_LEFT -> {
                switch (keyCode) {
                    case VC_SHIFT -> handler.handleShiftMsg(pressed);
                    case VC_CONTROL -> handler.updateKnobSensitivity(pressed);
                }
            }
            case KEY_LOCATION_RIGHT -> {
                switch (keyCode) {
                    case 0xe36 -> {
                        shiftPressed = pressed;
//                        handler.shiftPressed = pressed;
                        handler.updateSliders();
                    }
                    case VC_CONTROL -> {
                        ctrlPressed = pressed;
                        handleNumpad(pressed ? handler.buttonPressed : -1);
                    }
                    case VC_ALT -> altPressed = pressed;
                }
            }
            case KEY_LOCATION_NUMPAD -> {
                switch (keyCode) {
                    case VC_ENTER -> {if (pressed) disable();}
                    case VC_9 -> handler.handleMsg(new Msg(shiftPressed ? 17 : 18, gesture, FOCUS_LAYER, shiftPressed ? 49 : 50, null));

                    //Encoder
                    case 0xe4a -> {if (pressed) handler.handleEncoderMsg(new Msg(handler.buttonStatus(), OFF, FOCUS_LAYER));}   //Minus
                    case 0xe4e -> {if (pressed) handler.handleEncoderMsg(new Msg(handler.buttonStatus(), ONN, FOCUS_LAYER));}   //Plus

                    // NUMPAD
                    case VC_0 -> handleNumpad(pressed ? 0 : -1);
                    case VC_SEPARATOR -> handleNumpad(pressed ? 1 : -1);
                    case VC_1, VC_2, VC_3, VC_4, VC_5, VC_6-> handleNumpad(pressed ? keyCode - VC_1 + 2 : -1);

                    //FUN KEYS
                    case VC_INSERT -> dispatchMsg(14, gesture, FUN_KEYS, pressed);
                    case VC_DELETE -> dispatchMsg(15, gesture, FUN_KEYS, pressed);

                    //META KEYS
                    case VC_SLASH -> {if (pressed) handler.handleEncoderMsg(new Msg(40, OFF, FOCUS_LAYER));}
                    case VC_PRINTSCREEN -> {if (pressed) handler.handleEncoderMsg(new Msg(40, ONN, FOCUS_LAYER));}

                    //QWE_KEYS
                    case VC_7 -> dispatchMsg(14, gesture, QWE_KEYS, pressed);
                    case VC_8 -> dispatchMsg(15, gesture, QWE_KEYS, pressed);
                }
            }
            case KEY_LOCATION_STANDARD -> {
                switch (keyCode) {
                    //FUN KEYS
                    case VC_ESCAPE -> dispatchMsg(0, gesture, FUN_KEYS, pressed);
                    case VC_F1,VC_F2,VC_F3,VC_F4,VC_F5,VC_F6,VC_F7,VC_F8,VC_F9,VC_F10 ->
                            dispatchMsg(keyCode - VC_F1 + 1, gesture, FUN_KEYS, pressed);
                    case VC_F11 -> dispatchMsg(11, gesture, FUN_KEYS, pressed);
                    case VC_F12 -> dispatchMsg(12, gesture, FUN_KEYS, pressed);
                    case VC_PRINTSCREEN -> dispatchMsg(13, gesture, FUN_KEYS, pressed);
                    //NUM  KEYS
                    case VC_BACKQUOTE -> dispatchMsg(0, gesture, NUM_KEYS, pressed);
                    case VC_1,VC_2,VC_3,VC_4,VC_5,VC_6,VC_7 -> dispatchMsg(keyCode - VC_1 + 1, gesture, NUM_KEYS, pressed);
                    case VC_9,VC_0,VC_MINUS -> updateLayer(keyCode - VC_9, pressed);
                    case VC_EQUALS -> updateLayer(4, pressed);
                    case VC_8 -> updateLayer(3, pressed);
                    case VC_BACKSPACE -> updateLayer(5, pressed);
                    //QWE_KEYS
                    case VC_TAB,VC_Q,VC_W,VC_E,VC_R,VC_T,VC_Y,VC_U,VC_I,VC_O,VC_P,VC_OPEN_BRACKET,VC_CLOSE_BRACKET ->
                            dispatchMsg(keyCode - VC_TAB, gesture, QWE_KEYS, pressed);
                    case VC_BACK_SLASH -> dispatchMsg(13, gesture, QWE_KEYS, pressed);
//                    case VC_A, VC_S, VC_D, VC_F, VC_G, VC_H, VC_J, VC_K ->
//                            dispatchMsg(keyCode - VC_A, gesture, ASD_KEYS, pressed);
                }
            }
        }
//        host.showPopupNotification("Group: "+ACTIVE_GROUP.name());
    }
    private void dispatchMsg(int note, GESTURE gesture, GROUP group, boolean pressed) {
        LAYER target;
            if (pressed) {
            target = switch (group) {
                case NONE -> FOCUS_LAYER;
                case META_KEYS -> META;
                case NUM_KEYS -> switch (ACTIVE_GROUP) {
                    case FUN_KEYS, META_KEYS -> FUN_LAYER.context;
                    case NONE -> switch (ACTIVE_TEMPLATE) {

                        case DEFAULT, PIANO -> FUN_LAYER.context;
                        case CHORD -> SCALES;
                    };
//                    case NONE -> FUN_LAYER.context;
                    case NUM_KEYS, QWE_KEYS, ASD_KEYS -> FOCUS_LAYER.context;
                };
                case FUN_KEYS -> switch (ACTIVE_GROUP) {
                    case NONE, FUN_KEYS -> switch (ACTIVE_TEMPLATE) {

                        case DEFAULT, CHORD, PIANO -> FUN_LAYER;
                    };
                    case META_KEYS -> FUN_LAYER;
                    case NUM_KEYS -> FOCUS_LAYER.context;
                    case QWE_KEYS -> FOCUS_LAYER.child;
                    case ASD_KEYS -> null;
                };
                case QWE_KEYS -> switch (ACTIVE_GROUP) {
//                    case NONE, QWE_KEYS -> keyPiano.isEnabled() ? NOTES : QWE_LAYER;
                    case NONE, QWE_KEYS -> switch (ACTIVE_TEMPLATE) {

                        case DEFAULT -> QWE_LAYER;
                        case PIANO -> NOTES;
                        case CHORD -> CHORDS_NOTES;
                    };
                    case FUN_KEYS, META_KEYS, NUM_KEYS, ASD_KEYS -> FOCUS_LAYER.child;
                };
                case ASD_KEYS -> CLIP_2;
            };
            if (target == null) target = BLANK;
        } else target = FOCUS_LAYER;

        int rawNote = note + switch (group) {
            case NONE, FUN_KEYS -> 0;
            case NUM_KEYS       -> 16;
            case META_KEYS      -> 24;
            case QWE_KEYS       -> 32;
            case ASD_KEYS       -> 48;
        };
        handler.handleMsg(new Msg(note, gesture, target, rawNote, ACTIVE_GROUP));
        if (pressed) {
            updateGroup(group);
            FOCUS_LAYER = target;
        }
//        else retreatFocus();
    }
    private void handleNumpad(int note) {
        handler.buttonPressed = note + (ctrlPressed ? ctrlOffset : 0);
        handler.updateSliders();
    }
    private void updateLayer(int i, boolean pressed) {
        int layerHistory = QWE_LAYER.id - 1;
        handler.handleMetaMsg(new Msg(i, pressed ? ONN : OFF, META, ACTIVE_GROUP, layerHistory, i + 24));
        if (pressed) {
            LAYER l = LAYER.getLayer(i);
            if (FUN_LAYER != l) {
                QWE_LAYER = FUN_LAYER;
                FUN_LAYER = l;
            }
            updateGroup(META_KEYS);
            LAST_FOCUS = FOCUS_LAYER;
            FOCUS_LAYER = FUN_LAYER;
            host.showPopupNotification(FUN_LAYER.name() + "/" + QWE_LAYER.name());
        } else FOCUS_LAYER = LAST_FOCUS;
    }
    void retreatLayer(LAYER l) {
        if (QWE_LAYER != l) {
            FUN_LAYER = QWE_LAYER;
            QWE_LAYER = l;
        }
        host.showPopupNotification(FUN_LAYER.name()+"/"+QWE_LAYER.name());
    }
    void updateGroup(GROUP g) {
        ACTIVE_GROUP = g;
    }
    void retreatFocus() {
        if (FOCUS_LAYER.getParent() != null) FOCUS_LAYER = FOCUS_LAYER.getParent();
    }
    void updateTemplate(TEMPLATE t) {
        ACTIVE_TEMPLATE = t;
//        host.showPopupNotification("Template: "+ACTIVE_TEMPLATE.name());
    }
    void enable() {
        isEnabled = true;
        host.showPopupNotification("Keys: ON");
    }
    void init() {
        try {
            GlobalScreen.registerNativeHook();
        }
        catch (NativeHookException ex) {
            throw new RuntimeException(ex);
        }
        GlobalScreen.addNativeKeyListener(this);
        host.showPopupNotification("Keyboard Hooked");
    }
    private void disable() {
        isEnabled = false;
        host.showPopupNotification("Keys: OFF");
    }
    static boolean shiftPressed() {
        return shiftPressed;
    }
    static boolean ctrlPressed() {
        return ctrlPressed;
    }
    static boolean altPressed() {
        return altPressed;
    }
    static private InputHandler handler;
    static private ControllerHost host;
    static private GROUP ACTIVE_GROUP = NONE;
    static private LAYER FUN_LAYER = TRACK;
    static private LAYER QWE_LAYER = DEVICE;
    static private LAYER FOCUS_LAYER = FUN_LAYER;
    static private LAYER LAST_FOCUS = FUN_LAYER;
    static private TEMPLATE ACTIVE_TEMPLATE = DEFAULT;
    static private boolean isEnabled;
    static private boolean shiftPressed;
    static private boolean altPressed;
    static private boolean ctrlPressed;
    static final int ctrlOffset = 10;
}
enum GROUP {
    NONE,
    META_KEYS,
    FUN_KEYS,
    NUM_KEYS,
    QWE_KEYS,
    ASD_KEYS
}
enum TEMPLATE {
    DEFAULT,
    PIANO,
    CHORD
}