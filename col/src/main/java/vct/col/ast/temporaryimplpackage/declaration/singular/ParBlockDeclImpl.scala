package vct.col.ast.temporaryimplpackage.declaration.singular

import vct.col.ast.ParBlockDecl
import vct.col.rewrite.ScopeContext

trait ParBlockDeclImpl[G] { this: ParBlockDecl[G] =>
  override def declareDefault[Pre](scope: ScopeContext[Pre, G]): Unit = scope.parBlockScopes.top += this
}