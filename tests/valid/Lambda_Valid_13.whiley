type intargfunc is function(int) -> (int)
type voidargfunc is function(void)->(int)

type intargmethod is method(int) -> (int)
type voidargmethod is method(void)->(int)

method f(int x) -> int:
    return x + 1

public export method test():
    intargfunc m = &(int x -> x)
    int y = m(3)
    assume y == 3
    voidargfunc n = m
    assume m == n

    intargmethod o = &f
    int z = o(5)
    assume z == 6
    voidargmethod p = o
    assume o == p
