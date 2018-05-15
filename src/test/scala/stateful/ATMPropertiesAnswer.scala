package stateful

import org.scalacheck._
import org.scalacheck.commands.Commands
import scala.util._

object ATMPropertiesAnswer extends Properties("ATM") {
  property("commands") = ATMCommandsAnswer.property()
}

object ATMCommandsAnswer extends Commands {
  import Prop._

  /** The set of *authorized* users. */
  type State = Set[User]

  type Sut = ATM

  def canCreateNewSut(newState: State, initSuts: Traversable[State],
    runningSuts: Traversable[Sut]): Boolean = true

  def initialPreCondition(state: State): Boolean = state.isEmpty

  def newSut(state: State): Sut = new MutableATM(passwords, initialBalances)

  val users = Set(User("a"), User("b"), User("c"))

  val passwords =
    Map(
      User("a") -> Password("a"),
      User("b") -> Password("b"),
      User("c") -> Password("c"))

  val initialBalances =
    Map(
      User("a") -> Amount(12d),
      User("b") -> Amount(1d))

  def destroySut(sut: Sut): Unit = ()

  def genInitialState: Gen[State] = Gen.const(Set.empty)

  def genCommand(state: State): Gen[Command] =
    Gen.oneOf[Command](
      for {
        user <- Gen.oneOf(users.toSeq)
        pass = passwords(user)
      } yield Authorize(user, pass),
      for {
        user <- Gen.oneOf(users.toSeq)
      } yield Balance(user),
      for {
        user <- Gen.oneOf(users.toSeq)
        amount <- Gen.chooseNum[Double](0d, 10d)
      } yield Withdraw(user, amount))

  trait AuthorizationRequired { self: Command =>
    def isAuthorized(user: User, state: State, result: Try[Either[ATM.Error, _]]): Prop =
      result match {
        case Success(Left(ATM.Error.Unauthorized)) => s"$user is authorized but $this returned unauthorized" |: !state.contains(user)
        case Success(Right(_)) => s"$this succeeded but $user hasn't been authorized" |: state.contains(user)
        case _ => true
      }
  }

  case class Authorize(user: User, pass: Password) extends Command {
    type Result = Boolean

    def run(sut: Sut): Boolean = sut.authorize(user, pass)
    def nextState(state: State): State = state + user
    def preCondition(state: State): Boolean = true

    def postCondition(state: State, result: Try[Result]): Prop =
      result == Success(true)
  }

  case class Balance(user: User) extends Command with AuthorizationRequired {
    type Result = Either[ATM.Error, Amount]

    def run(sut: Sut): Result = sut.balance(user)
    def nextState(state: State): State = state
    def preCondition(state: State): Boolean = true
    def postCondition(state: State, result: Try[Result]): Prop =
      isAuthorized(user, state, result)
  }

  case class Withdraw(user: User, amount: Double) extends Command with AuthorizationRequired {
    type Result = Either[ATM.Error, Amount]

    def run(sut: Sut): Result = sut.withdraw(user, amount)
    def nextState(state: State): State = state
    def preCondition(state: State): Boolean = true
    def postCondition(state: State, result: Try[Result]): Prop =
      isAuthorized(user, state, result)
  }
}