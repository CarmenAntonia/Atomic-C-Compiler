import java.io.FileReader;
import java.io.IOException;
import java.io.PushbackReader;
import java.util.ArrayList;

public class TokenHandler {
    private ArrayList<Token> tokens;

    public TokenHandler() {
        this.tokens = new ArrayList<>();
    }


    public void readTokens(String fileName) throws IOException {
        PushbackReader reader = new PushbackReader(new FileReader(fileName), 1);
        int line = 1;
        int ch;

        while ((ch = reader.read()) != -1) {
            if(tokens.size() > 1){
                if(tokens.get(tokens.size() - 2).getType() == TokenType.LESS || tokens.get(tokens.size() - 2).getType() == TokenType.GREATER){
                    if(tokens.get(tokens.size() - 1).getType() == TokenType.ASSIGN){
                        int gr = 0;
                        if(tokens.get(tokens.size() - 2).getType() == TokenType.GREATER) gr = 1;
                        tokens.remove(tokens.size() - 2);
                        tokens.remove(tokens.size() - 1);
                        tokens.add(new Token(gr == 0 ? TokenType.LESSEQ : TokenType.GREATEREQ, gr == 0? "<=": ">=", line));
                    }   
                }
            }
            char current = (char) ch;

            if (Character.isWhitespace(current)) {
                if (current == '\n') {
                    line++;
                }
                if(current == '\t' || current == '\r'){
                    continue;
                }
                continue;
            }

            if (Character.isDigit(current)) {
                StringBuilder number = new StringBuilder();
                boolean isReal = false;
                boolean isHexa = false;
                boolean isBinary = false;

                ch = reader.read();
                if(current == '0' && (ch == 'x' || ch == 'X')){
                    isHexa = true;
                } else if(current == '0' && (ch == 'b' || ch == 'B')){
                    isBinary = true;
                } else {
                    number.append(current);
                    reader.unread(ch);
                }

                while ((ch = reader.read()) != -1 && (Character.isDigit((char) ch) || (char) ch == '.' || (isHexa && Character.isLetter((char) ch)))) {
                    if ((char) ch == '.') {
                        if (isReal) {
                            System.out.println("Error: Real number with 2 . at line " + line);
                            System.exit(1);
                        } 
                        isReal = true;
                    }
                    if(isHexa && !Character.isLetterOrDigit((char) ch)){
                        System.out.println("Error: Hexadecimal number with invalid character at line " + line);
                        System.exit(1);
                    }
                    if(isBinary && (char) ch != '0' && (char) ch != '1'){
                        System.out.println("Error: Binary number with invalid character at line " + line);
                        System.exit(1);
                    }
                    number.append((char) ch);
                }

                if(Character.toLowerCase(ch) == 'e'){
                    StringBuilder power = new StringBuilder();
                    ch = reader.read();
                    boolean hasSign = false;

                    if(ch == '+' || ch == '-'){
                        power.append((char) ch);
                        hasSign = true;
                    }
                    else{
                        reader.unread(ch);
                    }

                    while((ch = reader.read()) != -1 && Character.isDigit((char) ch)){
                        power.append((char) ch);
                    }
                    reader.unread(ch);

                    if(power.toString().length() == 0 || (hasSign && power.toString().length() == 1)){
                        System.out.println("Error: Missing power at line " + line);
                        System.exit(1);
                    }

                    Double d = Double.parseDouble(number.toString())* Math.pow(10, Double.parseDouble(power.toString()));
                    tokens.add(new Token(TokenType.CT_REAL, d, line));
                    continue;  
                }

                reader.unread(ch);
                
                if (isReal) {
                    tokens.add(new Token(TokenType.CT_REAL, Double.parseDouble(number.toString()), line));
                }else if(isHexa){
                    tokens.add(new Token(TokenType.CT_INT, Integer.parseInt(number.toString(), 16), line));
                }else if(isBinary){
                    tokens.add(new Token(TokenType.CT_INT, Integer.parseInt(number.toString(), 2), line));
                }else {
                    tokens.add(new Token(TokenType.CT_INT, Integer.parseInt(number.toString()), line));
                }
                continue;
            }

            if (Character.isLetter(current) || current == '_') {
                StringBuilder identifier = new StringBuilder();
                identifier.append(current);

                while ((ch = reader.read()) != -1 && (Character.isLetterOrDigit((char) ch) || (char) ch == '_')) {
                    identifier.append((char) ch);
                }
                reader.unread(ch);
                
                String text = identifier.toString();
                TokenType type = getTextType(text);

                tokens.add(new Token(type, text, line));
                continue;
            }

            if(current == '"') {
                StringBuilder string = new StringBuilder();
                while ((ch = reader.read()) != -1 && ((char) ch != '"')) {
                    if((char) ch == '\\'){
                        ch = reader.read();
                        if((char) ch == '"'){
                           string.append('"');
                        }
                        else if((char) ch == 'n'){
                            string.append('\n');
                        }
                        else if((char) ch == 't'){
                            string.append('\t');
                        } 
                        else if((char) ch == '\\'){
                            string.append('\\');
                        }
                        else reader.unread(ch);
                    }
                    else string.append((char) ch);
                    
                }
                tokens.add(new Token(TokenType.CT_STRING, string.toString(), line));
                continue;
            }

            if(current == '\''){
                ch = reader.read();
                if(ch != '\''){
                    if(ch == '\\'){
                        ch = reader.read();
                        if(ch == 'n'){
                            tokens.add(new Token(TokenType.CT_CHAR, "\n", line));
                        }
                        else if(ch == 't'){
                            tokens.add(new Token(TokenType.CT_CHAR, "\t", line));
                        }
                        else if(ch == '\\'){
                            tokens.add(new Token(TokenType.CT_CHAR, "\\", line));
                        }
                        else if(ch == '\''){
                            tokens.add(new Token(TokenType.CT_CHAR, "'", line));
                        }
                    }
                    else{
                        tokens.add(new Token(TokenType.CT_CHAR, String.valueOf((char) ch), line));
                    }
                    ch = reader.read();
                    if(ch != '\''){
                        System.out.println("Error: Missing ' at line " + line);
                       System.exit(1);
                    }
                }
                continue;
            }

            if(current == '/') {
                int nextChar = reader.read();
                if (nextChar == '/') {
                    while ((ch = reader.read()) != -1 && ch != '\n');
                    line++;
                } else if (nextChar == '*') {
                    while ((ch = reader.read()) != -1) {
                        if (ch == '*' && reader.read() == '/') {
                            break;
                        }
                        if(ch == '\n') {
                            line++;
                        }
                    }
                } else {
                    reader.unread(nextChar);
                    tokens.add(new Token(TokenType.DIV, "/", line));
                }
                continue;
            }
            
            TokenType type = getOperatorType(current);
            tokens.add(new Token(type, String.valueOf(current), line));
        }
        reader.close();
    }

