package io.underscore.testing.todo

import cats._
import cats.implicits._
import io.circe.Encoder
import io.circe.generic.semiauto._
import io.circe.java8.time._
import java.time.LocalDate

trait TodoAlgebra[F[_]] {

  type Item
  type ItemId = Long

  /** Construct an `Item`. */
  def item(value: String, due: Option[LocalDate]): Item

  /** Append an `Item` to the list. */
  def append(item: Item): F[ItemId]

  /** Find all the `Item`s in the list. */
  def findAll(): F[List[Item]]

  /** Find an `Item` by its `ItemId`. */
  def find(id: ItemId): F[Option[Item]]
}

object TodoAlgebra {

  type Aux[F[_], Item0] = TodoAlgebra[F] { type Item = Item0 }

  case class Item(value: String, due: Option[LocalDate])

  object Item {
    implicit def encoder: Encoder[Item] = deriveEncoder[Item]
  }

  class InMemoryTodo[F[_] : Applicative] extends TodoAlgebra[F] {

    type Item = TodoAlgebra.Item

    private var items: List[Item] = List.empty

    def item(value: String, due: Option[LocalDate]): Item =
      Item(value, due)

    def append(item: Item): F[ItemId] =
      Applicative[F].pure {
        items = items :+ item
        items.length.toLong
      }

    def findAll(): F[List[Item]] =
      Applicative[F].pure(items)

    def find(id: ItemId): F[Option[Item]] =
      Applicative[F].pure(items.lift(id.toLong.toInt - 1))
  }
}

object Bugs {
  class ReturnsWrongLocation[F[_] : Applicative] extends TodoAlgebra.InMemoryTodo[F] {
    override def append(item: Item): F[ItemId] =
      super.append(item) map (_ + 5)
  }
}

