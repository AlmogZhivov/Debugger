package il.ac.bgu.se.bp.utils;

import il.ac.bgu.se.bp.error.ErrorCode;
import il.ac.bgu.se.bp.rest.response.BooleanResponse;

public class ResponseHelper {

    public static BooleanResponse createSuccessResponse() {
        return new BooleanResponse(true);
    }

    public static BooleanResponse createErrorResponse(ErrorCode errorCode) {
        return new BooleanResponse(false, errorCode);
    }
}
