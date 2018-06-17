# Extended Exercise: TODO web application

We've created an example web application, complete with bugs, for you to test.
It's a simple "TODO" application, where a user can add new TODO items, view
their list of TODOs, and mark a TODO as done.

## *Notes to Instructor*

Outline of extended exercise:

- Get familiar with TODO app behavior
  - expected request/response of HTTP endpoints
  - *Exercise*: execute requests in the REPL
  - *Exercise*: run the service locally, send requests via `curl`
  - *Exercise*: properties brainstorm
    - what properties can you think of, based on the existing patterns (associativity, invertibility, etc.)?
- Get familiar with the code
  - `TodoService`: the `http4s` routes
    - pattern match to extract requests
    - decoding form data with `UrlForm`
    - invoke algebra
    - return response
  - `TodoAlgebra`: interface for the business logic (*details useful only if students are advanced*)
    - explain what `F[_]` is
    - `InMemoryTodo` implementation
  - `TodoSpec`: our properties checked against an algebra
    - Uses `TodoAlgebra[IO]`: why we use `IO` as our testing effect
    - `TodoRequest`: ADT with `toRequest: Request[IO]` method so we can write generators of requests
    - `run` method: returns `Http4sTest` RWST monad
      - we need to use the same `HttpService[IO]` for all requests made in a given test, since the service is stateful/mutable
      - We record the requests and responses as a `List[Log]` in the writer part of `Http4sTest` monad, and hook the log up to properties via `|:` so failing tests can show what requests were run.
- Mechanics of the exercise
  - We will re-use the `TodoSpec` with different `TodoAlgebra` *instances*.
  - The `TodoAlgebra.InMemoryTodo` is the "correct" implementation, and we will inject various bugs with other instances.
  - New properties should be added to `TodoSpec`.
- *Exercises*
  - test `DELETE` endpoint using the pattern from `"read your writes"`
  - explore injecting bugs via `Bugs`: what breaks? what doesn't break?
  - inject more bug types via `Bugs`
  - Implement and test authentication (see section below)
  - Implement and test idempotent posts (see section below)
  - Implement and test pagination (see section below)
  - Implement and test filtering (see section below)


## Endpoints

First let's understand the application's expected behavior via HTTP.

We can add a new TODO item to our list:

```
POST /todos?value=get+milk&due=2018-05-13 HTTP/1.1
Host: localhost:8080

HTTP/1.1 201 Created
Location: /todos/68
```

We can get a particular item by id:

```
GET /todos/68
Host: localhost:8080

HTTP/1.1 200 OK
Content-Type: application/json; charset=UTF-8
...

{
  "value": "get milk"
  "due": "2018-05-13"
}
```

We get all our items:

```
GET /todos
Host: localhost:8080

HTTP/1.1 200 OK
Content-Type: application/json; charset=UTF-8
...

[
  {
    "value": "get milk"
    "due": "2018-05-13"
  },
  {
    "value": "get cookies"
    "due": "2018-05-12"
  },
  ...
]
```

We can mark an item as done (by `DELETE`ing it):

```
DELETE /todos/68
Host: localhost:8080

HTTP/1.1 204 No Content
```

