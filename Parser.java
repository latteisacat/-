import java.util.*;

public class Parser {
    // Recursive descent parser that inputs a C++Lite program and 
    // generates its abstract syntax.  Each method corresponds to
    // a concrete syntax grammar rule, which appears as a comment
    // at the beginning of the method.
  
    Token token;          // current token from the input stream
    Lexer lexer;
  
    public Parser(Lexer ts) { // Open the C++Lite source program
        lexer = ts;                          // as a token stream, and
        token = lexer.next();            // retrieve its first Token
    }
  
    private String match (TokenType t) {
        String value = token.value();
        if (token.type().equals(t))
            token = lexer.next();
        else
            error(t);
        return value;
    }
  
    private void error(TokenType tok) {
        System.err.println("Syntax error: expecting: " + tok 
                           + "; saw: " + token);
        lexer.getLineNum();
        System.exit(1);
    }
  
    private void error(String tok) {
        System.err.println("Syntax error: expecting: " + tok 
                           + "; saw: " + token);
        lexer.getLineNum();
        System.exit(1);
    }

    public Program program() {
        // Program --> {Type Identifier FunctionOrGlobal} MainFunction
        Program prog = new Program();
        prog.globals = new Declarations();
        prog.functions = new Functions();
        Type t;
        Declaration d;
        Function f;
        while(!token.type().equals(TokenType.Eof)){
            t=type();
            if(token.type().equals(TokenType.Identifier)){
                d = new Declaration(new Variable(token.value()),t);
                token = lexer.next();
                TokenType tt = token.type();
                if(tt.equals(TokenType.Comma)||tt.equals(TokenType.Semicolon)){
                    prog.globals.add(d);
                    while(token.type().equals(TokenType.Comma)){
                        token = lexer.next();
                        if(token.type().equals(TokenType.Identifier)){
                            d = new Declaration(new Variable(token.value()),t);
                            prog.globals.add(d);
                            token = lexer.next();
                        }
                        else{
                            error("Identifier");
                        }
                    }
                    token = lexer.next();
                }
                else if (tt.equals(TokenType.LeftParen)){
                    f = new Function(t, d.v.toString());

                    f = functionRest(f);
                    prog.functions.add(f);
                }
                else{
                    error("FunctionOrGlobal");
                }
            }
            else{
                error("Identifier");
            }
        }
        return prog;
    }
  
    private Declarations declarations() {
        // Declarations --> { Declaration }
	Declarations ds = new Declarations();
	while (isType()){
		declaration(ds);
	}
        return ds;  // student exercise
    }
  
    private void declaration (Declarations ds) {
        // Declaration  --> Type Identifier { , Identifier } ;
        Variable v;
        Declaration d;
        Type t = type();
        v = new Variable(match(TokenType.Identifier));
        d = new Declaration(v, t);	
        ds.add(d);

            while (isComma()) {	
                token = lexer.next();	
                v = new Variable(match(TokenType.Identifier));
                d = new Declaration(v, t);
                //d = (v, t);	
                ds.add(d);
            }
        match(TokenType.Semicolon);
	}

    private Function functionRest(Function f){
        //functionRest --> ( Parameters ) {Declarations Statements}
        //Parameters --> Parameter { , Parameter }
        f.params = new Declarations();
        f.locals = new Declarations();
        match(TokenType.LeftParen);
        while(isType()){
            f.params=parameter(f.params);
            if(token.type().equals(TokenType.Comma)){
                match(TokenType.Comma);
            }
        }
        match(TokenType.RightParen);
        match(TokenType.LeftBrace);
        f.locals = declarations();
        f.body = progStatements(new Variable(f.id));
        match(TokenType.RightBrace);
        return f;
    }

    private Declarations parameter(Declarations params){
        //Parameter --> Type Identifier
        Declaration d = new Declaration();
        d.t = new Type(token.value());
        token = lexer.next();
        if(token.type().equals(TokenType.Identifier)){
            d.v = new Variable(match(TokenType.Identifier));
        }
        else{
            error("Identifier");
        }
        params.add(d);
        return params;
    }

    private Type type () {
        // Type  -->  int | bool | float | char 
        // look up enum in API amke sure that this is working 
        Type t = null;
        switch(token.type()){
            case Int:
                t = Type.INT;
                break;
            case Bool:
                t = Type.BOOL;
                break;
            case Float:
                t = Type.FLOAT;
                break;
            case Char:
                t = Type.CHAR;
                break;
            case Void:
                t = Type.VOID;
                break;
            default: error ("Error in Type construction"); break;
        }
        token = lexer.next();
        return t;          
    }
  
    private Statement statement(Variable functionName) {
        // Statement --> ; | Block | Assignment | IfStatement | WhileStatement
        Statement s = null;
        switch(token.type()){
            case Semicolon:
                s = new Skip();
                break;
            case LeftBrace:
                s = statements(functionName);
                break;
            case If:
                s = ifStatement(functionName);
                break;
            case While:
                s = whileStatement(functionName);
                break;
            case Identifier:
                s = assignOrCall();
                break;
            case Return:
                s = returnStatement(functionName);
                break;
            default: error ("Error in Statement construction"); break;
        }
        return s;
    }
  
