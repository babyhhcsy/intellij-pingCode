/*
 *  Copyright 2016-2019 码云 - Gitee
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.pingCode.authentication.util


import com.intellij.execution.process.ProcessIOExecutorService
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.util.Computable
import com.intellij.openapi.util.Disposer
import com.pingCode.authentication.util.PingCodeAsyncUtil.extractError
import com.pingCode.authentication.util.PingCodeAsyncUtil.isCancellation
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicReference
import java.util.function.BiFunction
import java.util.function.Supplier

object PingCodeAsyncUtil {

  @JvmStatic
  fun <T> awaitFuture(progressIndicator: ProgressIndicator, future: Future<T>): T {
    var result: T
    while (true) {
      try {
        result = future.get(50, TimeUnit.MILLISECONDS)
        break
      }
      catch (e: TimeoutException) {
        progressIndicator.checkCanceled()
      }
      catch (e: Exception) {
        if (isCancellation(e)) throw ProcessCanceledException()
        if (e is ExecutionException) throw e.cause ?: e
        throw e
      }
    }
    return result
  }

  @JvmStatic
  fun <T> futureOfMutable(futureSupplier: () -> CompletableFuture<T>): CompletableFuture<T> {
    val result = CompletableFuture<T>()
    handleToOtherIfCancelled(futureSupplier, result)
    return result
  }

  private fun <T> handleToOtherIfCancelled(futureSupplier: () -> CompletableFuture<T>, other: CompletableFuture<T>) {
    futureSupplier().handle { result, error ->
      if (result != null) other.complete(result)
      if (error != null) {
        if (isCancellation(error)) handleToOtherIfCancelled(futureSupplier, other)
        other.completeExceptionally(error.cause)
      }
    }
  }

  fun isCancellation(error: Throwable): Boolean {
    return error is ProcessCanceledException
        || error is CancellationException
        || error is InterruptedException
        || error.cause?.let(::isCancellation) ?: false
  }

  fun extractError(error: Throwable): Throwable {
    return when (error) {
      is CompletionException -> extractError(error.cause!!)
      is ExecutionException -> extractError(error.cause!!)
      else -> error
    }
  }
}

fun <T> ProgressManager.submitIOTask(progressIndicator: ProgressIndicator,
                                     task: (indicator: ProgressIndicator) -> T): CompletableFuture<T> =
  CompletableFuture.supplyAsync(Supplier { runProcess(Computable { task(progressIndicator) }, progressIndicator) },
    ProcessIOExecutorService.INSTANCE)

fun <T> CompletableFuture<T>.handleOnEdt(disposable: Disposable,
                                         handler: (T?, Throwable?) -> Unit): CompletableFuture<Unit> {
  val handlerReference = AtomicReference(handler)
  Disposer.register(disposable, Disposable {
    handlerReference.set(null)
  })

  return handleAsync(BiFunction<T?, Throwable?, Unit> { result: T?, error: Throwable? ->
    val handlerFromRef = handlerReference.get() ?: throw ProcessCanceledException()
    handlerFromRef(result, error?.let { extractError(it) })
  }, getEDTExecutor(null))
}

fun <T, R> CompletableFuture<T>.handleOnEdt(modalityState: ModalityState? = null,
                                            handler: (T?, Throwable?) -> R): CompletableFuture<R> =
  handleAsync(BiFunction<T?, Throwable?, R> { result: T?, error: Throwable? ->
    handler(result, error?.let { extractError(it) })
  }, getEDTExecutor(modalityState))

fun <T, R> CompletableFuture<T>.successOnEdt(modalityState: ModalityState? = null, handler: (T) -> R): CompletableFuture<R> =
  handleOnEdt(modalityState) { result, error ->
    @Suppress("UNCHECKED_CAST")
    if (error != null) throw extractError(error) else handler(result as T)
  }

fun <T> CompletableFuture<T>.errorOnEdt(modalityState: ModalityState? = null,
                                        handler: (Throwable) -> Unit): CompletableFuture<T> =
  handleOnEdt(modalityState) { result, error ->
    if (error != null) {
      val actualError = extractError(error)
      if (isCancellation(actualError)) throw ProcessCanceledException()
      handler(actualError)
      throw actualError
    }
    @Suppress("UNCHECKED_CAST")
    result as T
  }

fun <T> CompletableFuture<T>.cancellationOnEdt(modalityState: ModalityState? = null,
                                               handler: (ProcessCanceledException) -> Unit): CompletableFuture<T> =
  handleOnEdt(modalityState) { result, error ->
    if (error != null) {
      val actualError = extractError(error)
      if (isCancellation(actualError)) handler(ProcessCanceledException())
      throw actualError
    }
    @Suppress("UNCHECKED_CAST")
    result as T
  }

fun <T> CompletableFuture<T>.completionOnEdt(modalityState: ModalityState? = null,
                                             handler: () -> Unit): CompletableFuture<T> =
  handleOnEdt(modalityState) { result, error ->
    @Suppress("UNCHECKED_CAST")
    if (error != null) {
      if (!isCancellation(error)) handler()
      throw extractError(error)
    }
    else {
      handler()
      result as T
    }
  }

fun getEDTExecutor(modalityState: ModalityState? = null) = Executor { runnable -> runInEdt(modalityState) { runnable.run() } }