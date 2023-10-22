package com.cutiegirl;

final class NoteComboBuilder extends AbstractCombo{
    NoteComboBuilder() {
        super(0);
    }

    @Override
    protected void buildAction(int step, int i) {
        if (step == 0) {
            noteCombo = new NoteCombo(y, i,0,1,0,0,0, 16, 4);
            targetSlot = -1;
            targetParam = -1;
        }
    }

    @Override
    protected void tapAction(int step, int i) {
        switch (step) {
            case 1 -> noteCombo.len = noteCombo.len + ((i + 1)/8.0 ) * switch (InputHandler.modifierStatus()) {
                case 1 -> 0.66;
                case 2 -> 0.75;
                case 4 -> 0.8;
                default -> 1;
            };
            case 2 -> noteCombo.mode = i + 1;
            case 3 -> noteCombo.loop = i + 1;
        }
    }

    @Override
    protected void retreatAction(int step, int i) {
        switch (step) {
            case 1 -> noteCombo.rep = i + 1;
            case 2 -> noteCombo.gap = i + 1;
            case 3 -> noteCombo.basis = switch (i) {

                case 1 -> 5;
                case 2 -> 3.5;
                case 3 -> 2.75;
                case 4 -> 3.25;
                default -> 3;
            };
        }
    }

    int getY() {
        return y;
    }

    NoteCombo noteCombo;
    int targetSlot;
    int targetParam;
    private int y = 0;
}
