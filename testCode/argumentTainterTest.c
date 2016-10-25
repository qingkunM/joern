
int main(int argc, char **argv){
  p_caller();
  p_caller2(argc);
}
int arg_tainter_basic_test()
{
  struct myVar;
  memset(myVar, 0, sizeof(myVar));
  p_caller();
}

int interproc_arg_tainter_test()
{
  int x;
  interproc_callee(&x);
  sink12(x);
}

int interproc_callee(int *x)
{
  *x = source12();
}


/**
   In the following setting, taintedArgs should NOT
   detect a possible initialization of
   "b = sourceB()" AND "a = sourceA()", because it's
   either one, or the other.
*/

int caller1()
{
  int a = 0;
  int b = sourceB();

  two_arg_sink_caller(a,b);
}

int caller2()
{
  int a = sourceA();
  int b = 0;

  two_arg_sink_caller(a,b);
}

int two_arg_sink_caller(int x, int y)
{
  asink(x,y);
}


int p_caller()
{
  int a = sourceA();
  int b = sourceB();
  
  two_arg_sink_caller_p(a,b);
}

int p_caller2(int a)
{
  //int a = sourceA();
  int b = sourceB();
  if(b == 0) return;
  
  two_arg_sink_caller_p(a,b);
}

int two_arg_sink_caller_p(int x, int y)
{
  int x1 = x+fun(y);//这个statement“use”了三个符号x,fun,y;定义了一个符号x1。
  asink(x1,y);

}
