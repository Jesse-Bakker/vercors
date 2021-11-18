package vct.col.rewrite

import vct.col.ast._

class Rewriter extends AbstractRewriter {
  override def dispatch(program: Program): Program = rewriteDefault(program)

  override def dispatch(stat: Statement): Statement = rewriteDefault(stat)
  override def dispatch(e: Expr): Expr = rewriteDefault(e)
  override def dispatch(t: Type): Type = rewriteDefault(t)
  override def dispatch(decl: Declaration): Unit = rewriteDefault(decl)

  override def dispatch(node: ApplicableContract): ApplicableContract = rewriteDefault(node)
  override def dispatch(node: LoopContract): LoopContract = rewriteDefault(node)

  override def dispatch(parRegion: ParRegion): ParRegion = rewriteDefault(parRegion)
  override def dispatch(catchClause: CatchClause): CatchClause = rewriteDefault(catchClause)
  override def dispatch(node: SignalsClause): SignalsClause = rewriteDefault(node)
  override def dispatch(fieldFlag: FieldFlag): FieldFlag = rewriteDefault(fieldFlag)
  override def dispatch(iterVariable: IterVariable): IterVariable = rewriteDefault(iterVariable)

  override def dispatch(silverPredAcc: SilverPredicateAccess): SilverPredicateAccess = rewriteDefault(silverPredAcc)

  override def dispatch(node: CDeclarator): CDeclarator = rewriteDefault(node)
  override def dispatch(cDeclSpec: CDeclarationSpecifier): CDeclarationSpecifier = rewriteDefault(cDeclSpec)
  override def dispatch(node: CTypeQualifier): CTypeQualifier = rewriteDefault(node)
  override def dispatch(node: CPointer): CPointer = rewriteDefault(node)
  override def dispatch(node: CInit): CInit = rewriteDefault(node)

  override def dispatch(node: JavaModifier): JavaModifier = rewriteDefault(node)
  override def dispatch(node: JavaImport): JavaImport = rewriteDefault(node)
  override def dispatch(node: JavaName): JavaName = rewriteDefault(node)

}
