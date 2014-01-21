import println from whiley.lang.System

type ur4nat is int where $ > 0

type tur4nat is int where $ > 10

type wur4nat is ur4nat | tur4nat

function f(wur4nat x) => string:
    return Any.toString(x)

method main(System.Console sys) => void:
    sys.out.println(f(1))