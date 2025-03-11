enum TokenType {
    ID, BREAK, CHAR, DOUBLE, ELSE, FOR, IF, INT, RETURN, STRUCT, VOID, WHILE,
    CT_INT, CT_REAL, CT_CHAR, CT_STRING,
    COMMA, SEMICOLON, LPAR, RPAR, LBRACKET, RBRACKET, LACC, RACC,
    ADD, SUB, MUL, DIV, DOT, AND, OR, NOT, ASSIGN, EQUAL, NOTEQ, LESS, LESSEQ, GREATER, GREATEREQ,
    SPACE, LINECOMMENT, COMMENT, ERROR
}

public class Token{
    private TokenType type;
    private Object value;
    private int line;

    public Token(TokenType type, Object value, int line) {
        this.type = type;
        this.value = value;
        this.line = line;
    }

    public TokenType getType(){
        return type;
    }

    public Object getValue(){
        return value;
    }

    public int getLine(){
        return line;
    }
}