    private Block statements(Variable functionName) {
        // Block --> '{' Statements '}'
        // Statements --> {Statement}
	    Statement s;
	    Block b = new Block();
	
	    match(TokenType.LeftBrace);
        System.out.println(token.type().toString());
        while (isStatement()) {
            s = statement(functionName);
            b.members.add(s);
        }
        match(TokenType.RightBrace);// end of the block
        return b;
    }

    private Block progStatements(Variable functionName) {
        // Block --> '{' Statements '}'
        // Statements --> {Statement}
	    Statement s;
	    Block b = new Block();

	    // match(TokenType.LeftBrace, "Block progStatements LeftBrace검사");
        while (isStatement()) {
            s = statement(functionName);
            b.members.add(s);
        }
        return b;
    }

    private Assignment assignment( ) {
        // Assignment --> Identifier = Expression ;
        Expression source; 
        Variable target; 
        
        target = new Variable(match(TokenType.Identifier));
        match(TokenType.Assign);
        source = expression();
        match(TokenType.Semicolon);
        return new Assignment(target, source); 
    }

    private Statement assignOrCall(){
        Variable v = new Variable(token.value());
        Call c = new Call();
        token = lexer.next(); // skip identifier
        if(token.type().equals(TokenType.Assign)){
            token = lexer.next();
            Expression src =expression();
            match(TokenType.Semicolon);
            return new Assignment(v, src);
        }
        else if(token.type().equals(TokenType.LeftParen)){
            c.name = v.toString();
            token = lexer.next(); // skip left paren
            c.args = arguments();
            match(TokenType.RightParen);
            match(TokenType.Semicolon);
            return c;
        }
        else{
            error("Assign or Call");
            return null;
        }
    }

    private Expressions arguments(){
        //Arguments -->[ Expression { , Expression }]
        Expressions args = new Expressions();
        args.add(expression());
        while(!token.type().equals(TokenType.RightParen)){
            if(token.type().equals(TokenType.Comma)){
                match(TokenType.Comma);
            }
            else if(!token.type().equals(TokenType.RightParen)){
                error("Expression");
            }
            args.add(expression());
        }
        if(args.size() == 0)
            args = null;
        return args;
    }

    private Statement returnStatement(Variable functionName){
        //ReturnStatement --> return Expression ;
        Return r = new Return();
        match(TokenType.Return); // skip return
        r.target = new Variable(functionName.toString());
        r.result = expression();
        match(TokenType.Semicolon);
        return r;
    }
    private Conditional ifStatement(Variable functionName) {
        // IfStatement --> if ( Expression ) Statement [ else Statement ]
        Conditional con;
        Statement s;
        Expression test;
        
        match(TokenType.If);
        match(TokenType.LeftParen);
        test = expression();
        match(TokenType.RightParen);

        if(token.type().equals(TokenType.Semicolon)){
            // if문 뒤에 바로 세미콜론이 나오는 경우
            s = new Skip();
            token = lexer.next();
        }
        else{
            s = statement(functionName);
        }

        if (token.type().equals(TokenType.Else)) {
            match(TokenType.Else);
            Statement elseState = statement(functionName);
            con = new Conditional(test, s, elseState);
        }
        else {
            con = new Conditional(test, s);
        }
	    return con;
    }
  
    private Loop whileStatement(Variable functionName) {
        // WhileStatement --> while ( Expression ) Statement
        Statement body;
        Expression test;

        match(TokenType.While);
        match(TokenType.LeftParen);
        test = expression();
        match(TokenType.RightParen);
        body = statement(functionName);
        return new Loop(test, body);
    }

    private Expression expression() {
        // Expression --> Conjunction { || Conjunction }
        Expression c = conjunction();
        while (token.type().equals(TokenType.Or)) {
            Operator op = new Operator(match(token.type()));
            Expression e = conjunction();
            c = new Binary(op, c, e);
        }
        return c;  // student exercise
    }
  
    private Expression conjunction() {
        // Conjunction --> Equality { && Equality }
        Expression eq = equality();
        while (token.type().equals(TokenType.And)) {
            Operator op = new Operator(match(token.type()));
            Expression c = equality();
            eq = new Binary(op, eq, c);
        }
        return eq;  
    }
  
    private Expression equality() {
        // Equality --> Relation [ EquOp Relation ]
        Expression rel = relation();
        if (isEqualityOp()) {
            Operator op = new Operator(match(token.type()));
            Expression rel2 = relation();
            rel = new Binary(op, rel, rel2);
        }
        return rel;  // student exercise
    }

    private Expression relation() {
        // Relation --> Addition [RelOp Addition] 
        Expression a = addition();
        if (isRelationalOp()) {
            Operator op = new Operator(match(token.type()));
            Expression a2 = addition();
            a = new Binary(op, a, a2);
        }
        return a;  // student exercise
    }
  
