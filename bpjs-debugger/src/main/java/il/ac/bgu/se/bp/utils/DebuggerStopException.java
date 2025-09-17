package il.ac.bgu.se.bp.utils;

public class DebuggerStopException extends RuntimeException {
    private static final long serialVersionUID = 4509504530418856272L;

    public DebuggerStopException(String message) {
        super(message);
    }

}
