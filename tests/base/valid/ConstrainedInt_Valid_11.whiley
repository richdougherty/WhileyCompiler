define a_nat as int
define b_nat as int

b_nat f(a_nat x):
    if x == 0:
        return 1
    else:
        return f(x-1)

void System::main([string] args):
    x = |args|    
    x = f(x)    
    out.println(str(x))
