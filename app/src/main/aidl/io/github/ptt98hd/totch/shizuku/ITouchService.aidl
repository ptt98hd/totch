package io.github.ptt98hd.totch.shizuku;

import io.github.ptt98hd.totch.shizuku.ILineCallback;

/** Shizuku UserService running as uid shell (docs/02-architecture.md §4.1). */
interface ITouchService {
    /** Called by Shizuku when the service is unbound with remove=true. Fixed transaction code. */
    void destroy() = 16777114;

    /** Picks the touchscreen and streams its events to callback until stop(); replaces any running stream. */
    void start(ILineCallback callback) = 1;

    void stop() = 2;

    /** Runs a shell command, returns its exit code (-1 on timeout/failure). */
    int exec(String command) = 3;
}
