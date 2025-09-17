package il.ac.bgu.se.bp.utils.visitor;

import il.ac.bgu.se.bp.socket.console.ConsoleMessage;
import il.ac.bgu.se.bp.socket.state.BPDebuggerState;
import il.ac.bgu.se.bp.socket.status.ProgramStatus;

public interface PublisherVisitor {
    void visit(String userId, BPDebuggerState debuggerState);
    void visit(String userId, ConsoleMessage consoleMessage);
    void visit(String userId, ProgramStatus programExit);
}
