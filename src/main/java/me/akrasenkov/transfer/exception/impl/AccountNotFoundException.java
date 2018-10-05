package me.akrasenkov.transfer.exception.impl;

public class AccountNotFoundException extends AccountException {
    public AccountNotFoundException(String accountId) {
        super(accountId);
    }
}
