type mymethod is method()->(mymethod)

method m() -> mymethod:
    return &m

public export method test():
    mymethod x = m()
    assume x == &m