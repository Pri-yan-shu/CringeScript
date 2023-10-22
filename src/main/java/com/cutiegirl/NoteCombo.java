package com.cutiegirl;

import com.bitwig.extension.controller.api.*;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.IntStream;

import static com.cutiegirl.Combos.clip;
import static com.cutiegirl.Combos.host;

final class NoteCombo {
    NoteCombo(int y, int x, int len, int rep, int gap, int channel, int mode, int loop, int basis) {
        this.y = y;
        this.x = x;
        this.len = len;
        this.rep = rep;
        this.gap = gap;
        this.channel = channel;
        this.mode = mode;
        this.loop = loop;
        this.basis = basis;
        yOffset = 0;
    }
    void setNotes() {
        if (len == 0) len = 0.0625;
        clip.setStepSize(1/basis);

        int[] noteY = keyPiano.getRawNotes();
        int[] steps = getSteps();
        boolean[] octaves = keyPiano.getOctaves();
        boolean[] notePress = keyPiano.getNotePress();

        if (octaves[0]) {
            clip.scrollKeysPageDown();
            for (int i = 0; i < 7; i++)
                if (notePress[i])
                    for (int step : steps) clip.setStep(step, noteY[i] % 12, 127, len);
            clip.scrollKeysPageUp();
        }

        if (octaves[1])
            for (int j = 7; j < (1 + 1) * 7; j++)
                if (notePress[j])
                    for (int step : steps) clip.setStep(step, noteY[j] % 12, 127, len);
        if (octaves[2] || octaves[3]) clip.scrollKeysPageUp();
        if (octaves[2])
            for (int j = 14; j < 21; j++)
                if (notePress[j])
                    for (int step : steps) clip.setStep(step, noteY[j] % 12, 127, len);
        if (octaves[3]) {
            clip.scrollKeysPageUp();
            for (int j = 21; j < 28; j++)
                if (notePress[j])
                    for (int step : steps) clip.setStep(step, noteY[j] % 12, 127, len);
            clip.scrollKeysPageDown();
        }
        if (octaves[2] || octaves[3]) clip.scrollKeysPageDown();
    }
    void adjustParam(int param, double delta) {
        double finalVal = parameterValues[param] + delta;
        if (finalVal > 0 && finalVal < 1) {
            parameterValues[param] = finalVal;
            forEachStep(step -> setParam(step, param, finalVal));
        }
        host.showPopupNotification(paramNames[param]+" : "+parameterValues[param]);
    }
    void applyCurve(Curve curve, int param) {
        for (int i = 0; i < rep; i++) {
            int notePos = getStep(i);
            double noteVal = curve.getValue(notePos)/127.0;
            NoteStep step = clip.getStep(channel, notePos, y);
            setParam(step, param, noteVal);
        }
    }
    void cycleOccurrence() {
        forEachStep(step -> {
            int index = step.occurrence().ordinal();
            step.setOccurrence(occurrence[index < occurrenceLength - 1 ? index + 1 : 0]);
        });
    }
    void cycleOccurrenceReverse() {
        forEachStep(step -> {
            int index = step.occurrence().ordinal();
            step.setOccurrence(occurrence[index > 0 ? index - 1 : occurrenceLength - 1]);
        });
    }
    void setRecurrenceMask(int index) {
        recurrenceMask = recurrenceMask ^ (int) Math.pow(2, index);
        recurrenceLength = Math.max(recurrenceLength, index + 1);
        forEachStep(step -> step.setRecurrence(recurrenceLength, recurrenceMask));
    }
    void setParam(NoteStep step, int param, double val) {
        parameters.get(param).apply(step).accept(val);
    }
    void toggleParam(int param) {
        forEachStep(step -> booleanParameters.get(param).apply(step).accept(!booleanParameterStatus.get(param).apply(step)));
    }
    private void forEachStep(Consumer<NoteStep> task) {
        int[] Y = keyPiano.getRawNotes();
        for (int y : Y)
            IntStream.range(0, rep)
                .map(this::getStep)
                .mapToObj(notePos -> clip.getStep(channel, notePos, y))
                .forEach(task);
    }
    private int getStep(int i) {
        int gap = this.gap == 0 ? Math.max((int) len/4, 1) : this.gap;
        int stepPos = x + i * gap;
        int warp = stepPos % loop;
        return switch (mode) {
            case 1 -> {
                yOffset = Math.min(Math.max(stepPos/16, Math.abs(y - InputHandler.noteComboBuilder.getY())), 11-y);
                yield warp;
            }
            case 2 -> (stepPos/loop)%2 == 0 ? warp : loop - 1 - warp;
            case 3 -> {
                yOffset = Math.min(stepPos/16, 11-y);
                yield (stepPos/loop)%2 == 0 ? warp : loop - 1 - warp;
            }
            case 4 -> (x + i*i*gap)%loop;
            default -> warp;
        };
    }
    private int[] getSteps() {
        return IntStream.range(0, rep)
                .map(i -> (x + i*(this.gap == 0 ? Math.max((int) len/4, 1) : this.gap)) % loop)
                .toArray();
    }
    int x;
    int y;
    double len;
    int rep;
    int gap;
    int channel;
    int mode;
    int loop;
    double basis;
    private int recurrenceLength;
    private int recurrenceMask;
    private int yOffset;
    private final double[] parameterValues = new double[parameters.size()];
    static final List<Function<NoteStep, Consumer<Double>>> parameters = List.of(
            step -> step::setVelocity,
            step -> step::setReleaseVelocity,
            step -> step::setPan,
            step -> step::setTimbre,
            step -> step::setPressure,
            step -> step::setGain,

            step -> step::setTranspose,
            step -> step::setVelocitySpread,
            step -> step::setChance,
            step -> step::setRepeatCurve,
            step -> step::setRepeatVelocityEnd,
            step -> step::setRepeatVelocityCurve
    );
    static final String[] paramNames = new String[]{
            "Velocity",
            "Release Velocity",
            "Pan",
            "Timbre",
            "Pressure",
            "Gain",

            "Transpose",
            "Velocity Spread",
            "Chance",
            "Repeat Curve",
            "Repeat Velocity End",
            "Repeat Velocity Curve",
    };
    static final List<Function<NoteStep, Consumer<Boolean>>> booleanParameters = List.of(
            step -> step::setIsChanceEnabled,
            step -> step::setIsOccurrenceEnabled,
            step -> step::setIsRecurrenceEnabled,
            step -> step::setIsRepeatEnabled,
            step -> step::setIsMuted
    );
    static final List<Function<NoteStep, Boolean>> booleanParameterStatus = List.of(
            NoteStep::isChanceEnabled,
            NoteStep::isOccurrenceEnabled,
            NoteStep::isRecurrenceEnabled,
            NoteStep::isRepeatEnabled,
            NoteStep::isMuted
    );
    private static final int occurrenceLength = NoteOccurrence.values().length;
    private static final NoteOccurrence[] occurrence = NoteOccurrence.values();
    static KeyPiano keyPiano;
}
