package com.znsio.kendo.exceptions;

public class TestExecutionException extends RuntimeException {
    public TestExecutionException(String failureMessage) {
        super(failureMessage);
    }
}