    private TokenType getTextType(String text) {
        switch (text) {
            case "break": return TokenType.BREAK;
            case "char": return TokenType.CHAR;
            case "double": return TokenType.DOUBLE;
            case "else": return TokenType.ELSE;
            case "for": return TokenType.FOR;
            case "if": return TokenType.IF;
            case "int": return TokenType.INT;
            case "return": return TokenType.RETURN;
            case "struct": return TokenType.STRUCT;
            case "void": return TokenType.VOID;
            case "while": return TokenType.WHILE;
            default: return TokenType.ID;
        }
    }

    private TokenType getOperatorType(char operator) {
        switch (operator) {
            case ',': return TokenType.COMMA;
            case ';': return TokenType.SEMICOLON;
            case '(': return TokenType.LPAR;
            case ')': return TokenType.RPAR;
            case '[': return TokenType.LBRACKET;
            case ']': return TokenType.RBRACKET;
            case '{': return TokenType.LACC;
            case '}': return TokenType.RACC;
            case '+': return TokenType.ADD;
            case '-': return TokenType.SUB;
            case '*': return TokenType.MUL;
            case '/': return TokenType.DIV;
            case '.': return TokenType.DOT;
            case '&': return TokenType.AND;
            case '|': return TokenType.OR;
            case '!': return TokenType.NOT;
            case '=': return TokenType.ASSIGN;
            case '<': return TokenType.LESS;
            case '>': return TokenType.GREATER;
            default: return TokenType.ERROR;
        }
    }

    public ArrayList<Token> getTokens() {
        return tokens;
    }
}
