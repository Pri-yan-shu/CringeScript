package com.cutiegirl;

import com.bitwig.extension.controller.api.Bank;
import com.bitwig.extension.controller.api.CursorDeviceSlot;
import com.bitwig.extension.controller.api.Device;
import com.bitwig.extension.controller.api.DeviceLayerBank;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/*If the track is changed mid selection, the cursorDevice position and the bank itemCount change
* which may lead to unpredictable behaviour, especially when the track's deviceChain is empty
*
* It can be circumvented by avoiding using device.position() (which can be -1 when track is empty)
* and bank.getItemAt(device.position().get) as much as possible
* and do bound checks if using them (loPos < bank.itemCount().get && device.exists().get())*/

final class SelectionDevice extends SelectionItem<Device> {
    SelectionDevice(InputHandler handler, Bank<Device> bank) {
        super(handler, bank, handler.cursorDevice, LAYER.SEL_DEVICE, LAYER.DEVICE, Device[]::new, List.of(
                Device::isEnabled,
                Device::isExpanded,
                Device::isRemoteControlsSectionVisible
        ));
        deviceLayerBank = handler.layerBank;
        cursorDeviceSlot = handler.cursorDeviceSlot;
        cursorDeviceSlot.name().markInterested();

        selectParent = handler.cursorDevice::selectParent;
        selectLastInSlot = handler.cursorDevice::selectLastInSlot;

        cursor.isEnabled().markInterested();
        cursor.isExpanded().markInterested();
        cursor.isRemoteControlsSectionVisible().markInterested();
        cursor.deviceType().markInterested();
    }
    void cycleSlot(boolean reverse) {
        if (cursor.hasSlots().get()) {
            String[] slotNames = cursor.slotNames().get();
            int index = Arrays.asList(slotNames).indexOf(cursor.getCursorSlot().name().get());
            String slot = slotNames[handler.cycle(index, slotNames.length, reverse)];
            cursorDeviceSlot.selectSlot(slot);
            selectLastInSlot.accept(slot);
        }
    }
    void group(int container) {
        if (container > slotNames.length - 1) return;
        insertAndAddToFirstSlot(container, getBankItems());
        if (isActivated) deactivate();
        selectParent.run();
    }
    private void insertAndAddToFirstSlot(int container, Device...devices) {
        bank.getItemAt(hiPos).afterDeviceInsertionPoint().insertBitwigDevice(
                switch (container) {
                    case 1 -> DELAY;
                    case 2 -> DELAY_2;
                    case 3 -> DELAY_PLUS;
                    default -> CHAIN_UUID;
                }
        );
        handler.host.scheduleTask(() -> {
            cursorDeviceSlot.selectSlot(slotNames[container]);
            cursorDeviceSlot.endOfDeviceChainInsertionPoint().moveDevices(devices);
            selectParent.run();
        }, 100);
    }
    void addParallel() {
        UUID container = switch (cursor.deviceType().get()) {
            case "note-effect" -> NOTE_LAYER;
            case "instrument" -> INSTRUMENT_LAYER;
            default -> FX_LAYER;
        };
        bank.getItemAt(hiPos).afterDeviceInsertionPoint().insertBitwigDevice(container);
        int numDevices = Math.min(hiPos - loPos + 1, deviceLayerBank.getSizeOfBank());
        handler.host.scheduleTask(() -> {
            for (int i = 0; i < 8; i++) {
                //As one device is moved the cursor moves to the deviceChain inside the deviceLayer
                //and the bank starts to refer to the chain inside layer
                //So need to selectParent
                if (i < numDevices) {
                    deviceLayerBank.getItemAt(i).endOfDeviceChainInsertionPoint().moveDevices(bank.getItemAt(loPos));
                    selectParent.run();
                } else deviceLayerBank.getItemAt(i).isActivated().set(false);
            }
            selectParent.run();
        }, 200); //Not 40
        if (isActivated) deactivate();
    }
    void moveToNewTrack() {
        handler.cursorTrack.afterTrackInsertionPoint().moveDevices(getBankItems());
        if (isActivated) deactivate();
    }
    void moveUp() {
        if (hiPos < bank.itemCount().get() - 1) {
            bank.getItemAt(loPos).beforeDeviceInsertionPoint().moveDevices(bank.getItemAt(hiPos + 1));
            super.moveUp();
        }
    }
    void moveDown() {
        if (loPos > 0) {
            bank.getItemAt(hiPos).afterDeviceInsertionPoint().moveDevices(bank.getItemAt(loPos - 1));
            super.moveDown();
        }
    }
    private final Runnable selectParent;
    private final Consumer<String> selectLastInSlot;
    private final CursorDeviceSlot cursorDeviceSlot;
    private final DeviceLayerBank deviceLayerBank;
    private final UUID CHAIN_UUID = UUID.fromString("c86d21fb-d544-4daf-a1bf-57de22aa320c");
    private final UUID FX_LAYER = UUID.fromString("a0913b7f-096b-4ac9-bddd-33c775314b42");
    private final UUID INSTRUMENT_LAYER = UUID.fromString("5024be2e-65d6-4d40-bbfe-8b2ea993c445");
    private final UUID NOTE_LAYER = UUID.fromString("96456481-4c52-423a-8485-4604b15d0183");
    private final UUID DELAY = UUID.fromString("2a7a7328-3f7a-4afb-95eb-5230c298bb90");
    private final UUID DELAY_2 = UUID.fromString("71539d5d-1c7a-4dac-8f74-29e23b89b599");
    private final UUID DELAY_PLUS = UUID.fromString("f2baa2a8-36c5-4a79-b1d9-a4e461c45ee9");
    private final String[] slotNames = new String[]{
            "CHAIN",
            "FB FX",
            "FB FX",
            "FB FX",
    };
}
