package com.cutiegirl;

abstract class AbstractCombo {
    AbstractCombo(int comboLength) {
        step = 0;
        comboSet = false;
        this.comboLength = comboLength;
    }
    void build(int index) {
        if (step == 0) comboSet = false;
        buildAction(step, index);
        step++;
    }
    void tap(int index) {
        if (step == comboLength) comboSet = true;
        step--;
        tapAction(step, index);
    }
    void retreat(int index) {
        step--;
        retreatAction(step, index);
    }
    boolean isBuilding() {
        return step > 0;
    }
    boolean comboSet() {
        return comboSet;
    }
    protected abstract void buildAction(int step, int index);
    protected abstract void tapAction(int step, int index);
    protected abstract void retreatAction(int step, int index);

    private int step;
    private boolean comboSet;
    private final int comboLength;
}
