package vct.col.ast.temporaryimplpackage.declaration.singular

import vct.col.ast.Variable
import vct.col.rewrite.ScopeContext

trait VariableImpl[G] { this: Variable[G] =>
  override def declareDefault[Pre](scope: ScopeContext[Pre, G]): Unit = scope.variableScopes.top += this
}