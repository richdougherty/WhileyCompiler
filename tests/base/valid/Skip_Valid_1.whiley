import * from whiley.lang.*

int f(int x):
    if x > 0:
        skip
    else:
        return -1
    return x

void ::main(System.Console sys):
    sys.out.println(Any.toString(f(1)))
    sys.out.println(Any.toString(f(-10)))
