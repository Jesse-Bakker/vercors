package vct.col.ast

import vct.col.ast.ScopeContext.WrongDeclarationCount
import vct.col.check.{CheckContext, CheckError, TypeError, TypeErrorText}
import vct.col.coerce.{CoercingRewriter, NopCoercingRewriter}
import vct.col.origin._
import vct.result.VerificationResult.{SystemError, Unreachable}

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.reflect.ClassTag
import scala.runtime.ScalaRunTime

sealed abstract class Declaration extends Node {
  def succeedDefault(scope: ScopeContext, pred: Declaration): Unit = {
    declareDefault(scope)
    scope.successionMap(pred) = this
  }

  def declareDefault(scope: ScopeContext): Unit

  /**
   * Create a Ref to this declaration. This is often useful in a place where the type of the ref can be directly
   * inferred, e.g. `FunctionInvocation(func.ref, ...)`. The witness to `this.type <:< T` demands that the
   * inferred T at least supports the type of this declaration.
   */
  def ref[T <: Declaration](implicit tag: ClassTag[T], witness: this.type <:< T): Ref[T] = new DirectRef[T](this)

  override def check(context: CheckContext): Seq[CheckError] =
    try {
      NopCoercingRewriter.coerce(this)
      Nil
    } catch {
      case CoercingRewriter.Incoercible(e, t) => Seq(TypeError(e, t))
      case CoercingRewriter.IncoercibleText(e, m) => Seq(TypeErrorText(e, _ => m))
    }
}

object Ref {
  val EXC_MESSAGE = "The AST is in an invalid state: a Ref contains a declaration of the wrong kind."

  def unapply[T <: Declaration](obj: Ref[T]): Option[T] = obj match {
    case ref: Ref[T] => Some(ref.decl)
    case _ => None
  }
}

/* NB: While Ref's can be stricter than just any Declaration (e.g. Ref[Function]), we can construct any variant with
   just a Declaration. This is because:
   - We cannot prove that the successor of a Declaration is of the correct type when we construct it: then it wouldn't
     be lazy and we wouldn't be able to cross-reference declarations out of AST order.
   - We do not want to cast or check refs to be of the correct kind every time we want to use it.
   The most acceptable solution is then to pretend to have a safe interface that returns a declaration of the right
   kind, but quietly check the type on first access.
 */
trait Ref[T <: Declaration] {
  def decl: T

  def tryResolve(resolver: String => Declaration): Unit = {}

  override def equals(obj: scala.Any): Boolean = obj match {
    case other: Ref[_] => decl == other.decl
  }

  override def hashCode(): Int = ScalaRunTime._hashCode((Ref.getClass, decl))
}

case class MistypedRef(received: Declaration, expected: ClassTag[_]) extends ASTStateError {
  override def text: String =
    "A reference in the AST is referencing a declaration of the wrong kind.\n" +
      s"A ${expected.runtimeClass.getSimpleName} was expected here, but we got a ${received.getClass.getSimpleName}"
}

class DirectRef[T <: Declaration](genericDecl: Declaration)(implicit tag: ClassTag[T]) extends Ref[T] {
  override def decl: T = genericDecl match {
    case decl: /*tagged*/ T => decl
    case other => throw MistypedRef(other, tag)
  }
}

class LazyRef[T <: Declaration](lazyDecl: => Declaration)(implicit tag: ClassTag[T]) extends Ref[T] {
  val made = Thread.currentThread().getStackTrace.toSeq
  def decl: T = lazyDecl match {
    case decl: /*tagged*/ T => decl
    case other => throw MistypedRef(other, tag)
  }
}

case class NotResolved(ref: UnresolvedRef[_ <: Declaration], expected: ClassTag[_]) extends ASTStateError {
  override def text: String =
    "The declaration of an unresolved reference was queried, but it is not yet resolved.\n" +
      s"We expected the name `${ref.name}` to resolve to a ${expected.runtimeClass.getSimpleName}."
}

