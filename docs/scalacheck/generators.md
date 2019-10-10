## Generators: Producing Values to Test

When we create a `Prop` using `forAll`, we give it our function that
asserts the invariant on its input. Where does this input value come from?
These inputs come from *generators*, named `Gen` in ScalaCheck:

```scala
// (edited for brevity)
sealed abstract class Gen[+T] {
  def sample: Option[T]
}
```

At its heart, `Gen` produces sample values of a given type. The result of
`sample` is an `Option[T]` because, as we've just seen above, the generator may
fail to generate a sample.


ScalaCheck comes with many, many generators out-of-the-box, which you'll find on
the `Gen` companion object.

```scala mdoc:silent
import org.scalacheck.Gen

Gen.alphaStr    // `String` values like "abc", etc.
Gen.numStr      // numeric `String` values: "123", etc.
Gen.posNum[Int] // positive numbers: 42, etc.
Gen.const(1)    // always generates the given value

Gen.choose(0, 9)         // pick any number in a range, inclusive
Gen.oneOf('a', 'b', 'c') // pick any of these values
```

There are many more.

Let's `sample` some values from these generators:

```scala mdoc:silent
// A useful utility to generate samples from a Gen
def sample[A](n: Int)(gen: Gen[A]): List[Option[A]] =
  List.fill(n)(gen.sample)
```

```scala mdoc
sample(3)(Gen.alphaStr)
sample(3)(Gen.numStr)
sample(3)(Gen.posNum[Int])
sample(3)(Gen.const(1))
sample(3)(Gen.choose(0, 9))
sample(3)(Gen.oneOf('a', 'b', 'c'))
```

## Generator combinators

There are many ways to combine generators to create new ones.

Generators are monads, meaning they have `map` and `flatMap` methods.
(`Gen.const` is the `pure` method that rounds out the monad interface.) We can
use these in the usual way to transform generators.


For example, we can generate the even positive integers by `mapping` the
positive integers.

```scala mdoc
// Generate even positive integers
sample(3)(Gen.posNum[Int].map(x => x * 2))
```

With `flatMap` we can do more complicated transforms, such as using our input to
choose a generator.


```scala mdoc
sample(3)(Gen.choose(0,2).flatMap{ 
  case 0 => Gen.alphaStr
  case 1 => Gen.numStr
  case _ => Gen.const("Hi!")
})
```

These kind of switching is so common that ScalaCheck provides a number of
combinators to make it easy. We've already seen `oneOf` above. It also works
with generators, so we could have also written

```scala mdoc
sample(3)(Gen.oneOf(Gen.alphaStr, Gen.numStr, Gen.const("Hi!")))
```

Using `oneOf` each generator is chosen with equal probability. For more
complicated choices we can use `frequency`, where we give each generator a
weight. Generators are chosen in proportion to their weight.

```scala mdoc
// generate a 'b' twice as much as an 'a', a 'c' three times as much
sample(3)(Gen.frequency(1 -> 'a', 2 -> 'b', 3 -> 'c'))
```

Another common use case is to generate a container of a certain size. The
`containerOf` and `containerOfN` methods do this.

```scala mdoc
// generate a container whose elements come from another `Gen`
List.fill(3)(Gen.containerOf[List, Int](Gen.oneOf(1, 3, 5)).sample)

// generate a fixed-size container whose elements come from another `Gen`
List.fill(3)(Gen.containerOfN[Seq, String](3, Gen.alphaStr).sample)
```


## Expressing Preconditions

The final methods on `Gen` that we will look at are those for expressing
preconditions. The main method is `suchThat`, or it's direct synonym
`filter`.

Above we expressed even positive integers as

```scala mdoc
// Generate even positive integers
sample(3)(Gen.posNum[Int].map(x => x * 2))
```

We could alternatively express it as

```scala mdoc:silent:
sample(3)(Gen.posNum[Int].suchThat(_ % 2 == 0))
// The exact equivalent using filter
sample(3)(Gen.posNum[Int].filter(_ % 2 == 0))
```

What's the difference between the two? Think about this before you read on.

The first way of defining the even positive integers suffers from overflow, and can end up generating negative numbers! The second ends up filtering out half of the numbers generated from `Gen.psNum`, so it is less efficient (though correct!)



### Exercises

Now we've explored the basics of `Gen`, it's time for you to get some practice using the API.

#### Finger Exercisess

These exercises are all about learning to use the API effectively.

- Generate a `String` of lowercase alphabetic characters
- Generate a `String` of numeric characters
- Generate integers between 100 and 200, inclusive
- Generate strings that start with one of "a", "b", or "c"
- Generate odd integers
- Generate a list of between 3 and 5 positive integers

#### More Complicated Situations

Create a `Gen[User]`, where `User` is

```scala mdoc:silent:
final case class User(name: String, age: Int, email: String)
```

Make justifiable decisions for the choice of generators for the fields.


