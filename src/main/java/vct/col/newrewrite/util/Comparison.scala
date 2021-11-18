package vct.col.newrewrite.util

import vct.col.ast._
import vct.col.origin.Origin

case object Comparison {
  val LESS: Comparison = Comparison(less = true)
  val LESS_EQ: Comparison = Comparison(less = true, eq = true)
  val EQ: Comparison = Comparison(eq = true)
  val GREATER_EQ: Comparison = Comparison(eq = true, greater = true)
  val GREATER: Comparison = Comparison(greater = true)

  val NEQ: Comparison = Comparison(less = true, greater = true)
  val TRUE: Comparison = Comparison(less = true, eq = true, greater = true)
  val FALSE: Comparison = Comparison()

  def of(comparison: Expr): Option[(Expr, Comparison, Expr)] = Some(comparison match {
    case Less(left, right) => (left, LESS, right)
    case LessEq(left, right) => (left, LESS_EQ, right)
    case Eq(left, right) => (left, EQ, right)
    case GreaterEq(left, right) => (left, GREATER_EQ, right)
    case Greater(left, right) => (left, GREATER, right)
    case Neq(left, right) => (left, NEQ, right)
    case _ => return None
  })

  def matching(comparand: Expr, comparison: Expr): Option[(Comparison, Expr)] =
    of(comparison) match {
      case Some((left, comp, right)) =>
        if(left == comparand) Some((comp, right))
        else if(right == comparand) Some((comp.flip, left))
        else None
      case None => None
    }
}

case class Comparison(less: Boolean = false, eq: Boolean = false, greater: Boolean = false) {
  def flip: Comparison = Comparison(
    less = greater,
    eq = eq,
    greater = less,
  )

  def make(left: Expr, right: Expr)(implicit o: Origin): Expr = this match {
    case Comparison(false, false, false) => Constant.BooleanValue(false)
    case Comparison(true, true, true) => Constant.BooleanValue(true)
    case Comparison(true, false, true) => Neq(left, right)

    case Comparison(true, false, false) => Less(left, right)
    case Comparison(true, true, false) => LessEq(left, right)
    case Comparison(false, true, false) => Eq(left, right)
    case Comparison(false, true, true) => GreaterEq(left, right)
    case Comparison(false, false, true) => Greater(left, right)
  }
}
