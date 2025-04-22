abstract class ExtFunc {
    public abstract void run(Stack stack);
}

class PutI extends ExtFunc {
    @Override
    public void run(Stack stack) {
        System.out.println("#" + stack.popInt());
    }
}