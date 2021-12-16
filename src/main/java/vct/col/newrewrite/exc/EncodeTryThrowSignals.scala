package vct.col.newrewrite.exc

import hre.util.ScopedStack
import vct.col.ast._
import vct.col.util.AstBuildHelpers._
import RewriteHelpers._
import vct.col.newrewrite.error.ExcludedByPassOrder
import vct.col.newrewrite.util.Substitute
import vct.col.origin.{AssertFailed, Blame, FramedGetLeft, FramedGetRight, Origin, ThrowNull}
import vct.col.ref.Ref
import vct.col.rewrite.{Generation, Rewriter, RewriterBuilder, Rewritten}
import vct.col.util.AstBuildHelpers
import RewriteHelpers._
import vct.result.VerificationResult.Unreachable

case object EncodeTryThrowSignals extends RewriterBuilder {
  case class ThrowNullAssertFailed(t: Throw[_]) extends Blame[AssertFailed] {
    override def blame(error: AssertFailed): Unit =
      t.blame.blame(ThrowNull(t))
  }
}

case class EncodeTryThrowSignals[Pre <: Generation]() extends Rewriter[Pre] {
  import EncodeTryThrowSignals._

  val currentException: ScopedStack[Variable[Post]] = ScopedStack()
  val exceptionalHandlerEntry: ScopedStack[LabelDecl[Post]] = ScopedStack()
  val returnHandler: ScopedStack[LabelDecl[Post]] = ScopedStack()

  val signalsBinding: ScopedStack[(Variable[Pre], Expr[Post])] = ScopedStack()

  val rootClass: ScopedStack[Ref[Post, Class[Post]]] = ScopedStack()

  def getExc(implicit o: Origin): Expr[Post] =
    currentException.top.get

  override def dispatch(program: Program[Pre]): Program[Rewritten[Pre]] =
    program.rootClass match {
      case Some(TClass(Ref(cls))) =>
        rootClass.having(succ[Class[Post]](cls)) {
          program.rewrite()
        }
      case _ => throw Unreachable("Root class unknown or not a class.")
    }

  override def dispatch(stat: Statement[Pre]): Statement[Post] = {
    implicit val o: Origin = stat.o
    stat match {
      case TryCatchFinally(body, after, catches) =>
        val handlersEntry = new LabelDecl[Post]()
        val finallyEntry = new LabelDecl[Post]()

        val newBody = exceptionalHandlerEntry.having(handlersEntry) {
          dispatch(body)
        }

        val catchImpl = Block[Post](catches.map {
          case CatchClause(decl, body) =>
            successionMap(decl) = currentException.top
            Branch(Seq((
              (getExc !== Null[Post]()) && InstanceOf(getExc, TypeValue(dispatch(decl.t))),
              Block(Seq(
                exceptionalHandlerEntry.having(finallyEntry) { dispatch(body) },
                Assign(getExc, Null()),
              ),
            ))))
        })

        val finallyImpl = Block[Post](Seq(
          Label(finallyEntry, Block(Nil)),
          dispatch(after),
          Branch(Seq((
            getExc !== Null(),
            Goto(exceptionalHandlerEntry.top.ref),
          ))),
        ))

        Block(Seq(
          newBody,
          Label(handlersEntry, Block(Nil)),
          catchImpl,
          finallyImpl,
        ))

      case t @ Throw(obj) =>
        Block(Seq(
          Assign(getExc, dispatch(obj)),
          Assert(getExc !== Null())(ThrowNullAssertFailed(t)),
          Goto(exceptionalHandlerEntry.top.ref),
        ))

      case Return(result) =>
        result match {
          case Void() => Block(Seq(
            Label(returnHandler.top, Block(Nil)),
            Return(Void()),
          ))
          case other => throw ExcludedByPassOrder("Return values that are not void cannot be evaluated at the return site for EncodeTryThrowSignals.", Some(other))
        }

      case inv: InvokeMethod[Pre] =>
        Block(Seq(
          inv.rewrite(outArgs = currentException.top.ref +: inv.outArgs.map(succ[Variable[Post]])),
          Branch(Seq((
            getExc !== Null(),
            Goto(exceptionalHandlerEntry.top.ref),
          ))),
        ))

      case other => rewriteDefault(other)
    }
  }

  override def dispatch(decl: Declaration[Pre]): Unit = decl match {
    case method: AbstractMethod[Pre] =>
      implicit val o: Origin = method.o

      val exc = new Variable[Post](TClass(rootClass.top)) // PB TODO: TClass(?)

      currentException.having(exc) {
        val body = method.body.map(body => {
          val returnPoint = new LabelDecl[Post]()
          // The return point is at this moment guaranteed to be the last logical statement of the method, since either
          // all return statements are encoded as a goto to the end of the method, or they are encoded as exceptions.
          // Because of that, we can designate the (only) return statement as the outermost exception handler, returning
          // Left(exc) if an exception is being handled, Right(result) otherwise.
          // However, we should figure out a better way of doing this. Perhaps first translating to out-parameters is
          // the way to go? But then we should perhaps add an out-parameter option<exception> or so, instead of changing
          // the return type to either<exc, _>.
          Block(Seq(
            Assign(exc.get, Null()),
            returnHandler.having(returnPoint) {
              exceptionalHandlerEntry.having(returnPoint) {
                currentException.having(exc) {
                  dispatch(body)
                }
              }
            },
          ))
        })

        val ensures: Expr[Post] =
          ((exc.get === Null()) ==> dispatch(method.contract.ensures)) &*
            AstBuildHelpers.foldStar(method.contract.signals.map {
              case SignalsClause(binding, assn) =>
                binding.drop()
                ((exc.get !== Null()) && InstanceOf(exc.get, TypeValue(dispatch(binding.t)))) ==>
                  signalsBinding.having((binding, exc.get)) { dispatch(assn) }
            })

        method.rewrite(
          body = body,
          outArgs = collectInScope(variableScopes) { exc.declareDefault(this); method.outArgs.foreach(dispatch) },
          contract = method.contract.rewrite(ensures = ensures, signals = Nil),
        ).succeedDefault(this, method)
      }

    case other => rewriteDefault(other)
  }

  override def dispatch(e: Expr[Pre]): Expr[Post] = e match {
    case Local(Ref(v)) if signalsBinding.nonEmpty && signalsBinding.top._1 == v =>
      implicit val o: Origin = e.o
      signalsBinding.top._2

    case other => rewriteDefault(other)
  }
}