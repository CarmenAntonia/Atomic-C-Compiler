import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

public class Stack {
    private static final int STACK_SIZE = 32 * 1024;
    private final ByteBuffer stack = ByteBuffer.allocate(STACK_SIZE);
    private int SP = 0; // Stack Pointer

    private int nGlobals = 0;
    private final Map<Integer, Instr> instrAddresses = new HashMap<>();

    public int allocGlobal(int size) {
        if (nGlobals + size > STACK_SIZE) {
            throw new RuntimeException("Insufficient globals space");
        }
        int addr = nGlobals;
        nGlobals += size;
        // Always keep SP just above globals
        if (SP < nGlobals) SP = nGlobals;
        return addr;
    }

    public byte getByte(int addr) {
        return stack.get(addr);
    }

    public void setByte(int addr, byte value) {
        stack.put(addr, value);
    }

    public void pushInt(int value) {
        checkOverflow(4);
        stack.putInt(SP, value);
        SP += 4;
    }

    public int popInt() {
        SP -= 4;
        checkUnderflow();
        return stack.getInt(SP);
    }

    public void pushDouble(double value) {
        checkOverflow(8);
        stack.putDouble(SP, value);
        SP += 8;
    }

    public double popDouble() {
        SP -= 8;
        checkUnderflow();
        return stack.getDouble(SP);
    }

    public void pushChar(byte value) {
        checkOverflow(1);
        stack.put(SP, value);
        SP += 1;
    }

    public byte popChar() {
        SP -= 1;
        checkUnderflow();
        return stack.get(SP);
    }

    public void pushAddress(int addr) {
        pushInt(addr);
    }

    public int popAddress() {
        return popInt();
    }

    public void pushInstr(Instr ip) {
        int slot = SP;
        instrAddresses.put(slot, ip);
        pushAddress(slot);
    }

    public Instr popInstr() {
        int slot = popAddress();
        Instr ip = instrAddresses.remove(slot);
        if (ip == null) throw new RuntimeException("Bad return address");
        return ip;
    }

    public void drop(int n) {
        SP -= n;
        checkUnderflow();
    }

    public int getSP() {
        return SP;
    }

    public void setSP(int newSP) {
        SP = newSP;
        checkOverflow(0);
        checkUnderflow();
    }

    public void insert(int i, int n) {
        checkOverflow(n);

        int sourcePos = SP - i;
        if (sourcePos < STACK_SIZE) throw new RuntimeException("INSERT underflow");

        // read n bytes to temp
        byte[] temp = new byte[n];
        for (int j = 0; j < n; j++) {
            temp[j] = stack.get(sourcePos + j);
        }

        //  shift top i bytes down
        for (int j = i - 1; j >= 0; j--) {
            byte move = stack.get(SP - i + j);
            stack.put(SP + j, move);
        }

        // insert temp at the top
        for (int j = 0; j < n; j++) {
            stack.put(SP - n + j, temp[j]);
        }

        SP += n;
    }

    private void checkOverflow(int size) {
        if (SP + size > STACK_SIZE) {
            throw new StackOverflowError("Stack overflow");
        }
    }

    private void checkUnderflow() {
        if (SP < 0) {
            throw new RuntimeException("Stack underflow");
        }
    }
}