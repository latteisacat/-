// StaticTypeCheck.java

// Static type checking for Clite is defined by the functions
// V and the auxiliary functions typing and typeOf.  These
// functions use the classes in the Abstract Syntax of Clite.

// V는 타당성 검사 함수이다.

public class StaticTypeCheck {

    public static TypeMap typing (Declarations d) {
        TypeMap map = new TypeMap();
        for (Declaration di : d) 
            map.put (di.v, di.t);
        return map;
    }

    public static TypeMap typing(Declarations d, Functions f){
        TypeMap map = new TypeMap();
        for(Declaration di : d){
            map.put(di.v, di.t);
        }
        for(Function fi : f){
            map.put(new Variable(fi.id), new Prototype(fi.type, fi.params));
        }
        return map;
    }
    public static void check(boolean test, String msg) {
        if (test)  return;
        System.err.println(msg);
        System.exit(1);
    }

    // 선언된 변수 이름들의 중복을 검사..?
    public static void V (Declarations d) {
        for (int i=0; i<d.size() - 1; i++)
            for (int j=i+1; j<d.size(); j++) {
                Declaration di = d.get(i);
                Declaration dj = d.get(j);
                check( ! (di.v.equals(dj.v)),
                       "duplicate declaration: " + dj.v);
            }
    }

    public static void V(Declarations ds1, Declarations ds2){
        V(ds1);
        V(ds2);
        for(int i = 0; i < ds1.size(); i++){
            Declaration di = ds1.get(i);
            for(int j = 0; j < ds2.size(); j++){
                Declaration dj = ds2.get(j);
                check(!di.v.equals(dj.v), "duplicate declaration: " + dj.v);
            }
        }
    }

    public static void V(Declarations ds, Functions fs){
        for(int i = 0; i < ds.size(); i++){
            Declaration di = ds.get(i);
            for(int j = i + 1; j < ds.size();j++){
                Declaration dj = ds.get(j);
                check(!di.v.equals(dj.v), "duplicate declaration: " + dj.v);
            }
            for(int j = 0;j < fs.size(); j++){
                Function fj = fs.get(j);
                check(!di.v.equals(fj.id), "duplicate declaration: " + fj.id);
            }
        }
    }



    public static void V (Program p) {
        V (p.globals, p.functions);
        boolean foundMain = false;
        TypeMap tmg = typing(p.globals, p.functions);
        System.out.println("Global Type Map:");
        p.globals.display(1);
        for(Function f : p.functions){
            if(f.id.equals("main")){
                if(foundMain){
                    check(false, "duplicate main function");
                }
                else{
                    foundMain = true;
                }
            }
            V(f.params, f.locals);
            TypeMap tmf = typing(f.params).onion(typing(f.locals));
            tmf = tmg.onion(tmf);
            System.out.print("Function " + f.id + " Type Map:\n");
            tmf.display();
            V(f.body, tmf);
        }
    }

    public static Type typeOf(Function f, TypeMap tm){
        Variable v = new Variable(f.id);
        check(tm.containsKey(v), "undefined function: " + f.id);
        return tm.get(v); //Prototype Return
    }


    //Expression(수식)은 Value, Variable, Binary, Unary로 구성됨.
    public static Type typeOf (Expression e, TypeMap tm) {
        //Expression(수식)이 value면 결과 역시 value의 타입을 반환한다.
        if (e instanceof Value) return ((Value)e).type;
        //Expression(수식)이 Variable(변수)이면 결과 역시 Variable(변수)의 타입을 반환한다.
        if (e instanceof Variable) {
            Variable v = (Variable)e;
            check (tm.containsKey(v), "undefined variable: " + v);
            return (Type) tm.get(v);
        }
        //Expression(수식)이 Binary(이항 연산자)일 때
        /*
        1. op(Operator)가 산술 연산자(+, -, *, /)일 때 결과 타입은 피연산자의 타입이 되어야 한다.
        2. op(Operator)가 관계 연산자(==, !=, <, <=, >, >=)일 때 결과 타입은 bool이 된다.
        * */
        if (e instanceof Binary) {
            Binary b = (Binary)e;
            if (b.op.ArithmeticOp( ))
                if (typeOf(b.term1,tm).equals(Type.FLOAT))
                    return (Type.FLOAT);
                else return (Type.INT);
            if (b.op.RelationalOp( ) || b.op.BooleanOp( )) 
                return (Type.BOOL);
        }
        //Expression(수식)이 Unary(단항 연산자)일 때
        /*
        1. op(Operator)가 NotOp(!)일 때 결과 타입은 bool이 된다.
        2. op(Operator)가 NegateOp(-)일 때 결과 타입은 피연산자의 타입이 된다.
        3. op(Operator)가 타입 변환 연산일 때 결과 타입은 연산에 따라 결정된다.
        * */
        if (e instanceof Unary) {
            Unary u = (Unary)e;
            if (u.op.NotOp( ))        return (Type.BOOL);
            else if (u.op.NegateOp( )) return typeOf(u.term,tm);
            else if (u.op.intOp( ))    return (Type.INT);
            else if (u.op.floatOp( )) return (Type.FLOAT);
            else if (u.op.charOp( ))  return (Type.CHAR);
        }

        if (e instanceof Call){
            Call c = (Call)e;
            check(tm.containsKey(new Variable(c.name)),
                    "undefined function call: " + c.name);
            return tm.get(new Variable(c.name));
        }
        throw new IllegalArgumentException("should never reach here");
    } 

