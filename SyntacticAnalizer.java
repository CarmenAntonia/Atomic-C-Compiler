import java.io.IOException;
import java.util.ArrayList;

public class SyntacticAnalizer {
    private ArrayList<Token> tokens;
    private int index = 0;
    private SemanticAnalizer semanticAnalizer;
    private RetVal rv;
    private char sign = ' ';
    private int prevVal;

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

    public boolean expression() {
        if(functionCall(false)){
            return true;
        }

        //sign = ' ';
        rv = new RetVal(new Type(TypeBase.TB_INT,null,-1), false, true, CtVal.fromInt(0));
        Type currentType = rv.type;

        if (!term()) {
            error("Invalid expression");
            return false;
        }

        currentType = semanticAnalizer.getArithType(currentType, rv.type);
        
        //addition and subtraction
        while (consume(TokenType.ADD) || consume(TokenType.SUB)) {
            if(tokens.get(index-1).getType() == TokenType.ADD){
                sign = '+';
            }
            else{
                sign = '-';
            }

            if (!term()) {
                error("Missing term after operator");
                return false;
            }
            currentType = semanticAnalizer.getArithType(currentType, rv.type);
            rv.type = currentType;
        }
        return true;
    }
    
    //multiplication and division
    private boolean term() {
        if(functionCall(false)){
            return true;
        }
        if (!factor()) {
            return false;
        }
    
        while (consume(TokenType.MUL) || consume(TokenType.DIV)) {
            if(tokens.get(index-1).getType() == TokenType.MUL){
                sign = '*';
            }
            else{
                sign = '/';
            }

            if (!factor()) {
                error("Missing factor after operator");
                return false;
            }
        }
    
        return true;
    }
    
    //constants, variables, (expressions), neg
    private boolean factor() {
        boolean hasBrackets = false;
        if (consume(TokenType.CT_INT) || consume(TokenType.CT_REAL) || consume(TokenType.CT_CHAR) || consume(TokenType.CT_STRING)) {
            if (tokens.get(index - 1).getType() == TokenType.CT_INT) {
                 rv.type = new Type(TypeBase.TB_INT, null, -1);
            } else if (tokens.get(index - 1).getType() == TokenType.CT_REAL) {
                rv.type = new Type(TypeBase.TB_DOUBLE, null, -1);
            } else if (tokens.get(index - 1).getType() == TokenType.CT_CHAR) {
                rv.type = new Type(TypeBase.TB_CHAR, null, -1);
            }

            return true;
        } 
        else if (consume(TokenType.ID)) {
            String name = tokens.get(index-1).getValue().toString();
            if (consume(TokenType.LBRACKET)) {
                hasBrackets = true;
                if (!expression()) {
                    error("Missing expression inside array index");
                    return false;
                }
                if (!consume(TokenType.RBRACKET)) {
                    error("Missing closing bracket ]");
                    return false;
                }
            }
            Symbol s = semanticAnalizer.findSymbol(semanticAnalizer.symbols, name);

            if(s == null){
                if(semanticAnalizer.crtFunc != null && semanticAnalizer.findSymbol(semanticAnalizer.crtFunc.args, name) == null)
                    error("Symbol "+ name + " must be defined before initialization");
            }
            else{
                rv.type = s.type;
                if(s.type.nElements >= 0 && sign != ' '){
                    error("cannot apply operation "+ sign+ " to array "+ name);
                }
                if(s.type.nElements < 0 && hasBrackets){
                    error("only an array can be indexed");
                }
            }

            return true;
        } 
        else if (consume(TokenType.NOT) || consume(TokenType.SUB) || consume(TokenType.ADD)) {
            return factor();
        }
    
        return false;
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
                    if(!expression()){
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
                    if(!expression()){
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
                    if(tokens.get(index).getType() == TokenType.ID && tokens.get(index+1).getType() != TokenType.LPAR){
                        String varName = tokens.get(index).getValue().toString();
                        consume(TokenType.ID);
                        Symbol var = semanticAnalizer.findSymbol(semanticAnalizer.symbols, varName); //find decl for argument
                        if(var != null)
                            semanticAnalizer.cast(s.args.get(argIndex).type, var.type); //check types for function signiture
                    }
                    else {
                        expression();
                    }
                    argIndex++;
                    System.out.println("argument count: "+ argIndex);
                }while(consume(TokenType.COMMA));

                if(argIndex < s.args.size()){
                    error("too few argument in function "+ name+ " call");
                }
                if(argIndex > s.args.size()){
                    error("too many argument in function "+ name+ " call");
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
        int size = 0;
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
                    if(tokens.get(index).getType() != TokenType.RBRACKET) {
                        if (tokens.get(index).getType() == TokenType.CT_INT) {
                            size = (int) tokens.get(index).getValue();
                        }
                    }
                    if(tokens.get(index).getType() != TokenType.RBRACKET)
                        expression();

                   if(rv.type.typeBase != TypeBase.TB_INT){
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
                    if(isArray){
                        Symbol array = semanticAnalizer.symbols.getLast();
                        array.type.nElements = 0;
//                        if(size != 0)
//                            array.type.nElements = size;
//                        else
//                            array.type.nElements = this.rv.ctVal.i;
                   }
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
                }

                expression();

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

                    if(expression()){
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

            if(condition() || expression()){
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
        SyntacticAnalizer syntacticAnalizer = new SyntacticAnalizer("tests/4.c");
        syntacticAnalizer.analize(false);
        syntacticAnalizer.semanticAnalizer.printSymbols();
    }
}
