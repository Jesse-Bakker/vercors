package vct.main.stages

import vct.col.ast.Program
import vct.col.rewrite.Generation
import vct.col.util.ExpectedError
import vct.options.Options
import viper.api.{Carbon, Silicon}

case object Backend {
  def ofOptions(options: Options): Backend = options.backend match {
    case vct.options.Backend.Silicon => SilverBackend(Silicon(
      z3Settings = Map.empty,
      z3Path = options.z3Path,
    ))
    case vct.options.Backend.Carbon => SilverBackend(Carbon)
  }
}

trait Backend extends ContextStage[Program[_ <: Generation], Seq[ExpectedError], Unit] {
  override def friendlyName: String = "Verification"
  override def progressWeight: Int = 5
}

case class SilverBackend(backend: viper.api.SilverBackend) extends Backend {
  override def runWithoutContext(input: Program[_ <: Generation]): Unit =
    backend.submit(input)
}