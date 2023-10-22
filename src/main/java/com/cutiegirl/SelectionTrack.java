package com.cutiegirl;

import com.bitwig.extension.controller.api.Bank;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extensions.framework.RelativeHardwareControlBinding;

import java.util.List;

import static com.cutiegirl.CringeScriptExtension.NUM_KNOBS;

final class SelectionTrack extends SelectionItem<Track> {
    SelectionTrack(InputHandler handler, Bank<Track> bank) {
        super(handler, bank, handler.cursorTrack, LAYER.SEL_TRACK, LAYER.TRACK, Track[]::new, List.of(
                Track::isActivated,
                Track::arm,
                Track::solo,
                Track::mute
        ));

        cursor.isActivated().markInterested();
        cursor.arm().markInterested();
        cursor.solo().markInterested();
        cursor.mute().markInterested();

        cursor.position().markInterested();
        cursor.isStopped().markInterested();
        cursor.crossFadeMode().markInterested();
        cursor.isQueuedForStop().markInterested();

        for (int i = 0; i < bank.getSizeOfBank(); i++)
            handler.displayEnum(bank.getItemAt(i).crossFadeMode(), "Crossfade Mode");

        for (int i = 0; i < bank.getSizeOfBank(); i++) {
            bindings[i][0] = new RelativeHardwareControlBinding(handler.knobs[0], bank.getItemAt(i).volume());
            bindings[i][1] = new RelativeHardwareControlBinding(handler.knobs[1], bank.getItemAt(i).pan());
            for (int j = 0; j < 6; j++)
                bindings[i][j+2] = new RelativeHardwareControlBinding(handler.knobs[j+2], bank.getItemAt(i).sendBank().getItemAt(j));
            for (int j = 0; j < NUM_KNOBS; j++)
                bindings[i][j].setIsActive(false);
        }
    }

    void stop() {
        if (isActivated) {
            if (cursor.isStopped().get() || cursor.isQueuedForStop().get())
                for (Track track : getBankItems()) track.returnToArrangement();
            else
                for (Track track : getBankItems()) track.stop();
        } else {
            if (cursor.isStopped().get() || cursor.isQueuedForStop().get()) cursor.returnToArrangement();
            else cursor.stop();
        }
    }
    void scrollCrossfade() {
        String id = handler.cycleEnum(cursor.crossFadeMode(), false);
        if (isActivated) for (Track bankItem : getBankItems()) bankItem.crossFadeMode().set(id);
    }
    void toggleGroupExpanded() {
        cursor.isGroupExpanded().toggle();
    }
    void moveUp() {
        if (hiPos < bank.itemCount().get() - 1) {
            bank.getItemAt(loPos).beforeTrackInsertionPoint().moveTracks(bank.getItemAt(hiPos + 1));
            super.moveUp();
        }
    }
    void moveDown() {
        if (loPos > 0) {
            bank.getItemAt(hiPos).afterTrackInsertionPoint().moveTracks(bank.getItemAt(loPos - 1));
            super.moveDown();
        }
    }
    @Override
    public void activate() {
        for (int i = loPos; i <= hiPos; i++)
            for (RelativeHardwareControlBinding b : bindings[i]) b.setIsActive(true);
        super.activate();
    }
    @Override
    void deactivate() {
        for (int i = loPos; i <= hiPos; i++)
            for (RelativeHardwareControlBinding binding : bindings[i]) binding.setIsActive(false);
        super.deactivate();
    }
    private final RelativeHardwareControlBinding[][] bindings = new RelativeHardwareControlBinding[bank.getSizeOfBank()][NUM_KNOBS];
}