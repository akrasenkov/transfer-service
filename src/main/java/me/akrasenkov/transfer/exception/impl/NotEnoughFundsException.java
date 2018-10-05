package me.akrasenkov.transfer.exception.impl;

import lombok.Data;
import lombok.EqualsAndHashCode;
import me.akrasenkov.transfer.exception.TransferServiceException;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = false)
public class NotEnoughFundsException extends TransferServiceException {

    private final BigDecimal amountRequested;
    private final BigDecimal amountAvailable;

}
