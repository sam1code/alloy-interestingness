sig Person {
    parent: set Person
}

fact NoSelfParent {
    no p: Person | p in p.parent
}

fact NoSymmetricParent {
    no disj p1, p2: Person |
        p1 in p2.parent and p2 in p1.parent
}

pred someFamily {}

run someFamily for exactly 4 Person