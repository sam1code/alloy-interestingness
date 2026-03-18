sig State {
    transitions: set State,
    invariant: set Condition
}

sig Condition {}

sig Event {
    trigger: one State,
    target: one State,
    guard: set Condition
}

one sig InitState extends State {}
sig AcceptState extends State {}

fact InitHasTransitions {
    some InitState.transitions
}

fact AcceptNoOutgoing {
    no s: AcceptState | some s.transitions
}

fact EventConsistent {
    all e: Event |
        e.target in e.trigger.transitions
}

fact GuardSubsetOfInvariant {
    all e: Event |
        e.guard in e.trigger.invariant
}

fact ReachableFromInit {
    all s: State |
        s in InitState.*transitions
}

pred someStateMachine {}

run someStateMachine for
    exactly 5 State,
    exactly 4 Condition,
    exactly 6 Event,
    exactly 2 AcceptState