import java.util.ArrayList;

// Following is the semantics class:
// The meaning M of a Statement is a State
// The meaning M of a Expression is a Value
public class Semantics {
    //M은 Clite의 추상구문 Program의 의미(Meaning)을 나타내는 함수 이다.
    //Program은 새로운 state를 생성한다.
    //Program은 Declarations 와 Functions로 구성되어 있다.

    State globalState;
    Functions functions;
    State M (Program p) {
        globalState = new State();
        // 전역변수 스택에 집어넣고 초기화하는 부분
        globalState = globalState.allocate(p.globals);
        globalState.dynamicLink = globalState.stackPointer;
        globalState.staticLink = globalState.stackPointer;
        functions = p.functions;
        System.out.println("Initial State :");
        globalState.display();
        return M(p.functions, globalState);
    }

    State M (Functions fs, State callerState){
        Function main = findFunction("main");
        if(main == null){
            System.out.println("main function not found");
            System.exit(0);
        }
        State myState = new State(callerState);
        myState.dynamicLink = callerState.stackPointer;

        myState = myState.allocate(main.locals);
        System.out.println();
        System.out.println("------------------------------------");
        System.out.println("Start Main Function :");
        myState.display();
        System.out.println("------------------------------------");
        System.out.println();

        myState = M(main.body, myState);

        System.out.println();
        System.out.println("------------------------------------");
        System.out.println("Finish Main Function :");
        myState.display();
        System.out.println("------------------------------------");
        System.out.println();

        myState = myState.deallocate(main.locals);

        callerState = callerState.onion(myState);
        return callerState;
    }

    Function findFunction(String name){
        for(int i = 0; i < functions.size(); i++){
            Function f = functions.get(i);
            if(f.id.equals(name)){
                return f;
            }
        }
        return null;
    }

    //Statement는 Skip, Assignment, Conditional, Loop, Block으로 구성된다.
    //Statement의 의미는 State(상태)의 전이 이다. 각 Statement의 의미는 문장의 종류에따라 결정된다.
    State M (Statement s, State state) {
        if (s instanceof Skip) return M((Skip)s, state);
        if (s instanceof Assignment) return M((Assignment)s, state);
        if (s instanceof Conditional) return M((Conditional)s, state);
        if (s instanceof Loop) return M((Loop)s, state);
        if (s instanceof Block) return M((Block)s, state);
        if (s instanceof Call) return M((Call)s, state);
        if (s instanceof Return) return M((Return)s, state);
        throw new IllegalArgumentException("should never reach here");
    }
    //Skip은 상태를 변경하지 않는다.
    State M (Skip s, State state) {
        return state;
    }
    //Assignment는 Variable과 Expression으로 구성된다.
    //Assignment는 현재 상태의 Expression(source)을 연산하여 얻어낸 값을(Variable)target에 대입한다.
    State M (Assignment a, State state) {
        return state.onion(new State(state ,a.target, M (a.source, state)));
    }

    State M (Block b, State state) {
        for (Statement s : b.members) {
            state = M(s, state);
            if (s instanceof Return){
                return state;
            }
        }
        return state;
    }
    //Conditional은 Expression test, Statement thenbranch, Statement elsebranch로 구성된다.
            //elsebranch가 생략될 경우 skip으로 대체한다.
            //Expression test의 계산결과에 따라 state가 달라진다.
    State M (Conditional c, State state) {
        if (M(c.test, state).boolValue( ))
            return M (c.thenbranch, state);
        else
            return M (c.elsebranch, state);
    }
    //Loop는 Expression test, Statement body로 구성된다.
    //Expression test의 계산결과가 true일 경우 body를 실행한다.
    //Expression test의 계산결과가 false일 경우 state는 변하지 않는다.
    State M (Loop l, State state) {
        if (M (l.test, state).boolValue( ))
            //반복문이 재귀적으로 정의되어 있다.
            return M(l, M (l.body, state));
        else return state;
    }

