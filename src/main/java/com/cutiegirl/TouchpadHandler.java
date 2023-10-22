//package com.cutiegirl;
//
//import com.bitwig.extension.controller.api.SettableRangedValue;
//import jwinpointer.JWinPointerReader;
//
//import static com.cutiegirl.GESTURE.*;
//import static com.cutiegirl.InputHandler.ACTIVE_LAYER;
//
//public class TouchpadHandler implements JWinPointerReader.PointerEventListener {
//    TouchpadHandler(InputHandler handler) {
//        this.handler = handler;
//    }
//
//    @Override
//    public void pointerXYEvent(int deviceType, int pointerID, int eventType, boolean inverted, int x, int y, int pressure) {
//        int xIndex = x/32;
//        int yIndex = y/32;
//        int note = x + 8*y;
//        switch (eventType) {
//            case 3:
//                handler.handleMsg(new Msg(note, ONN, ACTIVE_LAYER));
//                break;
//            case 4:
//                handler.handleMsg(new Msg(note, OFF, ACTIVE_LAYER));
//                break;
//        }
//    }
//
//    @Override
//    public void pointerButtonEvent(int i, int i1, int i2, boolean b, int i3) {}
//
//    @Override
//    public void pointerEvent(int i, int i1, int i2, boolean b) {}
//
//    public void init() {
//        JWinPointerReader j = new JWinPointerReader("Knobs");
//        j.addPointerEventListener(this);
//        handler.host.println("Touchpad Init");
//    }
//    InputHandler handler;
//    SettableRangedValue[][] values;
//}
