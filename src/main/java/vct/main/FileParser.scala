package vct.main

import hre.ast.FileOrigin
import hre.config.Configuration
import hre.lang.System.Progress
import hre.tools.TimeKeeper
import vct.col.ast.stmt.decl.ProgramUnit
import vct.logging.PassReport
import vct.silver.ErrorDisplayVisitor

import java.nio.file.Paths

trait FileParserTrait {
  def parseInputs(inputPaths: Array[String], timeKeeper: TimeKeeper): PassReport
}

class FileParser extends FileParserTrait {

  def parseInputs(inputPaths: Array[String], timeKeeper: TimeKeeper): PassReport = {
    Progress("parsing inputs...")
    val report = new PassReport(new ProgramUnit)
    report.setOutput(report.getInput)
    report.add(new ErrorDisplayVisitor)

    for (pathName <- inputPaths) {
      val path = Paths.get(pathName)
      if (!Configuration.currentConfiguration.noContext.get) {
        FileOrigin.add(path, Configuration.currentConfiguration.guiContext.get)
      }
//      report.getOutput.add(Parsers.parseFile(path))
    }

    Progress("Parsed %d file(s) in: %dms", Int.box(inputPaths.length), Long.box(timeKeeper.show))

    report
  }
}