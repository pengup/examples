package ch2

object Main23 {
def curry[A,B,C] (f: (A, B) => C) : A => (B=>C) = 
  a => (b => f(a,b))
}
