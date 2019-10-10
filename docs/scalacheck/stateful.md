# Testing Stateful Systems

> For ordinary properties, ScalaCheck generates random input values and evaluates a boolean expression.
>
> For stateful systems, we can let the input be a sequence of commands, and evaluate a post-condition for each command.

- from [Testing Stateful Systems with ScalaCheck](http://scalacheck.org/files/scaladays2014/)

This is a generalization of the technique we used in the testing of the TODO web application.
Instead of only generating random commands, we generate random *sequences* of commands, and
assert our invariants after *every* step. Simultaneously, we *explictly* track, in the test,
any hidden state of the system-under-test, and where appropriate, assert that our calculation
of the state matches the system's value.

Let's take a simple (mutable) counter as an example:

```scala mdoc
class Counter {
  private var n = 0

  def inc()   = n += 1
  def dec()   = n -= 1
  def get()   = n
  def reset() = n = 0
}
```

When we increment, decrement, or reset a counter, we can't see what the new state is. The state
is *hidden*; you can tell by the return type of those functions: they all return `Unit`. Only when
you explicitly get the value of the counter can you know the internal state:

```scala mdoc
val c = new Counter
c.inc
c.inc
c.get
```

How do we know the counter is incremented correctly, no matter what commands are given to it?

*Exercise*: Think about what properties we want to prove, and what state *we* need to keep track of in the test
in order to to make these assertions.

## Writing a Stateful Test

For our counter, we need to:

* Track the expected value of the counter in the test itself.
* Generate, using `Gen`, commands that represent calls to the `inc`, `dec`, `get`, and `reset` methods.
* For each command, we need to call the actual method on the counter, and update our state according to what command was run. For the command that invokes `get`, we need to assert if *our* computed value of the counter matches what the counter outputs.

*See `CounterProperties.scala` for implementation, and to get familiar with the ScalaCheck `Command` types*

To generalize this procedure, to define stateful tests we:

* Define the *type of state* we will track, no matter how the system is tested.
* Generate commands for each operation of the system we want to test.
* For each command, link it to the actual methods of the system, properly update our locally-tracked state, and make assertions using the computed and actual state.

## Exercise: Testing an ATM

Let's think about how we would test an ATM that has this interface:

```scala

trait ATM {
  /** Return true iff the password for the user is correct;
    * that user is then authorized for other requests. */
  def authorize(user: User, pass: Password): Boolean

  /** Return `Left(ATM.Error.Unauthorized)` if not previously authorized. */
  def balance(user: User): Either[ATM.Error, Amount]

  /** Return `Left(ATM.Error.Unauthorized)` if not previously authorized.
    * Return `Left(ATM.Error.NotEnoughFunds)` if the balance is less than 0. */
  def withdraw(user: User, amount: Double): Either[ATM.Error, Amount]
}
```

(See `stateful/ATM.scala`)

*What state is hidden, that our test should track?*

*What pre- or post-conditions should we assert after running any of these commands?*

Work with the stateful test template `stateful/ATMProperties.scala`, get it to compile
by replacing all `???` implementations with real implementations.

*Extra exercise*: Add a `deposit` method and update the tests
