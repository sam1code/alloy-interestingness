sig Node {}

sig Edge {
  from, to: one Node
}

fact NoSelfLoops {
  all e: Edge | e.from != e.to
}

fact NoDuplicateEdges {
  all disj e1, e2: Edge |
    not (e1.from = e2.from and e1.to = e2.to)
}

pred someGraph {}

run someGraph for exactly 3 Node, 3 Edge