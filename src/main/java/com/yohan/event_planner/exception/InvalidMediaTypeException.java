package com.yohan.event_planner.exception;

public class InvalidMediaTypeException extends RuntimeException implements HasErrorCode {

    @Override
    public ErrorCode getErrorCode() {
        return ErrorCode.INVALID_MEDIA_TYPE;
    }

    @Override
    public String getMessage() {
        return "Invalid media type provided.";
    }
}