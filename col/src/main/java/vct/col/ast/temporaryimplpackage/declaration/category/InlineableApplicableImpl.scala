package vct.col.ast.temporaryimplpackage.declaration.category

import vct.col.ast.InlineableApplicable

trait InlineableApplicableImpl[G] extends ApplicableImpl[G] { this: InlineableApplicable[G] =>
  def inline: Boolean
}