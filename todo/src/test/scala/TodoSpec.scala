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
import org.http4s.headers.Location
import org.http4s.implicits._
import org.scalacheck._
import org.scalacheck.Prop._

object CorrectTodoSpec extends TodoSpec("Correct", new TodoService(new TodoAlgebra.InMemoryTodo[IO]).service)

abstract class TodoSpec(name: String, service: HttpService[IO]) extends Properties(s"TodoService.$name") {

  import TodoRequest._

  property("GET /todos returns 200") =
    Trace.run(TodoRequest.GetTodos.toTracedRequest, service)
      .response
      .status == Status.Ok

  property("GET /todos returns JSON []") =
    Trace.run(TodoRequest.GetTodos.toTracedRequest, service)
      .entity[Json]
      .unsafeRunSync == Json.arr()

  property("read your writes") =
    forAll(genPostTodo) { (post: TodoRequest.PostTodo) =>
      val trace = Trace.run(Trace.runAndFollowLocation(Trace.lift(post.toRequest)), service)
      val entity = trace.entity[TodoRequest.PostTodo].unsafeRunSync

      val statusIsOk = s"\tresponse.status: ${trace.response.status} != ${Status.Ok}" |: trace.response.status == Status.Ok
      val readEntityMatchesWritten = s"\tentity: $entity != ${Right(post)}" |: entity == Right(post)

      trace.showLog |: statusIsOk && readEntityMatchesWritten
    }

  // TODO: properties for DELETE /todos/{id}

  val genPostTodo: Gen[TodoRequest.PostTodo] =
    for {
      value <- Gen.alphaStr
      due <- Gen.option(Gen.choose(0L, 365L * 50) map (LocalDate.ofEpochDay))
    } yield TodoRequest.PostTodo(value, due)
}

/** Algebraic data type representing the various kinds of requests we can make to a `TodoService`. */
sealed trait TodoRequest {

  def toTracedRequest: Trace.M[IO, Response[IO]] =
    Trace.lift(toRequest)

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

case class Trace(response: Response[IO], log: Trace.Log[IO]) {

  def entity[A](implicit decoder: EntityDecoder[IO, A]): IO[Either[Throwable, A]] =
    response.as[A].attempt

  def showLog: String = Trace.Log.show(log)
}

object Trace {

  /** We log the request/response pairs. */
  type Log[F[_]] = List[(Request[F], Response[F])]

  object Log {
    // Pretty-print the log.
    def show[F[_]](log: Log[F]): String =
      ("\trequest/response sequence:" :: log.flatMap { case (req, res) => List(s"\t>>> $req", s"\t<<< $res") })
        .mkString("\n")
  }

  /** Computation that requires a `HttpService` and also logs the request and response.
    * We keep no other state (it is type `Unit`). */
  type M[F[_], A] = ReaderWriterState[HttpService[F], Trace.Log[F], Unit, A]

  def lift(request: Request[IO]): M[IO, Response[IO]] =
    for {
      service <- ReaderWriterState.ask[HttpService[IO], Trace.Log[IO], Unit]
      response = service.orNotFound(request).unsafeRunSync()
      _ <- ReaderWriterState.tell(List(request -> response))
    } yield response

  def run(request: M[IO, Response[IO]], service: HttpService[IO]): Trace = {
    val (log, _, getResponse) = request.runEmpty(service).value

    Trace(getResponse, log)
  }

  def runAndFollowLocation(request: M[IO, Response[IO]]): M[IO, Response[IO]] =
    for {
      response <- request

      location = response.headers.get(headers.Location).get // TODO: don't just blow up

      // TODO: assert that returned Location matches the /todos/{id} endpoint, maybe URI Template thing

      // actually fetch the content at the Location URI to test the response
      getRequest = Request[IO](Method.GET, location.uri)
      getResponse <- lift(getRequest)
    } yield getResponse
}