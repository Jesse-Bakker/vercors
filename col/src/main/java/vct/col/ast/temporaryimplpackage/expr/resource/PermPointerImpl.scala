package vct.col.ast.temporaryimplpackage.expr.resource

import vct.col.ast.{PermPointer, TResource, Type}

trait PermPointerImpl[G] { this: PermPointer[G] =>
  override def t: Type[G] = TResource()
}