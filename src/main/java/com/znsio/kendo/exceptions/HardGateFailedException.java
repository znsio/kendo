package com.znsio.kendo.exceptions;

public class HardGateFailedException extends RuntimeException {
    public HardGateFailedException(String failureMessage) {
        super(failureMessage);
    }
}
