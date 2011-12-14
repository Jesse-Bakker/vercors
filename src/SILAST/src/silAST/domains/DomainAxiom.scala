package silAST.domains

import silAST.ASTNode
import silAST.expressions.DExpression
import silAST.source.SourceLocation

final class DomainAxiom private[silAST](
                                         sl: SourceLocation,
                                         val name: String,
                                         val expression: DExpression
                                         ) extends ASTNode(sl) {
  def substitute(ts: TypeSubstitution) : DomainAxiom = new DomainAxiom(sl,name,expression.substitute(new DSubstitutionC(ts.types,Set())))

  override def toString = "axiom " + name + " = " + expression.toString
}