package vct.col.ast.temporaryimplpackage.expr.binder

import vct.col.ast.{Exists, TBool, Type}

trait ExistsImpl[G] { this: Exists[G] =>
  override def t: Type[G] = TBool()
}