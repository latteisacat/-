//프언론 200쪽 참고
//T는 변환함수
public class TypeTransformer {

    public static Program T (Program p, TypeMap tm) {
        Functions functions = T(p.functions, tm);
        return new Program(p.globals, functions);
    }


    public static Functions T(Functions fs, TypeMap globals) {
        Functions out = new Functions();
        for (int i = 0; i < fs.size(); i++) {
            TypeMap tm = new TypeMap();
            tm.putAll(globals);
            tm.putAll(StaticTypeCheck.typing(fs.get(i).params));
            tm.putAll(StaticTypeCheck.typing(fs.get(i).locals));
            for (int j = 0; j < fs.size(); j++) {
                tm.put(new Variable(fs.get(j).id), fs.get(j).type);
            }
            Function f = new Function(fs.get(i).type, fs.get(i).id,
                    fs.get(i).params, fs.get(i).locals, (Block) T(fs.get(i).body, tm));
            out.add(f);
        }
        return out;
    }
    //Expression = Value | Variable | Binary | Unary
    public static Expression T (Expression e, TypeMap tm) {
        //Expression(수식)이 Value(값)일 경우 그대로 리턴한다.
        if (e instanceof Value) 
            return e;
        //Expression(수식)이 Variable(변수)일 경우 그대로 리턴한다.
        if (e instanceof Variable) 
            return e;
        //Expression(수식)이 Binary(이항 연산자)일 경우 피연산자의 타입에 따라 결과 타입을 변환한다.
        if (e instanceof Binary) {
            Binary b = (Binary)e; 
            Type typ1 = StaticTypeCheck.typeOf(b.term1, tm);
            Type typ2 = StaticTypeCheck.typeOf(b.term2, tm);
            Expression t1 = T (b.term1, tm);
            Expression t2 = T (b.term2, tm);
            if (typ1.equals(Type.INT))
                return new Binary(b.op.intMap(b.op.val), t1,t2);
            else if (typ1.equals(Type.FLOAT))
                return new Binary(b.op.floatMap(b.op.val), t1,t2);
            else if (typ1.equals(Type.CHAR))
                return new Binary(b.op.charMap(b.op.val), t1,t2);
            else if (typ1.equals(Type.BOOL))
                return new Binary(b.op.boolMap(b.op.val), t1,t2);
            throw new IllegalArgumentException("should never reach here");
        }
        //Expression(수식)이 Unary(단항 연산자)일 경우 피연산자의 타입에 따라 결과 타입을 변환한다.
        //혹은 연산자에 따라 타입이 뒤바뀔수도..?
        if (e instanceof Unary){
            Unary u = (Unary)e;
            Type typ = StaticTypeCheck.typeOf(u.term, tm);
            Expression t = T (u.term, tm);
            if ((typ.equals(Type.INT)) && (u.op.NotOp()))
                return new Unary(u.op.boolMap(u.op.val), t);
            else if((typ.equals(Type.FLOAT)) && (u.op.NegateOp()))
                return new Unary(u.op.floatMap(u.op.val), t);
            else if((typ.equals(Type.INT)) && (u.op.NegateOp()))
                return new Unary(u.op.intMap(u.op.val), t);
            else if(typ.equals(Type.FLOAT)  && u.op.val == "int")
                return new Unary(u.op.floatMap(u.op.val), t);
            else if(typ.equals(Type.CHAR) && u.op.val == "int")
                return new Unary(u.op.charMap(u.op.val), t);
            else if(typ.equals(Type.INT) && u.op.val == "float")
                return new Unary(u.op.intMap(u.op.val), t);
            else if(typ.equals(Type.INT) && u.op.val == "char")
                return new Unary(u.op.intMap(u.op.val), t);
            throw new IllegalArgumentException("should never reach here");
        }
        if (e instanceof Call) {
            return e;
        }
        // student exercise
        throw new IllegalArgumentException("should never reach here");
    }

    // Statement = Skip | Block | Assignment | Conditional | Loop
    public static Statement T (Statement s, TypeMap tm) {
        // Statement(상태) 가 Skip일 경우 그대로 리턴한다.
        if (s instanceof Skip) return s;
        // Statement(상태) 가 Assignment(할당)일 경우 좌변의 Variable(변수) 타입에 따라 우변 Expression(수식)의 타입을 변환한다.
        if (s instanceof Assignment) {
            Assignment a = (Assignment)s;
            Variable target = a.target;
            Expression src = T (a.source, tm);
            Type ttype = (Type)tm.get(a.target);
            Type srctype = StaticTypeCheck.typeOf(a.source, tm);
            if (ttype.equals(Type.FLOAT)) {
                if (srctype.equals(Type.INT)) {
                    src = new Unary(new Operator(Operator.I2F), src);
                    srctype = Type.FLOAT;
                }
            }
            else if (ttype.equals(Type.INT)) {
                if (srctype.equals(Type.CHAR)) {
                    src = new Unary(new Operator(Operator.C2I), src);
                    srctype = Type.INT;
                }
            }
            StaticTypeCheck.check( ttype.equals(srctype),
                      "bug in assignment to " + target);
            return new Assignment(target, src);
        }
        // Statement(상태) 가 Call(함수호출)일 경우 그대로 리턴한다.
        if (s instanceof Call) {
            Expressions expressions = new Expressions();
            for(Expression arg : ((Call)s).args) {
                expressions.add(T(arg, tm));
            }
            return new Call(((Call) s).name, expressions);
        }
        // Statement(상태) 가 Conditional(조건문)일 경우 Expression(수식), thenbranch, elsebranch의 타입변환을 수행한다.
        if (s instanceof Conditional) {
            Conditional c = (Conditional)s;
            Expression test = T (c.test, tm);
            Statement tbr = T (c.thenbranch, tm);
            Statement ebr = T (c.elsebranch, tm);
            return new Conditional(test,  tbr, ebr);
        }
        // Statement(상태) 가 Loop(반복문)일 경우 Expression(수식), body의 타입변환을 수행한다.
        if (s instanceof Loop) {
            Loop l = (Loop)s;
            Expression test = T (l.test, tm);
            Statement body = T (l.body, tm);
            return new Loop(test, body);
        }
        // Statement(상태) 가 Block(블록)일 경우 내부의 모든 Statement(상태)의 타입변환을 수행한다.
        if (s instanceof Block) {
            Block b = (Block)s;
            Block out = new Block();
            for (Statement stmt : b.members)
                out.members.add(T(stmt, tm));
            return out;
        }
        // Statement(상태) 가 Return(반환)일 경우 Expression(수식)의 타입변환을 수행한다.
        if (s instanceof Return) {
            Expression resultExpression = T(((Return) s).result, tm);
            return new Return(((Return)s).target , resultExpression);
        }
        throw new IllegalArgumentException("should never reach here");
    }
    

    public static void main(String args[]) {
        Parser parser  = new Parser(new Lexer(args[0]));
        Program prog = parser.program();
        prog.display(0);           // student exercise
        System.out.println("\nBegin type checking...");
        System.out.println("Type map:");
        TypeMap map = StaticTypeCheck.typing(prog.globals, prog.functions);
        map.display();    // student exercise
        StaticTypeCheck.V(prog);
        Program out = T(prog, map);
        System.out.println("Output AST");
        out.display(0);    // student exercise
    } //main

    } // class TypeTransformer

    
