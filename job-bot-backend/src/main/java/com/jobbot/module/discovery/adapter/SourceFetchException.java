package com.jobbot.module.discovery.adapter;

/** Raised when a source adapter cannot fetch or parse a public feed. */
public class SourceFetchException extends RuntimeException {
    public SourceFetchException(String message, Throwable cause) {
        super(message, cause);
    }
}

