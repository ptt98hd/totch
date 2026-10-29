package io.github.ptt98hd.totch.shizuku

import io.github.ptt98hd.totch.core.action.OneHandedFallback
import io.github.ptt98hd.totch.core.action.OneHandedMethod

/** OH-5: tries the saved one-handed method first, falls back to the other and remembers the one that worked. */
class OneHandedRunner(
    private val preferred: () -> OneHandedMethod,
    private val remember: suspend (OneHandedMethod) -> Unit,
    private val attempt: suspend (OneHandedMethod) -> Boolean,
) {
    suspend fun run(): Result<Unit> {
        val current = preferred()
        val used = OneHandedFallback.run(current, attempt)
            ?: return Result.failure(IllegalStateException("one-handed mode had no effect"))
        if (used != current) remember(used)
        return Result.success(Unit)
    }
}
