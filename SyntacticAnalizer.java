import java.io.IOException;
import java.util.ArrayList;

public class SyntacticAnalizer {
    private ArrayList<Token> tokens;
    private int index = 0;
    private SemanticAnalizer semanticAnalizer;
    private RetVal rv;

    public SyntacticAnalizer(String filename) throws IOException {
        LexicalAnalizer lexicalAnalizer = new LexicalAnalizer(filename);
        lexicalAnalizer.validTokens();
        this.tokens = lexicalAnalizer.getTokens();
        semanticAnalizer = new SemanticAnalizer();
        rv = new RetVal(new Type(TypeBase.TB_INT,null,-1), false, true, CtVal.fromInt(0));
    }

    public boolean consume(TokenType type){
        if (index >= tokens.size()) return false;
        if(tokens.get(index).getType() == type){
            index++;
            return true;
        }
        return false;
    }

    public void error(String message){
        System.out.println("Error: " + message + " at line " + tokens.get(index).getLine());
        System.exit(1);
    }

    public RetVal expression() {
        if (functionCall(false)) {
            return rv;
        }

        RetVal left = term();
        if (left == null) {
            error("Invalid expression");
            return null;
        }

        TokenType op;
        while (consume(TokenType.ADD) || consume(TokenType.SUB)) {
            op = tokens.get(index - 1).getType();
            RetVal right = term();
            if (right == null) {
                error("Missing term after operator");
                return null;
            }

            Type resultType = semanticAnalizer.getArithType(left.type, right.type);
            CtVal resultCt = null;

            if (left.isCtVal && right.isCtVal) {
                if (resultType.typeBase == TypeBase.TB_INT) {
                    int a = left.ctVal.i;
                    int b = right.ctVal.i;
                    resultCt = CtVal.fromInt(op == TokenType.ADD ? a + b : a - b);
                } else if (resultType.typeBase == TypeBase.TB_DOUBLE) {
                    double a = left.type.typeBase == TypeBase.TB_INT ? left.ctVal.i : left.ctVal.d;
                    double b = right.type.typeBase == TypeBase.TB_INT ? right.ctVal.i : right.ctVal.d;
                    resultCt = CtVal.fromDouble(op == TokenType.ADD ? a + b : a - b);
                }
            }

            left = new RetVal(resultType, false, resultCt != null, resultCt);
        }

        return left;
    }

    //multiplication and division
    private RetVal term() {
        RetVal left = factor();
        if (left == null) return null;

        TokenType op;
        while (consume(TokenType.MUL) || consume(TokenType.DIV)) {
            op = tokens.get(index - 1).getType();
            RetVal right = factor();
            if (right == null) {
                error("Missing factor after operator");
                return null;
            }

            if ((left.type.typeBase == TypeBase.TB_CHAR && left.type.nElements != -1) ||
                    (right.type.typeBase == TypeBase.TB_CHAR && right.type.nElements != -1)) {
                error("Cannot apply arithmetic operations to strings");
                return null;
            }

            Type resultType = semanticAnalizer.getArithType(left.type, right.type);
            CtVal resultCt = null;

            if (left.isCtVal && right.isCtVal) {
                if (resultType.typeBase == TypeBase.TB_INT) {
                    int a = left.ctVal.i;
                    int b = right.ctVal.i;
                    resultCt = CtVal.fromInt(op == TokenType.MUL ? a * b : a / b);
                } else if (resultType.typeBase == TypeBase.TB_DOUBLE) {
                    double a = left.type.typeBase == TypeBase.TB_INT ? left.ctVal.i : left.ctVal.d;
                    double b = right.type.typeBase == TypeBase.TB_INT ? right.ctVal.i : right.ctVal.d;
                    resultCt = CtVal.fromDouble(op == TokenType.MUL ? a * b : a / b);
                }
            }

            left = new RetVal(resultType, false, resultCt != null, resultCt);
        }

        return left;
    }

