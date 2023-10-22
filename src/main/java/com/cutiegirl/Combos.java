package com.cutiegirl;

import com.bitwig.extension.controller.api.Clip;
import com.bitwig.extension.controller.api.ControllerHost;

class Combos {
    Combos(InputHandler handler) {
        host = handler.host;
        clip = handler.cursorClip;
    }

    void add(NoteCombo noteCombo, int i) {
        noteCombos[i] = noteCombo;
        host.showPopupNotification("Combo Added To: "+i);
    }
    void setActiveCombo(int i) {
        activeCombo = i;
    }
    NoteCombo activeCombo() {
        return noteCombos[activeCombo];
    }
    final NoteCombo[] noteCombos = new NoteCombo[8];
    private int activeCombo;
    static ControllerHost host;
    static Clip clip;
}
