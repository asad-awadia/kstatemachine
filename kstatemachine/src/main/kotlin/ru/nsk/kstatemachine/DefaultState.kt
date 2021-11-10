package ru.nsk.kstatemachine

open class DefaultState(name: String? = null, childMode: ChildMode = ChildMode.EXCLUSIVE) :
    BaseStateImpl(name, childMode), State

open class DefaultDataState<out D>(name: String? = null, childMode: ChildMode = ChildMode.EXCLUSIVE) :
    BaseStateImpl(name, childMode), DataState<D> {
    private var _data: D? = null
    override val data: D get() = checkNotNull(_data) { "Data is not set. Is the state active?" }

    override fun onDoEnter(transitionParams: TransitionParams<*>) {
        if (this == transitionParams.direction.targetState) {
            @Suppress("UNCHECKED_CAST")
            val event = transitionParams.event as? DataEvent<D>
            checkNotNull(event) { "${transitionParams.event} does not contain data required by $this" }
            _data = event.data
        } else {
            error(
                "$this is implicitly activated, this might be a result of a cross-level transition. " +
                        "Currently there is no way to get data for this state."
            )
        }
    }

    override fun onDoExit(transitionParams: TransitionParams<*>) {
        _data = null
    }
}

open class DefaultFinalState(name: String? = null) : DefaultState(name), FinalState {
    override fun <E : Event> addTransition(transition: Transition<E>) = super<FinalState>.addTransition(transition)
}

open class DefaultFinalDataState<out D>(name: String? = null) : DefaultDataState<D>(name), FinalDataState<D> {
    override fun <E : Event> addTransition(transition: Transition<E>) = super<FinalDataState>.addTransition(transition)
}

/**
 * It is open for subclassing as all other [State] implementations, but I do not know real use cases for it.
 */
open class DefaultHistoryState(
    name: String? = null,
    override val historyType: HistoryType = HistoryType.SHALLOW,
    private var _defaultState: State? = null
) : BaseStateImpl(name, ChildMode.EXCLUSIVE), HistoryState {

    override fun setParent(parent: InternalState) {
        super.setParent(parent)

        if (_defaultState == null)
            _defaultState = parent.initialState as State
        else
            require(parent.states.contains(defaultState)) { "Default state is not a parent child" }
    }

    override val defaultState get() = checkNotNull(_defaultState) { "Default state is not set" }

    private var _storedState: State? = null

    override fun storeState(owner: IState, currentState: IState) {
        _storedState = currentState as State
    }

    override fun <S : IState> addState(state: S, init: StateBlock<S>?) =
        throw UnsupportedOperationException("HistoryState can not have child states")


    override fun <E : Event> addTransition(transition: Transition<E>) =
        throw UnsupportedOperationException("HistoryState can not have transitions")
}
