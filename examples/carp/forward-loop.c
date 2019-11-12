// -*- tab-width:2 ; indent-tabs-mode:nil -*-
//:: cases ForwardDep
//:: tools silicon
//:: verdict Pass

/*@
  context_everywhere a != NULL && b != NULL && c != NULL;
  context \pointer(a, len, write);
  context \pointer(b, len, 1\2);
  context \pointer(c, len, write);

  requires (\forall int tid; 0 <= tid && tid < len ; b [ tid ] == tid);

  ensures  (\forall int i; 0 <= i && i < len ;  a[i] == i+1);
  ensures  (\forall int i; 0 <= i && i < len ;  b[i] == i  );
  ensures  (\forall int i; 0  < i && i < len ;  c[i] == i+2);
@*/
void example(int a[],int b[],int c[],int len){
  for(int i=0;i < len;i++) /*@
    requires \pointer_index(a, i, write);
    requires \pointer_index(b, i, 1\2);
    requires \pointer_index(c, i, write);

    ensures \pointer_index(a, i, 1\2);
    ensures \pointer_index(b, i, 1\2);
    ensures \pointer_index(c, i, write);

    requires b[i]==i;

    ensures  i>0 ==> \pointer_index(a, i-1, 1\2);
    ensures  i==len-1 ==> \pointer_index(a, i, 1\2);

    ensures  a[i]==i+1;
    ensures  b[i]==i;
    ensures  i>0 ==> c[i]==i+2;
  @*/ {
    a[i]=b[i]+1;
    /*@
      S1:if (i < len-1) {
        send 0 <= i ** i < len ** \pointer_index(a, i, 1\2) ** a[i]==i+1 to S2,1;
      }
    @*/
    S2:if (i>0) {
      //@ recv 0 < i ** i < len ** \pointer_index(a, i-1, 1\2) ** a[i-1]==i from S1,1;
      c[i]=a[i-1]+2;
    }
  }
}