    //constants, variables, (expressions), neg
    private RetVal factor() {
        if (functionCall(false)) {
            return rv;
        }else if (consume(TokenType.CT_INT)) {
            int val = (int) tokens.get(index - 1).getValue();
            return new RetVal(new Type(TypeBase.TB_INT, null, -1), false, true, CtVal.fromInt(val));

        } else if (consume(TokenType.CT_REAL)) {
            double val = (double) tokens.get(index - 1).getValue();
            return new RetVal(new Type(TypeBase.TB_DOUBLE, null, -1), false, true, CtVal.fromDouble(val));

        } else if (consume(TokenType.CT_CHAR)) {
            Object tokVal = tokens.get(index - 1).getValue();
            int val = 0;

            if (tokVal instanceof Character)
                val = (char) tokVal;
            else if (tokVal instanceof String str && str.length() == 1)
                val = str.charAt(0);

            return new RetVal(new Type(TypeBase.TB_CHAR, null, -1), false, true, CtVal.fromInt(val));

        } else if (consume(TokenType.CT_STRING)) {
            String val = tokens.get(index - 1).getValue().toString();
            return new RetVal(
                    new Type(TypeBase.TB_CHAR, null, 0),
                    false,
                    true,
                    CtVal.fromString(val)
            );

        } else if (consume(TokenType.ID)) {
            String name = tokens.get(index - 1).getValue().toString();
            Symbol s = semanticAnalizer.findSymbol(semanticAnalizer.symbols, name);
            if (s == null) {
                error("Undefined symbol " + name);
                return null;
            }

            Type t = s.type;

            if (consume(TokenType.LBRACKET)) {
                RetVal idx = expression();
                if (idx == null || idx.type.typeBase != TypeBase.TB_INT || idx.type.nElements != -1) {
                    error("Array index must be an integer scalar");
                    return null;
                }

                if (!consume(TokenType.RBRACKET)) {
                    error("Missing closing bracket ]");
                    return null;
                }

                if (t.nElements < 0) {
                    error("Only an array can be indexed");
                    return null;
                }

                return new RetVal(
                        new Type(t.typeBase, null, -1),
                        true,
                        false,
                        null
                );
            }

            RetVal val = new RetVal(s.type, true, false, null);
            val.ctVal = s.ctVal;
            return val;

        } else if (consume(TokenType.LPAR)) {
            RetVal inner = expression();
            if (!consume(TokenType.RPAR)) {
                error("Missing closing parenthesis");
                return null;
            }
            return inner;
        }

        return null;
    }

    //i<5, i>5, i<=5, i>=5...
    public boolean condition(){
        String name = "";
        String structMemName = "";
        boolean hasBrackets = false;

        do{
            if(consume(TokenType.ID) || consume(TokenType.CT_INT) || consume(TokenType.CT_REAL) || consume(TokenType.CT_CHAR) || consume(TokenType.CT_STRING)){
                if(tokens.get(index-1).getType() == TokenType.ID){
                    name = tokens.get(index-1).getValue().toString();
                }

                if(consume(TokenType.LBRACKET)){ //for array
                    hasBrackets = true;
                    if(expression() == null){
                        error("Missing expression inside array index");
                    }
                    if(!consume(TokenType.RBRACKET)){
                        error("Missing closing bracket ]");
                    }

                    if(consume(TokenType.DOT)){ //for struct
                        if(!consume(TokenType.ID)){
                            error("Missing identifier after .");
                        }
                        structMemName = tokens.get(index-1).getValue().toString();
                    }
                }

                if(consume(TokenType.DOT)){ //for struct
                    if(!consume(TokenType.ID)){
                        error("Missing identifier after .");
                    }
                    structMemName = tokens.get(index-1).getValue().toString();
                }

                if(consume(TokenType.LESS) || consume(TokenType.LESSEQ) || consume(TokenType.GREATER) || consume(TokenType.GREATEREQ) || (consume(TokenType.ASSIGN) && consume(TokenType.ASSIGN))){
                    if(!name.isEmpty()){
                        Symbol s = semanticAnalizer.findSymbol(semanticAnalizer.symbols, name);
                        if(s == null){
                            if(semanticAnalizer.crtFunc != null && semanticAnalizer.findSymbol(semanticAnalizer.crtFunc.args, name) == null)
                                error("Symbol "+ name + " must be defined before initialization");
                        }
                        else {
                            if(!structMemName.isEmpty()){
                                Symbol structSymbol = s.type.s;
                                if(semanticAnalizer.findSymbol(structSymbol.members, structMemName) == null){
                                    error("struct "+name+" does not have member "+structMemName);
                                }
                            }
                            if (s.type.typeBase == TypeBase.TB_STRUCT && structMemName.isEmpty()) {
                                error("a structure cannot be logically tested");
                            }
                            if(s.type.nElements >= 0 && !hasBrackets){
                                error("an array cannot be logically tested");
                            }
                            if(s.type.nElements < 0 && hasBrackets){
                                error("only an array can be indexed");
                            }
                        }
                    }

                    if(expression() == null){
                        error("Missing expression after operator");
                    }
                }
                else{
                    index--;
                    return false;
                }
            }
        }while((consume(TokenType.AND) && consume(TokenType.AND)) || (consume(TokenType.OR) && consume(TokenType.OR)));
        return true;
    }