class UnresolvedRef[T <: Declaration](val name: String)(implicit tag: ClassTag[T]) extends Ref[T] {
  private var resolvedDecl: Option[Declaration] = None

  override def tryResolve(resolver: String => Declaration): Unit = resolve(resolver(name))

  def resolve(decl: Declaration): Unit = resolvedDecl = Some(decl)

  def decl: T = resolvedDecl match {
    case None => throw NotResolved(this, tag)
    case Some(decl: /*tagged*/ T) => decl
    case Some(other) => throw MistypedRef(other, tag)
  }
}

object ScopeContext {
  case class WrongDeclarationCount(kind: ClassTag[_], count: Int) extends SystemError {
    override def text: String =
      s"Expected exactly one declaration of kind ${kind.runtimeClass.getSimpleName}, but got $count."
  }
}

class ScopeContext {
  // The default action for declarations is to be succeeded by a similar declaration, for example a copy.
  val successionMap: mutable.Map[Declaration, Declaration] = mutable.Map()

  val globalScopes: mutable.Stack[ArrayBuffer[GlobalDeclaration]] = mutable.Stack()
  val classScopes: mutable.Stack[ArrayBuffer[ClassDeclaration]] = mutable.Stack()
  val adtScopes: mutable.Stack[ArrayBuffer[ADTDeclaration]] = mutable.Stack()
  val variableScopes: mutable.Stack[ArrayBuffer[Variable]] = mutable.Stack()
  val labelScopes: mutable.Stack[ArrayBuffer[LabelDecl]] = mutable.Stack()
  val parBlockScopes: mutable.Stack[ArrayBuffer[ParBlockDecl]] = mutable.Stack()
  val parInvariantScopes: mutable.Stack[ArrayBuffer[ParInvariantDecl]] = mutable.Stack()
  val modelScopes: mutable.Stack[ArrayBuffer[ModelDeclaration]] = mutable.Stack()

  val javaLocalScopes: mutable.Stack[ArrayBuffer[JavaLocalDeclaration]] = mutable.Stack()
  val cLocalScopes: mutable.Stack[ArrayBuffer[CDeclaration]] = mutable.Stack()
  val cParams: mutable.Stack[ArrayBuffer[CParam]] = mutable.Stack()

  def collectInScope[T](scope: mutable.Stack[ArrayBuffer[T]])(f: => Unit): Seq[T] = {
    scope.push(ArrayBuffer())
    f
    scope.pop().toSeq
  }

  def collectOneInScope[T](scope: mutable.Stack[ArrayBuffer[T]])(f: => Unit)(implicit tag: ClassTag[T]): T = {
    val result = collectInScope(scope)(f)

    if(result.size != 1) {
      throw WrongDeclarationCount(tag, result.size)
    }

    result.head
  }

  def succ[T <: Declaration](decl: Declaration)(implicit tag: ClassTag[T]): LazyRef[T] =
    new LazyRef[T](successionMap(decl))
}

abstract class ExtraDeclarationKind extends Declaration

sealed abstract class GlobalDeclaration extends Declaration {
  override def declareDefault(scope: ScopeContext): Unit = scope.globalScopes.top += this
}
abstract class ExtraGlobalDeclaration extends GlobalDeclaration

sealed abstract class ClassDeclaration extends Declaration {
  override def declareDefault(scope: ScopeContext): Unit = scope.classScopes.top += this
}
abstract class ExtraClassDeclaration extends ClassDeclaration

/* Common type for unit names: locals, bindings, arguments, etc. (but not fields, as they are only in reference to an
  object) */
class Variable(val t: Type)(implicit val o: Origin) extends Declaration {
  override def declareDefault(scope: ScopeContext): Unit = scope.variableScopes.top += this
}

