/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package konan.internal

import kotlin.coroutines.experimental.*
import kotlin.coroutines.experimental.intrinsics.*

@Intrinsic
@PublishedApi
internal external fun <T> getContinuation(): Continuation<T>

@Intrinsic
@PublishedApi
internal external suspend fun <T> returnIfSuspended(@Suppress("UNUSED_PARAMETER") argument: Any?): T

// Single-threaded continuation.
class SafeContinuation<in T>
constructor(
        private val delegate: Continuation<T>,
        initialResult: Any?
) : Continuation<T> {

    constructor(delegate: Continuation<T>) : this(delegate, UNDECIDED)

    public override val context: CoroutineContext
        get() = delegate.context

    private var result: Any? = initialResult

    companion object {
        private val UNDECIDED: Any? = Any()
        private val RESUMED: Any? = Any()
    }

    private class Fail(val exception: Throwable)

    override fun resume(value: T) {
        when {
            result === UNDECIDED -> result = value
            result === COROUTINE_SUSPENDED -> {
                result = RESUMED
                delegate.resume(value)
            }
            else -> throw IllegalStateException("Already resumed")
        }
    }

    override fun resumeWithException(exception: Throwable) {
        when  {
            result === UNDECIDED -> result = Fail(exception)
            result === COROUTINE_SUSPENDED -> {
                result = RESUMED
                delegate.resumeWithException(exception)
            }
            else -> throw IllegalStateException("Already resumed")
        }
    }

    fun getResult(): Any? {
        if (this.result === UNDECIDED) this.result = COROUTINE_SUSPENDED
        val result = this.result
        when {
            result === RESUMED -> return COROUTINE_SUSPENDED // already called continuation, indicate COROUTINE_SUSPENDED upstream
            result is Fail -> throw result.exception
            else -> return result // either COROUTINE_SUSPENDED or data
        }
    }
}

@ExportForCompiler
internal fun <T> interceptContinuationIfNeeded(
        context: CoroutineContext,
        continuation: Continuation<T>
) = context[ContinuationInterceptor]?.interceptContinuation(continuation) ?: continuation

/**
 * @suppress
 */
@ExportForCompiler
@PublishedApi
internal fun <T> normalizeContinuation(continuation: Continuation<T>): Continuation<T> =
        (continuation as? CoroutineImpl)?.facade ?: continuation

/**
 * @suppress
 */
@ExportForCompiler
abstract internal class CoroutineImpl(
        protected var completion: Continuation<Any?>?
) : Continuation<Any?> {

    // label == -1 when coroutine cannot be started (it is just a factory object) or has already finished execution
    // label == 0 in initial part of the coroutine
    protected var label: NativePtr = if (completion != null) NativePtr.NULL else NativePtr.NULL + (-1L)

    private val _context: CoroutineContext? = completion?.context

    override val context: CoroutineContext
        get() = _context!!

    private var _facade: Continuation<Any?>? = null

    val facade: Continuation<Any?> get() {
        if (_facade == null) _facade = interceptContinuationIfNeeded(_context!!, this)
        return _facade!!
    }

    override fun resume(value: Any?) {
        processBareContinuationResume(completion!!) {
            doResume(value, null)
        }
    }

    override fun resumeWithException(exception: Throwable) {
        processBareContinuationResume(completion!!) {
            doResume(null, exception)
        }
    }

    protected abstract fun doResume(data: Any?, exception: Throwable?): Any?

    open fun create(completion: Continuation<*>): Continuation<Unit> {
        throw IllegalStateException("create(Continuation) has not been overridden")
    }

    open fun create(value: Any?, completion: Continuation<*>): Continuation<Unit> {
        throw IllegalStateException("create(Any?;Continuation) has not been overridden")
    }
}
