import java.io.IOException;
import java.util.ArrayList;

public class LexicalAnalizer {
    private ArrayList<Token> tokens;

    public LexicalAnalizer(String filename) throws IOException {
        TokenHandler tokenHandler = new TokenHandler();
        tokenHandler.readTokens(filename);
        this.tokens = tokenHandler.getTokens();
    }

    public void validTokens(){
        Token prevToken = null;
        int b = 0, p = 0, a = 0;

        for (Token token : tokens) {
            if(prevToken != null){
                if(prevToken.getType() == TokenType.CT_INT || prevToken.getType() == TokenType.CT_REAL){
                    if(token.getType() == TokenType.ID){
                        System.out.println("Error: Identifier starting with number at line " + token.getLine());
                        System.exit(1);
                    }
                }
            }

            if(token.getType() == TokenType.LBRACKET){
                b++;
            }
            if(token.getType() == TokenType.RBRACKET){
                if(b == 0){
                    System.out.println("Error: Missing [ at line " + token.getLine());
                    System.exit(1);
                }
                b--;
            }
            if(token.getType() == TokenType.LPAR){
                p++;
            }
            if(token.getType() == TokenType.RPAR){
                if(p == 0){
                    System.out.println("Error: Missing ( at line " + token.getLine());
                    System.exit(1);
                }
                p--;
            }
            if(token.getType() == TokenType.LACC){
                a++;
            }
            if(token.getType() == TokenType.RACC){
                if(a == 0){
                    System.out.println("Error: Missing { at line " + token.getLine());
                    System.exit(1);
                }
                a--;
            }
        
            prevToken = token;
        }

        if(b != 0){
            System.out.println("Error: Missing ]");
            System.exit(1);
        }
        if(p != 0){
            System.out.println("Error: Missing )");
            System.exit(1);
        }
        if(a != 0){
            System.out.println("Error: Missing }");
            System.exit(1);
        }
    }

    public void printTokens(){
        for (Token token : tokens) {
            System.out.println(token.getType() + " " + token.getValue());
        }
    }

    public static void main(String[] args) {
        try {
            LexicalAnalizer lexicalAnalizer = new LexicalAnalizer("tests/8.c");
            lexicalAnalizer.validTokens();
            lexicalAnalizer.printTokens();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }    
}
