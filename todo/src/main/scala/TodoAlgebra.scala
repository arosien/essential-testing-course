package io.underscore.testing.todo

import cats._
import cats.implicits._
import io.circe.Encoder
import io.circe.generic.semiauto._
import io.circe.java8.time._
import java.time.LocalDate

trait TodoAlgebra[F[_]] {

  /** Construct an `Item`. */
  def item(value: String, due: Option[LocalDate]): TodoAlgebra.Item

  /** Append an `Item` to the list. */
  def append(item: TodoAlgebra.Item): F[TodoAlgebra.ItemId]

  /** Find all the `Item`s in the list. */
  def findAll(): F[List[TodoAlgebra.Item]]

  /** Find an `Item` by its `ItemId`. */
  def find(id: TodoAlgebra.ItemId): F[Option[TodoAlgebra.Item]]

  /** Complete an `Item` with a given `ItemId`. */
  def complete(id: TodoAlgebra.ItemId): F[Unit]
}

object TodoAlgebra {

  case class Item(value: String, due: Option[LocalDate])

  object Item {
    implicit def encoder: Encoder[Item] = deriveEncoder[Item]
  }

  type ItemId = Long

  class InMemoryTodo[F[_] : Applicative] extends TodoAlgebra[F] {

    /* Non-complete items are `Some`, completed items are `None`,
     * so we don't break the Itemid <-> List index invariant. */
    private var items: List[Option[Item]] = List.empty

    def item(value: String, due: Option[LocalDate]): Item =
      Item(value, due)

    def append(item: Item): F[ItemId] =
      Applicative[F].pure {
        items = items :+ Some(item)
        items.length.toLong
      }

    def findAll(): F[List[Item]] =
      Applicative[F].pure(items collect { case Some(item) => item })

    def find(id: ItemId): F[Option[Item]] =
      Applicative[F].pure(items.lift(id.toLong.toInt - 1).flatten)

    def complete(id: ItemId): F[Unit] =
      Applicative[F].pure {
        val index = id.toLong.toInt - 1

        if (items.isDefinedAt(index)) items = items.updated(index, None)

        ()
      }
  }
}
