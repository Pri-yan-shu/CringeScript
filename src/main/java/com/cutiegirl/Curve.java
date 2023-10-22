package com.cutiegirl;

import com.bitwig.extension.controller.api.ControllerHost;

import java.util.Map;
import java.util.TreeMap;

import static com.cutiegirl.CringeScriptExtension.NUM_KNOBS;

final class Curve {
    Curve(final int index, final ControllerHost host) {
        points = new TreeMap<>();
        gridRes = 64;
        size = gridRes;
        pageSize = gridRes;
        pagePos = 0;
        this.index = index;
        this.host = host;
    }
    void setPoint(int index, int val) {
        int pos = getPos(index);
        if (val < 128)
            if (points.containsKey(pos)) points.replace(pos, val);
            else points.put(pos, val);
        showCurve();
    }
    void adjustPoint(int index, int delta) {
        int pos = getPos(index);
        if (points.containsKey(pos)) {
            int adjustedVal = points.get(pos) + delta;
            if (adjustedVal < 128 && adjustedVal >= 0) points.replace(pos, adjustedVal);
        } else if (delta > 0) points.put(pos, delta);
        showCurve();
    }
    int getValue(int index) {
        int pos = getPos(index);
        int value;
        if (points.containsKey(pos)) value = points.get(pos);
        else {
            Map.Entry<Integer, Integer> floor = points.floorEntry(pos);
            Map.Entry<Integer, Integer> ceiling = points.ceilingEntry(pos);
            int floorKey = 0;
            int ceilingKey = 8;
            int floorVal = 0;
            int ceilingVal = 0;
            if (floor != null) {
                floorKey = floor.getKey();
                floorVal = floor.getValue();
            }
            if (ceiling != null) {
                ceilingKey = ceiling.getKey();
                ceilingVal = ceiling.getValue();
            }
            value = ((pos - floorKey)*(ceilingVal - floorVal))/(ceilingKey - floorKey) + floorVal;
        }
        return value;
    }
    int getPos(int index) {
        return (pageSize*index)/NUM_KNOBS + pagePos;
    }
    void scrollPageForward() {
        pagePos += pageSize;
        size = pagePos+pageSize;
        showCurveLoop();
    }
    void scrollPageBackward() {
        if (pagePos >= pageSize) pagePos -= pageSize;
        showCurveLoop();
    }
    void doublePageSize() {
        pageSize *= 2;
        showCurveLoop();
    }
    void halvePageSize() {
        if (pageSize >= NUM_KNOBS) pageSize /= 2;
        showCurveLoop();
    }
    void trim() {
        size = (points.lastKey()/gridRes + 1)*gridRes;
        showCurveLoop();
    }
    void clear() {
        points.clear();
    }
    void showCurve() {
        StringBuilder str = new StringBuilder();
        for (int i = 0; i < NUM_KNOBS; i++) str.append(getValue(i)).append("   ");
        host.showPopupNotification("Curve "+index+" : "+ str);
    }
    void showCurveLoop() {
        int numBars = size/gridRes + (size%gridRes == 0 ? 0 : 1);
        StringBuilder str = new StringBuilder();
        for (int i = 0; i < numBars; i++) {
            str.append('|');
            for (int j = 0; j < NUM_KNOBS; j++) str.append(getValue(j * gridRes / NUM_KNOBS + i * gridRes) * 9 / 127);
            str.append('|');
        }
        str.insert(pagePos/64, '[').insert((pagePos+pageSize)/64, ']');
        host.showPopupNotification(String.valueOf(str));
    }
    void adjustGlobalMultiplier(double delta) {
        final double finalVal = amount + delta;
        if (finalVal > 0 && finalVal < 1) amount = finalVal;
        host.showPopupNotification(String.format("Curve Amount: %.2f", amount));
    }
    private final ControllerHost host;
    TreeMap<Integer, Integer> points;
    int gridRes;
    int size;
    int pageSize;
    int pagePos;
    final int index;
    private double amount;
}