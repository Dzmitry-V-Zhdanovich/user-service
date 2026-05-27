package com.innowise.userservice.exception;

public class TooManyCardsException extends RuntimeException {

    private static final int MAX_CARDS = 5;

    public TooManyCardsException(Object userId, int currentCount) {
        super(String.format("Пользователь с id '%s' уже имеет %d карт. Максимальное количество: %d",
                userId, currentCount, MAX_CARDS));
    }

    public TooManyCardsException(String message) {
        super(message);
    }
}
