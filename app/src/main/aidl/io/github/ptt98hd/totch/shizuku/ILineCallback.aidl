package io.github.ptt98hd.totch.shizuku;

/** Events from TouchUserService to the app; oneway so the reader thread never blocks on the app. */
oneway interface ILineCallback {
    /** Touchscreen chosen from `getevent -pl` (GD-3). */
    void onDevice(String path, String name, int minX, int maxX, int minY, int maxY);
    /** `getevent -lt` lines of one input frame (up to and including SYN_REPORT). */
    void onLines(in String[] lines);
    /** Stream stopped; error is the ordinal of engine.PipelineError. */
    void onError(int error);
}
