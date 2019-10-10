## ScalaCheck Test Scaffolding

We've almost finished our initial tour of ScalaCheck. All that remains is to see the scaffolding that ScalaCheck provides to define test cases that we can run from our build tool.

There are two concepts here, that unfortunately share the same name in ScalaCheck: the `Properties` class, which declares a collection of tests, and the `property` method in `Properties`, which associates a name to a property and makes it runnable as a test.

To disambiguate these concepts let's introduce some terminology: we'll call a *test suite* a collection of tests, and a *test case* the smallest testable unit. Then:

- we declare test suites by extending `Properties`; and
- within a test suite we create test cases by using the `property` method to associate a name to a property.

That's about it. Unlike most of test frameworks there isn't a lot of ceremony in ScalaCheck.

Here's a complete example. You can run this within `sbt` in the usual way: put the file somewhere under the `src/test/scala` directory hierarchy, include the correct dependencies, and run the `test` command.

```scala mdoc:silent:
import org.scalacheck._
import org.scalacheck.Prop.forAll

object AdditionSpecification extends Properties("Addition") {
  property("identity") = forAll { (x: Int) => x + 0 == 0 }

  property("associativity") = forAll { (x: Int, y: Int, z: Int) =>
    (x + y) + z == x + (y + z)
  }

  property("commutativity") = forAll { (x: Int, y: Int) =>
    x + y == y + x
  }
}
```
