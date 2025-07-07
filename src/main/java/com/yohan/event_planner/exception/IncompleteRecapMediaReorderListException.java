package com.yohan.event_planner.exception;

public class IncompleteRecapMediaReorderListException extends RuntimeException implements HasErrorCode {
    private final Long recapId;

    public IncompleteRecapMediaReorderListException(Long recapId) {
        super("Recap media reorder list incomplete for recap ID " + recapId);
        this.recapId = recapId;
    }

    @Override
    public ErrorCode getErrorCode() {
        return ErrorCode.INCOMPLETE_RECAP_MEDIA_REORDER_LIST;
    }
}
