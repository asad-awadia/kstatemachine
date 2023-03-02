package ru.nsk.kstatemachine

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.verify
import io.mockk.verifySequence
import ru.nsk.kstatemachine.ProcessingResult.IGNORED

class IgnoredEventHandlerTest : StringSpec({
    CoroutineStarterType.values().forEach { coroutineStarterType ->
        "ignored event handler" {
            val callbacks = mockkCallbacks()

            val machine = createTestStateMachine(coroutineStarterType) {
                initialState("first")

                ignoredEventHandler = StateMachine.IgnoredEventHandler {
                    callbacks.onIgnoredEvent(SwitchEvent)
                }
            }

            machine.processEvent(SwitchEvent) shouldBe IGNORED
            verifySequence { callbacks.onIgnoredEvent(SwitchEvent) }
        }

        "exceptional ignored event handler" {
            val machine = createTestStateMachine(coroutineStarterType) {
                initialState("first")

                ignoredEventHandler = StateMachine.IgnoredEventHandler {
                    testError("unexpected ${it.event}")
                }
            }

            shouldThrow<TestException> { machine.processEvent(SwitchEvent) }
            machine.isDestroyed shouldBe false
        }

        "process event on finished state machine" {
            val callbacks = mockkCallbacks()

            val machine = createTestStateMachine(coroutineStarterType) {
                setInitialState(finalState("final"))

                onFinished { callbacks.onFinished(this) }

                ignoredEventHandler = StateMachine.IgnoredEventHandler {
                    callbacks.onIgnoredEvent(it.event)
                }
            }

            verifySequenceAndClear(callbacks) { callbacks.onFinished(machine) }

            machine.processEvent(SwitchEvent) shouldBe IGNORED
            verifySequence { callbacks.onIgnoredEvent(SwitchEvent) }
        }

        "ignored event on conditional noTransition()" {
            val callbacks = mockkCallbacks()

            val machine = createTestStateMachine(coroutineStarterType) {
                initialState {
                    transitionConditionally<SwitchEvent> { direction = { noTransition() } }
                }

                ignoredEventHandler = StateMachine.IgnoredEventHandler {
                    callbacks.onIgnoredEvent(it.event)
                }
            }

            machine.processEvent(SwitchEvent) shouldBe IGNORED
            verify { callbacks.onIgnoredEvent(SwitchEvent) }
        }
    }
})