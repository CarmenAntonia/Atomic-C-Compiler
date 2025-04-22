public class VM {
    private final Stack stack = new Stack();
    private int FP = 0; //frame pointer

    public int allocGlobal(int size) {
        return stack.allocGlobal(size);
    }

    public void run(Instr IP) {
        while (IP != null) {
            Instr next = IP.next;
            switch (IP.opcode) {
                case ADD_C: {
                    stack.pushChar((byte) (stack.popChar() + stack.popChar()));
                    break;
                }
                case ADD_I: {
                    stack.pushInt(stack.popInt() + stack.popInt());
                    break;
                }
                case ADD_D: {
                    stack.pushDouble(stack.popDouble() + stack.popDouble());
                    break;
                }

                case AND_C: {
                    byte b = stack.popChar();
                    byte a = stack.popChar();
                    byte res = (byte)((a != 0 && b != 0) ? 1 : 0);
                    stack.pushChar(res);
                    break;
                }
                case AND_I: {
                    int b = stack.popInt();
                    int a = stack.popInt();
                    stack.pushInt((a != 0 && b != 0) ? 1 : 0);
                    break;
                }
                case AND_D: {
                    double b = stack.popDouble();
                    double a = stack.popDouble();
                    stack.pushDouble((a != 0.0 && b != 0.0) ? 1.0 : 0.0);
                    break;
                }
                case AND_A: {
                    int b = stack.popAddress();
                    int a = stack.popAddress();
                    stack.pushAddress((a != 0 && b != 0) ? 1 : 0);
                    break;
                }

                case CALL: {
                    stack.pushInstr(IP.next);
                    next = (Instr)IP.args[0];
                    break;
                }
                case CALLEXT: {
                    ((ExtFunc) IP.args[0]).run(stack);
                    break;
                }

                case CAST_C_D: {
                    byte c = stack.popChar();
                    stack.pushDouble(c);
                    break;
                }
                case CAST_C_I: {
                    byte c = stack.popChar();
                    stack.pushInt(c);
                    break;
                }
                case CAST_D_C: {
                    double d = stack.popDouble();
                    stack.pushChar((byte) d);
                    break;
                }
                case CAST_D_I: {
                    double d = stack.popDouble();
                    stack.pushInt((int) d);
                    break;
                }
                case CAST_I_C: {
                    int i = stack.popInt();
                    stack.pushChar((byte) i);
                    break;
                }
                case CAST_I_D: {
                    int i = stack.popInt();
                    stack.pushDouble(i);
                    break;
                }

                case DIV_I: {
                    int divisor = stack.popInt();
                    int dividend = stack.popInt();
                    stack.pushInt(dividend / divisor);
                    break;
                }
                case DIV_D: {
                    double divisor = stack.popDouble();
                    double dividend = stack.popDouble();
                    stack.pushDouble(dividend / divisor);
                    break;
                }
                case DIV_C: {
                    byte divisor = stack.popChar();
                    byte dividend = stack.popChar();
                    stack.pushChar((byte) (dividend / divisor));
                    break;
                }

                case DROP: {
                    int dropSize = (Integer) IP.args[0];
                    stack.drop(dropSize);
                    break;
                }

                case ENTER: {
                    int n = (Integer)IP.args[0];
                    stack.pushAddress(FP);
                    FP = stack.getSP();
                    stack.setSP(FP + n);
                    break;
                }

                case EQ_A: {
                    stack.pushAddress(stack.popAddress() == stack.popAddress() ? 1 : 0);
                    break;
                }
                case EQ_C: {
                    stack.pushChar((byte) (stack.popChar() == stack.popChar() ? 1 : 0));
                    break;
                }
                case EQ_D: {
                    stack.pushDouble(stack.popDouble() == stack.popDouble() ? 1 : 0);
                    break;
                }
                case EQ_I: {
                    stack.pushInt(stack.popInt() == stack.popInt() ? 1 : 0);
                    break;
                }

                case GREATER_C: {
                    stack.pushChar((byte) (stack.popChar() < stack.popChar() ? 1 : 0));
                    break;
                }
                case GREATER_D: {
                    stack.pushDouble(stack.popDouble() < stack.popDouble() ? 1 : 0);
                    break;
                }
                case GREATER_I: {
                    stack.pushInt(stack.popInt() < stack.popInt() ? 1 : 0);
                    break;
                }

                case GREATEREQ_C: {
                    stack.pushChar((byte) (stack.popChar() <= stack.popChar() ? 1 : 0));
                    break;
                }
                case GREATEREQ_D: {
                    stack.pushDouble(stack.popDouble() <= stack.popDouble() ? 1 : 0);
                    break;
                }
                case GREATEREQ_I: {
                    stack.pushInt(stack.popInt() <= stack.popInt() ? 1 : 0);
                    break;
                }

                case HALT:
                    return;

                case INSERT: {
                    int i = (Integer) IP.args[0];
                    int n = (Integer) IP.args[1];
                    stack.insert(i, n);
                    break;
                }

                case JF_I: {
                    int v = stack.popInt();
                    if (v == 0) next = (Instr) IP.args[0];
                    break;
                }
                case JF_C: {
                    byte v = stack.popChar();
                    if (v == 0) next = (Instr) IP.args[0];
                    break;
                }
                case JF_D: {
                    double v = stack.popDouble();
                    if (v == 0.0) next = (Instr) IP.args[0];
                    break;
                }
                case JF_A: {
                    int v = stack.popAddress();
                    if (v == 0) next = (Instr) IP.args[0];
                    break;
                }

                case JMP: {
                    next = (Instr) IP.args[0];
                    break;
                }

                case JT_I: {
                    int v = stack.popInt();
                    if (v != 0) next = (Instr) IP.args[0];
                    break;
                }
                case JT_C: {
                    byte v = stack.popChar();
                    if (v != 0) next = (Instr) IP.args[0];
                    break;
                }
                case JT_D: {
                    double v = stack.popDouble();
                    if (v != 0.0) next = (Instr) IP.args[0];
                    break;
                }
                case JT_A: {
                    int v = stack.popAddress();
                    if (v != 0) next = (Instr) IP.args[0];
                    break;
                }

                case LESS_C: {
                    stack.pushChar((byte) (stack.popChar() > stack.popChar() ? 1 : 0));
                    break;
                }
                case LESS_D: {
                    stack.pushDouble(stack.popDouble() > stack.popDouble() ? 1 : 0);
                    break;
                }
                case LESS_I: {
                    stack.pushInt(stack.popInt() > stack.popInt() ? 1 : 0);
                    break;
                }

                case LESSEQ_C: {
                    stack.pushChar((byte) (stack.popChar() >= stack.popChar() ? 1 : 0));
                    break;
                }
                case LESSEQ_D: {
                    stack.pushDouble(stack.popDouble() >= stack.popDouble() ? 1 : 0);
                    break;
                }
                case LESSEQ_I: {
                    stack.pushInt(stack.popInt() >= stack.popInt() ? 1 : 0);
                    break;
                }

                case LOAD: {
                    int loadSize = (Integer) IP.args[0];
                    int addr = stack.popAddress();
                    for (int i = 0; i < loadSize; i++) {
                        byte b = stack.getByte(addr + i);
                        stack.pushChar(b);
                    }
                    break;
                }

                case MUL_C: {
                    stack.pushChar((byte) (stack.popChar() * stack.popChar()));
                    break;
                }
                case MUL_D: {
                    stack.pushDouble(stack.popDouble() * stack.popDouble());
                    break;
                }
                case MUL_I: {
                    stack.pushInt(stack.popInt() * stack.popInt());
                    break;
                }

                case NEG_C: {
                    stack.pushChar((byte) (-1 * stack.popChar()));
                    break;
                }
                case NEG_D: {
                    stack.pushDouble(-1 * stack.popDouble());
                    break;
                }
                case NEG_I: {
                    stack.pushInt(-1 * stack.popInt());
                    break;
                }

                case NOP: {
                    break;
                }

                case NOT_I: {
                    int v = stack.popInt();
                    stack.pushInt(v == 0 ? 1 : 0);
                    break;
                }
                case NOT_C: {
                    byte v = stack.popChar();
                    stack.pushChar((byte)(v == 0 ? 1 : 0));
                    break;
                }
                case NOT_D: {
                    double v = stack.popDouble();
                    stack.pushDouble(v == 0.0 ? 1.0 : 0.0);
                    break;
                }
                case NOT_A: {
                    int v = stack.popAddress();
                    stack.pushAddress(v == 0 ? 1 : 0);
                    break;
                }

                case NOTEQ_A: {
                    stack.pushAddress(stack.popAddress() != stack.popAddress() ? 1 : 0);
                    break;
                }
                case NOTEQ_C: {
                    stack.pushChar((byte) (stack.popChar() != stack.popChar() ? 1 : 0));
                    break;
                }
                case NOTEQ_D: {
                    stack.pushDouble(stack.popDouble() != stack.popDouble() ? 1 : 0);
                    break;
                }
                case NOTEQ_I: {
                    stack.pushInt(stack.popInt() != stack.popInt() ? 1 : 0);
                    break;
                }

                case OFFSET: {
                    int n = stack.popInt();
                    int addr = stack.popAddress();
                    stack.pushAddress(addr + n);
                    break;
                }

                case OR_A: {
                    stack.pushAddress((stack.popAddress() != 0 || stack.popAddress() != 0) ? 1 : 0);
                    break;
                }
                case OR_C: {
                    stack.pushChar((byte) ((stack.popChar() != 0 || stack.popChar() != 0) ? 1 : 0));
                    break;
                }
                case OR_D: {
                    stack.pushDouble((stack.popDouble() != 0 || stack.popDouble() != 0) ? 1 : 0);
                    break;
                }
                case OR_I: {
                    stack.pushInt((stack.popInt() != 0 || stack.popInt() != 0) ? 1 : 0);
                    break;
                }

                case PUSHFPADDR: {
                    int i = (Integer) IP.args[0];
                    stack.pushAddress(FP + i);
                    break;
                }

                case PUSHCT_A: {
                    stack.pushAddress((Integer) IP.args[0]);
                    break;
                }
                case PUSHCT_C: {
                    stack.pushChar((Byte) IP.args[0]);
                    break;
                }
                case PUSHCT_D: {
                    stack.pushDouble((Double) IP.args[0]);
                    break;
                }
                case PUSHCT_I: {
                    stack.pushInt((Integer) IP.args[0]);
                    break;
                }

                case RET: {
                    int na = (Integer) IP.args[0];   // total size of args
                    int nr = (Integer) IP.args[1];   // size of return value (0 if void)

                    // save return value
                    byte[] retVal = null;
                    if (nr > 0) {
                        retVal = new byte[nr];
                        for (int i = nr - 1; i >= 0; i--) {
                            retVal[i] = stack.popChar();
                        }
                    }

                    // restore old frame pointer
                    FP = stack.popAddress();

                    // pop return address
                    Instr retAddr = stack.popInstr();

                    //drop args
                    stack.drop(na);

                    // push return value
                    if (nr > 0) {
                        for (int i = 0; i < nr; i++) {
                            stack.pushChar(retVal[i]);
                        }
                    }

                    next = retAddr;
                    break;
                }

                case STORE: {
                    int storeSize = (Integer) IP.args[0];
                    byte[] value = new byte[storeSize];
                    for (int i = storeSize - 1; i >= 0; i--) {
                        value[i] = stack.popChar();             // pop bytes from stack
                    }
                    int storeAddr = stack.popAddress();         // get target address
                    for (int i = 0; i < storeSize; i++) {
                        stack.setByte(storeAddr + i, value[i]);  // store byte to memory
                    }
                    break;
                }

                case SUB_C: {
                    byte b = stack.popChar();
                    byte a = stack.popChar();
                    stack.pushChar((byte) (a - b));
                    break;
                }
                case SUB_D: {
                    double b = stack.popDouble();
                    double a = stack.popDouble();
                    stack.pushDouble(a - b);
                    break;
                }
                case SUB_I: {
                    int b = stack.popInt();
                    int a = stack.popInt();
                    stack.pushInt(a - b);
                    break;
                }

                default:
                    throw new RuntimeException("Unknown opcode: " + IP.opcode);
            }

            IP = next;
        }
    }
}