    //Expression(수식)은 Value, Variable, Binary, Unary로 구성됨.
    public static void V (Expression e, TypeMap tm) {
        //Value는 항상 타당하다.
        if (e instanceof Value)
            return;
        // Variable(변수)은 선언되어 있어야 한다.
        else if (e instanceof Variable) {
            Variable v = (Variable)e;
            check( tm.containsKey(v)
                   , "undeclared variable: " + v);
            return;
        }
        // Binary(이항 연산자)는 다음을 모두 만족할 때 타당하다.
        /*
        1. Expression(수식) term1과 term2가 타당해야 한다.
        2. op가 산술 연산자(+, -, *, /)일 때 term1과 term2의 타입이 int 혹은 float 여야 한다.
        3. op가 관계 연산자(==, !=, <, <=, >, >=)일 때 term1과 term2의 타입이 같아야 한다.
        4. op가 논리 연산자(&&, ||)일 때 term1과 term2의 타입이 bool이어야 한다.
        * */
        else if (e instanceof Binary) {
            Binary b = (Binary) e;
            Type typ1 = typeOf(b.term1, tm);
            Type typ2 = typeOf(b.term2, tm);
            V (b.term1, tm);
            V (b.term2, tm);
            boolean test = typ1.equals(typ2);
            if (b.op.ArithmeticOp( ))  
                check( typ1.equals(typ2) &&
                       (typ1.equals(Type.INT) || typ1.equals(Type.FLOAT))
                       , "type error for " + b.op);
            else if (b.op.RelationalOp( )) 
                check( typ1.equals(typ2) , "type error for " + b.op);
            else if (b.op.BooleanOp( )) 
                check( typ1.equals(Type.BOOL) && typ2.equals(Type.BOOL),
                       b.op + ": non-bool operand");
            else
                throw new IllegalArgumentException("should never reach here");
            return;
        }
        // Unary(단항 연산자)는 다음을 모두 만족할 때 타당하다.
        /*
        1. Expression(수식) term이 타당해야 한다.
        2. op가 NotOp(!)일 때 term의 타입이 bool이어야 한다.
        3. op가 NegateOp(-)일 때 term의 타입이 int 혹은 float이어야 한다.
        4. op가 타입변환 연산(float(), char())일 때 term의 타입이 int 여야 한다.
        5. op가 타입변환 연산(int()) 일 때 term의 타입이 float혹은 char 여야 한다.
        * */
        else if(e instanceof Unary){
            Unary u = (Unary)e;
            V(u.term, tm);
            Type termType = typeOf(u.term, tm);
            if(u.op.NotOp()){
                check(termType.equals(Type.BOOL), "non-bool operand for " + u.op);
            }
            else if(u.op.NegateOp()){
                check(termType.equals(Type.INT) || termType.equals(Type.FLOAT), "non-int/float operand for " + u.op);
            }
            else if(u.op.intOp()){
                check(termType.equals(Type.FLOAT)|| termType.equals(Type.CHAR), "non-int operand for " + u.op);
            }
            else if(u.op.floatOp() || u.op.charOp()){
                check(termType.equals(Type.INT), "non-float operand for " + u.op);
            }
            else{
                throw new IllegalArgumentException("should never reach here");
            }
            return;
        }
        else if(e instanceof Call){
            Variable v = new Variable(((Call)e).name);
            Expressions es = ((Call)e).args;
            check(tm.containsKey(v),
                    "undeclared function: " + v);
            Prototype p = (Prototype)tm.get(v);
            checkProtoType(p, tm, typeOf(e, tm),es);
            return;
        }
        // student exercise
        throw new IllegalArgumentException("should never reach here");
    }


