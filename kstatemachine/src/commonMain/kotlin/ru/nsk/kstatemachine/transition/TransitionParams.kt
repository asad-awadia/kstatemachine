/*
 * Author: Mikhail Fedotov
 * Github: https://github.com/KStateMachine
 * Copyright (c) 2024.
 * All rights reserved.
 */

package ru.nsk.kstatemachine.transition

import ru.nsk.kstatemachine.event.Event
import ru.nsk.kstatemachine.event.StartEvent
import ru.nsk.kstatemachine.event.WrappedEvent
import ru.nsk.kstatemachine.statemachine.StateMachineDslMarker

@ConsistentCopyVisibility
@StateMachineDslMarker
data class TransitionParams<E : Event> internal constructor(
    val transition: Transition<E>,
    val direction: TransitionDirection,
    val event: E,
    /**
     * This parameter may be used to pass arbitrary data with the event,
     * so there is no need to define [Event] subclasses every time.
     * Subclassing should be preferred if the event always contains data of some type.
     */
    val argument: Any? = null,
)

/**
 * Convenience property for unwrapping original event.
 * If the event is not [WrappedEvent] this is same as [TransitionParams.event] property
 */
val TransitionParams<*>.unwrappedEvent get() = if (event is WrappedEvent) event.event else event

/**
 * Convenience property for unwrapping original argument.
 * If the event is not [WrappedEvent] this is same as [TransitionParams.argument] property
 */
val TransitionParams<*>.unwrappedArgument get() = if (event is WrappedEvent) event.argument else argument

/**
 * Returns true is the transition is triggered by [StartEvent].
 * This means that the StateMachine is starting.
 * Might be useful to check if you're entering some State just by a machine startup
 * or by outside event.
 */
val TransitionParams<*>.isStartTransition get() = event is StartEvent