    private Expression addition() {
        // Addition --> Term { AddOp Term }
        Expression e = term();
        while (isAddOp()) {
            Operator op = new Operator(match(token.type()));
            Expression term2 = term();
            e = new Binary(op, e, term2);
        }
        return e;
    }
  
    private Expression term() {
        // Term --> Factor { MultiplyOp Factor }
        Expression e = factor();
        while (isMultiplyOp()) {
            Operator op = new Operator(match(token.type()));
            Expression term2 = factor();
            e = new Binary(op, e, term2);
        }
        return e;
    }
  
    private Expression factor() {
        // Factor --> [ UnaryOp ] Primary 
        if (isUnaryOp()) {
            Operator op = new Operator(match(token.type()));
            Expression term = primary();
            return new Unary(op, term);
        }
        else return primary();
    }
  
    private Expression primary () {
        // Primary --> Identifier | Literal | ( Expression )
        //             | Type ( Expression )
        Expression e = null;
        if (token.type().equals(TokenType.Identifier)) {
            Variable v = new Variable(match(TokenType.Identifier));
            e = v;
            if (token.type().equals(TokenType.LeftParen)) {
                token = lexer.next();
                Call c = new Call();
                c.name = v.toString();
                c.args = arguments();
                match(TokenType.RightParen);
                e = c;
            }
        } else if (isLiteral()) {
            e = literal();
        } else if (token.type().equals(TokenType.LeftParen)) {
            token = lexer.next();
            e = expression();       
            match(TokenType.RightParen);
        } else if (isType( )) {
            Operator op = new Operator(match(token.type()));
            match(TokenType.LeftParen);
            Expression term = expression();
            match(TokenType.RightParen);
            e = new Unary(op, term);
        } else error("Identifier | Literal | ( | Type");
        return e;
    }

    private Value literal( ) { // take the stringy part and convert it to the correct return new  typed value. cast it to the correct value
        Value value = null;
        String stval = token.value();
        
        switch(token.type()){
            case IntLiteral:
                value = new IntValue (Integer.parseInt(stval));
                break;
            case FloatLiteral:
                value = new FloatValue(Float.parseFloat(stval));
                break;
            case CharLiteral:
                value = new CharValue(stval.charAt(0));
                break;
            case True:
                value = new BoolValue(true);
                break;
            case False:
                value = new BoolValue(false);
                break;
            default: error ("Error in literal value contruction"); break;
        }
        token = lexer.next();
	    return value;
    }
  
    private boolean isBooleanOp() {
	return token.type().equals(TokenType.And) || 
	    token.type().equals(TokenType.Or);
    } 

    private boolean isAddOp( ) {
        return token.type().equals(TokenType.Plus) ||
               token.type().equals(TokenType.Minus);
    }
    
    private boolean isMultiplyOp( ) {
        return token.type().equals(TokenType.Multiply) ||
               token.type().equals(TokenType.Divide);
    }
    
    private boolean isUnaryOp( ) {
        return token.type().equals(TokenType.Not) ||
               token.type().equals(TokenType.Minus);
    }
    
    private boolean isEqualityOp( ) {
        return token.type().equals(TokenType.Equals) ||
            token.type().equals(TokenType.NotEqual);
    }
    
    private boolean isRelationalOp( ) {
        return token.type().equals(TokenType.Less) ||
               token.type().equals(TokenType.LessEqual) || 
               token.type().equals(TokenType.Greater) ||
               token.type().equals(TokenType.GreaterEqual);
    }
    
    private boolean isType( ) {
        return token.type().equals(TokenType.Int)
            || token.type().equals(TokenType.Bool) 
            || token.type().equals(TokenType.Float)
            || token.type().equals(TokenType.Char);
    }
    
    private boolean isLiteral( ) {
        return token.type().equals(TokenType.IntLiteral) ||
            isBooleanLiteral() ||
            token.type().equals(TokenType.FloatLiteral) ||
            token.type().equals(TokenType.CharLiteral);
    }
    
    private boolean isBooleanLiteral( ) {
        return token.type().equals(TokenType.True) ||
            token.type().equals(TokenType.False);
    }

    private boolean isComma( ) {
	return token.type().equals(TokenType.Comma);
    }
   
    private boolean isSemicolon( ) {
	return token.type().equals(TokenType.Semicolon);
    }

    private boolean isLeftBrace() {
	return token.type().equals(TokenType.LeftBrace);
    } 
 
    private boolean isRightBrace() {
	return token.type().equals(TokenType.RightBrace);
    } 

    private boolean isStatement() {
	return 	isSemicolon() ||
		isLeftBrace() ||
		token.type().equals(TokenType.If) ||
		token.type().equals(TokenType.While) ||
		token.type().equals(TokenType.Identifier)||
        token.type().equals(TokenType.Return);
    }
 
    public static void main(String args[]) {
        Parser parser  = new Parser(new Lexer(args[0]));
        Program prog = parser.program();
        prog.display(0);           // display abstract syntax tree
    } //main

} // Parser