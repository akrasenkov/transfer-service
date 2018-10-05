package me.akrasenkov.transfer.exception;

import me.akrasenkov.transfer.exception.impl.AccountException;

public class AccountBlockedException extends AccountException {
    public AccountBlockedException(String accountId) {
        super(accountId);
    }
}
