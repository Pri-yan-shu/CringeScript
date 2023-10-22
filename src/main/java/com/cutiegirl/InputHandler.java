package com.cutiegirl;

import com.bitwig.extension.controller.api.*;
import com.bitwig.extensions.framework.AbsoluteHardwareControlBinding;
import com.bitwig.extensions.framework.RelativeHardwareControlBinding;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;

import static com.cutiegirl.CringeScriptExtension.*;
import static com.cutiegirl.KeyHandler.*;
import static com.cutiegirl.LAYER.*;
import static com.cutiegirl.GESTURE.*;

final class InputHandler {

    InputHandler (CringeScriptExtension driver) {
        this.driver = driver;
        this.host           = driver.getHost();
        this.knobs          = driver.knobs;
        this.sliders        = driver.sliders;

        Msg.setHandler(this);

        ACTIVE_LAYER        = DEVICE;
        activeKnobBinding   = DEVICE;
        activeSliderBinding = 0;
        Arrays.fill(pressStatus, false);
        application         = host.createApplication();

        new ControllerHandler(driver, this);

        keyHandler = new KeyHandler(host, this);
        keyPiano = new KeyPiano(driver, keyHandler);

        knobBindings[TRACK.id]          = new RelativeHardwareControlBinding[NUM_KNOBS];
        knobBindings[TRACK_PARAM.id]    = new RelativeHardwareControlBinding[NUM_KNOBS];
        knobBindings[DEVICE.id]         = new RelativeHardwareControlBinding[NUM_KNOBS];
        knobBindings[CLIP.id]           = new RelativeHardwareControlBinding[NUM_KNOBS];
        knobBindings[CLIP_2.id]         = new RelativeHardwareControlBinding[NUM_KNOBS];
        knobBindings[ARP.id]            = new RelativeHardwareControlBinding[NUM_KNOBS];
        knobBindings[DEVICE_LAYER.id]   = new RelativeHardwareControlBinding[NUM_KNOBS];
        knobBindings[LAYER_CTX.id]      = new RelativeHardwareControlBinding[NUM_KNOBS];
        knobBindings[CURVE.id]          = new RelativeHardwareControlBinding[NUM_KNOBS];
        knobBindings[TRANSPORT.id]      = new RelativeHardwareControlBinding[NUM_KNOBS];
        knobBindings[NOTE_CTX.id]       = new RelativeHardwareControlBinding[NUM_KNOBS];

        trackBank = host.createTrackBank(BANK_BUTTONS,8,8);
        cursorTrack = host.createCursorTrack(8,8);
        trackBank.followCursorTrack(cursorTrack);

        initCommands();

        for (int i = 0; i < 10; i++) keyPiano.decOctave();
        for (int i = 0; i < 5; i++) keyPiano.incOctave();

    }

    private void initCommands() {
        initMeta();
        initTrack();
        initDevice();
        initClip();
        initBrowser();
        initArp();
        initTransport();
        initSelection();
    }
    private void initMeta() {
        final Action focusTrackHeader = application.getAction("focus_track_header_area");
        final Action focusDevicePanel = application.getAction("focus_or_toggle_device_panel");
        final Action focusDetailEditor = application.getAction("focus_or_toggle_detail_editor");
        final Action focusClipLauncher = application.getAction("focus_or_toggle_clip_launcher");
        commands[META.id][0][ONN.id] = focusTrackHeader::invoke;
        commands[META.id][1][ONN.id] = focusDevicePanel::invoke;
        commands[META.id][2][ONN.id] = focusDetailEditor::invoke;
        commands[META.id][1][TAP.id] = commands[META.id][1][TAP.id] = focusClipLauncher::invoke;
        commands[META.id][2][TAP.id] = commands[META.id][2][TAP.id] = focusClipLauncher::invoke;
    }

    private void initTrack() {
        rootTrackGroup = host.getProject().getRootTrackGroup();

        final Action focusTrackHeader = application.getAction("focus_track_header_area");
        final TrackBank trackBank = InputHandler.trackBank;
        final CursorTrack cursorTrack = InputHandler.cursorTrack;
        final CursorRemoteControlsPage trackControls = cursorTrack.createCursorRemoteControlsPage(NUM_KNOBS);
        final CursorRemoteControlsPage projectControls = rootTrackGroup.createCursorRemoteControlsPage(NUM_KNOBS);

        trackBank.itemCount().markInterested();

        for (int i = 0; i < NUM_KNOBS; i++) {
            knobBindings[TRACK.id][i] = new RelativeHardwareControlBinding(knobs[i], trackControls.getParameter(i));
            knobBindings[TRACK_PARAM.id][i] = new RelativeHardwareControlBinding(knobs[i], projectControls.getParameter(i));
        }

        for (int i = 0; i < 4; i++) {
            sliderBindings[buttonStatus(-1, false)][i]  = new AbsoluteHardwareControlBinding(sliders[i], trackBank.getItemAt(i).volume());
            sliderBindings[buttonStatus(-1, true)][i]   = new AbsoluteHardwareControlBinding(sliders[i], trackBank.getItemAt(i+4).volume());
            sliderBindings[buttonStatus(9, false)][i]   = new AbsoluteHardwareControlBinding(sliders[i], trackBank.getItemAt(i+8).volume());
            sliderBindings[buttonStatus(9, true)][i]    = new AbsoluteHardwareControlBinding(sliders[i], trackBank.getItemAt(i+12).volume());
            sliderBindings[buttonStatus(0, false)][i]   = new AbsoluteHardwareControlBinding(sliders[i], trackBank.getItemAt(i).pan());
            sliderBindings[buttonStatus(0, true)][i]    = new AbsoluteHardwareControlBinding(sliders[i], trackBank.getItemAt(i+4).pan());
            sliderBindings[buttonStatus(10, false)][i]  = new AbsoluteHardwareControlBinding(sliders[i], trackBank.getItemAt(i+8).pan());
            sliderBindings[buttonStatus(10, true)][i]   = new AbsoluteHardwareControlBinding(sliders[i], trackBank.getItemAt(i+12).pan());

            for (int j = 0; j < NUM_SENDS; j++) {
                sliderBindings[buttonStatus(j+1, false)][i] =
                        new AbsoluteHardwareControlBinding(sliders[i], trackBank.getItemAt(i).sendBank().getItemAt(j));
                sliderBindings[buttonStatus(j+1,  true)][i] =
                        new AbsoluteHardwareControlBinding(sliders[i], trackBank.getItemAt(i+4).sendBank().getItemAt(j));
                sliderBindings[buttonStatus(j+11, false)][i] =
                        new AbsoluteHardwareControlBinding(sliders[i], trackBank.getItemAt(i+8).sendBank().getItemAt(j));
                sliderBindings[buttonStatus(j+11, true)][i] =
                        new AbsoluteHardwareControlBinding(sliders[i], trackBank.getItemAt(i+12).sendBank().getItemAt(j));
            }
        }

        for (int i = 0; i < BANK_BUTTONS; i++) {
            final int I = i;
            commands[TRACK.id][I][ONN.id] = () -> {
//                if (trackBank.itemCount().get() == 0) return;
                int index = Math.min(I, trackBank.itemCount().get() - 1);
                cursorTrack.selectChannel(trackBank.getItemAt(index));
                selectionTrack.build(index);
//                focusTrackHeader.invoke();
            };
            commands[TRACK.id][I][RET.id] = () -> {
                if (selectionTrack.isBuilding()) cursorTrack.selectChannel(trackBank.getItemAt(currentMsg.history));
                selectionTrack.retreat(Math.min(I, trackBank.itemCount().get() - 1));
            };
            commands[TRACK.id][I][TAP.id] = () -> selectionTrack.tap(Math.min(I, trackBank.itemCount().get() - 1));
        }

        commands[TRACK.id][16][ONN.id] = () -> selectionTrack.deactivate();
        commands[TRACK.id][18][ONN.id] = keyHandler::init;
        encoderCommands[TRACK.id][0][ONN.id] = ()-> selectionTrack.moveUp();
        encoderCommands[TRACK.id][0][OFF.id] = ()-> selectionTrack.moveDown();
        encoderCommands[TRACK.id][ctrlOffset][ONN.id] = ()-> application.navigateIntoTrackGroup(cursorTrack);
        encoderCommands[TRACK.id][ctrlOffset][OFF.id] = application::navigateToParentTrackGroup;
        encoderCommands[TRACK.id][buttonStatus(-1, true)][ONN.id] = () -> selectionTrack.moveUp();
        encoderCommands[TRACK.id][buttonStatus(-1, true)][OFF.id] = () -> selectionTrack.moveDown();
    }

