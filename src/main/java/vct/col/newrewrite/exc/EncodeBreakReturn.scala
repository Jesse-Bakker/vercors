package vct.col.newrewrite.exc

import vct.col.ast._
import RewriteHelpers._
import hre.util.ScopedStack
import vct.col.newrewrite.error.ExcludedByPassOrder
import vct.col.origin.{Origin, PanicBlame}
import vct.col.ref.{LazyRef, Ref}
import vct.col.util.AstBuildHelpers._
import vct.col.rewrite.{Generation, Rewriter, RewriterBuilder, Rewritten}
import vct.col.util.SuccessionMap

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.reflect.ClassTag

case object EncodeBreakReturn extends RewriterBuilder {
  case class PostLabeledStatementOrigin(label: LabelDecl[_]) extends Origin {
    override def preferredName: String = "break_" + label.o.preferredName
    override def messageInContext(message: String): String =
      s"[At node generated to jump past a statement]: $message"
  }

  case object ReturnClass extends Origin {
    override def preferredName: String = "Return"
    override def messageInContext(message: String): String =
      s"[At class generated to encode return with an exception]: $message"
  }

  case object ReturnField extends Origin {
    override def preferredName: String = "value"
    override def messageInContext(message: String): String =
      s"[At field generated to encode return with an exception]: $message"
  }

  case object ReturnTarget extends Origin {
    override def preferredName: String = "end"
    override def messageInContext(message: String): String =
      s"[At label generated for the end of the method]: $message"
  }

  case object ReturnVariable extends Origin {
    override def preferredName: String = "return"
    override def messageInContext(message: String): String =
      s"[At variable generated for the result of the method]: $message"
  }

  case object BreakException extends Origin {
    override def preferredName: String = "Break"
    override def messageInContext(message: String): String =
      s"[At exception class generated to break on a label or loop]: $message"
  }
}

