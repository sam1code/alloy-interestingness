sig Node {
    adj: set Node
}

fact NoSelfAdj {
    no n: Node | n in n.adj
}

pred anyGraph {}

run anyGraph for exactly 6 Node