Dates are [ISO 8601](https://en.wikipedia.org/wiki/ISO_8601#Dates) formatted (`YYYY-MM-DD`).

### `TodoService.scala`

Here's an example of making requests and getting (non-buggy) responses:

```tut:silent:book
import cats.effect._
import io.circe.Json
import io.underscore.testing.todo._
import java.time.LocalDate
import org.http4s._
import org.http4s.circe._
import org.http4s.implicits._
```

```tut:book
val service = new TodoService(new TodoAlgebra.InMemoryTodo[IO]).service

val post = TodoRequest.PostTodo("get milk", Some(LocalDate.of(2018, 5, 18))).toRequest

// Alternatively, you can "manually" create a http4s request like this:
//val post = Request[IO](Method.POST, Uri.uri("/todos")).withBody(UrlForm(Map("value" -> Seq("get milk"), "due" -> Seq("2018-05-13")))).unsafeRunSync()

service.orNotFound(post).unsafeRunSync()

val get =
  for {
    response <- service.orNotFound(TodoRequest.GetTodos.toRequest)
    json <- response.as[Json]
  } yield (response, json)

get.unsafeRunSync()
```

Alternatively you can actually run the service and interact with it via your browser or `curl`:

```
$ sbt ~todo/reStart
... a bunch of sbt messages ...
todo Starting io.underscore.testing.todo.TodoServer.main()
[success] Total time: 8 s, completed May 12, 2018 6:18:20 PM
1. Waiting for source changes... (press enter to interrupt)
todo [main] INFO  o.h.b.c.n.NIO1SocketServerGroup - Service bound to address /0:0:0:0:0:0:0:0:8080
todo [main] INFO  o.h.s.b.BlazeBuilder -   _   _   _        _ _
todo [main] INFO  o.h.s.b.BlazeBuilder -  | |_| |_| |_ _ __| | | ___
todo [main] INFO  o.h.s.b.BlazeBuilder -  | ' \  _|  _| '_ \_  _(_-<
todo [main] INFO  o.h.s.b.BlazeBuilder -  |_||_\__|\__| .__/ |_|/__/
todo [main] INFO  o.h.s.b.BlazeBuilder -              |_|
todo [main] INFO  o.h.s.b.BlazeBuilder - http4s v0.18.11 on blaze v0.12.13 started at http://[0:0:0:0:0:0:0:0]:8080/
```

(The `~todo/reStart` argument to sbt reloads the server whenever the source is changed, in case you
want to interactively develop. It uses the [`sbt-revolver`](https://github.com/spray/sbt-revolver) plugin.)

Then in another terminal:

```
$ curl -v -d value=get+milk -d due=2018-05-13 http://localhost:8080/todos
> POST /todos HTTP/1.1
> Host: localhost:8080
> User-Agent: curl/7.54.0
> Accept: */*
> Content-Length: 29
> Content-Type: application/x-www-form-urlencoded
>

< HTTP/1.1 201 Created
< Location: /todos/1
< Date: Sun, 13 May 2018 01:20:34 GMT
< Content-Length: 0
<

$ curl -v http://localhost:8080/todos
> GET /todos HTTP/1.1
> Host: localhost:8080
> User-Agent: curl/7.54.0
> Accept: */*
>
< HTTP/1.1 200 OK
< Content-Type: application/json
< Date: Sun, 13 May 2018 01:21:09 GMT
< Content-Length: 41
<
[{"value":"get milk","due":"2018-05-13"}]
```

## Property: Request Validation

* `POST /todos?value=get+milk&due=2018-05-13`
  - `value` parameter is required
  - `due` parameter is optional and must be a ISO 8601 date


## Property: Read Your Writes

* `POST /todos` returns `201 Created`, `GET <Host><Location>` returns correct JSON for initial request parameters

## Property: Authentication

Authentication is implication:

* `Authentication` header present and value is valid

`403 Forbidden`

See also: [Github API: Authentication](https://developer.github.com/v3/#authentication)


## Property: Idempotent POSTs

More than one `POST /todos` request with the same `Idempotency-Key` header
will always return the same `Location` header.

The same key can't be reused with different parameters.

References:

- [Designing robust and predictable APIs with idempotency](https://stripe.com/blog/idempotency)
- [Idempotent Requests](https://stripe.com/docs/api#idempotent_requests) from Stripe
- [What are idempotent and/or safe methods?](http://restcookbook.com/HTTP%20Methods/idempotency/)
- [When should we use PUT and when should we use POST?](http://restcookbook.com/HTTP%20Methods/put-vs-post/)


## Property: Pagination

* `page`: optional, must be > 0, defaults to 1
* `per_page`: optional, must be > 0, defaults to 30

Invariants:

* Given n items, a response will contain `min(n, per_page)` items
* Responses will contain a `first` link if `page` > 1
* Responses will contain a `last` link if ...

See also [GitHub API: Pagination](https://developer.github.com/v3/#pagination).


## Property: Filtering

* `due_by=<ISO8601-date>` query parameter: return only those items who have a `due` property <= `due_by`