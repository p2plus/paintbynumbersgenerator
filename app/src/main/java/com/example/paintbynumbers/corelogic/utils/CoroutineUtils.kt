package com.example.paintbynumbers.corelogic.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive

/**
 * A simple wrapper for cancellation state, primarily for porting logic
 * that used a CancellationToken class. In a full Coroutine-based implementation,
 * relying on `coroutineContext.isActive` and structured concurrency is preferred.
 *
 * This can be useful if passing Job instances around is less convenient than
 * a mutable flag holder for some deeply nested or recursive logic being ported.
 *
 * Consider replacing with direct `CoroutineScope.isActive` checks where possible.
 */
class CoroutineCancellationToken(private val job: Job? = null, private val scope: CoroutineScope? = null) {
    val isCancelled: Boolean
        get() = job?.isCancelled ?: scope?.isActive?.not() ?: _isManuallyCancelled

    private var _isManuallyCancelled: Boolean = false

    /**
     * Manually triggers cancellation if not tied to a specific Job or Scope.
     * If tied to a Job or Scope, this is a no-op as cancellation should be managed by them.
     */
    fun cancel() {
        if (job == null && scope == null) {
            _isManuallyCancelled = true
        }
        // If tied to a job/scope, their cancellation mechanism should be used.
        // This manual flag is a fallback for direct porting.
    }

    fun reset() {
        _isManuallyCancelled = false
        // Cannot reset Job/Scope cancellation status here.
    }
}
