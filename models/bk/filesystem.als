abstract sig FSObject {}

sig File extends FSObject {}

sig Dir extends FSObject {
    contents: set FSObject
}

one sig Root extends Dir {}

fact NoCycle {
    no d: Dir | d in d.^contents
}

fact RootContainsAll {
    FSObject = Root + Root.^contents
}

pred someFS {}

run someFS for exactly 1 Root, 2 Dir, 3 File
