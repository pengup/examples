package ch2

object Main25 {
  def compose[A,B,C](f: B=>C, g: A=> B): A=> C = {
    (a) => f(g(a))
  }
}
