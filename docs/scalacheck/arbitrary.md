## Arbitrary

We've learned that generators create data for our tests, but so far we haven't actually used them. When we run a property like

```scala mdoc:silent
import org.scalacheck.Prop.forAll
import org.scalacheck.Gen

val invertability = forAll { (x: Int) => x + -x == 0 }
```

ScalaCheck somehow magically creates a generator for us. How does this work? In this section we learn about `Arbitrary`, which is a type class based approach to creating generators.

The `Arbitrary` type is very simple. It's entire definition is

```scala
sealed abstract class Arbitrary[A] {
  def arbitrary: Gen[A]
}
```

As you can see instances of `Arbitrary[A]` just wrap a `Gen[A]`.

The secret sauce that makes `forAll` "just work" is that it has an implicit `Arbitrary` parameter. When there is an `Arbitrary` instance in the implicit scope the compiler will provide that instaces for us, and ScalaCheck will use it to generate values for testing. When there is no instance available we get an error, as shown below.

```scala mdoc:silent
trait Foo
```
```scala mdoc:fail:
forAll( (x: Foo) => x == x)
```


### Arbitrary and Gen

A few questions probably come to mind:

- how do we write a property that uses a generator that is not an `Arbitrary` instance?
- of all the possible generators for a given type, which one shold be used for `Arbitrary`?
- how do we construct `Arbitrary` instances from generators?
- why do we have two concepts that are so similar, and which should we prefer?

Let's take them one by one.

#### Explicit Generators

We can write properties that use a given generator by calling an alternate form of the `forAll` method that takes an explicit `Gen` parameter. The `Gen` instances are always the first parameter list to `forAll`.

Let's say we wanted to test the property that adding two odd integers is always even. Wecould write this by first defining a `Gen[Int]` that constucts only odd numbers. Do this before you read on.

```scala mdoc:silent:
val odd: Gen[Int] = Gen.choose(Int.MinValue, Int.MaxValue).suchThat(_ % 2 == 1)
```

Now reate a property where we explictly pass generators to `forAll`. Try this yourself now.

```scala mdoc:silent
forAll(odd, odd){ (x: Int, y: Int) => (x + y) % 2 == 0 }
```

This main trick here is that we can to pass two instances of `odd`, as the function we're testing has two parameters.


#### Which Generator is Arbitrary?
 
Now let's address the question of which generator is used for an `Arbitrary` instance? What do you think the correct chioce is?

The answer is that the `Arbitrary` generator should cover the entire set of possible values for the type. So the `Arbitrary[Int]` instance should generate any possible `Int`, the `Arbitrary[String]` instance should generate an possible `String`, and so on. This ties back to the role of types in testing.


#### Constructing Arbitrary Instances from Generators

To construct an `Arbitrary` instance from a `Gen` we just call the `apply` method on the `Arbitrary` companion object.

```scala mdoc:silent:
import org.scalacheck.Arbitrary

val arbitraryOdd = Arbitrary(odd)
```

Type class composition works in the usual way. Given `Arbitrary` instances for base types the compiler will construct types for tuples and so on. Here's a quick example of constructing a tuple instance. You wouldn't normally write this explicitly in your code.

```scala mdoc
val tupleArbitrary: Arbitrary[(Int, Int)] = implicitly
```


#### Generators vs Arbitrary

Given there are concepts in ScalaCheck that are basically the same, which should we prefer? What do you think?

In an ideal world our types would always define exactly the set of values that our properties accept. Unfortunately this is not the world we live in, and it is fairly common for types to only weakly constrain the set of possible values. For example, Scala has no unsigned integer type and I have yet to see a codebase that defines its own instead of using `Int`. In this world we have to use generators.

Given the potential confusion for developers (should I use an explicit generator or implicit `Arbitrary` instance here?) I have defaulted to always using explicit generators.

_Exercises here?_
