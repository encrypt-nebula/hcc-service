package com.example.hcc.exceptions;


public class UserMissingFromDatabaseException extends RuntimeException {
    public UserMissingFromDatabaseException(String message) {
        super(message);
    }
}
