import * from whiley.lang.*

{int} f({int} xs):
    return { -x | x ∈ xs } 

void ::main(System.Console sys):
    sys.out.println(Any.toString(f({1,2,3,4})))
