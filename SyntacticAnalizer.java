import java.io.IOException;
import java.util.ArrayList;

public class SyntacticAnalizer {
    private ArrayList<Token> tokens;
    private int index = 0;

    public SyntacticAnalizer(String filename) throws IOException {
        LexicalAnalizer lexicalAnalizer = new LexicalAnalizer(filename);
        lexicalAnalizer.validTokens();
        this.tokens = lexicalAnalizer.getTokens();
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

        if (!term()) {
            error("Invalid expression");
            return false;
        }
        
        //addition and subtraction
        while (consume(TokenType.ADD) || consume(TokenType.SUB)) {
            if (!term()) {
                error("Missing term after operator");
                return false;
            }
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
            if (!factor()) {
                error("Missing factor after operator");
                return false;
            }
        }
    
        return true;
    }
    
    //constants, variables, (expressions), neg
    private boolean factor() {
        if (consume(TokenType.CT_INT) || consume(TokenType.CT_REAL) || consume(TokenType.CT_CHAR) || consume(TokenType.CT_STRING)) {
            return true;
        } 
        else if (consume(TokenType.ID)) {
            if (consume(TokenType.LBRACKET)) {
                if (!expression()) {
                    error("Missing expression inside array index");
                    return false;
                }
                if (!consume(TokenType.RBRACKET)) {
                    error("Missing closing bracket ]");
                    return false;
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
        do{
            if(consume(TokenType.ID) || consume(TokenType.CT_INT) || consume(TokenType.CT_REAL) || consume(TokenType.CT_CHAR) || consume(TokenType.CT_STRING)){
                if(consume(TokenType.LBRACKET)){ //for array
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
                    }
                }

                if(consume(TokenType.DOT)){ //for struct
                    if(!consume(TokenType.ID)){
                        error("Missing identifier after .");
                    }
                }

                if(consume(TokenType.LESS) || consume(TokenType.LESSEQ) || consume(TokenType.GREATER) || consume(TokenType.GREATEREQ) || (consume(TokenType.ASSIGN) && consume(TokenType.ASSIGN))){
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
                if (!consume(TokenType.ID)) {
                    error("Missing identifier after type");
                }

                if (consume(TokenType.LBRACKET)) {
                    if (!consume(TokenType.CT_INT)) {
                        error("Missing constant in array size declaration");
                    }
                    if (!consume(TokenType.RBRACKET)) {
                        error("Missing closing bracket ]");
                    }
                }
            }
        } while (consume(TokenType.COMMA));
    }
    
    public boolean functionCall(boolean hasSemicolon){
        if(consume(TokenType.ID)){

            if(!consume(TokenType.LPAR)){
                index--;
                return false;
            }
            if(tokens.get(index).getType() != TokenType.RPAR){
                do{
                    expression();
                }while(consume(TokenType.COMMA));
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
        if (consume(TokenType.INT) || consume(TokenType.DOUBLE) || consume(TokenType.CHAR) || consume(TokenType.STRUCT)) {
            do {
                if (!consume(TokenType.ID)) {
                    error("Missing identifier after type");
                }
                if(tokens.get(index).getType() == TokenType.LACC){
                    index -= 2;
                    return false;
                }

                consume(TokenType.ID); //struct declaration

                if (consume(TokenType.LBRACKET)) {
                    expression();
                    if (!consume(TokenType.RBRACKET)) {
                        error("Missing closing bracket ]");
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
            if(consume(TokenType.ID)){
                if(consume(TokenType.LPAR)){
                    functionParam();
                    if(consume(TokenType.RPAR)){
                        if(consume(TokenType.LACC)){
                            analize(false);
                            if(!consume(TokenType.RACC)){
                                error("Missing closing curly bracket }");
                            }
                        }
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
            if(consume(TokenType.ASSIGN)){
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
                if(consume(TokenType.LBRACKET)){
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
                analize(false); //can have all the statements inside if rule
                if(!consume(TokenType.RACC)){
                    error("Missing closing curly bracket }");
                }
            }
            else{
                //can have 1 statement without curly brackets
                analize(true);
            }
            if(consume(TokenType.ELSE)){
                if(consume(TokenType.LACC)){
                    analize(false);
                    if(!consume(TokenType.RACC)){
                        error("Missing closing curly bracket }");
                    }
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
                analize(false); //can have all the statements inside for loop
                if(!consume(TokenType.RACC)){
                    error("Missing closing curly bracket }");
                }
            }
            return true;
        }
        return false;
    }

    public boolean returnStatement(){
        if(consume(TokenType.RETURN)){
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
                if(consume(TokenType.LACC)){
                    variableDeclaration();
                    if(consume(TokenType.RACC)){
                       if(consume(TokenType.SEMICOLON)){
                           return true;
                       }
                       else{
                           error("Missing semicolon ;");
                       }
                    }
                    else{
                        error("Missing closing curly bracket }");
                    }
                
                    return true;
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
    }
}