    State M(Call c, State outState){
        Function f = findFunction(c.name);
        if(f == null){
            System.out.println("function not found");
            System.exit(0);
        }
        State myState = new State(outState);
        myState.dynamicLink = outState.dynamicLink;
        myState = addFrame(myState, c, f);
        myState.dynamicLink = outState.stackPointer;

        System.out.println("Call Function " + c.name + " : ");
        myState.display();

        myState = M(f.body, myState);
        myState = removeFrame(myState, c, outState);
        return myState;
    }

    State addFrame(State current, Call c, Function f){
        State myState = new State(current);
        myState.minus(current.stackPointer  -  current.dynamicLink);
        myState.dynamicLink = current.stackPointer;
        myState = myState.onion(globalState);
        myState = myState.allocate(f.params);
        for(int i = 0; i < f.params.size(); i++){
            Expression e = c.args.get(i);
            Declaration d = f.params.get(i);
            Variable v = d.v;
            myState = myState.onion(new State(myState, v, M(e, current)));
        }
        myState = myState.allocate(f.locals);
        Declarations ds = new Declarations();
        ds.add(new Declaration(new Variable(f.id), f.type));
        myState = myState.allocate(ds);
        myState.dynamicLink = current.stackPointer;
        return myState;
    }

    State removeFrame(State current, Call c, State former){
        Function f = findFunction(c.name);
        if(f == null){
            System.out.println("function not found");
            System.exit(0);
        }
        Declarations ds = new Declarations();
        ds.add(new Declaration(new Variable(f.id), f.type));
        State myState = current.deallocate(ds);
        myState = myState.deallocate(f.locals);
        myState = myState.deallocate(f.params);
        myState = myState.onion(globalState);
        myState = myState.plus(former);
        myState.dynamicLink = former.dynamicLink;
        return myState;
    }
    State M (Return r, State outState){
        return outState.onion(new State(outState ,r.target, M(r.result, outState)));
    }

