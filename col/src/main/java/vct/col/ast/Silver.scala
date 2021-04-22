package vct.col.ast

case class SilverPredicateAccess(ref: Ref, args: Seq[Expr], perm: Expr)(implicit val o: Origin)
  extends Check(perm.checkSubType(TRational())) with NodeFamily

sealed trait SilverExpr extends ExtraExpr
case class SilverDeref(obj: Expr, field: Ref)(val blame: SilverInsufficientPermissionBlame)(implicit val o: Origin)
  extends Check(obj.checkSubType(TRef())) with SilverExpr {
  override def t: Type = field.asSilverField.t
}
sealed trait SilverResource extends SilverExpr {
  override def t: Type = TResource()
}
case class SilverPerm(obj: Expr, field: Ref, perm: Expr)(implicit val o: Origin)
  extends Check(perm.checkSubType(TRational()), obj.checkSubType(TRef())) with SilverResource
case class SilverPredPerm(access: SilverPredicateAccess)(implicit val o: Origin) extends SilverResource with NoCheck
case class SilverUnfolding(access: SilverPredicateAccess, body: Expr)(implicit val o: Origin) extends SilverExpr with NoCheck {
  override def t: Type = body.t
}
case class SilverCurFieldPerm(obj: Expr, field: Ref)(implicit val o: Origin) extends Check(obj.checkSubType(TRef())) with SilverExpr {
  override def t: Type = TRational()
}
case class SilverCurPredPerm(ref: Ref, args: Seq[Expr])(implicit val o: Origin) extends SilverExpr with NoCheck {
  override def t: Type = TRational()
}

sealed trait SilverStatement extends ExtraStatement
case class SilverUnfold(access: SilverPredicateAccess)(val blame: SilverUnfoldBlame)(implicit val o: Origin) extends SilverStatement with NoCheck
case class SilverFold(access: SilverPredicateAccess)(val blame: SilverFoldBlame)(implicit val o: Origin) extends SilverStatement with NoCheck
case class SilverWhile(cond: Expr, invariant: Expr, body: Statement)(val blame: SilverWhileInvariantBlame)(implicit val o: Origin)
  extends Check(cond.checkSubType(TBool()), invariant.checkSubType(TResource())) with SilverStatement
case class SilverIf(cond: Expr, whenTrue: Statement, whenFalse: Statement)(implicit val o: Origin)
  extends Check(cond.checkSubType(TBool())) with SilverStatement
case class SilverNewRef(v: Ref, fields: Seq[Ref])(implicit val o: Origin) extends SilverStatement with NoCheck

sealed trait SilverAssign extends SilverStatement
case class SilverFieldAssign(obj: Expr, field: Ref, value: Expr)(val blame: SilverAssignBlame)(implicit val o: Origin)
  extends Check(value.checkSubType(field.asSilverField.t)) with SilverAssign
case class SilverLocalAssign(v: Ref, value: Expr)(implicit val o: Origin)
  extends Check(value.checkSubType(v.asVariable.t)) with SilverAssign

sealed abstract class SilverDeclaration extends ExtraGlobalDeclaration
class SilverField(val t: Type)(implicit val o: Origin) extends SilverDeclaration with NoCheck