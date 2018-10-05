package me.akrasenkov.transfer.model.domain;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Account state domain representation.
 */
@Data
@Builder
public class AccountState {
    private final String accountId;
    private final boolean blocked;
    private final BigDecimal balance;

    public static AccountStateBuilder from(AccountState state) {
        return builder()
                .accountId(state.getAccountId())
                .blocked(state.isBlocked())
                .balance(state.getBalance());
    }
}
