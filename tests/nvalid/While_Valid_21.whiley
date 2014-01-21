import println from whiley.lang.System

type nat is int where $ >= 0

function create(nat count, int value) => [int]
ensures |$| == count:
    r = []
    i = 0
    while i < count where (i <= count) && (i == |r|):
        r = r + [value]
        i = i + 1
    return r

method main(System.Console sys) => void:
    sys.out.println(Any.toString(create(3, 3)))
    sys.out.println(Any.toString(create(2, 2)))
    sys.out.println(Any.toString(create(2, 1)))
    sys.out.println(Any.toString(create(1, 1)))
    sys.out.println(Any.toString(create(0, 0)))