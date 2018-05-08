import org.scalacheck._
import org.scalacheck.Prop.forAll

object AbsSpecification extends Properties("Abs") {
  property("non-negative") = forAll { (x: Int) => Abs.abs(x) >= 0 }
}