    private void initDevice() {
        cursorDevice = cursorTrack.createCursorDevice();
        layerBank = cursorDevice.createLayerBank(8);
        cursorDeviceSlot = (CursorDeviceSlot) cursorDevice.getCursorSlot();
        deviceBank = cursorTrack.createDeviceBank(BANK_BUTTONS);
        CursorRemoteControlsPage cursorRemoteControlsPage = cursorDevice.createCursorRemoteControlsPage(8);
        CursorDeviceLayer cursorLayer = cursorDevice.createCursorLayer();
        drumPadBank = cursorDevice.createDrumPadBank(16);

        deviceBank.itemCount().markInterested();
        drumPadBank.exists().markInterested();
        cursorDevice.exists().markInterested();
        cursorDevice.position().markInterested();
        cursorDevice.hasSlots().markInterested();
        cursorDevice.isNested().markInterested();
        cursorDevice.slotNames().markInterested();
        cursorDeviceSlot.exists().markInterested();

        for (int i = 0; i < NUM_KNOBS; i++) {
            knobBindings[DEVICE.id][i] = new RelativeHardwareControlBinding(knobs[i], cursorRemoteControlsPage.getParameter(i));
            knobBindings[DEVICE_LAYER.id][i] = new RelativeHardwareControlBinding(knobs[i], layerBank.getItemAt(i).volume());

            knobBindings[DEVICE.id][i].setIsActive(true);
        }
        knobBindings[LAYER_CTX.id][0] = new RelativeHardwareControlBinding(knobs[0], cursorLayer.volume());
        knobBindings[LAYER_CTX.id][1] = new RelativeHardwareControlBinding(knobs[1], cursorLayer.pan());
        for (int i = 0; i < 6; i++)
            knobBindings[LAYER_CTX.id][i + 2] = new RelativeHardwareControlBinding(knobs[i + 2], cursorLayer.sendBank().getItemAt(i));

        for (int i = 0; i < BANK_BUTTONS; i++) {
            final int I = i;
            commands[DEVICE.id][I][ONN.id] = () -> {
                if (deviceBank.itemCount().get() == 0) {
                    cursorDevice.selectFirstInChannel(cursorTrack);
                    if (deviceBank.itemCount().get() == 0) return;
                }
                int index = Math.min(I, deviceBank.itemCount().get() - 1);
                cursorDevice.selectDevice(deviceBank.getItemAt(index));
                application.focusPanelBelow();
                selectionDevice.build(index);
            };
            commands[DEVICE.id][I][RET.id] = () -> {
                cursorDevice.selectDevice(deviceBank.getItemAt(currentMsg.history));
                selectionDevice.retreat(I);
            };
            commands[DEVICE.id][I][TAP.id] = () -> selectionDevice.tap(I);
            commands[DEVICE_PARAM.id][I][ONN.id] = () -> {
                cursorRemoteControlsPage.selectedPageIndex().set(I);
                drumPadBank.getItemAt(I).selectInEditor();
            };
            commands[DEVICE_PARAM.id][I][RET.id] = () -> cursorRemoteControlsPage.selectedPageIndex().set(currentMsg.history);

            commands[DEVICE_LAYER.id][I][ONN.id] = () -> cursorLayer.selectChannel(layerBank.getItemAt(I));
            commands[DEVICE_LAYER.id][I][RET.id] = () -> cursorLayer.selectChannel(layerBank.getItemAt(currentMsg.history));
        }
        commands[LAYER_CTX.id][0][ONN.id] = commands[DEVICE_LAYER.id][8][ONN.id] = cursorLayer.isActivated()::toggle;
        commands[LAYER_CTX.id][1][ONN.id] = commands[DEVICE_LAYER.id][9][ONN.id] = cursorLayer.solo()::toggle;
        commands[LAYER_CTX.id][2][ONN.id] = commands[DEVICE_LAYER.id][10][ONN.id] = cursorLayer.mute()::toggle;
        commands[LAYER_CTX.id][3][ONN.id] = commands[DEVICE_LAYER.id][11][ONN.id] = cursorLayer::duplicate;
        commands[LAYER_CTX.id][4][ONN.id] = commands[DEVICE_LAYER.id][12][ONN.id] = cursorLayer::deleteObject;

        encoderCommands[DEVICE.id][0][ONN.id] = () -> cursorRemoteControlsPage.selectNextPage(true);
        encoderCommands[DEVICE.id][0][OFF.id] = () -> cursorRemoteControlsPage.selectPreviousPage(true);
        encoderCommands[DEVICE.id][ctrlOffset][ONN.id] = () -> selectionDevice.cycleSlot(false);
        encoderCommands[DEVICE.id][ctrlOffset][OFF.id] = cursorDevice::selectParent;
        encoderCommands[DEVICE.id][11][ONN.id] = () -> selectionDevice.cycleSlot(true);
        encoderCommands[DEVICE.id][buttonStatus(-1, true)][ONN.id] = () -> selectionDevice.moveUp();
        encoderCommands[DEVICE.id][buttonStatus(-1, true)][OFF.id] = () -> selectionDevice.moveDown();
        encoderCommands[DEVICE.id][40][ONN.id] = () -> drumPadBank.scrollForwards();
        encoderCommands[DEVICE.id][40][OFF.id] = () -> drumPadBank.scrollBackwards();

        encoderCommands[DEVICE_LAYER.id][0][ONN.id] = cursorLayer::selectNext;
        encoderCommands[DEVICE_LAYER.id][0][OFF.id] = cursorLayer::selectNext;
    }

