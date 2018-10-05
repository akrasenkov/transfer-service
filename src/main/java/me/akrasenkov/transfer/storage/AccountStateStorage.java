package me.akrasenkov.transfer.storage;

import me.akrasenkov.transfer.exception.TransferServiceException;
import me.akrasenkov.transfer.model.domain.AccountState;

import java.util.Optional;

/**
 * An interface for account storage providers.
 */
public interface AccountStateStorage {

    /**
     * Get an account state for account with specified ID.
     *
     * @param accountId account ID
     * @return Optional(AccountState) - a found account state
     */
    Optional<AccountState> getAccountStateById(String accountId);

    /**
     * Save an account state.
     *
     * @param state account state
     * @return saved account state
     */
    AccountState saveAccountState(AccountState state);

    /**
     * Create a new datastore-wise unique ID for a record.
     *
     * @return unique ID for a record.
     */
    String generateUniqueId();

    /**
     * Execute a transaction within datastore context.
     *
     * @param transaction transaction to execute
     * @return result of transaction - updated account state
     * @throws TransferServiceException if an exception occurred during datastore transaction
     */
    AccountState performTransaction(Transaction<AccountState> transaction) throws TransferServiceException;

    /**
     * Datastore transaction functional interface.
     *
     * @param <T> type of transaction result
     */
    @FunctionalInterface
    interface Transaction<T> {
        T apply(AccountStateStorage storage) throws TransferServiceException;
    }
}
