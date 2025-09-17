package il.ac.bgu.se.bp.error;

public enum ErrorCode {
    BP_SETUP_FAIL(0),
    SETUP_REQUIRED(1),
    NOT_STARTED(2),
    NOT_RUNNING(3),
    ALREADY_RUNNING(4),
    NOT_IN_JS_DEBUG_STATE(5),
    NOT_IN_BP_SYNC_STATE(6),
    CANNOT_REPLACE_SNAPSHOT(7),
    CANNOT_ADD_EXTERNAL_EVENT_ON_JS_DEBUG_STATE(8),


    WAITING_FOR_EXTERNAL_EVENT(19),
    NOT_WAITING_FOR_EXTERNAL_EVENT(20),
    INVALID_EVENT(21),
    INVALID_SYNC_SNAPSHOT_STATE(22),


    BREAKPOINT_NOT_ALLOWED(30), // todo: add lineNumber


    FAILED_ADDING_COMMAND(40),


    INVALID_REQUEST(80),
    INVALID_SOURCE_CODE(81),
    UNKNOWN_USER(82),


    GENERAL_ERROR(100),
    NOT_SUPPORTED(101),


    IMPORT_SYNC_SNAPSHOT_FAILURE(300)
    ;


    private final int errorCode;

    ErrorCode(int errorCode) {
        this.errorCode = errorCode;
    }

    @Override
    public String toString() {
        return name();
    }
}