    private void initClip() {
        cursorClip = cursorTrack.createLauncherCursorClip(BANK_BUTTONS,12);
        slotBank = cursorTrack.clipLauncherSlotBank();

        noteComboBuilder = new NoteComboBuilder();

        Combos combos = new Combos(this);
        Curves curves = new Curves(this);
        LoopCombo loopCombo = new LoopCombo(cursorClip);

        SceneBank sceneBank = trackBank.sceneBank();

        displayEnum(cursorClip.launchMode(), "Launch Mode");
        displayEnum(cursorClip.launchQuantization(), "Launch Quantization");

        knobBindings[CLIP_2.id][0] = new RelativeHardwareControlBinding(knobs[0], cursorClip.getPlayStart().beatStepper());
        knobBindings[CLIP_2.id][1] = new RelativeHardwareControlBinding(knobs[1], cursorClip.getPlayStop().beatStepper());
        knobBindings[CLIP_2.id][2] = new RelativeHardwareControlBinding(knobs[2], cursorClip.getLoopStart().beatStepper());
        knobBindings[CLIP_2.id][3] = new RelativeHardwareControlBinding(knobs[3], cursorClip.getLoopLength().beatStepper());
        knobBindings[CLIP_2.id][4] = new RelativeHardwareControlBinding(knobs[4], cursorClip.getAccent());
        knobBindings[CLIP_2.id][5] = new RelativeHardwareControlBinding(knobs[5], host.createRelativeHardwareControlStepTarget(
                host.createAction(() -> cursorClip.transpose(1), () -> ""),
                host.createAction(() -> cursorClip.transpose(-1), () -> "")));
        knobBindings[CLIP_2.id][6] = new RelativeHardwareControlBinding(knobs[6], host.createRelativeHardwareControlStepTarget(
                host.createAction(() -> cycleEnum(cursorClip.launchQuantization(), false), () -> ""),
                host.createAction(() -> cycleEnum(cursorClip.launchQuantization(), true), () -> "")));

        for (int i = 0; i < NUM_KNOBS; i++) {
            final int I = i;
            ClipLauncherSlot s = slotBank.getItemAt(I);
            s.isPlaying().markInterested();
            s.isSelected().markInterested();
            s.hasContent().markInterested();

            commands[CLIP_2.id][I][ONN.id] = () -> {
                if (noteComboBuilder.isBuilding()) return;
                ClipLauncherSlot slot = slotBank.getItemAt(I);

                if (sceneLauncher) {
                    Scene scene = sceneBank.getItemAt(I);
                    if (ctrlPressed()) {
                        if (altPressed()) scene.launchAlt();
                        else scene.launch();
                    } else scene.selectInEditor();
                    sceneIndex = I;
                } else if (!slot.hasContent().get()) {
                    if (modifierStatus() == 4) slot.record();
                    else slot.createEmptyClip(4);
                } else if (slot.isSelected().get()) {
                    if (slot.isPlaying().get()) slotBank.stop();
                    else if (modifierStatus() == 4) slot.launchAlt();
                    else slot.launch();
                } else slot.select();
            };

            commands[CLIP_2.id][I][TAP.id] = commands[CLIP_2.id][I][RET.id] = () -> {
                ClipLauncherSlot slot = slotBank.getItemAt(I);
                if (sceneLauncher) {
                    Scene scene = sceneBank.getItemAt(sceneIndex);
                    if (altPressed()) scene.launchReleaseAlt();
                    else scene.launchRelease();
                } else {
                    if (modifierStatus() == 4) slot.launchReleaseAlt();
                    else slot.launchRelease();
                }
            };

            commands[CHORDS.id][I][ONN.id] = () -> keyPiano.setChord(Math.min(I, 6));
            commands[CHORDS.id][I + 8][ONN.id] = () -> keyPiano.toggleChordModifier(I);


            commands[CURVE.id][I][ONN.id] = () -> {
                if (noteComboBuilder.isBuilding()) combos.setActiveCombo(I);
                else if (combos.activeCombo() != null) combos.activeCombo().setNotes();
            };
            commands[CURVE.id][I+8][ONN.id] = () -> curves.setActiveCurve(I);
            commands[CURVE_CTX.id][I][ONN.id] = () -> noteComboBuilder.targetParam = Math.min(I, 5);
            commands[NOTE_CTX.id][I][ONN.id] = () -> combos.activeCombo().setRecurrenceMask(I);
            commands[NOTE_CTX.id][I+8][ONN.id] = () -> combos.activeCombo().toggleParam(I);

            commands[SCALES.id][I][ONN.id] = () -> {
                keyPiano.setActiveScale(I);
                keyPiano.setTranspose(keyPiano.getRawNotes()[0]);
            };

            knobBindings[CLIP.id][I] = new RelativeHardwareControlBinding(knobs[I], host.createRelativeHardwareControlStepTarget(
                    host.createAction(() -> curves.activeCurve().adjustPoint(I, 2), () -> ""),
                    host.createAction(() -> curves.activeCurve().adjustPoint(I, -2), () -> "")));
            knobBindings[CURVE.id][I] = new RelativeHardwareControlBinding(knobs[I], host.createRelativeHardwareControlStepTarget(
                    host.createAction(() -> curves.activeCurve().adjustPoint(I, 2), () -> ""),
                    host.createAction(() -> curves.activeCurve().adjustPoint(I, -2), () -> "")));
            knobBindings[NOTE_CTX.id][I] = new RelativeHardwareControlBinding(knobs[I], host.createRelativeHardwareControlAdjustmentTarget(
                    value -> combos.activeCombo().adjustParam(I, value)));
        }
        commands[NOTE_CTX.id][13][ONN.id] = () -> curves.activeCurve().trim();
        commands[NOTE_CTX.id][14][ONN.id] = () -> curves.activeCurve().clear();
        commands[NOTE_CTX.id][15][ONN.id] = null;

        for (int i = 0; i < BANK_BUTTONS; i++) {
            final int I = i;
            commands[NOTES.id][I][ONN.id] = () -> {
                keyPiano.pressNote(I);
                if (drumPadBank.exists().get()) {
                    drumPadBank.getItemAt(I).selectInEditor();
                    drumPadInsertionPoint = drumPadBank.getItemAt(I).insertionPoint();
                    cursorDevice.selectLastInChannel(drumPadBank.getItemAt(I));
                }
            };
            commands[NOTES.id][I][OFF.id] = () -> keyPiano.releaseNote(I);
            commands[NOTES.id][I][TAP.id] = () -> keyPiano.tapNote(I);
            commands[NOTES.id][I][RET.id] = () -> keyPiano.retreatNote(I);
            commands[CLIP.id][I][ONN.id] = () -> {
                if (shiftPressed()) loopCombo.build(I);
                else noteComboBuilder.build(I);
            };

            commands[CLIP.id][I][TAP.id] = () -> {
                if (loopCombo.isBuilding()) loopCombo.tap(0);
                else {
                    noteComboBuilder.tap(I);
                    if (!noteComboBuilder.isBuilding()) {
                        NoteCombo noteCombo = noteComboBuilder.noteCombo;
                        noteCombo.setNotes();
                        if (noteComboBuilder.targetSlot != -1) combos.add(noteCombo, noteComboBuilder.targetSlot);
                    }
                }
            };

            commands[CLIP.id][I][RET.id] = () -> {
                if (loopCombo.isBuilding()) loopCombo.retreat(0);
                else {
                    noteComboBuilder.retreat(I);
                    if (!noteComboBuilder.isBuilding()) {
                        NoteCombo noteCombo = noteComboBuilder.noteCombo;
                        noteCombo.setNotes();
                        if (noteComboBuilder.targetSlot != -1) combos.add(noteCombo, noteComboBuilder.targetSlot);
                        if (noteComboBuilder.targetParam != -1) noteCombo.applyCurve(curves.activeCurve(), noteComboBuilder.targetParam);
                    }
                }
            };
            commands[LAUNCHER.id][I][ONN.id] = () -> {
                cursorTrack.selectChannel(trackBank.getItemAt(I));
                if (altPressed()) slotBank.getItemAt(sceneIndex).launchAlt();
                else slotBank.getItemAt(sceneIndex).launch();
            };

            commands[LAUNCHER.id][I][TAP.id] = commands[LAUNCHER.id][I][RET.id] = () -> {
                if (altPressed()) slotBank.getItemAt(I).launchReleaseAlt();
                else slotBank.getItemAt(I).launchRelease();
            };

            commands[LOOP.id][I][ONN.id] = () -> loopCombo.build(I);
            commands[LOOP.id][I][TAP.id] = () -> loopCombo.tap(0);
            commands[LOOP.id][I][RET.id] = () -> loopCombo.retreat(0);

            commands[CHORDS_NOTES.id][I][ONN.id] = () -> {
                keyPiano.setChord(I);
                keyPiano.playChord(I);
            };
            commands[CHORDS_NOTES.id][I][OFF.id] = () -> keyPiano.stopChord(I);
        }

        commands[CLIP_CTX.id][0][ONN.id] = cursorClip.getShuffle()::toggle;
        commands[CLIP_CTX.id][1][ONN.id] = cursorClip.isLoopEnabled()::toggle;
        commands[CLIP_CTX.id][2][ONN.id] = cursorClip::duplicate;
        commands[CLIP_CTX.id][3][ONN.id] = cursorClip::duplicateContent;
        commands[CLIP_CTX.id][4][ONN.id] = cursorClip.useLoopStartAsQuantizationReference()::toggle;
        commands[CLIP_CTX.id][5][ONN.id] = () -> cycleEnum(cursorClip.launchMode(), true);
        commands[CLIP_CTX.id][6][ONN.id] = () -> cursorClip.quantize(1);
        commands[CLIP_CTX.id][7][ONN.id] = cursorClip::clearSteps;

        encoderCommands[CLIP.id][0][ONN.id] = cursorClip::scrollStepsPageForward;
        encoderCommands[CLIP.id][0][OFF.id] = cursorClip::scrollStepsPageBackwards;
        encoderCommands[CLIP.id][40][ONN.id] = keyPiano::togglePiano;
        encoderCommands[CLIP.id][40][OFF.id] = () -> {
            sceneLauncher = !sceneLauncher;
            host.showPopupNotification("Scene Launcher: "+sceneLauncher);
        };
        encoderCommands[NOTES.id][40][ONN.id] = encoderCommands[CHORDS_NOTES.id][40][ONN.id] = keyPiano::toggleChordMode;

        encoderCommands[CURVE.id][0][ONN.id] = () -> curves.activeCurve().scrollPageForward();
        encoderCommands[CURVE.id][0][OFF.id] = () -> curves.activeCurve().scrollPageBackward();
        encoderCommands[CURVE.id][buttonStatus(-1, true)][ONN.id] = () -> curves.activeCurve().halvePageSize();
        encoderCommands[CURVE.id][buttonStatus(-1, true)][OFF.id] = () -> curves.activeCurve().doublePageSize();

        encoderCommands[NOTE_CTX.id][0][ONN.id] = () -> combos.activeCombo().cycleOccurrence();
        encoderCommands[NOTE_CTX.id][0][ONN.id] = () -> combos.activeCombo().cycleOccurrenceReverse();

        encoderCommands[NOTES.id][0][ONN.id] = encoderCommands[CHORDS_NOTES.id][0][ONN.id] = keyPiano::incOctave;
        encoderCommands[NOTES.id][0][OFF.id] = encoderCommands[CHORDS_NOTES.id][0][OFF.id] = keyPiano::decOctave;

    }

