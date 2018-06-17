package io.underscore.testing.todo

import cats._
import cats.effect.IO
import cats.implicits._
import enumeratum._
import java.time.LocalDate

sealed trait Bug extends EnumEntry { self =>

  /** Decorate an algebra with this bug. */
  def apply[F[_] : Applicative](todo: TodoAlgebra[F]): TodoAlgebra[F] =
    new TodoAlgebra[F] {
      def append(item: TodoAlgebra.Item): F[TodoAlgebra.ItemId] =
        self match {
          case Bug.AppendReturnsWrongId => todo.append(item) map (_ + 5)
          case _ => todo.append(item)
        }

      def complete(id: TodoAlgebra.ItemId): F[Unit] =
        todo.complete(id)

      def find(id: TodoAlgebra.ItemId): F[Option[TodoAlgebra.Item]] =
        self match {
          case Bug.FindAlwaysFails => Option.empty.pure[F]
          case Bug.FindReturnsWrongItem => todo.find(id) map (_ map (_.copy(value = "ha ha ha!")))
          case _ => todo.find(id)
        }

      def findAll(): F[List[TodoAlgebra.Item]] =
        todo.findAll()

      def item(value: String, due: Option[LocalDate]): TodoAlgebra.Item =
        todo.item(value, due)
    }
}

object Bug extends Enum[Bug] {
  val values = findValues

  case object AppendReturnsWrongId extends Bug
  case object FindAlwaysFails extends Bug
  case object FindReturnsWrongItem extends Bug
}

// These specs need to be non-nested, otherwise sbt can't find them:

object AppendReturnsWrongIdSpec extends BuggyTodoSpec(Bug.AppendReturnsWrongId, new TodoAlgebra.InMemoryTodo)

object FindAlwaysFailsSpec extends BuggyTodoSpec(Bug.FindAlwaysFails, new TodoAlgebra.InMemoryTodo)

object FindReturnsWrongItemSpec extends BuggyTodoSpec(Bug.FindReturnsWrongItem, new TodoAlgebra.InMemoryTodo)

abstract class BuggyTodoSpec(bug: Bug, alg: TodoAlgebra[IO]) extends TodoSpec(bug.entryName, new TodoService(bug(alg)).service)
