package examples

case class Entity()

class ImplicitExample {

}
object ImplicitExample extends App {
  val name: String = Entity

  println(name)

  implicit def entityName[T](entity: T): String = {
    entity.getClass.getSimpleName.dropRight(1).toLowerCase
  }

}
