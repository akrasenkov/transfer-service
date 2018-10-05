package me.akrasenkov.transfer.provider;

import me.akrasenkov.transfer.exception.TransferServiceException;
import me.akrasenkov.transfer.model.domain.Transfer;
import me.akrasenkov.transfer.model.domain.TransferReceipt;

/**
 * Provider of funds transfer service. Can perform a funds transfer between accounts.
 */
public interface TransferServiceProvider {

    /**
     * Perform a funds transfer.
     *
     * @param transfer funds transfer parameters
     * @return receipt for performed funds transfer
     * @throws TransferServiceException if an exception occurred during the transfer
     */
    TransferReceipt performTransfer(Transfer transfer) throws TransferServiceException;

}
