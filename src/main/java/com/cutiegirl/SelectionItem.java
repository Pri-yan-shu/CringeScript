package com.cutiegirl;

import com.bitwig.extension.controller.api.*;
import com.bitwig.extensions.framework.RelativeHardwareControlBinding;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.stream.IntStream;

class SelectionItem <I extends ObjectProxy & DeleteableObject> extends AbstractCombo {
    protected SelectionItem(InputHandler handler, Bank<I> bank, I cursor, LAYER selLayer, LAYER itemLayer, IntFunction<I[]> generator, List<Function<I, SettableBooleanValue>> properties) {
        super(2);
        SelectionItem.handler = handler;
        this.bank = bank;
        this.cursor = cursor;
        SelectionItem.selLayer = selLayer;
        SelectionItem.itemLayer = itemLayer;
        this.properties = properties;

        bankItems = IntStream.range(0, 16).mapToObj(bank::getItemAt).toArray(generator);
    }
    @Override
    protected void buildAction(int step, int index) {
        switch (step) {
            case 0 -> {
                if (isActivated) deactivate();
                hiPos = loPos = index;
            }
            case 1 -> {
                if (index > loPos) hiPos = index;
                else {
                    hiPos = loPos;
                    loPos = index;
                }
                activate();
            }
        }
    }

    @Override
    protected void tapAction(int step, int index) {
        if (!isBuilding()) deactivate();
    }

    @Override
    protected void retreatAction(int step, int index) {
        if (!isBuilding() && !comboSet()) deactivate();
    }
    final void deleteObject() {
        if (isActivated) {
            for (I item : getBankItems()) item.deleteObject();
            deactivate();
        } else cursor.deleteObject();
    }
    final void toggleProperty(int index) {
        if (isActivated) {
            boolean b = !properties.get(index).apply(cursor).get();
            for (I item : getBankItems()) properties.get(index).apply(item).set(b);
        } else properties.get(index).apply(cursor).toggle();
    }
    final I[] getBankItems() {
        return Arrays.copyOfRange(bankItems, loPos, hiPos + 1);
    }
    void moveUp() {
        loPos++;
        hiPos++;
    }
    void moveDown() {
        loPos--;
        hiPos--;
    }
    protected void activate() {
        isActivated = true;
        handler.updateLayer(selLayer);
        for (RelativeHardwareControlBinding b : handler.knobBindings[itemLayer.id]) b.setIsActive(false);
    }
    void deactivate() {
        isActivated = false;
        handler.updateLayer(itemLayer);
        for (RelativeHardwareControlBinding b : handler.knobBindings[itemLayer.id]) b.setIsActive(true);
    }
    static protected InputHandler handler;
    protected final Bank<I> bank;
    protected final I[] bankItems;
    protected final I cursor;
    private final List<Function<I, SettableBooleanValue>> properties;
    static protected boolean isActivated;
    static protected int loPos;
    static protected int hiPos;
    static private LAYER selLayer;
    static private LAYER itemLayer;
}
