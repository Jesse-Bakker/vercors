package vct.main

import vct.col.ast.Program
import vct.col.check.{CheckError, IncomparableTypes, OutOfScopeError, TypeError, TypeErrorText}
import vct.col.feature.FeatureRainbow
import vct.col.newrewrite.ImportADT
import vct.col.newrewrite.lang.{LangSpecificToCol, LangTypesToCol}
import vct.col.origin.DiagnosticOrigin
import vct.col.resolve.{ResolveReferences, ResolveTypes}
import vct.parsers.{ParseResult, Parsers}
import vct.result.VerificationResult
import vct.result.VerificationResult.{SystemError, UserError}
import vct.test.CommandLineTesting

import java.io.File
import java.nio.file.Path
import scala.jdk.CollectionConverters._

case object Test {
  var files = 0
  var systemErrors = 0
  var errorCount = 0
  var crashes = 0

  val start = System.currentTimeMillis()

  def main(args: Array[String]): Unit = {
    try {
//      for(f <- new File("examples/arrays/array-example.pvl").listFiles()) {
//        tryParse(Seq(f.toPath))
//      }

      CommandLineTesting.getCases.values.filter(_.tools.contains("silicon")).toSeq.sortBy(_.files.asScala.toSeq.head).foreach(c => {
        if(c.files.asScala.forall(f =>
            f.toString.endsWith(".java") ||
              f.toString.endsWith(".c") ||
              f.toString.endsWith(".pvl"))) {
          tryParse(c.files.asScala.toSeq)
        } else {
          println(s"Skipping: ${c.files.asScala.mkString(", ")}")
        }
      })

//      tryParse(Seq(Path.of("examples/arrays/backward-dep-e1.c")))
    } finally {
      println(s"Out of $files filesets, $systemErrors threw a SystemError, $crashes crashed and $errorCount errors were reported.")
      println(s"Time: ${(System.currentTimeMillis() - start)/1000.0}s")
    }
  }

  def printErrorsOr(errors: Seq[CheckError])(otherwise: => Unit): Unit = {
    errorCount += errors.size
    if(errors.isEmpty) otherwise
    else errors.foreach {
      case TypeError(expr, expectedType) =>
        expectedType.superTypeOf(expr.t)
        println(expr.o.messageInContext(s"Expected to be of type $expectedType, but got ${expr.t}"))
      case TypeErrorText(expr, message) =>
        println(expr.o.messageInContext(message(expr.t)))
      case OutOfScopeError(use, ref) =>
        println(use.o.messageInContext("This use is out of scope"))
        println(ref.decl.o.messageInContext("Declaration occurs here"))
      case IncomparableTypes(left, right) =>
        println(s"Types $left and $right are incomparable")
    }
  }

  def tryParse(paths: Seq[Path]): Unit = try {
    files += 1
    println(paths.mkString(", "))
    val ParseResult(decls, expectedErrors) = ParseResult.reduce(paths.map(Parsers.parse))
    var program = Program(decls)(DiagnosticOrigin)(DiagnosticOrigin)
    val extraDecls = ResolveTypes.resolve(program)
    program = Program(program.declarations ++ extraDecls)(DiagnosticOrigin)(DiagnosticOrigin)
    val typesToCol = LangTypesToCol()
    program = typesToCol.dispatch(program)
    val errors = ResolveReferences.resolve(program)
    printErrorsOr(errors) {
      program = LangSpecificToCol().dispatch(program)
      printErrorsOr(program.check) {
        val features = new FeatureRainbow()
        features.scan(program)
        println(features.features)
//        program = ImportADT().dispatch(program)
//        printErrorsOr(program.check) {}
      }
    }
  } catch {
    case err: SystemError =>
      println(err.text)
      systemErrors += 1
    case res: VerificationResult =>
      errorCount += 1
      println(res.text)
    case e: Throwable =>
      e.printStackTrace()
      crashes += 1
  }
}