import println from whiley.lang.System

type sr9nat is int where $ > 0

type sr9tup is {sr9nat f, int g} where g > f

type sr9arr is [{sr9nat f, int g}] where some { z in $ | z.f == 1 }

method main(System.Console sys) => void:
    x = [{f: 1, g: 2}, {f: 1, g: 8}]
    x[0].f = 2
    sys.out.println(Any.toString(x))