package vct.col.rewrite

import vct.col.ast._

class NonLatchingRewriter[Pre, Post]() extends AbstractRewriter[Pre, Post] {
  override def dispatch(program: Program[Pre]): Program[Post] = rewriteDefault(program)

  override def dispatch(stat: Statement[Pre]): Statement[Post] = rewriteDefault(stat)
  override def dispatch(e: Expr[Pre]): Expr[Post] = rewriteDefault(e)
  override def dispatch(t: Type[Pre]): Type[Post] = rewriteDefault(t)
  override def dispatch(decl: Declaration[Pre]): Unit = rewriteDefault(decl)

  override def dispatch(node: ApplicableContract[Pre]): ApplicableContract[Post] = rewriteDefault(node)
  override def dispatch(node: LoopContract[Pre]): LoopContract[Post] = rewriteDefault(node)

  override def dispatch(parRegion: ParRegion[Pre]): ParRegion[Post] = rewriteDefault(parRegion)
  override def dispatch(catchClause: CatchClause[Pre]): CatchClause[Post] = rewriteDefault(catchClause)
  override def dispatch(node: SignalsClause[Pre]): SignalsClause[Post] = rewriteDefault(node)
  override def dispatch(fieldFlag: FieldFlag[Pre]): FieldFlag[Post] = rewriteDefault(fieldFlag)
  override def dispatch(iterVariable: IterVariable[Pre]): IterVariable[Post] = rewriteDefault(iterVariable)

  override def dispatch(silverPredAcc: SilverPredicateAccess[Pre]): SilverPredicateAccess[Post] = rewriteDefault(silverPredAcc)

  override def dispatch(node: CDeclarator[Pre]): CDeclarator[Post] = rewriteDefault(node)
  override def dispatch(cDeclSpec: CDeclarationSpecifier[Pre]): CDeclarationSpecifier[Post] = rewriteDefault(cDeclSpec)
  override def dispatch(node: CTypeQualifier[Pre]): CTypeQualifier[Post] = rewriteDefault(node)
  override def dispatch(node: CPointer[Pre]): CPointer[Post] = rewriteDefault(node)
  override def dispatch(node: CInit[Pre]): CInit[Post] = rewriteDefault(node)

  override def dispatch(node: JavaModifier[Pre]): JavaModifier[Post] = rewriteDefault(node)
  override def dispatch(node: JavaImport[Pre]): JavaImport[Post] = rewriteDefault(node)
  override def dispatch(node: JavaName[Pre]): JavaName[Post] = rewriteDefault(node)

}
