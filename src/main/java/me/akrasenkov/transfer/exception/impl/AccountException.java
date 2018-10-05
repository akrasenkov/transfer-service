package me.akrasenkov.transfer.exception.impl;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import me.akrasenkov.transfer.exception.TransferServiceException;

@Getter
@RequiredArgsConstructor
public abstract class AccountException extends TransferServiceException {

    private final String accountId;

}