    public void functionParam(){
        do {
            if (consume(TokenType.INT) || consume(TokenType.DOUBLE) || consume(TokenType.CHAR)) {
                Type type = semanticAnalizer.getType(tokens.get(index-1).getType());
                if (!consume(TokenType.ID)) {
                    error("Missing identifier after type");
                }
                String name = tokens.get(index-1).getValue().toString();
                Symbol arg = semanticAnalizer.addFuncArg(semanticAnalizer.crtFunc, name, type);
                arg.type.nElements = -1;
                arg.mem = MemoryType.MEM_ARG;

                if (consume(TokenType.LBRACKET)) {
                    if (!consume(TokenType.CT_INT)) {
                        error("Missing constant in array size declaration");
                    }
                    arg.type.nElements =(int) tokens.get(index-1).getValue();
                    if (!consume(TokenType.RBRACKET)) {
                        error("Missing closing bracket ]");
                    }
                }
            }
        } while (consume(TokenType.COMMA));
    }
    
    public boolean functionCall(boolean hasSemicolon){
        if(consume(TokenType.ID)){
            String name = tokens.get(index-1).getValue().toString();
            if(!consume(TokenType.LPAR)){
                index--;
                return false;
            }

            Symbol s = semanticAnalizer.findSymbol(semanticAnalizer.symbols, name);
            if(s == null){
                error("Function "+ name + " must be defined before being called");
            }

            int argIndex = 0;

            if(tokens.get(index).getType() != TokenType.RPAR){
                do{
                    if(tokens.get(index).getType() == TokenType.ID) {
                        String varName = tokens.get(index).getValue().toString();
                        Symbol var = semanticAnalizer.findSymbol(semanticAnalizer.symbols, varName); //find decl for argument

                        if (var != null) {
                            if(tokens.get(index + 1).getType() == TokenType.LBRACKET)
                                semanticAnalizer.cast(s.args.get(argIndex).type, new Type(var.type.typeBase, null, -1)); //only 1 element out of array
                            else
                                semanticAnalizer.cast(s.args.get(argIndex).type, var.type); //check types for function signiture

                        }
                    }

                    expression();
                    argIndex++;

                }while(consume(TokenType.COMMA));

                if(argIndex < s.args.size()){
                    error("too few arguments in function "+ name+ " call");
                }
                if(argIndex > s.args.size()){
                    error("too many arguments in function "+ name+ " call");
                }
            }

            if(!consume(TokenType.RPAR)){
                error("Missing closing parenthesis )");
            }
            if(hasSemicolon && !consume(TokenType.SEMICOLON)){
                error("Missing semicolon ;");
            }
            return true;
        }
        return false;
    }