    private void initBrowser() {
        final Action focusFileList = application.getAction("focus_file_list");

        PopupBrowser browser = host.createPopupBrowser();
        BrowserFilterItemBank smartCollections = browser.smartCollectionColumn().createItemBank(BANK_BUTTONS + 2);
        BrowserResultsItemBank resultsBank = browser.resultsColumn().createItemBank(BANK_BUTTONS);
        BrowserFilterItemBank locations = browser.locationColumn().createItemBank(BANK_BUTTONS + 2);

        browser.exists().markInterested();
        smartCollections.getItemAt(1).isSelected().markInterested();   //Favourites column

        commands[META.id][3][ONN.id] = () -> {
            if (browser.exists().get()) browser.cancel();
            else {
                if (cursorDevice.exists().get()) {
                    switch (modifierStatus()) {
                        case 1 -> cursorDevice.beforeDeviceInsertionPoint().browse();       //Shift
                        case 2 -> {                                                         //Ctrl
                            if (!cursorDeviceSlot.exists().get() && cursorDevice.hasSlots().get())
                                cursorDeviceSlot.selectSlot(cursorDevice.slotNames().get(0));
                            cursorDeviceSlot.endOfDeviceChainInsertionPoint().browse();
                        }
                        case 3 -> {                                                         //Ctrl Shift
                            cursorDevice.replaceDeviceInsertionPoint().browse();
                            browser.selectedContentTypeIndex().set(3);
                        }
                        case 4 -> cursorDevice.replaceDeviceInsertionPoint().browse();      //Alt
                        case 5 -> {
                            if (drumPadInsertionPoint != null) drumPadInsertionPoint.browse();
                        }
                        default -> cursorDevice.afterDeviceInsertionPoint().browse();
                    }
                } else {
                    if (modifierStatus() == 3) {
                        cursorDevice.replaceDeviceInsertionPoint().browse();
                        browser.selectedContentTypeIndex().set(3);
                    } else cursorTrack.endOfDeviceChainInsertionPoint().browse();
                }

                browser.deviceTypeColumn().getWildcardItem().isSelected().set(true);
                browser.shouldAudition().set(false);
                smartCollections.getItemAt(1).isSelected().set(true);
                host.scheduleTask(focusFileList::invoke, 250);
//                host.scheduleTask(focusFileList::invoke, 500);
//                focusFileList.invoke();
            }
        };
        commands[META.id][3][TAP.id] = commands[META.id][3][RET.id] = () -> {
            browser.commit();
            if (modifierStatus() == 1) cursorDevice.isEnabled().set(false);
        };

        for (int i = 0; i < BANK_BUTTONS; i++) {
            resultsBank.getItemAt(i).isSelected().markInterested();

            final int I = i;
            commands[BROWSER.id][i][ONN.id] = () -> {
                smartCollections.getItemAt(I + 2).isSelected().toggle();
                host.scheduleTask(focusFileList::invoke, 250);
//                focusFileList.invoke();
            };
            commands[BROWSER_CTX.id][i][ONN.id] = locations.getItemAt(i + 2).isSelected()::toggle;
            commands[BROWSER_2.id][i][ONN.id] = () -> {
                if (resultsBank.getItemAt(I).isSelected().get()) {
                    browser.selectedContentTypeIndex().set(1);
                    host.scheduleTask(() -> resultsBank.getItemAt(0).isSelected().set(true), 100);
                } else resultsBank.getItemAt(I).isSelected().set(true);
            };
        }
        encoderCommands[BROWSER.id][0][ONN.id] = encoderCommands[BROWSER_2.id][0][ONN.id] = () -> {
            browser.selectedContentTypeIndex().inc(1);
            browser.deviceTypeColumn().getWildcardItem().isSelected().set(true);
            browser.deviceColumn().getWildcardItem().isSelected().set(true);
        };
        encoderCommands[BROWSER.id][0][OFF.id] = encoderCommands[BROWSER_2.id][0][OFF.id] = () -> browser.selectedContentTypeIndex().inc(-1);
    }

