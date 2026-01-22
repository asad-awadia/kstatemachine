/*
 * Author: Mikhail Fedotov
 * Github: https://github.com/KStateMachine
 * Copyright (c) 2024.
 * All rights reserved.
 */

package ru.nsk.kstatemachine.state

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.mockk.coVerify
import io.mockk.mockk
import ru.nsk.kstatemachine.CoroutineStarterType
import ru.nsk.kstatemachine.createTestStateMachine
import ru.nsk.kstatemachine.state.StateCleanupTestData.State1
import ru.nsk.kstatemachine.statemachine.destroyBlocking

private object StateCleanupTestData {
    class State1(private val onCleanupListener: () -> Unit) : DefaultState("state1") {
        override suspend fun onCleanup() {
            super.onCleanup()
            onCleanupListener()
        }
    }
}

class StateCleanupTest : FreeSpec({
    CoroutineStarterType.entries.forEach { coroutineStarterType ->
        "$coroutineStarterType" - {
            "cleanup is not called" {
                val listener = mockk<() -> Unit>(relaxed = true)
                val state = State1(listener)
                useInMachine(coroutineStarterType, state)
                coVerify(inverse = true) { listener() }
            }

            "cleanup is called on machine manual destruction" {
                val listener = mockk<() -> Unit>(relaxed = true)
                val state = State1(listener)
                useInMachine(coroutineStarterType, state).destroyBlocking()
                coVerify(exactly = 1) { listener() }
            }

            "cleanup is called on machine auto destruction" {
                val listener = mockk<() -> Unit>(relaxed = true)
                val state = State1(listener)
                val machine1 = useInMachine(coroutineStarterType, state)
                val machine2 = useInMachine(coroutineStarterType, state)

                coVerify(exactly = 1) { listener() }
                machine1.isDestroyed shouldBe true
                machine2.isDestroyed shouldBe false
            }
        }
    }
})

private suspend fun useInMachine(coroutineStarterType: CoroutineStarterType, state: IState) =
    createTestStateMachine(coroutineStarterType) {
        addInitialState(state)
    }