    Value applyBinary (Operator op, Value v1, Value v2) {
        StaticTypeCheck.check( ! v1.isUndef( ) && ! v2.isUndef( ),
                "reference to undef value");
        // 정수 산술 연산자
        if (op.val.equals(Operator.INT_PLUS))
            return new IntValue(v1.intValue( ) + v2.intValue( ));
        if (op.val.equals(Operator.INT_MINUS))
            return new IntValue(v1.intValue( ) - v2.intValue( ));
        if (op.val.equals(Operator.INT_TIMES))
            return new IntValue(v1.intValue( ) * v2.intValue( )); if (op.val.equals(Operator.INT_DIV))
            return new IntValue(v1.intValue( ) / v2.intValue( ));
        // 부동 소수점 산술 연산자
        if (op.val.equals(Operator.FLOAT_PLUS))
            return new FloatValue(v1.floatValue( ) + v2.floatValue( ));
        if (op.val.equals(Operator.FLOAT_MINUS))
            return new FloatValue(v1.floatValue( ) - v2.floatValue( ));
        if (op.val.equals(Operator.FLOAT_TIMES))
            return new FloatValue(v1.floatValue( ) * v2.floatValue( ));
        if (op.val.equals(Operator.FLOAT_DIV))
            return new FloatValue(v1.floatValue( ) / v2.floatValue( ));
        // 정수 관계연산자
        if (op.val.equals(Operator.INT_EQ))
            return new BoolValue(v1.intValue( ) == v2.intValue( ));
        if (op.val.equals(Operator.INT_NE))
            return new BoolValue(v1.intValue( ) != v2.intValue( ));
        if (op.val.equals(Operator.INT_LT))
            return new BoolValue(v1.intValue( ) < v2.intValue( ));
        if (op.val.equals(Operator.INT_LE))
            return new BoolValue(v1.intValue( ) <= v2.intValue( ));
        if (op.val.equals(Operator.INT_GT))
            return new BoolValue(v1.intValue( ) > v2.intValue( ));
        if (op.val.equals(Operator.INT_GE))
            return new BoolValue(v1.intValue( ) >= v2.intValue( ));
        // 부동소수점 관계 연산자
        if (op.val.equals(Operator.FLOAT_EQ))
            return new BoolValue(v1.floatValue( ) == v2.floatValue( ));
        if (op.val.equals(Operator.FLOAT_NE))
            return new BoolValue(v1.floatValue( ) != v2.floatValue( ));
        if (op.val.equals(Operator.FLOAT_LT))
            return new BoolValue(v1.floatValue( ) < v2.floatValue( ));
        if (op.val.equals(Operator.FLOAT_LE))
            return new BoolValue(v1.floatValue( ) <= v2.floatValue( ));
        if (op.val.equals(Operator.FLOAT_GT))
            return new BoolValue(v1.floatValue( ) > v2.floatValue( ));
        if (op.val.equals(Operator.FLOAT_GE))
            return new BoolValue(v1.floatValue( ) >= v2.floatValue( ));
        // Char 관계 연산자
        if (op.val.equals(Operator.CHAR_EQ)) return new BoolValue(v1.charValue( ) == v2.charValue( ));
        if (op.val.equals(Operator.CHAR_NE))
            return new BoolValue(v1.charValue( ) != v2.charValue( ));
        if (op.val.equals(Operator.CHAR_LT))
            return new BoolValue(v1.charValue( ) < v2.charValue( ));
        if (op.val.equals(Operator.CHAR_LE))
            return new BoolValue(v1.charValue( ) <= v2.charValue( ));
        if (op.val.equals(Operator.CHAR_GT))
            return new BoolValue(v1.charValue( ) > v2.charValue( ));
        if (op.val.equals(Operator.CHAR_GE))
            return new BoolValue(v1.charValue( ) >= v2.charValue( ));
        // Bool 관계연산자
        if (op.val.equals(Operator.BOOL_EQ))
            return new BoolValue(v1.boolValue( ) == v2.boolValue( ));
        if (op.val.equals(Operator.BOOL_NE))
            return new BoolValue(v1.boolValue( ) != v2.boolValue( ));
        if (op.val.equals(Operator.BOOL_LT))
        {
            if(v1.boolValue( ) == false && v2.boolValue( ) == true)
                return new BoolValue(true);
            else
                return new BoolValue(false);
        }
        if (op.val.equals(Operator.BOOL_LE))
        {
            if(v1.boolValue( ) == true && v2.boolValue( ) == false)
                return new BoolValue(false);
            else
                return new BoolValue(true);
        }
        if (op.val.equals(Operator.BOOL_GT))
        {
            if(v1.boolValue( ) == true && v2.boolValue( ) == false)
                return new BoolValue(true);
            else
                return new BoolValue(false);
        }
        if (op.val.equals(Operator.BOOL_GE))
        { if(v1.boolValue( ) == false && v2.boolValue( ) == true)
            return new BoolValue(false);
        else
            return new BoolValue(true);
        }
        //논리 연산자
        if (op.val.equals(Operator.AND))
        {
            if(!v1.boolValue()) {
                return new BoolValue(false);
            }
            else{
                return new BoolValue(v2.boolValue());
            }
        }
        if (op.val.equals(Operator.OR)){
            if(v1.boolValue()) {
                return new BoolValue(true);
            }
            else{
                return new BoolValue(v2.boolValue());
            }
        }
        throw new IllegalArgumentException("should never reach here");
    }
    //Unary의 의미는 다음과 같이 정의한다.
 /*
 1. 피연산자 term이 정의되어 있지 않다면 의미를 가질 수 없다.
 2. 논리 부정 연산자(!)의 경우 피연산자 term의 논리값을 반전시킨다.
 3. 부호 연산자(-)의 경우 피연산자 term의 부호를 반전시킨다.
 4. 정수를 부동 소수점으로 변환하는 연산자(i2f)의 경우 피연산자 term의 정수값을 부
동 소수점으로 변환한다.
 5. 부동 소수점을 정수로 변환하는 연산자(f2i)의 경우 피연산자 term의 부동 소수점 값
을 정수로 변환한다.
 단, float의 정수부가 int의 범위를 넘어간다면 정의되지 않은 값(undefined)을 반환한다.
 6. 문자를 정수로 변환하는 연산자(c2i)의 경우 피연산자 term의 문자값을 아스키 코드
기반의 정수로 변환한다. 7. 정수를 문자로 변환하는 연산자(i2c)의 경우 피연산자 term의 정수값을 아스키 코드
기반의 문자로 변환한다.
 단, 정수가 0보다 작거나 255보다 크다면 정의되지 않은 값(undefined)을 반환한다.
 **/
    Value applyUnary (Operator op, Value v) {
        StaticTypeCheck.check( ! v.isUndef( ),
                "reference to undef value");
        if (op.val.equals(Operator.NOT))
            return new BoolValue(!v.boolValue( ));
        else if (op.val.equals(Operator.INT_NEG))
            return new IntValue(-v.intValue( ));
        else if (op.val.equals(Operator.FLOAT_NEG))
            return new FloatValue(-v.floatValue( ));
        else if (op.val.equals(Operator.I2F))
            return new FloatValue((float)(v.intValue( )));
        else if (op.val.equals(Operator.F2I))
            return new IntValue((int)(v.floatValue( )));
        else if (op.val.equals(Operator.C2I))
            return new IntValue((int)(v.charValue( )));
        else if (op.val.equals(Operator.I2C))
            return new CharValue((char)(v.intValue( )));
        throw new IllegalArgumentException("should never reach here");
    }
    //Expression은 Value, Variable, Binary, Unary로 구성된다.
 /*
 1. Expression이 Value일 경우 해당 Value를 반환한다.
 2. Expression이 Variable일 경우 현재 상태에서 Variable이 가지는 Value의 값이다.
 3. Expression이 Binary일 경우 피연산자 term1과 term2의 의미를 먼저 결정하고
 그 Value에 Operator op를 적용한다.
 4. Expression이 Unary일 경우 피연산자 term의 의미를 먼저 결정하고
 그 Value에 Operator op를 적용한다.
 **/

