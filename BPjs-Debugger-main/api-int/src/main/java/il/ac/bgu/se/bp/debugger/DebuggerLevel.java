package il.ac.bgu.se.bp.debugger;

public enum DebuggerLevel {
    LIGHT(0),
    NORMAL(1);

    private final int level;

    DebuggerLevel(int level) {
        this.level = level;
    }

    public int getLevel() {
        return level;
    }
}
