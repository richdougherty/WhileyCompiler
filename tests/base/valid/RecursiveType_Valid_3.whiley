import * from whiley.lang.*

define nat as int
define expr as nat | {int op, expr left, expr right}

void ::main(System.Console sys):
    e = 14897
    sys.out.println(Any.toString(e))