    private void initArp() {
        Arpeggiator arp = driver.noteInput.arpeggiator();
        displayEnum(arp.mode(), "Arp Mode");

        arp.isFreeRunning().addValueObserver(val -> host.showPopupNotification("Arp Free: "+val));
        arp.shuffle().addValueObserver(val -> host.showPopupNotification("Arp Shuffle: "+val));
        arp.enableOverlappingNotes().addValueObserver(val -> host.showPopupNotification("Arp  Overlapping Notes: "+val));
        arp.usePressureToVelocity().addValueObserver(val -> host.showPopupNotification("Arp Pressure to Velocity: "+val));
        arp.terminateNotesImmediately().addValueObserver(val -> host.showPopupNotification("Arp Terminate Notes Immediately: "+val));

        arp.rate().addValueObserver(val -> host.showPopupNotification("Arp Rate: "+arpRate+(arpTriplets ? " T" : "")));
        arp.octaves().addValueObserver(val -> host.showPopupNotification("Arp Octave: "+val));
        arp.humanize().addValueObserver(val -> host.showPopupNotification(String.format("Arp Humanize: %.2f", val)));
        arp.gateLength().addValueObserver(val -> host.showPopupNotification(String.format("Arp Gate: %.2f", val)));

        commands[META.id][5][ONN.id] = () -> arp.isEnabled().set(true);
        commands[META.id][5][TAP.id] = () -> arp.isEnabled().set(false);


        knobBindings[ARP.id][0] = new RelativeHardwareControlBinding(knobs[0], host.createRelativeHardwareControlStepTarget(
                host.createAction(() -> {
                    if (arpRate < 6 ) arp.rate().set(scrollArpRate(false));
                }, () -> ""),
                host.createAction(() -> {
                    if (arpRate > -2) arp.rate().set(scrollArpRate(true));
                }, () -> "")));
        knobBindings[ARP.id][1] = new RelativeHardwareControlBinding(knobs[1], host.createRelativeHardwareControlStepTarget(
                host.createAction(() -> cycleEnum(arp.mode(), false), () -> ""),
                host.createAction(() -> cycleEnum(arp.mode(), true), () -> "")));
        knobBindings[ARP.id][2] = new RelativeHardwareControlBinding(knobs[2], arp.octaves());
        knobBindings[ARP.id][3] = new RelativeHardwareControlBinding(knobs[3], host.createRelativeHardwareControlAdjustmentTarget(delta -> {
            double finalVal = arp.gateLength().get() + delta;
            if (finalVal > 0 && finalVal < 2) arp.gateLength().set(finalVal);
        }));
        knobBindings[ARP.id][4] = new RelativeHardwareControlBinding(knobs[4], host.createRelativeHardwareControlAdjustmentTarget(delta -> {
            double finalVal = arp.humanize().get() + delta;
            if (finalVal > 0 && finalVal < 1) arp.humanize().set(finalVal);
        }));

        commands[ARP.id][0][ONN.id] = commands[ARP.id][0][RET.id] = arp.isFreeRunning()::toggle;
        commands[ARP.id][1][ONN.id] = commands[ARP.id][1][RET.id] = arp.shuffle()::toggle;
        commands[ARP.id][2][ONN.id] = commands[ARP.id][2][RET.id] = arp.enableOverlappingNotes()::toggle;
        commands[ARP.id][3][ONN.id] = commands[ARP.id][3][RET.id] = arp.usePressureToVelocity()::toggle;
        commands[ARP.id][4][ONN.id] = commands[ARP.id][4][RET.id] = arp.terminateNotesImmediately()::toggle;
        commands[ARP.id][5][ONN.id] = commands[ARP.id][4][RET.id] = arp::releaseNotes;
    }

