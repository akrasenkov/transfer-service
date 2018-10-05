package me.akrasenkov.transfer.storage.impl;

import me.akrasenkov.transfer.exception.TransferServiceException;
import me.akrasenkov.transfer.model.domain.AccountState;
import me.akrasenkov.transfer.storage.AccountStateStorage;
import me.akrasenkov.transfer.model.storage.AccountStateRecord;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static java.util.Optional.ofNullable;

public class InMemoryAccountStateStorage implements AccountStateStorage {

    /**
     * This implementation is based on not thread-safe {@link HashMap},
     * but we use synchronization outside to make write requests thread-safe.
     */
    private final Map<String, AccountStateRecord> storage = new HashMap<>();

    @Override
    public Optional<AccountState> getAccountStateById(String accountId) {
        // Get an account state for specified ID, or return empty Optional.
        // There is no need for synchronization here.
        return ofNullable(storage.get(accountId))
                .map(record -> stateFromRecord(accountId, record));
    }

    @Override
    public AccountState saveAccountState(AccountState state) {
        // Creating a datastore record with new state params
        // and storing it separately from ID, which become a record key
        AccountStateRecord record = AccountStateRecord.builder()
                .balance(state.getBalance())
                .blocked(state.isBlocked())
                .build();
        storage.put(state.getAccountId(), record);
        return state;
    }

    @Override
    public String generateUniqueId() {
        // Here we simply create a new UUID and retry if we already
        // have such key in datastore to prevent any key duplication
        String id;
        do {
            id = UUID.randomUUID().toString();
        } while (storage.containsKey(id));
        return id;
    }

    @Override
    public AccountState performTransaction(Transaction<AccountState> transaction) throws TransferServiceException {
        // Capture the monitor while executing transaction
        synchronized (storage) {
            return transaction.apply(this);
        }
    }

    /**
     * Convert datastore record to domain {@link AccountState} object.
     *
     * @param id     account ID
     * @param record account params
     * @return domain {@link AccountState} object for specified datastore record
     */
    private static AccountState stateFromRecord(String id, AccountStateRecord record) {
        return AccountState.builder()
                .accountId(id)
                .blocked(record.isBlocked())
                .balance(record.getBalance())
                .build();
    }

    /**
     * Convert domain {@link AccountState} object to datastore record.
     *
     * @param state domain {@link AccountState} object
     * @return datastore record for specified {@link AccountState} object
     */
    private static AccountStateRecord recordFromState(AccountState state) {
        return AccountStateRecord.builder()
                .blocked(state.isBlocked())
                .balance(state.getBalance())
                .build();
    }
}
