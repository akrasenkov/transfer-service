package me.akrasenkov.transfer.model.storage;

import me.akrasenkov.transfer.model.domain.AccountState;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

/**
 * Account state representation for usage in datastore.
 */
@Value
@Builder
public class AccountStateRecord {

    private boolean blocked;
    private BigDecimal balance;

    public static AccountStateRecord.AccountStateRecordBuilder from(AccountState state) {
        return builder()
                .blocked(state.isBlocked())
                .balance(state.getBalance());
    }

}
