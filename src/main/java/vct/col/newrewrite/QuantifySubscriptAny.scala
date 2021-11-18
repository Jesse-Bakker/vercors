package vct.col.newrewrite

import vct.col.ast._
import vct.col.util.AstBuildHelpers._
import vct.col.ast.RewriteHelpers._
import vct.col.origin.{FramedArrIndex, Origin, TriggerPatternBlame}
import vct.col.rewrite.Rewriter
import vct.result.VerificationResult.UserError

case object QuantifySubscriptAny {
  case object GeneratedQuantifierOrigin extends Origin {
    override def preferredName: String = "i"
    override def messageInContext(message: String): String =
      s"[At node generated for auto-quantified expressions containing `*`]: $message"
  }

  case class InvalidAnyPosition(any: Any) extends UserError {
    override def code: String = "any"
    override def text: String = any.o.messageInContext(
      "This instance of `*` occurs in a position where VerCors cannot recognize a pattern to quantify.")
  }
}

case class QuantifySubscriptAny() extends Rewriter {
  import QuantifySubscriptAny._

  override def dispatch(e: Expr): Expr = {
    implicit val o: Origin = GeneratedQuantifierOrigin
    e match {
      case Perm(ArraySubscript(arrIn, Any()), permIn) =>
        val i_var = new Variable(TInt())
        val i = Local(i_var.ref)
        val arr = dispatch(arrIn)
        val perm = dispatch(permIn)

        Starall(
          bindings = Seq(i_var),
          triggers = Seq(
            Seq(ArraySubscript(arr, i)(TriggerPatternBlame))
          ),
          body = Implies(
            const(0) <= i && i < Size(arr),
            Perm(ArraySubscript(arr, i)(FramedArrIndex), perm)
          )
        )

      case node: Any => throw InvalidAnyPosition(node)

      case other => rewriteDefault(other)
    }
  }
}