    private void initTransport() {
        Transport transport = host.createTransport();
        Groove groove = host.createGroove();

        transport.timeSignature().markInterested();
        transport.timeSignature().numerator().markInterested();
        transport.timeSignature().denominator().markInterested();

        displayEnum(transport.defaultLaunchQuantization(), "Default Launch Quantization");
        displayEnum(transport.clipLauncherPostRecordingAction(), "Post Recording Action");
        displayEnum(transport.automationWriteMode(), "Automation Write Mode");
        displayEnum(transport.preRoll(), "Pre Roll");
        displayEnum(application.recordQuantizationGrid(), "Record Quantization Grid");

        knobBindings[TRANSPORT.id][0] = new RelativeHardwareControlBinding(knobs[0], transport.tempo());
        knobBindings[TRANSPORT.id][1] = new RelativeHardwareControlBinding(knobs[1], transport.crossfade());
        knobBindings[TRANSPORT.id][2] = new RelativeHardwareControlBinding(knobs[2], host.createMasterTrack(0).volume());
        knobBindings[TRANSPORT.id][3] = new RelativeHardwareControlBinding(knobs[3], groove.getAccentPhase());
        knobBindings[TRANSPORT.id][4] = new RelativeHardwareControlBinding(knobs[4], groove.getShuffleAmount());
        knobBindings[TRANSPORT.id][5] = new RelativeHardwareControlBinding(knobs[5], groove.getShuffleRate());
        knobBindings[TRANSPORT.id][6] = new RelativeHardwareControlBinding(knobs[6], groove.getAccentAmount());
        knobBindings[TRANSPORT.id][7] = new RelativeHardwareControlBinding(knobs[7], groove.getAccentRate());

        knobBindings[TRANSPORT.id][0].setSensitivity(.02);

        encoderCommands[TRANSPORT.id][0][ONN.id] = () -> transport.playStartPosition().inc(0.25);
        encoderCommands[TRANSPORT.id][0][OFF.id] = () -> transport.playStartPosition().inc(-.25);

        commands[TRANSPORT.id][0][ONN.id] = commands[TRANSPORT.id][0][RET.id] = transport.isPlaying()::toggle;
        encoderCommands[TRANSPORT.id][1][ONN.id] = () -> cycleEnum(transport.defaultLaunchQuantization(), false);
        encoderCommands[TRANSPORT.id][1][OFF.id] = () -> cycleEnum(transport.clipLauncherPostRecordingAction(), false);

        commands[TRANSPORT.id][1][ONN.id] = commands[TRANSPORT.id][0][RET.id] = transport::record;
        encoderCommands[TRANSPORT.id][2][ONN.id] = () -> cycleEnum(transport.automationWriteMode(), false);
        encoderCommands[TRANSPORT.id][2][OFF.id] = () -> cycleEnum(transport.preRoll(), false);
        encoderCommands[TRANSPORT.id][buttonStatus(1,true)][ONN.id] = () -> cycleEnum(application.recordQuantizationGrid(), false);
        encoderCommands[TRANSPORT.id][buttonStatus(1,true)][OFF.id] = () -> cycleEnum(transport.preRoll(), false);

        commands[TRANSPORT.id][2][ONN.id] = () -> groove.getEnabled().set(1);
        commands[TRANSPORT.id][2][RET.id] = () -> groove.getEnabled().set(0);
        commands[TRANSPORT.id][8][ONN.id] = transport::tapTempo;


        commands[TRANSPORT.id][3][ONN.id] = commands[TRANSPORT.id][0][RET.id] = transport.isPunchInEnabled()::toggle;
        commands[TRANSPORT.id][4][ONN.id] = commands[TRANSPORT.id][0][RET.id] = transport.isPunchOutEnabled()::toggle;

        commands[TRANSPORT.id][5][ONN.id] = commands[TRANSPORT.id][0][RET.id] = transport.isMetronomeEnabled()::toggle;
        encoderCommands[TRANSPORT.id][6][ONN.id] = () -> transport.timeSignature().numerator().inc(1);
        encoderCommands[TRANSPORT.id][6][OFF.id] = () -> transport.timeSignature().numerator().inc(-1);
        encoderCommands[TRANSPORT.id][buttonStatus(5,true)][ONN.id] = () -> transport.timeSignature().denominator().inc(1);
        encoderCommands[TRANSPORT.id][buttonStatus(5,true)][OFF.id] = () -> transport.timeSignature().denominator().inc(-1);

        commands[TRANSPORT.id][6][ONN.id] = commands[TRANSPORT.id][0][RET.id] = transport.isArrangerLoopEnabled()::toggle;
        encoderCommands[TRANSPORT.id][7][ONN.id] = () -> transport.arrangerLoopStart().inc(0.25);
        encoderCommands[TRANSPORT.id][7][OFF.id] = () -> transport.arrangerLoopStart().inc(-.25);
        encoderCommands[TRANSPORT.id][buttonStatus(6,true)][ONN.id] = () -> transport.arrangerLoopDuration().inc(0.25);
        encoderCommands[TRANSPORT.id][buttonStatus(6,true)][OFF.id] = () -> transport.arrangerLoopDuration().inc(-.25);

        commands[TRANSPORT.id][7][ONN.id] = commands[TRANSPORT.id][0][RET.id] = transport::addCueMarkerAtPlaybackPosition;
        encoderCommands[TRANSPORT.id][8][ONN.id] = transport::jumpToNextCueMarker;
        encoderCommands[TRANSPORT.id][8][OFF.id] = transport::jumpToPreviousCueMarker;

    }

