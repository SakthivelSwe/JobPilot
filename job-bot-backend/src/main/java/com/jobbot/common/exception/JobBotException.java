package com.jobbot.common.exception;

public class JobBotException extends RuntimeException {

    public JobBotException(String message) {
        super(message);
    }

    public JobBotException(String message, Throwable cause) {
        super(message, cause);
    }
}

