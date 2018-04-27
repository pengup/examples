package ch2

import scala.annotation.tailrec

object Main21 {
  def fab(n: Int): Int = {
    n match {
      case x if x<0 => 0 
      case 0  => 1
      case 1 => 1
      case _ =>
        fab(n-1) + fab (n-2)
    }   
  }
 
  def fab1(n: Int) : Int = {
    @tailrec def fibHelper(x: Int, prev: Int, next: Int): Int = 
     x match {
      case 0 => 0
      case 1 => next
      case _ => 
        println(s"x:$x pre:$prev next:$next")
        fibHelper(x - 1, next, (next + prev))
    }
    fibHelper(n, 0,1)
  } 
      
  def main(args: Array[String]): Unit = {
    if (args.size < 1) {
      println("Please give an int")
      System.exit(1)
    }
    val n: Int = args(0).toInt
    println("Result: " +  fab1(n))
  }
}