    private void initSelection() {
        selectionTrack = new SelectionTrack(this, trackBank);
        selectionDevice = new SelectionDevice(this, deviceBank);

        encoderCommands[SEL_TRACK.id][0][ONN.id] = selectionTrack::moveUp;
        encoderCommands[SEL_TRACK.id][0][OFF.id] = selectionTrack::moveDown;
        encoderCommands[SEL_TRACK.id][SHIFT_OFFSET][ONN.id] = selectionTrack::moveUp;
        encoderCommands[SEL_TRACK.id][SHIFT_OFFSET][OFF.id] = selectionTrack::moveDown;

        encoderCommands[SEL_DEVICE.id][0][ONN.id] = selectionDevice::moveUp;
        encoderCommands[SEL_DEVICE.id][0][OFF.id] = selectionDevice::moveDown;
        encoderCommands[SEL_DEVICE.id][SHIFT_OFFSET][ONN.id] = selectionDevice::moveUp;
        encoderCommands[SEL_DEVICE.id][SHIFT_OFFSET][OFF.id] = selectionDevice::moveDown;

        commands[SEL_TRACK.id][0][ONN.id] = commands[SEL_TRACK.id][0][RET.id] = () -> selectionTrack.toggleProperty(0);
        commands[SEL_TRACK.id][1][ONN.id] = commands[SEL_TRACK.id][1][RET.id] = () -> selectionTrack.toggleProperty(1);
        commands[SEL_TRACK.id][2][ONN.id] = commands[SEL_TRACK.id][2][RET.id] = () -> selectionTrack.toggleProperty(2);
        commands[SEL_TRACK.id][3][ONN.id] = commands[SEL_TRACK.id][3][RET.id] = () -> selectionTrack.toggleProperty(3);
        commands[SEL_TRACK.id][4][ONN.id] = commands[SEL_TRACK.id][4][RET.id] = selectionTrack::stop;
        commands[SEL_TRACK.id][5][ONN.id] = commands[SEL_TRACK.id][5][RET.id] = selectionTrack::scrollCrossfade;
        commands[SEL_TRACK.id][6][ONN.id] = commands[SEL_TRACK.id][6][RET.id] = selectionTrack::toggleGroupExpanded;
        commands[SEL_TRACK.id][7][ONN.id] = selectionTrack::deleteObject;
        commands[SEL_TRACK.id][16][ONN.id] = selectionTrack::deactivate;

        commands[SEL_DEVICE.id][0][ONN.id] = commands[SEL_DEVICE.id][0][RET.id] = () -> selectionDevice.toggleProperty(0);
        commands[SEL_DEVICE.id][1][ONN.id] = commands[SEL_DEVICE.id][1][RET.id] = () -> selectionDevice.toggleProperty(1);
        commands[SEL_DEVICE.id][2][ONN.id] = commands[SEL_DEVICE.id][2][RET.id] = () -> selectionDevice.toggleProperty(2);
        commands[SEL_DEVICE.id][3][ONN.id] = commands[SEL_DEVICE.id][3][RET.id] = () -> selectionDevice.group(modifierStatus());
        commands[SEL_DEVICE.id][4][ONN.id] = commands[SEL_DEVICE.id][4][RET.id] = selectionDevice::addParallel;
        commands[SEL_DEVICE.id][5][ONN.id] = commands[SEL_DEVICE.id][5][RET.id] = selectionDevice::moveToNewTrack;
        commands[SEL_DEVICE.id][6][ONN.id] = selectionDevice::deleteObject;
        commands[SEL_DEVICE.id][16][ONN.id] = selectionDevice::deactivate;
    }
    void updateLayer(LAYER layer) {
        ACTIVE_LAYER = layer;
        layerButtonPressed = -1;
        histories[META.id] = layer.id - 1;
        updateKnobs();
    }
    private void updateKnobs() {
        if (knobBindings[ACTIVE_LAYER.id] == null) return;
        for (RelativeHardwareControlBinding binding : knobBindings[activeKnobBinding.id])
            if (binding != null) binding.setIsActive(false);
        for (RelativeHardwareControlBinding binding : knobBindings[ACTIVE_LAYER.id])
            if (binding != null) binding.setIsActive(true);
        activeKnobBinding = ACTIVE_LAYER;
    }
    void updateKnobSensitivity(boolean pressed){
        double sensitivity = pressed ? KNOB_SENSITIVITY/5 : KNOB_SENSITIVITY;
        for (RelativeHardwareKnob knob : knobs) {
            knob.setSensitivity(sensitivity);
        }
    }
    void updateSliders() {
        int target = buttonStatus();
        for (int i = 0; i < NUM_SLIDERS; i++) {
            sliderBindings[activeSliderBinding][i].setIsActive(false);
            sliderBindings[target][i].setIsActive(true);
        }
        activeSliderBinding = target;
    }
    void handleMsg(Msg msg) {
        host.println(msg.note+" "+msg.gesture.name()+" "+msg.layer.name()+" "+msg.history);
        currentMsg = msg;
        LAYER targetLayer = null;

        int note = msg.note;
        boolean isKey = (msg.group != null);

        switch (msg.gesture) {
            case ONN -> {
                pressEvent(msg, note);
                if (note < BANK_BUTTONS) {
                    if (!isKey) {
                        buttonPressed = layerButtonPressed = note;
                        updateSliders();
                    }
                        targetLayer = msg.layer.child;
                }
            }
            case OFF -> {
                pressStatus[msg.rawID] = false;
                handleGesture();
                if (!isKey && note < BANK_BUTTONS) {
                    buttonPressed = layerButtonPressed = -1;
                    updateSliders();
                }
            }
            case AFT -> pressEvent(msg, note);  //Add creation of new aft message
            case TAP -> {
                if (!msg.layer.isStatic && msg.note < BANK_BUTTONS) histories[msg.layer.id] = msg.history;
                if (!ACTIVE_LAYER.isStatic) targetLayer = msg.layer;
                if (isKey) {
                    keyHandler.updateGroup(msg.group);
                    keyHandler.retreatFocus();
                }
            }
            case RET -> {
                if (note < BANK_BUTTONS) histories[msg.layer.id] = msg.history;
                targetLayer = msg.layer;
                if (isKey) {
                    keyHandler.updateGroup(msg.group);
                    keyHandler.retreatFocus();
                }
            }
        }

        safeRun(commands[msg.layer.id][note][msg.gesture.id]);

        if (targetLayer != null && targetLayer != ACTIVE_LAYER) updateLayer(targetLayer);
    }
    void handleMetaMsg(Msg msg) {
        host.println(msg.note+" "+msg.gesture.name()+" "+msg.layer.name()+" "+msg.history);
        currentMsg = msg;
        LAYER targetLayer = null;

        int note = msg.note;
        boolean isKey = (msg.group != null);
        switch (msg.gesture) {
            case ONN -> {
                consumeTap = false;
                press.addLast(msg);
                pressStatus[msg.rawID] = true;
                targetLayer = LAYER.getLayer(note);
            }
            case OFF -> {
                pressStatus[msg.rawID] = false;
                handleGesture();
            }
            case TAP -> {
                if (!ACTIVE_LAYER.isStatic) {
                    targetLayer = LAYER.getLayer(msg.history);
                    if (isKey) {
                        keyHandler.retreatLayer(targetLayer);
                    }
                }
                if (isKey) keyHandler.updateGroup(GROUP.NONE);
            }
            case RET -> {
                targetLayer = LAYER.getLayer(msg.history);
                if (isKey) {
                    keyHandler.retreatLayer(targetLayer);
                    keyHandler.updateGroup(GROUP.NONE);
                }
            }
        }

        safeRun(commands[msg.layer.id][note][msg.gesture.id]);

        if (targetLayer != null && targetLayer != ACTIVE_LAYER) updateLayer(targetLayer);
    }
    void handleShiftMsg(boolean pressed) {
//        shiftPressed = pressed;
        updateSliders();
//        handleMsg(new Msg(16, pressed ? ONN : OFF, ACTIVE_LAYER, 48, null));
    }
    void handleEncoderMsg(Msg msg) {
        safeRun(encoderCommands[msg.layer.id][msg.note][msg.gesture.id]);
    }
    private void repeatLastMsg() {}
    private void pressEvent(Msg msg, int note) {
        consumeTap = false;
        press.addLast(msg);
        pressStatus[msg.rawID] = true;
        if (note < BANK_BUTTONS) histories[msg.layer.id] = note;
    }
    private void handleGesture() {
        Msg msg = fallback();
        while (msg != null) {
            if (msg.gesture == AFT && !msg.layer.aftToOn) return;
            Msg newMsg;
            if (consumeTap) {
                newMsg = new Msg(msg.note, RET, msg.layer, msg.group, msg.history);
            } else {
                consumeTap = true;
                newMsg = new Msg(msg.note, TAP, msg.layer, msg.group, msg.history);
            }
            if (msg.layer == META) handleMetaMsg(newMsg);
            else handleMsg(newMsg);

            msg = fallback();
        }
    }
    private Msg fallback() {
        if (press.isEmpty()) return null;
        else {
            int rawNote = press.peekLast().rawID;
            if (pressStatus[rawNote]) {
                consumeTap = true;
                return null;
            } else {
                return press.pollLast();
            }
        }
    }
    private void safeRun(Runnable runnable) {
        if (runnable != null) runnable.run();
    }
    void flush() {
        host.println("Press");
        StringBuilder str = new StringBuilder();
        while (!press.isEmpty()) str.append(press.pollFirst().note);
        host.println(str.toString());

        str = new StringBuilder();
        for (boolean status : pressStatus) str.append(status ? 1 : 0);
        host.println(str.toString());
    }
    private double scrollArpRate(boolean reverse) {
        arpTriplets = !arpTriplets;
        if (reverse) {
            if (!arpTriplets) arpRate--;
        } else {
            if (arpTriplets) arpRate++;
        }
        double rate = 1/Math.pow(2,arpRate);
        return rate * (arpTriplets ? .66 : 1);
    }
    String cycleEnum(SettableEnumValue value, boolean reverse) {
        EnumDefinition def = value.enumDefinition();
        int index = def.valueDefinitionFor(value.get()).getValueIndex();
        int targetIndex = cycle(index, def.getValueCount(), reverse);
        String id = def.valueDefinitionAt(targetIndex).getId();
        value.set(id);
        return id;
    }
    int cycle(int index, int length, boolean reverse) {
        return reverse ? (index > 0 ? index - 1 : length - 1) : (index < length - 1 ? index + 1 : 0);
    }
    void displayEnum(SettableEnumValue value, String name) {
        value.addValueObserver(val -> host.showPopupNotification(name+": "+val));
    }
    static int modifierStatus() {
        return (shiftPressed() ? 1 : 0) + (ctrlPressed() ? 2 : 0) + (altPressed() ? 4 : 0);
    }
    int buttonStatus() {
        return buttonPressed + 1 + (shiftPressed() ? SHIFT_OFFSET : 0);
    }
    int buttonStatus(int buttonPressed, boolean shiftPressed) {
        return buttonPressed + 1 + (shiftPressed ? SHIFT_OFFSET : 0);
    }