case class EncodeBreakReturn[Pre <: Generation]() extends Rewriter[Pre] {
  import EncodeBreakReturn._

  def hasFinally(stat: Statement[Pre]): Boolean =
    stat.transSubnodes.exists {
      case TryCatchFinally(_, Block(Nil), _) => false
      case TryCatchFinally(_, _, _) => true
      case _ => false
    }

  case class BreakReturnToGoto(returnTarget: LabelDecl[Post], resultVariable: Local[Post]) extends Rewriter[Pre] {
    val breakLabels: mutable.Set[LabelDecl[Pre]] = mutable.Set()
    val postLabeledStatement: SuccessionMap[LabelDecl[Pre], LabelDecl[Post]] = SuccessionMap()
    override val successionMap: SuccessionMap[Declaration[Pre], Declaration[Post]] = EncodeBreakReturn.this.successionMap

    override def dispatch(stat: Statement[Pre]): Statement[Post] = {
      implicit val o: Origin = stat.o
      stat match {
        case Label(decl, stat) =>
          val newBody = dispatch(stat)

          if (breakLabels.contains(decl)) {
            postLabeledStatement(decl) = new LabelDecl()(PostLabeledStatementOrigin(decl))
            Block(Seq(
              Label(collectOneInScope(labelScopes) { rewriteDefault(decl) }, Block(Nil)),
              newBody,
              Label(postLabeledStatement(decl), Block(Nil)),
            ))
          } else {
            Block(Seq(
              Label(collectOneInScope(labelScopes) { rewriteDefault(decl) }, Block(Nil)),
              newBody,
            ))
          }

        case Break(None) =>
          throw ExcludedByPassOrder("Break statements without a label are made explicit by SpecifyImplicitLabels", Some(stat))

        case Break(Some(Ref(label))) =>
          breakLabels += label
          Goto(postLabeledStatement.ref(label))

        case Return(result) =>
          Block(Seq(
            assignLocal(resultVariable, dispatch(result)),
            Goto(returnTarget.ref),
          ))

        case other => rewriteDefault(other)
      }
    }
  }

  case class BreakReturnToException(returnClass: Class[Post], valueField: InstanceField[Post]) extends Rewriter[Pre] {
    override val successionMap: SuccessionMap[Declaration[Pre], Declaration[Post]] = EncodeBreakReturn.this.successionMap
    override val globalScopes: ScopedStack[ArrayBuffer[GlobalDeclaration[Rewritten[Pre]]]] = EncodeBreakReturn.this.globalScopes
    val breakLabelException: SuccessionMap[LabelDecl[Pre], Class[Post]] = SuccessionMap()

    override def dispatch(stat: Statement[Pre]): Statement[Post] = {
      implicit val o: Origin = stat.o
      stat match {
        case Label(decl, stat) =>
          val newBody = dispatch(stat)

          if(breakLabelException.contains(decl)) {
            TryCatchFinally(
              body = Block(Seq(Label(collectOneInScope(labelScopes) { rewriteDefault(decl) }, Block(Nil)), newBody)),
              after = Block(Nil),
              catches = Seq(CatchClause(
                decl = new Variable(TClass(breakLabelException.ref(decl))),
                body = Block(Nil),
              )),
            )
          } else {
            Block(Seq(
              Label(collectOneInScope(labelScopes) { rewriteDefault(decl) }, Block(Nil)),
              newBody,
            ))
          }

        case Break(None) =>
          throw ExcludedByPassOrder("Break statements without a label are made explicit by SpecifyImplicitLabels", Some(stat))

        case Break(Some(Ref(label))) =>
          val cls = breakLabelException.getOrElseUpdate(label, {
            val cls1 = new Class[Post](Nil, Nil, tt)(BreakException)
            cls1.declareDefault(this)
            cls1
          })
          Throw(NewObject[Post](cls.ref))(PanicBlame("The result of NewObject is never null"))

        case Return(result) =>
          val exc = new Variable[Post](TClass(returnClass.ref))
          Scope(Seq(exc), Block(Seq(
            assignLocal(exc.get, NewObject(returnClass.ref)),
            assignField(exc.get, valueField.ref, dispatch(result), PanicBlame("Have write permission immediately after NewObject")),
            Throw(exc.get)(PanicBlame("The result of NewObject is never null")),
          )))

        case other => rewriteDefault(other)
      }
    }
  }

  override def dispatch(decl: Declaration[Pre]): Unit = decl match {
    case method: AbstractMethod[Pre] =>
      method.body match {
        case None => rewriteDefault(method)
        case Some(body) =>
          val newBody: Statement[Post] = if(hasFinally(body)) {
            implicit val o: Origin = body.o
            val returnField = new InstanceField[Post](dispatch(method.returnType), Set.empty)(ReturnField)
            val returnClass = new Class[Post](Seq(returnField), Nil, tt)(ReturnClass)
            returnClass.declareDefault(this)

            val caughtReturn = new Variable[Post](TClass(returnClass.ref))

            TryCatchFinally(
              body = BreakReturnToException(returnClass, returnField).dispatch(body),
              catches = Seq(CatchClause(caughtReturn,
                Return(Deref[Post](caughtReturn.get, returnField.ref)(PanicBlame("Permission for the field of a return exception cannot be non-write, as the class is only instantiated at a return site, and caught immediately.")))
              )),
              after = Block(Nil)
            )
          } else {
            val resultTarget = new LabelDecl[Post]()(ReturnTarget)
            val resultVar = new Variable(dispatch(method.returnType))(ReturnVariable)
            val newBody = BreakReturnToGoto(resultTarget, resultVar.get(ReturnVariable)).dispatch(body)
            implicit val o: Origin = body.o
            Scope(Seq(resultVar), Block(Seq(
              newBody,
              Label(resultTarget, Block(Nil)),
              Return(resultVar.get),
            )))
          }
          method.rewrite(body = Some(newBody)).succeedDefault(this, method)
      }
    case other => rewriteDefault(other)
  }
}