    //Statement(문장)는 Block, Assignment, IfStatement, WhileStatement 로 구성됨.
    public static void V (Statement s, TypeMap tm) {
        if ( s == null )
            throw new IllegalArgumentException( "AST error: null statement");
        // skip문은 항상 타당하다.
        if (s instanceof Skip) return;
        if (s instanceof Assignment) {
            // assignment는 다음을 모두 만족할 때 타당하다.
            /*
            1. target이 선언되어 있어야 한다.
            2. source의 타입이 target의 타입과 같아야 한다.(source가 타당해야 한다.)
            3. target이 float일 때 source의 타입은 int 혹은 float이어야 한다.
            4. target이 int일 때 source의 타입은 char 혹은 int이어야 한다.
            5. 이외에는 target의 타입과 source의 타입이 같아야 한다.
            * */
            Assignment a = (Assignment)s;
            check( tm.containsKey(a.target)
                   , " undefined target in assignment: " + a.target);
            V(a.source, tm);
            Type ttype = (Type)tm.get(a.target);
            Type srctype = typeOf(a.source, tm);
            if (!ttype.equals(srctype)) {
                if (ttype.equals(Type.FLOAT))
                    check( srctype.equals(Type.INT)
                           , "mixed mode assignment to " + a.target);
                else if (ttype.equals(Type.INT))
                    check( srctype.equals(Type.CHAR)
                           , "mixed mode assignment to " + a.target);
                else
                    check( false
                           , "mixed mode assignment to " + a.target);
            }
            return;
        }
        else if (s instanceof Conditional) {
            // Conditional(조건문)은 다음을 모두 만족할 때 타당하다.
            /*
            1. Expression(수식) test가 타당하며 bool 타입이어야 한다.
            2. thenbranch와 elsebranch가 모두 타당해야 한다.
            * */
            Conditional c = (Conditional)s;
            V(c.test, tm); // test -> Expression
            Type testType = typeOf(c.test, tm);
            if(testType.equals(Type.BOOL)) {
                V(c.thenbranch, tm);
                V(c.elsebranch, tm);
                return;
            }
            else {
                check(false, "non-bool type in conditional test(Expression)" + c.test);
            }
            return;
        }
        else if(s instanceof Loop){
            // Loop(반복문)은 다음을 모두 만족할 때 타당하다.
            /*
            1. Expression(수식) test가 타당하며 bool 타입이어야 한다.
            2. body가 타당해야 한다.
            * */
            Loop l = (Loop)s;
            V(l.test, tm);
            Type testType = typeOf(l.test, tm);
            if(testType.equals(Type.BOOL)) {
                V(l.body, tm);
                return;
            }
            else {
                check(false, "non-bool type in loop test(Expression)" + l.test);
            }
            return;
        }
        else if(s instanceof Block){
            // Block(블록)은 내부의 모든 statement가 타당할 때 타당하다.
            Block b = (Block)s;
            for(Statement stmt : b.members){
                V(stmt, tm);
            }
            return;
        }
        else if(s instanceof Call){
            // Call(함수 호출)은 다음을 모두 만족할 때 타당하다.
            /*
            1. 함수가 선언되어 있어야 한다.
            2. 함수의 타입이 void여야 한다.
            3. 함수의 파라미터의 수가 일치해야 한다.
            4. 함수의 파라미터의 타입이 일치해야 한다.
            * */
            Call c = (Call)s;
            Variable v = new Variable(c.name);
            Expressions es = c.args;
            check(tm.containsKey(v), "undefined function call: " + c.name);
            checkProtoType((Prototype)tm.get(v), tm, Type.VOID, es);
            return;
        }
        else if(s instanceof Return){
            Variable fid = ((Return) s).target;
            check(tm.containsKey(fid), "undefined function: " + fid);
            V(((Return) s).result, tm);
            check(tm.get(fid).equals(typeOf(((Return) s).result, tm)),
                    "return type mismatch");
            return;
        }
        // student exercise
        throw new IllegalArgumentException("should never reach here");
    }

    public static void checkProtoType(
            Prototype p, TypeMap tm, Type t, Expressions es
    ){
        TypeMap tmp = typing(p.params); // 파라미터들의 일시적인 type map
        check(p.equals(t), "calls can only be to void functions");
        check(es.size() == p.params.size(), "incorrect number of arguments");
        for(int i = 0; i < es.size(); i++){
            // match arg types with parameter types
            Expression e = es.get(i);
            Expression e2 = p.params.get(i).v;
            check(typeOf(e, tm).equals(typeOf(e2, tmp)), "argument type mismatch");
        }
    }

    public static void main(String args[]) {
        Parser parser  = new Parser(new Lexer(args[0]));
        Program prog = parser.program();
        prog.display(0);           // student exercise
//        TypeMap map = typing(prog.globals, prog.functions);
//        map.display();
        System.out.println("\nBegin type checking...");
        V(prog);
    } //main

} // class StaticTypeCheck

