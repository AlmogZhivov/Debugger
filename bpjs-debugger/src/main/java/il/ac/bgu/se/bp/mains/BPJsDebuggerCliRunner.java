package il.ac.bgu.se.bp.mains;

import com.fasterxml.jackson.databind.ObjectMapper;
import il.ac.bgu.se.bp.config.BPJsDebuggerConfiguration;
import il.ac.bgu.se.bp.debugger.BPJsDebugger;
import il.ac.bgu.se.bp.debugger.DebuggerLevel;
import il.ac.bgu.se.bp.debugger.manage.DebuggerFactory;
import il.ac.bgu.se.bp.rest.response.BooleanResponse;
import il.ac.bgu.se.bp.rest.response.GetSyncSnapshotsResponse;
import il.ac.bgu.se.bp.socket.console.ConsoleMessage;
import il.ac.bgu.se.bp.socket.state.BPDebuggerState;
import il.ac.bgu.se.bp.socket.status.ProgramStatus;
import il.ac.bgu.se.bp.socket.status.Status;
import il.ac.bgu.se.bp.utils.observer.BPEvent;
import il.ac.bgu.se.bp.utils.observer.Subscriber;
import il.ac.bgu.se.bp.utils.visitor.PublisherVisitor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Stream.generate;

public class BPJsDebuggerCliRunner implements Subscriber<BPEvent>, PublisherVisitor {

    private static final String commands = "b / rb / go / si / sov / sou / getss / n / e / re / we / h / tmb / tsp / sss / ex / stop";
    private static final String menu;
    private static final String prefix = "==========";
    private static final String suffix = "==========";
    private static final String menuWrapper = generate(() -> "=").limit(commands.length()).collect(joining());
    private Status status;

    static {
        String newLine = "\n";
        String enterCommand = " Enter command ";
        int whiteSpacesLengthForEnterCommand = (menuWrapper.length() - suffix.length()) / 2;
        String whiteSpacesForEnterCommand = generate(() -> "=").limit(whiteSpacesLengthForEnterCommand).collect(joining());
        StringBuilder stringBuilder = new StringBuilder()
                .append(prefix).append(whiteSpacesForEnterCommand).append(enterCommand).append(whiteSpacesForEnterCommand).append(suffix).append(newLine)
                .append(prefix).append("> ").append(commands).append(" <").append(suffix);
        menu = stringBuilder.toString();
    }

    private static final String debuggerId = UUID.randomUUID().toString();
    private static final String filename = "BPJSDebuggerForTesting.js";
//    private static final String filename = "BPJSDebuggerRecTest.js";

    private boolean isTerminated = false;
    private BufferedReader bufferedReader;
    private boolean isSkipSyncPoints = false;
    private boolean isSkipBreakPoints = false;
    private boolean isWaitForExternalEvents = false;
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private DebuggerFactory<BooleanResponse> debuggerFactory;

    private BPJsDebugger<BooleanResponse> bpJsDebugger;

    private Map<Integer, Boolean> breakpoints = new HashMap<>();

    public static void main(String[] args) {
        AnnotationConfigApplicationContext annotationConfigApplicationContext =
                new AnnotationConfigApplicationContext(BPJsDebuggerConfiguration.class);

        BPJsDebuggerCliRunner bpJsDebuggerCliRunner = annotationConfigApplicationContext.getBean(BPJsDebuggerCliRunner.class);
        bpJsDebuggerCliRunner.runBPJsDebuggerCliRunner();
    }

    public BPJsDebuggerCliRunner() {
    }

    private void init() {
        System.out.println("Debugger id: " + debuggerId);
        bufferedReader = new BufferedReader(new InputStreamReader(System.in));
        bpJsDebugger = debuggerFactory.getBPJsDebugger(debuggerId, filename, DebuggerLevel.NORMAL);
        bpJsDebugger.subscribe(this);
    }

    private void runBPJsDebuggerCliRunner() {
        init();
//        testGoStop();
        while (!isTerminated) {
            boolean isStop = !userMenuLoop(bufferedReader, bpJsDebugger);
            isTerminated = isTerminated || isStop;
        }

        System.out.println("BPJsDebuggerCliRunner exiting..");
    }

    private void testGoStop() {
        applyCommand(bpJsDebugger, new String[]{"go"}, "go");
        sleep(1000);
        applyCommand(bpJsDebugger, new String[]{"stop"}, "stop");
    }

