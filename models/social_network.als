sig Person {
    knows: set Person,
    likes: set Person,
    follows: set Person
}

sig Post {
    author: one Person,
    likes: set Person
}

sig Group {
    members: set Person,
    admin: one Person
}

fact NoSelfKnows {
    no p: Person | p in p.knows
}

fact NoSelfLikes {
    no p: Person | p in p.likes
}

fact AdminIsMember {
    all g: Group | g.admin in g.members
}

fact AuthorLikesOwnPost {
    all p: Post | p.author in p.likes
}

fact KnowsSymmetric {
    all p1, p2: Person |
        p1 in p2.knows implies p2 in p1.knows
}

pred someSocialNetwork {}

run someSocialNetwork for
    exactly 5 Person,
    exactly 4 Post,
    exactly 2 Group