package io.underscore.testing.todo

import cats._
import cats.data._
import cats.effect.IO
import cats.implicits._
import io.circe._
import io.circe.generic.semiauto._
import io.circe.java8.time._
import io.circe.syntax._
import java.time.LocalDate
import org.http4s._
import org.http4s.circe._
import org.http4s.implicits._
import org.scalacheck._
import org.scalacheck.Prop._

object CorrectTodoSpec extends TodoSpec("Correct", new TodoService(new TodoAlgebra.InMemoryTodo[IO]).service)

abstract class TodoSpec(name: String, service: HttpService[IO]) extends Properties(s"TodoService.$name") {

  import TodoRequest._

  property("GET /todos returns 200") =
    run(TodoRequest.GetTodos.toRequest)
      .runEmptyA(service)
      .value
      .status == Status.Ok

  property("GET /todos returns JSON []") =
    run(TodoRequest.GetTodos.toRequest)
      .runEmptyA(service)
      .value
      .as[Json].unsafeRunSync() == Json.arr()

  property("read your writes") =
    forAll(genPostTodo) { (post: TodoRequest.PostTodo) =>
      val r =
        for {
          postResponse <- run(post.toRequest)

          location = postResponse.headers.get(headers.Location).get.uri // TODO: don't just blow up

          // TODO: assert that returned Location matches the /todos/{id} endpoint, maybe URI Template thing

          // actually fetch the content at the Location URI to test the response
          getResponse <- run(Request[IO](Method.GET, location))
        } yield getResponse

      val (log, _, getResponse) = r.runEmpty(service).value
      val entity = getResponse.as[TodoRequest.PostTodo].attempt.unsafeRunSync()

      val statusIsOk = s"\tgetResponse.status: ${getResponse.status} != ${Status.Ok}" |: getResponse.status == Status.Ok
      val readEntityMatchesWritten = s"\tentity: $entity != ${Right(post)}" |: entity == Right(post)

      Log.show(log) |: statusIsOk && readEntityMatchesWritten
    }

  // TODO: properties for DELETE /todos/{id}

  /** Computation that requires a `HttpService` and also logs the request and response.
    * We keep no other state (it is type `Unit`). */
  type Http4sTest[F[_], A] = ReaderWriterState[HttpService[F], Log[F], Unit, A]

  /** We log the request/response pairs. */
  type Log[F[_]] = List[(Request[F], Response[F])]

  object Log {
    // Pretty-print the log.
    def show[F[_]](log: Log[F]): String =
      ("\trequest/response sequence:" :: log.flatMap { case (req, res) => List(s"\t>>> $req", s"\t<<< $res") })
        .mkString("\n")
  }

  def run(request: Request[IO]): Http4sTest[IO, Response[IO]] =
    for {
      service <- ReaderWriterState.ask[HttpService[IO], Log[IO], Unit]
      response = service.orNotFound(request).unsafeRunSync()
      _ <- ReaderWriterState.tell(List(request -> response))
    } yield response

  val genPostTodo: Gen[TodoRequest.PostTodo] =
    for {
      value <- Gen.alphaStr
      due <- Gen.option(Gen.choose(0L, 365L * 50) map (LocalDate.ofEpochDay))
    } yield TodoRequest.PostTodo(value, due)
}

/** Algebraic data type representing the various kinds of requests we can make to a `TodoService`. */
sealed trait TodoRequest {
  def toRequest: Request[IO] =
    this match {
      case TodoRequest.PostTodo(value, Some(due)) =>
        Request[IO](Method.POST, Uri.uri("/todos"))
          .withBody(UrlForm(Map("value" -> Seq(value), "due" -> Seq(due.toString))))
          .unsafeRunSync()

      case TodoRequest.PostTodo(value, None) =>
        Request[IO](Method.POST, Uri.uri("/todos"))
          .withBody(UrlForm(Map("value" -> Seq(value))))
          .unsafeRunSync()

      case TodoRequest.GetTodos =>
        Request[IO](Method.GET, Uri.uri("/todos"))

      case TodoRequest.GetTodo(id) =>
        Request[IO](Method.GET, Uri.uri("/todos") / id.toString)
    }
}

object TodoRequest {
  case class PostTodo(value: String, due: Option[LocalDate]) extends TodoRequest
  case object GetTodos extends TodoRequest
  case class GetTodo(id: Long) extends TodoRequest

  implicit def postDecoder: Decoder[PostTodo] = deriveDecoder[PostTodo]
  implicit val postEntityDecoder: EntityDecoder[IO, PostTodo] = jsonOf[IO, TodoRequest.PostTodo]
}