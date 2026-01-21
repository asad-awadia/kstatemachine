/*
 * Author: Mikhail Fedotov
 * Github: https://github.com/KStateMachine
 * Copyright (c) 2024.
 * All rights reserved.
 */

package ru.nsk.kstatemachine.statemachine

import kotlinx.coroutines.*
import ru.nsk.kstatemachine.coroutines.CoroutinesLibCoroutineAbstraction
import ru.nsk.kstatemachine.coroutines.createStateMachine
import ru.nsk.kstatemachine.event.Event
import ru.nsk.kstatemachine.state.ChildMode
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.coroutines.ContinuationInterceptor
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * Suspendable analog of [createStdLibStateMachine] function, with Kotlin Coroutines support.
 * This is preferred function. Use this one especially if you are going to use Kotlin Coroutines library from
 * KStateMachine callbacks.
 *
 * @param scope be careful while working with threaded scopes as KStateMachine classes are not thread-safe.
 * Usually you should use only single threaded scopes, for example:
 *
 *     CoroutineScope(newSingleThreadContext("single threaded context"))
 *
 * Note that all calls to created machine instance should be done only from that thread.
 */
@OptIn(ExperimentalContracts::class)
suspend fun createStateMachine(
    scope: CoroutineScope,
    name: String? = null,
    childMode: ChildMode = ChildMode.EXCLUSIVE,
    start: Boolean = true,
    creationArguments: CreationArguments = buildCreationArguments {},
    init: suspend BuildingStateMachine.() -> Unit
): StateMachine {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    checkCoroutineScopeValidity(scope, creationArguments)
    return CoroutinesLibCoroutineAbstraction(scope)
        .createStateMachine(name, childMode, start, creationArguments, init)
}

private fun checkCoroutineScopeValidity(scope: CoroutineScope, creationArguments: CreationArguments) {
    if (creationArguments.skipCoroutineScopeValidityCheck) return

    val dispatcher = scope.coroutineContext[ContinuationInterceptor]
    val dispatcherName = dispatcher.toString()
    if (dispatcher === Dispatchers.Default ||
        dispatcherName == "Dispatchers.Default" ||
        dispatcherName.startsWith("Dispatchers.Default.limitedParallelism") ||
        dispatcherName == "Dispatchers.IO" || // can't get IO dispatcher in commonMain
        dispatcherName.startsWith("Dispatchers.IO.limitedParallelism")
    ) {
        error(
            "Using Dispatchers.Default or Dispatchers.IO for StateMachine even with limitedParallelism(1) is the most likely an error," +
                    " as it is multi-threaded, see the docs: \n" +
                    "https://kstatemachine.github.io/kstatemachine/pages/multithreading.html#use-single-threaded-coroutinescope" +
            "You can opt-out this check by CreationArguments::skipCoroutineScopeValidityCheck flag."
        )
    }
}

/**
 * Processes event in async fashion (using launch() to start new coroutine).
 *
 * This API requires [StateMachine]'s [CoroutineScope] so it throws if called on
 * machines created by [createStdLibStateMachine].
 *
 * This method is not suspendable like original [StateMachine.processEvent],
 * so it can be called from any context easily.
 */
fun StateMachine.processEventByLaunch(
    event: Event,
    argument: Any? = null,
    coroutineContext: CoroutineContext = EmptyCoroutineContext,
    coroutineStart: CoroutineStart = CoroutineStart.DEFAULT,
) {
    val coroutineAbstraction = coroutineAbstraction
    require(coroutineAbstraction is CoroutinesLibCoroutineAbstraction) {
        "${::processEventByLaunch.name} API may be called on ${StateMachine::class.simpleName} " +
                "created with coroutines support only"
    }
    coroutineAbstraction.scope.launch(coroutineContext, coroutineStart) { processEvent(event, argument) }
}

/**
 * Processes event in async fashion (using async() to start new coroutine) and returns result as [Deferred].
 *
 * This API requires [StateMachine]'s [CoroutineScope] so it throws if called on
 * machines created by [createStdLibStateMachine].
 *
 * This method is not suspendable like original [StateMachine.processEvent],
 * so it can be called from any context easily.
 */
fun StateMachine.processEventByAsync(
    event: Event,
    argument: Any? = null,
    coroutineContext: CoroutineContext = EmptyCoroutineContext,
    coroutineStart: CoroutineStart = CoroutineStart.DEFAULT,
): Deferred<ProcessingResult> {
    val coroutineAbstraction = coroutineAbstraction
    require(coroutineAbstraction is CoroutinesLibCoroutineAbstraction) {
        "${::processEventByAsync.name} API may be called on ${StateMachine::class.simpleName} " +
                "created with coroutines support only"
    }
    return coroutineAbstraction.scope.async(coroutineContext, coroutineStart) { processEvent(event, argument) }
}