    public boolean variableDeclaration(){
        boolean isStruct = false;
        boolean isArray = false;
        RetVal size = null;
        int count = 0;
        if (consume(TokenType.INT) || consume(TokenType.DOUBLE) || consume(TokenType.CHAR) || consume(TokenType.STRUCT)) {
            Type type = semanticAnalizer.getType(tokens.get(index - 1).getType());
            do {
                if (!consume(TokenType.ID)) {
                    error("Missing identifier after type");
                }
                String name = tokens.get(index - 1).getValue().toString();

                if(tokens.get(index).getType() == TokenType.LACC){
                    index -= 2;
                    return false;
                }

                if(consume(TokenType.ID)) { //struct declaration
                    isStruct = true;
                    if(semanticAnalizer.findSymbol(semanticAnalizer.symbols,name) == null){ //struct need to be defined previously
                        error("Undifined struct symbol "+ name);
                    }

                    semanticAnalizer.addVar(this.tokens.get(index-1).getValue().toString(), type);
                    Symbol struct = semanticAnalizer.symbols.getLast();
                    struct.type.s = semanticAnalizer.findSymbol(semanticAnalizer.symbols,name);
                }

                if (consume(TokenType.LBRACKET)) {
                    isArray = true;

                    if(tokens.get(index).getType() != TokenType.RBRACKET)
                        size = expression();

                   if(size != null && size.type.typeBase != TypeBase.TB_INT){
                       error("The array " + name + " size is not an integer");
                   }

                    if (!consume(TokenType.RBRACKET)) {
                        error("Missing closing bracket ]");
                    }
                    if(consume(TokenType.ASSIGN)) {
                        if (consume(TokenType.LACC)) { //array initialization
                            do {
                                consume(TokenType.CT_CHAR);
                                consume(TokenType.CT_INT);
                                consume(TokenType.CT_REAL);
                                consume(TokenType.CT_STRING);
                                count++;
                            } while (consume(TokenType.COMMA));

                            if (!consume(TokenType.RACC)) {
                                error("Missing closing curly bracket }");
                            }
                        }
                        else{
                            error("Missing left curly bracket {");
                        }
                    }
                }

                if(!isStruct && tokens.get(index).getType() != TokenType.LPAR) { //not a struct declaration and not inside a function
                    semanticAnalizer.addVar(name, type);
                }

                if(isArray){
                    Symbol array = semanticAnalizer.symbols.getLast();
                    if(size != null)
                        array.type.nElements = size.ctVal.i;
                    if(count != 0)
                        array.type.nElements = count;
                }

            } while (consume(TokenType.COMMA));
    
            if (!consume(TokenType.SEMICOLON) && !consume(TokenType.LPAR)) {
                error("Missing semicolon at the end of declaration");
            }
            else{
                index--;
                if(consume(TokenType.SEMICOLON)){
                    return true;
                }
                else{
                    index = index - 2;
                    return false;
                }
            }
        }
        return false;
    }

    public boolean functionDeclaration(){
        if(consume(TokenType.INT) || consume(TokenType.DOUBLE) || consume(TokenType.CHAR) || consume(TokenType.VOID)){
            Type type = semanticAnalizer.getFunctionType(tokens.get(index-1).getType());
            if(consume(TokenType.ID)){
                String name = tokens.get(index-1).getValue().toString();
                if(consume(TokenType.LPAR)){
                    if(semanticAnalizer.crtDepth != 0){
                        error("Functions can be declared only in global context "+ name);
                    }
                    if(semanticAnalizer.findSymbol(semanticAnalizer.symbols, name) != null){
                        error("Symbol redefinition "+ name);
                    }

                    Symbol s = semanticAnalizer.addSymbol(semanticAnalizer.symbols,name,SymbolClass.CLS_FUNC);
                    s.type = type;
                    s.type.nElements = -1;
                    semanticAnalizer.crtFunc = s;
                    semanticAnalizer.crtDepth++;

                    functionParam();
                    if(consume(TokenType.RPAR)){
                        if(consume(TokenType.LACC)) {
                            analize(false);
                            if (!consume(TokenType.RACC)) {
                                error("Missing closing curly bracket }");
                            }
                        }
                        semanticAnalizer.deleteSymbolsAfter(semanticAnalizer.crtDepth);
                        semanticAnalizer.crtDepth--;
                        semanticAnalizer.crtFunc = null;
                        return true;
                    }
                    else{
                        error("Missing closing parenthesis )");
                    }
                }
                else{
                    index--;
                    return false;
                }
            }
            else{
                error("Missing identifier after type");
            }
        }
        return false;
    }


