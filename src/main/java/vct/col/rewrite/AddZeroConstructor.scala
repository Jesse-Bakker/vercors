package vct.col.rewrite

import hre.ast.BranchOrigin
import vct.col.ast.`type`.PrimitiveSort
import vct.col.ast.expr.StandardOperator
import vct.col.ast.stmt.decl.{ASTClass, ASTSpecial, DeclarationStatement, Method, ProgramUnit}
import vct.col.ast.util.{AbstractRewriter, ContractBuilder}
import vct.col.ast.stmt.decl.ASTSpecial

import scala.collection.convert.ImplicitConversions.`collection asJava`
import scala.jdk.CollectionConverters._

object AddZeroConstructor {
  def hasConstructor(cls: ASTClass): Boolean =
    cls.asScala.collectFirst {
      case method: Method if method.kind == Method.Kind.Constructor => ()
    }.nonEmpty
}

case class AddZeroConstructor(override val source: ProgramUnit) extends AbstractRewriter(source) {
  private def zeroConstructor(cls: ASTClass): Method = {
    create.enter()
    create.setOrigin(new BranchOrigin("auto-generated parameterless constructor", cls.getOrigin))

    val cb = new ContractBuilder
    val body = create.block()

    for (field <- cls.dynamicFields.asScala) {
      val init = field.`type`.zero
      val fieldNode = create.dereference(create.diz(), field.name)
      body.addStatement(create assignment(fieldNode, init))
      cb.ensures(create.expression(StandardOperator.PointsTo, fieldNode, create.fullPermission(), init))
    }

    if(cls.methods().asScala.map(_.name).contains("run")) {
      body.append(create.special(ASTSpecial.Kind.Inhale, create.invokation(create.diz, null, "idleToken")))
      cb.ensures(create.invokation(create.diz, null, "idleToken"));
    }

    val res = create method_kind(
      Method.Kind.Constructor,
      create.primitive_type(PrimitiveSort.Void),
      cb.getContract(false),
      cls.getName,
      Array.empty[DeclarationStatement],
      body
    )

    create.leave()

    res
  }

  override def visit(cls: ASTClass): Unit = {
    super.visit(cls)
    val res = result.asInstanceOf[ASTClass]

    if(!AddZeroConstructor.hasConstructor(cls)) {
      res.add_dynamic(zeroConstructor(cls))
    }
  }
}
