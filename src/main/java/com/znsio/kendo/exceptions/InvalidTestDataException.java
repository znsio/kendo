package com.znsio.kendo.exceptions;

public class InvalidTestDataException extends RuntimeException {
    public InvalidTestDataException(String failureMessage) {
        super(failureMessage);
    }
    public InvalidTestDataException(String failureMessage, Exception e) {
        super(failureMessage, e);
    }
}
