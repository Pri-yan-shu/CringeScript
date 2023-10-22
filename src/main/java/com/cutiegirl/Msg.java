package com.cutiegirl;

enum LAYER {
    BLANK           (27),
    CHORDS_NOTES    (26),
    CHORDS          (25),
    LOOP            (24),
    LAUNCHER        (23, LOOP, null, false, false),
    NOTE_CTX        (22),
    BROWSER_CTX     (21),
    LAYER_CTX       (19),
    DEVICE_LAYER    (18),
    CURVE_CTX       (17),
    CURVE           (16, NOTE_CTX, CURVE_CTX, false, false),
    CLIP_CTX        (14),
    BROWSER_2       (13),
    CLIP_2          (12, LAUNCHER, CLIP_CTX, false, false),
    DEVICE_PARAM    (11),
    TRACK_PARAM     (10),
    SEL_NOTE        (9),
    SEL_DEVICE      (8, null, DEVICE_LAYER, false, false),
    SEL_TRACK       (7),
    ARP             (6),
    TRANSPORT       (5),
    BROWSER         (4, BROWSER_2, BROWSER_CTX, false, true),
    CLIP            (3, CHORDS, CLIP_2, true, true),
    SCALES          (20, CHORDS, CLIP, false, false),
    NOTES           (15, CLIP, SCALES, false, false),
    DEVICE          (2, DEVICE_PARAM, SEL_DEVICE, true, true),
    TRACK           (1, TRACK_PARAM, SEL_TRACK, true, true),
    META            (0, null, null, false, false)
    ;
    //Clip_2 -> clip
    //NoteComb -> Clip
    LAYER(int id) {
        this(id, null, null, false, true);
    }
    LAYER(int id, LAYER child, LAYER context, boolean isStatic, boolean aftToOn) {
        this.id = id;
        this.child = child;
        this.context = context;
        if (child != null) child.parent = this;
        if (context != null) context.parent = this;
        this.isStatic = isStatic;
        this.aftToOn = aftToOn;
    }
    static LAYER getLayer(int index) {
        return switch (index) {
            case 0 -> TRACK;
            case 1 -> DEVICE;
            case 2 -> CLIP;
            case 3 -> BROWSER;
            case 4 -> TRANSPORT;
            case 5 -> ARP;
            default -> throw new IllegalStateException("Layer unavailable for index: " + index);
        };
    }
    final int id;
    final LAYER child;
    final LAYER context;
    private LAYER parent;
    final boolean isStatic;
    final boolean aftToOn;
    public LAYER getParent() {
        return parent;
    }
}
enum GESTURE {
    ONN(0),
    OFF(1),
    AFT(2),
    TAP(3),
    RET(4);
    GESTURE(int id) {this.id = id;}
    final int id;
}
final class Msg {
    Msg(int note, GESTURE gesture, LAYER layer, GROUP group, int history, int rawID) {
        this.note = note;
        this.gesture = gesture;
        this.layer = layer;
        this.group = group;
        this.history = history;
        this.rawID = rawID;
    }
    Msg(int note, GESTURE gesture, LAYER layer, GROUP group, int history) {
        this(note, gesture, layer, group, history, note);
    }
    Msg(int note, GESTURE gesture, LAYER layer, int rawID, GROUP group) {
        this(note, gesture, layer, group, handler.histories[layer.id], rawID);
    }
    Msg(int note, GESTURE gesture, LAYER layer) {
        this(note, gesture, layer, null, handler.histories[layer.id], note);
    }
    static void setHandler(InputHandler handler) {
        Msg.handler = handler;
    }
    final int note;
    final GESTURE gesture;
    final LAYER layer;
    final GROUP group;
    final int history;
    final int rawID;
    static InputHandler handler;
}