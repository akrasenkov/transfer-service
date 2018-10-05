package me.akrasenkov.transfer.provider.impl;

import me.akrasenkov.transfer.exception.AccountBlockedException;
import me.akrasenkov.transfer.exception.impl.AccountNotFoundException;
import me.akrasenkov.transfer.exception.impl.NotEnoughFundsException;
import me.akrasenkov.transfer.exception.TransferServiceException;
import me.akrasenkov.transfer.model.domain.AccountState;
import me.akrasenkov.transfer.model.domain.Transfer;
import me.akrasenkov.transfer.model.domain.TransferReceipt;
import me.akrasenkov.transfer.provider.TransferServiceProvider;
import me.akrasenkov.transfer.storage.AccountStateStorage;

import java.math.BigDecimal;
import javax.inject.Inject;

public class TransferServiceProviderImpl implements TransferServiceProvider {

    private final AccountStateStorage accountStateStorage;

    @Inject
    public TransferServiceProviderImpl(AccountStateStorage accountStateStorage) {
        this.accountStateStorage = accountStateStorage;
    }

    @Override
    public TransferReceipt performTransfer(Transfer transfer) throws TransferServiceException {
        accountStateStorage.performTransaction(storage -> {
            String senderId = transfer.getSenderId();
            String receiverId = transfer.getReceiverId();
            BigDecimal transferAmount = transfer.getAmount();

            // Get participating accounts states or throw exception if not found
            AccountState sender = storage.getAccountStateById(senderId)
                    .orElseThrow(() -> new AccountNotFoundException(senderId));
            AccountState receiver = storage.getAccountStateById(receiverId)
                    .orElseThrow(() -> new AccountNotFoundException(receiverId));
            // Check if any account is blocked
            if (sender.isBlocked()) throw new AccountBlockedException(senderId);
            if (receiver.isBlocked()) throw new AccountBlockedException(receiverId);

            // Calculate new balances
            BigDecimal newSenderBalance = sender.getBalance().subtract(transferAmount);
            BigDecimal newReceiverBalance = receiver.getBalance().add(transferAmount);
            AccountState newSenderState = updateAccountBalance(sender, newSenderBalance);
            AccountState newReceiverState = updateAccountBalance(receiver, newReceiverBalance);
            if (isNegative(newSenderBalance)) {
                // Throw exception, if senderId does not have enough funds to transfer.
                // Then transaction is terminated.
                throw new NotEnoughFundsException(transferAmount, sender.getBalance());
            }
            // Save states if transfer is OK
            storage.saveAccountState(newSenderState);
            storage.saveAccountState(newReceiverState);

            return newSenderState;
        });
        // Create transfer receipt based on performed transfer
        return TransferReceipt.from(transfer).build();
    }

    private static boolean isNegative(BigDecimal number) {
        return number.compareTo(BigDecimal.ZERO) < 0;
    }

    private static AccountState updateAccountBalance(AccountState account, BigDecimal balance) {
        return AccountState.from(account).balance(balance).build();
    }
}