    private static final int BANK_BUTTONS = 16;
    private static final int NUM_BUTTONS = 19;
    private static final int NUM_LAYERS = LAYER.values().length;
    private static final int NUM_GESTURES = GESTURE.values().length;
    private static final int SHIFT_OFFSET = 20;

    private final CringeScriptExtension driver;
    static ControllerHost host;
    private final Application application;
    static private TrackBank trackBank;
    static private DeviceBank deviceBank;
    private ClipLauncherSlotBank slotBank;
    DeviceLayerBank layerBank;
    DrumPadBank drumPadBank;
    static CursorTrack cursorTrack;
    static CursorDevice cursorDevice;
    CursorDeviceSlot cursorDeviceSlot;
    PinnableCursorClip cursorClip;
    InsertionPoint drumPadInsertionPoint;
    Track rootTrackGroup;
    final RelativeHardwareKnob[] knobs;
    private final HardwareSlider[] sliders;

    static private SelectionTrack selectionTrack;
    static private SelectionDevice selectionDevice;
    static private KeyPiano keyPiano;
    static private KeyHandler keyHandler;
    static NoteComboBuilder noteComboBuilder;

    static private final Runnable[][][] commands = new Runnable[NUM_LAYERS][NUM_BUTTONS][NUM_GESTURES];
    //As HIT gesture deprecated, two layer HIT commands were accommodated in encoder commands, at index 40
    static private final Runnable[][][] encoderCommands = new Runnable[NUM_LAYERS][NUM_CONTEXT + 1][2];
    static final RelativeHardwareControlBinding[][] knobBindings = new RelativeHardwareControlBinding[NUM_LAYERS][];
    static private final AbsoluteHardwareControlBinding[][] sliderBindings = new AbsoluteHardwareControlBinding[NUM_CONTEXT][NUM_SLIDERS];
    static LAYER ACTIVE_LAYER;
    static private final Deque<Msg> press = new ArrayDeque<>();
    //Due to the need to keep presses of keys of same notes, press status is stored separately for each key
    //But the commands are stored per note, not per key
    static private Msg currentMsg;
    static private LAYER activeKnobBinding;
    static private final boolean[] pressStatus = new boolean[16*3 + 3 + 8];
    static final int[] histories = new int[NUM_LAYERS];
    static private boolean consumeTap;
    static private int activeSliderBinding;
    static private int layerButtonPressed;
    static private int arpRate = 1;
    static private int sceneIndex;
    static private boolean arpTriplets = false;
    static private boolean sceneLauncher = false;
    static int buttonPressed;
}