    public boolean variableAssignment(boolean hasSemicolon){
        if(consume(TokenType.ID)){
            String name = tokens.get(index-1).getValue().toString();
            Symbol s = semanticAnalizer.findSymbol(semanticAnalizer.symbols, name);
            if(consume(TokenType.ASSIGN)){
                if(tokens.get(index).getType() == TokenType.ID && tokens.get(index+1).getType() == TokenType.LPAR){ //function call
                    hasSemicolon = false;
                }

                if(s == null){
                    if(semanticAnalizer.crtFunc != null && semanticAnalizer.findSymbol(semanticAnalizer.crtFunc.args, name) == null)
                        error("Symbol "+ name + "must be defined before initialization");
                }
                else{
                    if(s.type.nElements >= 0){
                        error("The array "+name+ " cannot be assigned");
                    }
                    if(s.type.typeBase == TypeBase.TB_STRUCT && s.type.s != null ) { //struct member
                        if(semanticAnalizer.findSymbol(s.type.s.members, name) == null){
                            error("Struct "+ s.type.s.name+ " doesn't have member "+ name);
                        }
                    }

                    RetVal val = expression();
                    if(val != null && val.ctVal != null){
                        if(val.ctVal.i != null || val.ctVal.d != null)
                            s.ctVal = val.ctVal;
                    }
                }

                if(!consume(TokenType.SEMICOLON) && hasSemicolon){
                    error("Missing semicolon ;");
                }
                return true;
            }
            else{
                if(consume(TokenType.LBRACKET)){
                    if(s.type.nElements < 0){
                        error("only an array can be indexed");
                    }

                    if(expression() != null){
                        if(!consume(TokenType.RBRACKET)){
                            error("Missing closing bracket ]");
                        }
                    }
                    if(!consume(TokenType.ASSIGN)){
                        error("Missing assignment operator");
                    }
                    if(tokens.get(index).getType() == TokenType.ID && tokens.get(index+1).getType() == TokenType.LPAR){ //function call
                        hasSemicolon = false;
                    }

                    expression();
                    if(!consume(TokenType.SEMICOLON) && hasSemicolon){
                        error("Missing semicolon ;");
                    }
                    return true;
                }
                else{
                    index--;
                    return false;
                }
            }
        }
        return false;
    }

    public boolean ifRule(){
        if(consume(TokenType.IF)){
            if(!consume(TokenType.LPAR)){
                error("Missing opening parenthesis (");
            }
            condition();
            if(!consume(TokenType.RPAR)){
                error("Missing closing parenthesis )");
            }
            if(consume(TokenType.LACC)){
                semanticAnalizer.crtDepth++;
                analize(false); //can have all the statements inside if rule
                if(!consume(TokenType.RACC)){
                    error("Missing closing curly bracket }");
                }
                semanticAnalizer.crtDepth--;
            }
            else{
                //can have 1 statement without curly brackets
                analize(true);
            }
            if(consume(TokenType.ELSE)){
                if(consume(TokenType.LACC)){
                    semanticAnalizer.crtDepth++;
                    analize(false);
                    if(!consume(TokenType.RACC)){
                        error("Missing closing curly bracket }");
                    }
                    semanticAnalizer.crtDepth--;
                }
                else{
                    analize(true);
                }
            }
            return true;
        }
        return false;
    }

    public boolean forRule(){
        if(consume(TokenType.FOR)){
            if(!consume(TokenType.LPAR)){
                error("Missing opening parenthesis (");
            }
            variableAssignment(true);
            condition();
            if(!consume(TokenType.SEMICOLON)){
                error("Missing semicolon ;");
            }
            
            variableAssignment(false);
            if(!consume(TokenType.RPAR)){
                error("Missing closing parenthesis )");
            }
            if(consume(TokenType.LACC)){
                semanticAnalizer.crtDepth++;
                analize(false); //can have all the statements inside for loop
                if(!consume(TokenType.RACC)){
                    error("Missing closing curly bracket }");
                }
                semanticAnalizer.crtDepth--;
            }
            return true;
        }
        return false;
    }