    Value applyCall (Call c, State outState) {
        Function f = findFunction(c.name);
        if(f == null){
            System.out.println("function not found");
            System.exit(0);
        }
        State myState = new State(outState);
        myState.dynamicLink = outState.dynamicLink;
        myState = addFrame(myState, c, f);
        myState.dynamicLink = outState.stackPointer;
        System.out.println("------------------------------------");
        System.out.println("Call Function " + c.name + " : ");
        myState.display();
        System.out.println("------------------------------------");

        myState = M(f.body, myState);
        Value returnValue = myState.memory.get(myState.getAddr(new Variable(f.id)));
        myState = removeFrame(myState, c, outState);
        System.out.println();
        System.out.println("Function " + c.name + " return " + returnValue);
        System.out.println();
        return returnValue;
    }
    Value M (Expression e, State state) {
        if (e instanceof Value)
            return (Value)e;
        if (e instanceof Variable)
            return (Value)(state.memory.get(state.getAddr((Variable)e)));
        if (e instanceof Binary) {
            Binary b = (Binary)e;
            return applyBinary (b.op,
                    M(b.term1, state), M(b.term2, state));
        }
        if (e instanceof Unary) {
            Unary u = (Unary)e;
            return applyUnary(u.op, M(u.term, state));
        }
        if (e instanceof Call) {
                return applyCall((Call)e, state);
        }
        throw new IllegalArgumentException("should never reach here");
    }
    public static void main(String args[]) {
        Parser parser = new Parser(new Lexer(args[0]));
        Program prog = parser.program();
        prog.display(0); // student exercise
        System.out.println("\nBegin type checking...");
        System.out.println("Type map:");
        TypeMap map = StaticTypeCheck.typing(prog.globals, prog.functions);
        map.display(); // student exercise
        StaticTypeCheck.V(prog);
        Program out = TypeTransformer.T(prog, map);
        System.out.println("Output AST");
        out.display(0); // student exercise
        Semantics semantics = new Semantics( );
        State state = semantics.M(out);
        System.out.println("Final State");
        state.display(); // student exercise
    }
}