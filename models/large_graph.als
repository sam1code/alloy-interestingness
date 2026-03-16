sig Node {
    adj: set Node,
    label: one Label
}

sig Label {}

sig Path {
    nodes: seq Node
}

fact NoSelfAdj {
    no n: Node | n in n.adj
}

fact AdjSymmetric {
    all n1, n2: Node |
        n1 in n2.adj implies n2 in n1.adj
}

fact ConnectedGraph {
    all disj n1, n2: Node |
        n2 in n1.^adj
}

fact PathNodesExist {
    all p: Path | all i: p.nodes.inds |
        p.nodes[i] in Node
}

pred largeGraph {}

run largeGraph for
    exactly 6 Node,
    exactly 3 Label,
    2 Path