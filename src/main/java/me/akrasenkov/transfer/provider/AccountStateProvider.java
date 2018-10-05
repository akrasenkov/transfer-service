package me.akrasenkov.transfer.provider;

import me.akrasenkov.transfer.exception.impl.AccountNotFoundException;
import me.akrasenkov.transfer.exception.TransferServiceException;
import me.akrasenkov.transfer.model.domain.AccountState;

/**
 * Provider of account states.
 * Can return a state for specified account and save an account state with provided params.
 */
public interface AccountStateProvider {

    /**
     * Get account state for specified ID.
     *
     * @param id account ID
     * @return account state for specified ID
     * @throws AccountNotFoundException if account with provided ID not found
     */
    AccountState getAccountState(String id) throws AccountNotFoundException;

    /**
     * Save an account state.
     *
     * @param state account state
     * @return saved account state
     * @throws TransferServiceException if an exception occurred during account saving
     */
    AccountState saveAccountState(AccountState state) throws TransferServiceException;

}
