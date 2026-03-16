sig Host {}

sig Connection {
    src, dst: one Host
}

fact NoSelfConnection {
    all c: Connection | c.src != c.dst
}

fact Symmetric {
    all c1: Connection | some c2: Connection |
        c2.src = c1.dst and c2.dst = c1.src
}

pred someNetwork {}

run someNetwork for exactly 3 Host, 4 Connection