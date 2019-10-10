## Properties: Asserting Invariants

For the example of the *invertibility* of addition--for every `x` there is an
integer `-x` such that `x + -x = 0`--we are expressing a *universally
quantified* property: the assertion should be true for *all*
`Int` values.

In ScalaCheck, we express universally quantified properties with the
`Prop.forAll` method:

```scala mdoc:silent
import org.scalacheck.Prop.forAll

val invertibility = forAll { (x: Int) => x + -x == 0 }
```

(We often `import` the `Prop.forAll` method directly into scope, for brevity.)

The function `forAll` is a higher-order function that transforms another
function that makes an assertion into a `Prop`, ScalaCheck's representation of
a property. Here we passed an anonymous function that takes an `Int` named
`x`, and it asserts that adding `x` to its inverse should always equal zero.

Let's reformat and annotate the expression to highlight its parts:

```scala mdoc:silent:reset
import org.scalacheck._
import org.scalacheck.Prop.forAll
```
```scala mdoc:silent
val invertibility =     // the property
  forAll { (x: Int) =>  // universal quantification over a type
    x + -x == 0         // the assertion
  }
```

Let's test our property:

```scala mdoc
import org.scalacheck._

Test.check(Test.Parameters.default, invertibility)
```

(You can use `Test.check` in the console; later we will detail the usual practice
of running the property tests via `sbt`)

You probably saw output similar to

```scala
// res0: org.scalacheck.Test.Result = Result(Passed,100,0,Map(),101)
```

The `Result` represents the result of ScalaCheck running the test. The first
field, with value `Passed`, tells us that the test passed. The next parameter,
`100`, is interesting. This tells us how many test cases ScalaCheck generated
for us. A third field, `0`, is the number of test cases that were generated but
discarded because they didn't meet a precondition. Being zero it indicates that
no cases were discarded. Complex preconditions can cause many cases to be
discarded, which in turn can lead to test cases that take a long time to run or
ScalaCheck giving up on generating cases. The final two fields are not
particularly important for our situation.


*TODO: Exercises*
