package it.reply.orchestrator.service;

public class IamServiceException extends RuntimeException {

    public IamServiceException(String message) {
        super(message);
    }

    public IamServiceException(String message, Throwable e) {
        super(message, e);
    }
}