    public boolean whileRule(){
        if(consume(TokenType.WHILE)){
            if(consume(TokenType.LPAR)){
                if(condition()){
                    if(consume(TokenType.RPAR)){
                        semanticAnalizer.crtDepth++;
                        if(consume(TokenType.LACC)){
                            analize(false);
                            if(!consume(TokenType.RACC)){
                                error("Missing closing curly bracket }");
                            }
                            semanticAnalizer.crtDepth--;
                            return true;
                        }
                        else{
                            error("Missing left curly bracket {");
                        }
                    }
                    else{
                        error("Missing right parenthesis )") ;
                    }
                }
                else{
                    error("Missing condition");
                }
            }
            else{
                error("Missing left parenthesis (") ;
            }
        }
        return false;
    }

    public boolean returnStatement(){
        if(consume(TokenType.RETURN)){
            if(semanticAnalizer.crtFunc != null && semanticAnalizer.crtFunc.type.typeBase == TypeBase.TB_VOID){
                error("a void function cannot return a value");
            }

            if(condition() || expression() != null){
                if(!consume(TokenType.SEMICOLON)){
                    error("Missing semicolon ;");
                }
                return true;
            }
        }
        return false;
    }

    public boolean isStruct(){
        if(consume(TokenType.STRUCT)){
            if(consume(TokenType.ID)){
                String name = tokens.get(index-1).getValue().toString();
                if(consume(TokenType.LACC)){
                    if(semanticAnalizer.crtDepth != 0){
                        error("Structs can be declared only in global context "+ name);
                    }

                    if(semanticAnalizer.findSymbol(semanticAnalizer.symbols, name) != null){
                        error("Symbol redefinition "+ name);
                    }

                    Symbol s = semanticAnalizer.addSymbol(semanticAnalizer.symbols,name,SymbolClass.CLS_STRUCT);
                    s.type = new Type(TypeBase.TB_STRUCT, null, -1);
                    semanticAnalizer.crtStruct = s;
                    semanticAnalizer.crtDepth++;

                    variableDeclaration();
                    if(consume(TokenType.RACC)){
                       if(consume(TokenType.SEMICOLON)){
                           semanticAnalizer.crtDepth--;
                           semanticAnalizer.crtStruct = null;
                           return true;
                       }
                       else{
                           error("Missing semicolon ;");
                       }
                    }
                    else{
                        error("Missing closing curly bracket }");
                    }
                }
                else{
                    index-=2;
                    return false;
                }
            }
            else{
                error("Missing identifier after struct");
            }
        }
        return false;
    }

    public void analize(boolean onlyOneStatement){
        while(index < tokens.size()){
            if(tokens.get(index).getType() == TokenType.RACC)
                break;
        
            if(variableDeclaration()){
                System.out.println("variable declaration");
            }
            else if(functionDeclaration()){
                System.out.println("function declaration");
            }
            else if(variableAssignment(true))
            {
                System.out.println("variable assignment");
            }
            else if(forRule()){
                System.out.println("for rule");
            }
            else if(whileRule()){
                System.out.println("while rule");
            }
            else if(returnStatement()){
                System.out.println("return statement");
            }
            else if(functionCall(true)){
                System.out.println("function call");
            }
            else if(ifRule()){
                System.out.println("if rule");
            }
            else if(isStruct()){
                System.out.println("struct");
            }
            else{
                error("Invalid syntax");
            }

            if(onlyOneStatement){
                break;
            }
        }
    }

    public static void main(String[] args) throws IOException {
        SyntacticAnalizer syntacticAnalizer = new SyntacticAnalizer("tests/9.c");
        syntacticAnalizer.analize(false);
        syntacticAnalizer.semanticAnalizer.printSymbols();
    }
}
