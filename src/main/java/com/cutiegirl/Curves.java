package com.cutiegirl;

import com.bitwig.extension.controller.api.ControllerHost;

final class Curves {
    Curves(InputHandler handler) {
        host = handler.host;
        for (int i = 0; i < curves.length; i++) curves[i] = new Curve(i, host);
        activeCurve = 0;
    }
    void setActiveCurve(int i) {
        activeCurve = i;
        activeCurve().showCurve();
    }
    Curve activeCurve() {
        return curves[activeCurve];
    }
    private final ControllerHost host;
    private final Curve[] curves = new Curve[8];
    private int activeCurve;
}