import java.util.ArrayList;
import java.util.List;

public class SemanticAnalizer {
    List<Symbol> symbols;
    int crtDepth = 0;
    Symbol crtFunc = null;
    Symbol crtStruct = null;

    public SemanticAnalizer() {
        symbols = new ArrayList<>();
        addExtFunctions();
    }

    private void addExtFunctions() {
        Symbol f = addExtFunc("put_s", new Type(TypeBase.TB_VOID, null, -1));
        addFuncArg(f, "s", new Type(TypeBase.TB_CHAR, null, 0));

        f = addExtFunc("get_s", new Type(TypeBase.TB_VOID, null, -1));
        addFuncArg(f, "s", new Type(TypeBase.TB_CHAR, null, 0));

        f = addExtFunc("put_i", new Type(TypeBase.TB_VOID, null, -1));
        addFuncArg(f, "i", new Type(TypeBase.TB_INT, null, -1));

        addExtFunc("get_i", new Type(TypeBase.TB_INT, null, -1));

        f = addExtFunc("put_d", new Type(TypeBase.TB_VOID, null, -1));
        addFuncArg(f, "d", new Type(TypeBase.TB_DOUBLE, null, -1));

        addExtFunc("get_d", new Type(TypeBase.TB_DOUBLE, null, -1));

        f = addExtFunc("put_c", new Type(TypeBase.TB_VOID, null, -1));
        addFuncArg(f, "c", new Type(TypeBase.TB_CHAR, null, -1));

        addExtFunc("get_c", new Type(TypeBase.TB_CHAR, null, -1));

        addExtFunc("seconds", new Type(TypeBase.TB_DOUBLE, null, -1));
    }

    public Symbol addSymbol(List<Symbol> list,String name, SymbolClass cls) {
        Symbol s = new Symbol(name, cls, null, null, crtDepth);
        list.add(s);
        return s;
    }

    public Symbol findSymbol(List<Symbol> list,String name) {
        for(Symbol s:list)
            System.out.println(s);
        for (int i = list.size() - 1; i >= 0; i--) {
            if (list.get(i).name.equals(name)) {
                return list.get(i);
            }
        }
        return null;
    }

    public void deleteSymbolsAfter(int currentDepth) {
        symbols.removeIf(symbol -> symbol.depth == currentDepth && symbol.cls != SymbolClass.CLS_FUNC);
    }

    public void printSymbols() {
        for (Symbol symbol : symbols) {
            System.out.println(symbol);
            if(!symbol.args.isEmpty()){
                for (Symbol arg : symbol.args) {
                    System.out.println(arg);
                }
            }
            if(!symbol.members.isEmpty()){
                for (Symbol member : symbol.members) {
                    System.out.println(member);
                }
            }
        }
    }

    public void addVar(String name,Type t) {
        Symbol s;
        if(this.crtStruct != null){
            if(findSymbol(this.crtStruct.members,name) != null)
                error("Symbol redefinition "+ name);
            s = addSymbol(this.crtStruct.members,name, SymbolClass.CLS_VAR);
        }
        else if(this.crtFunc != null){
            s = findSymbol(symbols,name);
            if(s==null){
                s = findSymbol(this.crtFunc.args,name);
            }
            if(s != null && s.depth==crtDepth)
                error("Symbol redefinition "+ name);
            s = addSymbol(this.symbols,name, SymbolClass.CLS_VAR);
            s.mem= MemoryType.MEM_LOCAL;
        }
        else{
            if(findSymbol(symbols,name) != null)
                error("Symbol redefinition "+ name);
            s = addSymbol(this.symbols,name, SymbolClass.CLS_VAR);
            s.mem= MemoryType.MEM_GLOBAL;
        }
        s.type = t;
        s.type.nElements = -1;
    }

    public void error(String message){
        System.out.println("Error: " + message);
        System.exit(1);
    }

    private Type basicType(TokenType type){
        return switch (type) {
            case TokenType.INT -> new Type(TypeBase.TB_INT, null, -1);
            case TokenType.CHAR -> new Type(TypeBase.TB_CHAR, null, -1);
            case TokenType.DOUBLE -> new Type(TypeBase.TB_DOUBLE, null, -1);
            default -> null;
        };
    }

    public Type getType(TokenType type) {
        Type t = basicType(type);
        if (t != null) {
            return t;
        } else if (type == TokenType.STRUCT) {
            return new Type(TypeBase.TB_STRUCT, null, -1);
        }
        return null;
    }

    public Type getFunctionType(TokenType type) {
        Type t = basicType(type);
        if (t != null) {
            return t;
        } else if (type == TokenType.VOID) {
            return new Type(TypeBase.TB_VOID, null, -1);
        }
        return null;
    }

    public void cast(Type dst, Type src) {
        if (src.nElements > -1) {
            if (dst.nElements > -1) {
                if (src.typeBase != dst.typeBase) {
                    error("An array cannot be converted to an array of another type");
                }
                else {
                    return; //arrays compatible
                }
            } else {
                error("An array cannot be converted to a non-array");
            }
        } else {
            if (dst.nElements > -1) {
                error("A non-array cannot be converted to an array");
            }
        }

        switch (src.typeBase) {
            case TB_CHAR, TB_INT, TB_DOUBLE -> {
                switch (dst.typeBase) {
                    case TB_CHAR, TB_INT, TB_DOUBLE -> {
                        return; // valid cast
                    }
                }
            }
            case TB_STRUCT -> {
                if (dst.typeBase == TypeBase.TB_STRUCT) {
                    if (src.s != dst.s) {
                        error("A structure cannot be converted to another one");
                    }
                    return; // same struct type
                }
            }
        }

        error("Incompatible types");
    }

    public Type getArithType(Type s1, Type s2) {
        if (s1.nElements != -1 || s2.nElements != -1) {
            error("Arithmetic operations not allowed on arrays");
        }

        if (s1.typeBase == TypeBase.TB_STRUCT || s2.typeBase == TypeBase.TB_STRUCT) {
            error("Arithmetic operations not allowed on struct");
        }

        if (s1.typeBase == TypeBase.TB_DOUBLE || s2.typeBase == TypeBase.TB_DOUBLE) {
            return new Type(TypeBase.TB_DOUBLE, null, -1);
        }

        if (s1.typeBase == TypeBase.TB_INT || s2.typeBase == TypeBase.TB_INT) {
            return new Type(TypeBase.TB_INT, null, -1);
        }

        if (s1.typeBase == TypeBase.TB_CHAR && s2.typeBase == TypeBase.TB_CHAR) {
            return new Type(TypeBase.TB_INT, null, -1);
        }

        return null;
    }

    Symbol addExtFunc(String name, Type type){
        Symbol s = addSymbol(symbols,name,SymbolClass.CLS_EXTFUNC);
        s.type = type;
        return s;
    }

    Symbol addFuncArg(Symbol func, String name, Type type){
        Symbol a=addSymbol(func.args,name,SymbolClass.CLS_VAR);
        a.type=type;
        return a;
    }

    public int computeValue(int a, int b, char s){
        return switch (s) {
            case '+' -> a + b;
            case '-' -> a - b;
            case '*' -> a * b;
            case '/' -> a / b;
            default -> 0;
        };
    }
}