class LabelDecl()(implicit val o: Origin) extends Declaration {
  override def declareDefault(scope: ScopeContext): Unit = scope.labelScopes.top += this
}
class ParBlockDecl()(implicit val o: Origin) extends Declaration {
  override def declareDefault(scope: ScopeContext): Unit = scope.parBlockScopes.top += this
}
class ParInvariantDecl()(implicit val o: Origin) extends Declaration {
  override def declareDefault(scope: ScopeContext): Unit = scope.parInvariantScopes.top += this
}

class SimplificationRule(val axiom: Expr)(implicit val o: Origin) extends GlobalDeclaration

class AxiomaticDataType(val decls: Seq[ADTDeclaration], val typeArgs: Seq[Variable])(implicit val o: Origin)
  extends GlobalDeclaration with Declarator {
  override def declarations: Seq[Declaration] = decls ++ typeArgs
}

sealed trait ADTDeclaration extends Declaration {
  override def declareDefault(scope: ScopeContext): Unit = scope.adtScopes.top += this
}
class ADTAxiom(val axiom: Expr)(implicit val o: Origin) extends ADTDeclaration {
  override def check(context: CheckContext): Seq[CheckError] = axiom.checkSubType(TBool())
}


sealed trait Applicable extends Declaration with Declarator {
  def args: Seq[Variable]
  def returnType: Type
  def body: Option[Node]
  def inline: Boolean

  override def declarations: Seq[Declaration] = args

  override def enterCheckContext(context: CheckContext): CheckContext = context.withApplicable(this)
}

sealed trait AbstractPredicate extends Applicable {
  override def body: Option[Expr]
  override def returnType: Type = TResource()
  def threadLocal: Boolean

  override def check(context: CheckContext): Seq[CheckError] = body.toSeq.flatMap(_.checkSubType(TResource()))
}

case class SignalsClause(binding: Variable, assn: Expr)(implicit val o: Origin) extends NodeFamily with Declarator {
  override def declarations: Seq[Declaration] = Seq(binding)
}

case class ApplicableContract(requires: Expr, ensures: Expr, contextEverywhere: Expr,
                              signals: Seq[SignalsClause], givenArgs: Seq[Variable], yieldsArgs: Seq[Variable])
                             (implicit val o: Origin) extends NodeFamily

sealed trait ContractApplicable extends Applicable {
  def contract: ApplicableContract
  def blame: Blame[PostconditionFailed]
  override def declarations: Seq[Declaration] =
    super.declarations ++ contract.givenArgs ++ contract.yieldsArgs ++ typeArgs

  // PB: Not necessarily the logical place to introduce type arguments, but it happens to be correct: as many places
  // as possible, but not predicates (for now), and ADT functions are dealt with in a special way: they inherit the
  // type parameters from the ADT itself.
  def typeArgs: Seq[Variable]
}

sealed trait AbstractFunction extends ContractApplicable {
  override def body: Option[Expr]
  override def check(context: CheckContext): Seq[CheckError] =
    body.toSeq.flatMap(_.checkSubType(returnType))
}

sealed trait AbstractMethod extends ContractApplicable {
  override def body: Option[Statement]
  def outArgs: Seq[Variable]
  def pure: Boolean

  override def declarations: Seq[Declaration] = super.declarations ++ outArgs

  override def check(context: CheckContext): Seq[CheckError] =
    body.toSeq.flatMap(_.transSubnodes.flatMap {
      case Return(e) => e.checkSubType(returnType)
      case _ => Seq()
  })
}

class Function(val returnType: Type, val args: Seq[Variable], val typeArgs: Seq[Variable],
               val body: Option[Expr], val contract: ApplicableContract, val inline: Boolean = false)
              (val blame: Blame[PostconditionFailed])(implicit val o: Origin)
  extends GlobalDeclaration with AbstractFunction

class Procedure(val returnType: Type,
                val args: Seq[Variable], val outArgs: Seq[Variable], val typeArgs: Seq[Variable],
                val body: Option[Statement],
                val contract: ApplicableContract,
                val inline: Boolean = false, val pure: Boolean = false)
               (val blame: Blame[PostconditionFailed])(implicit val o: Origin)
  extends GlobalDeclaration with AbstractMethod

