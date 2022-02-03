package vct.col.ast.temporaryimplpackage.expr.model

import vct.col.ast.{ModelAbstractState, TResource, Type}

trait ModelAbstractStateImpl[G] { this: ModelAbstractState[G] =>
  override def t: Type[G] = TResource()
}