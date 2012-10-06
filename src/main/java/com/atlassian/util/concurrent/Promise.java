package com.atlassian.util.concurrent;

import java.util.concurrent.Future;

import com.google.common.annotations.Beta;
import com.google.common.base.Function;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

/**
 * A promise that presents a nicer interface to {@link Future}. It can be
 * claimed without needing to catch checked exceptions, and it may be mapped to
 * new types of Promise via the {@link #map(Function)} and
 * {@link #flatMap(Function)} methods.
 * 
 * @since 1.2
 */
@Beta
public interface Promise<V> extends ListenableFuture<V> {
  /**
   * Blocks the thread waiting for a result. Exceptions are thrown as runtime
   * exceptions.
   * 
   * @return The promised object
   */
  V claim();

  /**
   * Registers a FutureCallback to handle both done (success) and fail
   * (exception) cases. May not be executed in the same thread as the caller.
   * <p>
   * See {@link Promises#futureCallback(Effect, Effect)}
   * {@link Promises#onSuccessDo(Effect)} and
   * {@link Promises#onFailureDo(Effect)} for easy ways of turning an
   * {@link Effect} into a {@link FutureCallback}
   * 
   * @param callback The future callback
   * @return This object for chaining
   */
  Promise<V> then(FutureCallback<V> callback);

  /**
   * Transforms this promise from one type to another by way of a transformation
   * function.
   * <p>
   * Note: This overload of {@code transform} is designed for cases in which the
   * transformation is fast and lightweight, as the method does not accept an
   * {@code Executor} to perform the the work in. For more details see the note
   * on {@link Futures#transform(Future, Function)}.
   * 
   * @param function The transformation function
   * @return A new promise resulting from the transformation
   */
  public <T> Promise<T> map(Function<? super V, ? extends T> function);

  /**
   * Transforms this promise from one type to another by way of a transformation
   * function that returns a new Promise.
   * 
   * @param function The transformation function
   * @return A new promise resulting from the transformation
   */
  public <T> Promise<T> flatMap(Function<? super V, Promise<T>> function);
}