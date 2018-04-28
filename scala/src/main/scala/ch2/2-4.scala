package ch2

object Main24 {
  def uncurry[A,B,C] (f: A=>B=>C) : (A,B) => C = 
    (a,b) => f(a)(b)
}
