# Essential Testing: Introduction

## A Testing Challenge

Below is the definition of the absolute value function defined on `Int`.

```tut:silent:book
def abs(x: Int): Int =
  if (x < 0) -x else x
```

What invariants do you think should hold for this method? A very simple invariant, which is enforced by the compiler, is that given an `Int` the method returns an `Int`. What else?

How would you go about testing this method? Do you think this method is correct or has a bug?

### Property-based Testing

The main invariant of `abs` is that the output should always be non-negative. There is a subtle bug in the implementation, which arises due to the two's-complement representation of `Int` that the JVM uses. The issue is that the smallest negative `Int`, `Int.MinValue`, cannot be represented as a positive `Int`.

```tut:book:
-Int.MinValue
```

This is fairly subtle point which relies on a lot of background knowledge. There is a good chance you uncovered it in this exercise, as the problem is very small and testing is the focus, but you can imagine how subtleties like this are overlooked in real project. In fact this example comes from a real and widely used project: Apache Kafka. It appears they [first noticed this issue][abs-introduced] in 2012, but their first fix [had a bug][kafka-1469] that was only fixed two years later!

[abs-introduced]: https://github.com/apache/kafka/commit/d1a22b2e3b59c2cf00adabd75d29ddd53938bacb
[kafka-1469]: https://issues.apache.org/jira/browse/KAFKA-1469

How can we do better? The notable thing about `abs` is that coming up with the important invariant is easy, but testing it using traditional techniques is surprisingly hard. With *property-based testing* we do the easy bit, coming up with the invariants, and we delegate the hard bit, generating test cases, to the computer.

Here's the complete source for such a test, written using ScalaCheck. This finds the problem very quickly.

```tut:silent:book
import org.scalacheck._
import org.scalacheck.Prop.forAll

object AbsSpecification extends Properties("Abs") {
  property("non-negative") = forAll { (x: Int) => Abs.abs(x) >= 0 }
}
```

In the above code, we've specified the property we want to hold with the line

```scala
property("non-negative") = forAll { (x: Int) => Abs.abs(x) >= 0 }
```

ScalaCheck takes care of the rest for us.

In this course we'll look at the practical and theoretical side of property-based testing using ScalaCheck. We'll start by practicing defining properties for some relatively simple functions. We'll then move on to exploring more of ScalaCheck's API. With this material under our belt we'll look at more complex and realistic examples. More advanced material on ScalaCheck follows, and we'll then look at ScalaCheck integration in other testing frameworks. We'll finish with a large example testing a web application.
