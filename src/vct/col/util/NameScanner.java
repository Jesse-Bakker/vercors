package vct.col.util;

import java.util.ArrayList;
import java.util.Hashtable;

import vct.col.ast.DeclarationStatement;
import vct.col.ast.NameExpression;
import vct.col.ast.RecursiveVisitor;
import vct.col.ast.Type;


public class NameScanner extends RecursiveVisitor<Object> {

  private Hashtable<String,Type> vars;
  
  public NameScanner(Hashtable<String,Type> vars) {
    super(null, null);
    this.vars=vars;
  }
  
  public void visit(NameExpression e){
    if (e.getKind()==NameExpression.Kind.Reserved) return;
    String name=e.getName();
    Type t=e.getType();
    if (vars.contains(name)){
      if (!t.equals(vars.get(name))) {
        Fail("type mismatch %s != %s",t,vars.get(name));
      }
    } else {
      vars.put(name,t);
    }
  }
  
  public void visit(DeclarationStatement d){
    Abort("missing case in free variable detection");
  }

}