class Predicate(val args: Seq[Variable], val body: Option[Expr],
                val threadLocal: Boolean = false, val inline: Boolean = false)(implicit val o: Origin)
  extends GlobalDeclaration with AbstractPredicate

class InstanceFunction(val returnType: Type, val args: Seq[Variable], val typeArgs: Seq[Variable],
                       val body: Option[Expr], val contract: ApplicableContract, val inline: Boolean)
                      (val blame: Blame[PostconditionFailed])(implicit val o: Origin)
  extends ClassDeclaration with AbstractFunction

class InstanceMethod(val returnType: Type,
                     val args: Seq[Variable], val outArgs: Seq[Variable], val typeArgs: Seq[Variable],
                     val body: Option[Statement],
                     val contract: ApplicableContract,
                     val inline: Boolean = false, val pure: Boolean = false)
                    (val blame: Blame[PostconditionFailed])(implicit val o: Origin)
  extends ClassDeclaration with AbstractMethod

class InstancePredicate(val args: Seq[Variable], val body: Option[Expr],
                        val threadLocal: Boolean = false, val inline: Boolean = false)(implicit val o: Origin)
  extends ClassDeclaration with AbstractPredicate

class ADTFunction(val args: Seq[Variable], val returnType: Type)(implicit val o: Origin) extends Applicable with ADTDeclaration {
  override def body: Option[Node] = None
  override def inline: Boolean = false
}

sealed trait FieldFlag extends NodeFamily
class Final()(implicit val o: Origin) extends FieldFlag

sealed trait Field extends ClassDeclaration {
  def t: Type
}

class InstanceField(val t: Type, val flags: Set[FieldFlag])(implicit val o: Origin) extends Field

class Class(val declarations: Seq[ClassDeclaration], val supports: Seq[Ref[Class]])(implicit val o: Origin) extends GlobalDeclaration with Declarator {
  private def transSupportArrows(seen: Set[Class]): Seq[(Class, Class)] =
    if(seen.contains(this)) throw Unreachable("Yes, you got me, cyclical inheritance is not supported!")
    else supports.map(other => (this, other.decl)) ++
      supports.flatMap(other => other.decl.transSupportArrows(Set(this) ++ seen))

  def transSupportArrows: Seq[(Class, Class)] = transSupportArrows(Set.empty)
}

sealed trait ModelDeclaration extends Declaration {
  override def declareDefault(scope: ScopeContext): Unit = scope.modelScopes.top += this
}

class ModelField(val t: Type)(implicit val o: Origin) extends ModelDeclaration

class ModelProcess(val args: Seq[Variable], val impl: Expr,
                   val requires: Expr, val ensures: Expr,
                   val modifies: Seq[Ref[ModelField]], val accessible: Seq[Ref[ModelField]])
                  (val blame: Blame[PostconditionFailed])
                  (implicit val o: Origin) extends ModelDeclaration with Applicable {
  override def returnType: Type = TProcess()
  override def body: Option[Node] = Some(impl)
  override def inline: Boolean = false
  override def check(context: CheckContext): Seq[CheckError] =
    impl.checkSubType(TProcess()) ++ requires.checkSubType(TBool()) ++ ensures.checkSubType(TBool())
}
class ModelAction(val args: Seq[Variable],
                  val requires: Expr, val ensures: Expr,
                  val modifies: Seq[Ref[ModelField]], val accessible: Seq[Ref[ModelField]])
                 (implicit val o: Origin) extends ModelDeclaration with Applicable {
  override def returnType: Type = TProcess()
  override def body: Option[Node] = None
  override def inline: Boolean = false

  override def check(context: CheckContext): Seq[CheckError] =
    requires.checkSubType(TBool()) ++ ensures.checkSubType(TBool())
}

class Model(val declarations: Seq[ModelDeclaration])(implicit val o: Origin) extends GlobalDeclaration with Declarator