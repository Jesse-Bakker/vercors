package vct.ast

import org.scalatest._
import vct.col.ast._

class ParallelBlockSpec extends FlatSpec with Matchers {
  
  "A parallel block" should "successfully instantiate with non-null (lists) arguments" in {
    val label = "block1"
    val builder = new ContractBuilder
    val contract = builder.getContract
    val iters = List[DeclarationStatement](new DeclarationStatement("iter1", null))
    val block = new BlockStatement
    val deps = Array[ASTNode](new DeclarationStatement("dep1", null))
    var parblock = new ParallelBlock(label, contract, iters, block, deps)
    
    parblock.itersLength should be (1)
    parblock.depsLength should be (1)
  }
  
  it should "successfully instantiate with non-null (array) arguments" in {
    val label = "block1"
    val builder = new ContractBuilder
    val contract = builder.getContract
    val iters = Array[DeclarationStatement](new DeclarationStatement("iter1", null))
    val block = new BlockStatement
    val deps = Array[ASTNode](new DeclarationStatement("dep1", null))
    var parblock = new ParallelBlock(label, contract, iters, block, deps)
    
    parblock.itersLength should be (1)
    parblock.depsLength should be (1)
  }
  
  it should "successfully instantiate when the deps array is given null (with an iters list)" in {
    val label = "block1"
    val builder = new ContractBuilder
    val contract = builder.getContract
    val iters = List[DeclarationStatement](new DeclarationStatement("iter1", null))
    val block = new BlockStatement
    val deps : Array[ASTNode] = null
    var parblock = new ParallelBlock(label, contract, iters, block, deps)
    
    parblock.itersLength should be (1)
    parblock.depsLength should be (0)
  }
  
  it should "successfully instantiate when the deps array is given null (with an iters array)" in {
    val label = "block1"
    val builder = new ContractBuilder
    val contract = builder.getContract
    val iters = Array[DeclarationStatement](new DeclarationStatement("iter1", null))
    val block = new BlockStatement
    val deps : Array[ASTNode] = null
    var parblock = new ParallelBlock(label, contract, iters, block, deps)
    
    parblock.itersLength should be (1)
    parblock.depsLength should be (0)
  }
}
