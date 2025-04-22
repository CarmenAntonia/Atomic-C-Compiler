enum Opcode {
    ADD_C, ADD_D, ADD_I,
    SUB_C, SUB_D, SUB_I,
    MUL_C, MUL_D, MUL_I,
    DIV_C, DIV_D, DIV_I,

    AND_A, AND_C, AND_D, AND_I,
    OR_A, OR_C, OR_D, OR_I,
    NOT_A, NOT_C, NOT_D, NOT_I,
    NEG_C, NEG_D, NEG_I,

    EQ_A, EQ_C, EQ_D, EQ_I,
    NOTEQ_A, NOTEQ_C, NOTEQ_D, NOTEQ_I,
    GREATER_C, GREATER_D, GREATER_I,
    GREATEREQ_C, GREATEREQ_D, GREATEREQ_I,
    LESS_C, LESS_D, LESS_I,
    LESSEQ_C, LESSEQ_D, LESSEQ_I,

    CAST_C_D, CAST_C_I,
    CAST_D_C, CAST_D_I,
    CAST_I_C, CAST_I_D,

    CALL, CALLEXT,
    JT_A, JT_C, JT_D, JT_I,
    JF_A, JF_C, JF_D, JF_I,
    JMP,
    RET,
    HALT,
    NOP,

    DROP, ENTER,
    PUSHFPADDR,
    PUSHCT_A, PUSHCT_C, PUSHCT_D, PUSHCT_I,
    LOAD, STORE,
    OFFSET,
    INSERT
}

public class Instr {
    public Opcode opcode;
    public Object[] args = new Object[2];
    public Instr next, prev;

    public Instr(Opcode opcode) {
        this.opcode = opcode;
    }
}

class InstructionList {
    public Instr head = null;
    public Instr tail = null;

    public Instr addInstr(Opcode opcode) {
        Instr i = new Instr(opcode);
        if (tail != null) {
            tail.next = i;
            i.prev = tail;
        } else {
            head = i;
        }
        tail = i;
        return i;
    }

    public Instr addInstrWithArg(Opcode opcode, Object arg0) {
        Instr i = addInstr(opcode);
        i.args[0] = arg0;
        return i;
    }

    public Instr addInstrWith2Args(Opcode opcode, Object arg0, Object arg1) {
        Instr i = addInstr(opcode);
        i.args[0] = arg0;
        i.args[1] = arg1;
        return i;
    }
}

