package ch2

import scala.annotation.tailrec

object Main22 {
  def isSorted[A] (as: Array[A], sorted: (A,A) => Boolean)= {
    @tailrec def sort(i: Int) : Boolean = { 
      if (i >= as.size)
        true
      else if (sorted(as(i), as(i+1)))
          sort(i+1)
      else false
    }    
    sort(0)
  }
}
