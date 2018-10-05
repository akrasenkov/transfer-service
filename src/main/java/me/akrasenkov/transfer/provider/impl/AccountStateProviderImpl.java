package me.akrasenkov.transfer.provider.impl;

import me.akrasenkov.transfer.exception.impl.AccountNotFoundException;
import me.akrasenkov.transfer.exception.TransferServiceException;
import me.akrasenkov.transfer.model.domain.AccountState;
import me.akrasenkov.transfer.provider.AccountStateProvider;
import me.akrasenkov.transfer.storage.AccountStateStorage;

import java.math.BigDecimal;
import javax.inject.Inject;

import static com.google.common.base.Predicates.isNull;
import static com.google.common.base.Strings.isNullOrEmpty;

public class AccountStateProviderImpl implements AccountStateProvider {

    private AccountStateStorage accountStateStorage;

    @Inject
    public AccountStateProviderImpl(AccountStateStorage accountStateStorage) {
        this.accountStateStorage = accountStateStorage;
    }

    @Override
    public AccountState getAccountState(String accountId) throws AccountNotFoundException {
        return accountStateStorage.getAccountStateById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));
    }

    @Override
    public AccountState saveAccountState(AccountState state) throws TransferServiceException {
        return accountStateStorage.performTransaction(storage -> {
            String accountId = state.getAccountId();
            BigDecimal accountBalance = state.getBalance();
            if (isNullOrEmpty(accountId)) {
                // Create a new ID if not provided
                accountId = storage.generateUniqueId();
            }
            if (accountBalance == null) {
                accountBalance = BigDecimal.ZERO;
            }
            AccountState stateWithId = AccountState.from(state).accountId(accountId).build();
            return accountStateStorage.saveAccountState(stateWithId);
        });
    }
}
