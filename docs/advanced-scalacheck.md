# Advanced ScalaCheck

In this section we discuss some issues that go beyond the basics of ScalaCheck usage.


## Discarding

We've seen how we can express preconditions by filtering a generator. This can have some issues. Consider the following example, creating a generator to produce strings beginning with "henry".

```scala mdoc:silent
import org.scalacheck._

val henry = Gen.alphaStr.filter(_.startsWith("henry"))
```

When we run a test using this generator we see that no test data was generated.

```scala mdoc
import org.scalacheck.Prop.forAll

val startsWithHenry = forAll(henry){ (s: String) => s.startsWith("henry") }

Test.check(Test.Parameters.default, startsWithHenry)
```

The result tells us that ScalaCheck is `Exhausted`, meaning it couldn't generate any samples that met our preconditions. This is the danger of using preconditions---they aren't very efficient. It's more efficient to generate only valid samples.

For the above example we could easily generate conforming strings with the following generator.

```scala mdoc:silent:reset
import org.scalacheck._
import org.scalacheck.Prop.forAll
```

```scala mdoc:silent
val henry = Gen.alphaStr.map(s => "henry" ++ s)
```

Now the test runs successfully.

```scala mdoc
val startsWithHenry = forAll(henry){ (s: String) => s.startsWith("henry") }

Test.check(Test.Parameters.default, startsWithHenry)
```


## Implication

There is another way to specify preconditions in ScalaCheck, which we have not seen so far. Instead of specifying a precondition as a filter on a generator we can instead specify it on a property, using the "implication" operator `==>`. The following code shows these two alternate ways in a test for invertability of `Int`.

```scala mdoc:silent
// Filter the generator
// In this simple case we could also write 
//  Gen.choose(Int.MinValue + 1, Int.MaxValue)
val nonMinValue: Gen[Int] = 
  Gen.choose(Int.MinValue, Int.MaxValue).suchThat(_ > Int.MinValue)

forAll(nonMinValue) { (x: Int) => x + -x == 0 }

// Alternate implementation putting the precondition on the property
//
// We need to import Prop._ to get into scope the implicit that adds the ==> syntax
import org.scalacheck.Prop._

forAll { (x: Int) => (x > Int.MinValue) ==> (x + -x == 0) }
```

Although using `==>` may be aesthetically pleasing, I find I don't use it in my tests. The reason being I often find I need to construct custom generators for my tests for other reasons (e.g. efficiency, as discussed above) and using just this single technique keeps the code base more consistent compared to switching between implication and custom generators on a case-by-case basis.


## Shrinking

When ScalaCheck finds a failing property it will attempt to find a minimal example that causes it to fail, a process known as *shrinking*.

Returning to our example of strings that start with "henry". Imagine we have the following generator, and are testing it with an incorrect property.

```scala mdoc:silent:reset
import org.scalacheck._
import org.scalacheck.Prop.forAll
```
```scala mdoc:silent
val henry = Gen.alphaStr.map(s => "henry" ++ s)
val startsWithHenry = forAll(henry){ (s: String) => s.startsWith("harry") }
```

If we run this test it will, as expected, fail. I've cleaned up the output below so it is a bit easier to read.

```scala
Test.check(Test.Parameters.default, startsWithHenry)
// res0: org.scalacheck.Test.Result =
//  Result(Failed(List(Arg(,,3,henry,<elided>,<elided>)),Set()),0,0,Map(),34)
```

The important bit is the value `Arg(,,3,henry,<elided>,<elided>)`. The parameters to this case class tell us, in order:

- there was no label attached to the property (we'll learn about labels in a moment);
- the property failed on test data that was the empty string;
- the test data was shrunk three times; and
- the original test data was the string "henry".

Now the important bit: notice that the shrunk test data, the empty string, is *not* a value the generator can produce! Our generator *cannot* produce the empty string---the smallest string it can create is "henry"! 

ScalaCheck will happily generate invalid test data during shrinking and then report this as if it was a valid error. Shrinking is hopelessly broken and this behaviour is absolutely infuriating in complex test cases. 

Luckily we can turn off shrinking. One way to avoid it is to use `Prop.forAllNoShrink` instead of `Prop.forAll`. 

```scala mdoc:silent:reset
import org.scalacheck._
import org.scalacheck.Prop.forAll
```
```scala mdoc:silent
val henry = Gen.alphaStr.map(s => "henry" ++ s)
val startsWithHenry = Prop.forAllNoShrink(henry){ (s: String) => s.startsWith("harry") }
```

```scala mdoc
Test.check(Test.Parameters.default, startsWithHenry)
```

Notice that no shrinking is done.


Another way to disable shrinking is to create an implicit instance of `Shrink` that does nothing for the type in question---in this case `String`.

```scala mdoc:silent:reset
import org.scalacheck._
import org.scalacheck.Prop.forAll
```
```scala mdoc:silent
implicit val noShrink: Shrink[String] = Shrink.shrinkAny

val henry = Gen.alphaStr.map(s => "henry" ++ s)
val startsWithHenry = forAll(henry){ (s: String) => s.startsWith("harry") }
```
- 
```scala mdoc
Test.check(Test.Parameters.default, startsWithHenry)
```

Once again no shrinking is done.

Working out which type is being shrunk might be difficult, so we can turn off *all* shrinking with this implicit.

```scala mdoc:silent
implicit def noShrinkAll: Shrink[Any] = Shrink.shrinkAny
```
- 
```scala mdoc
Test.check(Test.Parameters.default, startsWithHenry)
```

Finally, we can get shrinking to work correctly by adding preconditions to any generators that restrict their output through other means.

```scala mdoc:silent:reset
import org.scalacheck._
import org.scalacheck.Prop.forAll
```
```scala mdoc:silent
val henry = Gen.alphaStr.map(s => "henry" ++ s).suchThat(_.startsWith("henry"))
val startsWithHenry = forAll(henry){ (s: String) => s.startsWith("harry") }
```

```scala mdoc
Test.check(Test.Parameters.default, startsWithHenry)
```

I tend to use the `implicit def` option to turn off shrinking in an entire file.

Labels and other stupid symbolic operators

Generating recursive data. Bias. Unbiased generation?

Properties of different distributions?

Setup and teardown?

Reusing properties?

Organising tests?

`Gen`erating functions?