    private void sleep(int time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private boolean userMenuLoop(BufferedReader bufferedReader, BPJsDebugger<BooleanResponse> bpJsDebugger) {
        try {
            String[] splat = getUserInput(bufferedReader);
            String cmd = splat[0];
            applyCommand(bpJsDebugger, splat, cmd);
            return !cmd.equals("stop");
        } catch (IOException e) {
            e.printStackTrace();
            return true;
        }
    }

    private void applyCommand(BPJsDebugger<BooleanResponse> bpJsDebugger, String[] splat, String cmd) {
        switch (cmd) {
            case "b": {
                int lineNumber = Integer.parseInt(splat[1]);
                if (!bpJsDebugger.isSetup()) {
                    breakpoints.put(lineNumber, true);
                }
                else {
                    sendRequest(() -> bpJsDebugger.setBreakpoint(lineNumber, true));
                }
                break;
            }
            case "rb": {
                int lineNumber = Integer.parseInt(splat[1]);
                if (!bpJsDebugger.isSetup()) {
                    breakpoints.remove(lineNumber);
                }
                else {
                    sendRequest(() -> bpJsDebugger.setBreakpoint(lineNumber, false));
                }
                break;
            }
            case "go":
                if (!bpJsDebugger.isStarted()) {
                    sendRequest(() -> bpJsDebugger.startSync(breakpoints, isSkipSyncPoints, isSkipBreakPoints, isWaitForExternalEvents));
                }
                else {
                    sendRequest(bpJsDebugger::continueRun);
                }
                break;
            case "si":
                sendRequest(bpJsDebugger::stepInto);
                break;
            case "sov":
                sendRequest(bpJsDebugger::stepOver);
                break;
            case "sou":
                sendRequest(bpJsDebugger::stepOut);
                break;
            case "tmb":
                isSkipBreakPoints = !isSkipBreakPoints;
                sendRequest(() -> bpJsDebugger.toggleMuteBreakpoints(isSkipBreakPoints));
                break;
            case "n":
                sendRequest(bpJsDebugger::nextSync);
                break;
            case "e": {
                if (splat.length != 2) {
                    System.out.println("must enter event");
                    break;
                }
                sendRequest(() -> bpJsDebugger.addExternalEvent(splat[1]));
                break;
            }
            case "re": {
                if (splat.length != 2) {
                    System.out.println("must enter event");
                    break;
                }
                sendRequest(() -> bpJsDebugger.removeExternalEvent(splat[1]));
                break;
            }
            case "we": {
                if (splat.length != 2) {
                    System.out.println("must enter event");
                    break;
                }
                boolean shouldWait = Integer.parseInt(splat[1]) > 0;
                sendRequest(() -> bpJsDebugger.toggleWaitForExternalEvents(shouldWait));
                break;
            }
            case "h": {
                System.out.println("go - start the program \n" +
                        "n - next sync state \n" +
                        "e <event name>- add external event+ " +
                        "re <event name> - remove external event" +
                        "we <0/1>- wait for external events " +
                        "ex - export sync snapshot" +
                        "");
            }
            case "tsp":
                isSkipSyncPoints = !isSkipSyncPoints;
                sendRequest(() -> bpJsDebugger.toggleMuteSyncPoints(isSkipSyncPoints));
                break;
            case "getss":
                sendGetSyncSnapshotsResponse(bpJsDebugger::getSyncSnapshotsHistory);
                break;
            case "sss":  // set syncsnapshot
                sendRequest(() -> bpJsDebugger.setSyncSnapshot(Long.parseLong(splat[1])));
                break;
            case "stop":
                if (!isTerminated) {
                    sendRequest(bpJsDebugger::stop);
                }
                break;
            case "gets":
                sendRequest(bpJsDebugger::getState);
                break;
            case "ex":
                serialize();
                break;
        }
    }

    private void serialize() {
        try {
            Serializable serialize = bpJsDebugger.getSyncSnapshot();
            System.out.println(serialize);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendRequest(Callable<BooleanResponse> callable) {
        try {
            BooleanResponse booleanResponse = callable.call();
            if (!booleanResponse.isSuccess()) {
                System.out.println("booleanResponse.getErrorCode: " + booleanResponse.getErrorCode());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendGetSyncSnapshotsResponse(Callable<GetSyncSnapshotsResponse> callable) {
        try {
            GetSyncSnapshotsResponse getSyncSnapshotsResponse = callable.call();
            System.out.println(objectMapper.writeValueAsString(getSyncSnapshotsResponse));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String[] getUserInput(BufferedReader bufferedReader) throws IOException {
        printMenu();
        while (!bufferedReader.ready() && !isTerminated) {
            sleep(200);
        }
        String cmd = isTerminated ? "stop" : bufferedReader.readLine();
        return cmd.split(" ");
    }

    private void printMenu() {
        System.out.println(menu);
        if (status == null) {
            System.out.println(prefix + "==" + menuWrapper + "==" + suffix);
        }
        else {
            String statusStr = " Status: " + status + " ";
            int whiteSpacesLengthForStatus = (menuWrapper.length() - suffix.length()) / 2;
            String whiteSpacesForStatus = generate(() -> "=").limit(whiteSpacesLengthForStatus).collect(joining());
            System.out.println(prefix + whiteSpacesForStatus + statusStr + whiteSpacesForStatus + suffix);
        }
    }

    @Override
    public void update(BPEvent event) {
        event.accept(this);
    }

    @Override
    public void visit(String userId, BPDebuggerState debuggerState) {
        System.out.println("debuggerState event received, content: " + debuggerState.prettier());
        printMenu();
    }

    @Override
    public void visit(String userId, ConsoleMessage consoleMessage) {
        System.out.println("consoleMessage event received, content: " + consoleMessage.toString());
        printMenu();
    }

    @Override
    public void visit(String userId, ProgramStatus programStatus) {
        status = programStatus.getStatus();
        isTerminated = Status.STOP.equals(status);
        printMenu();
    }
}
