package me.akrasenkov.transfer.model.domain;

import lombok.Builder;
import lombok.Data;
import lombok.Singular;

import java.util.List;

/**
 * Exception message domain representation.
 */
@Data
@Builder
public class ExceptionMessage {

    private final Reason reason;

    @Singular
    private final List<String> values;

    public enum Reason {
        ACCOUNT_NOT_FOUND,
        NOT_ENOUGH_FUNDS,
        ACCOUNT_IS_BLOCKED,
        INVALID_PARAM,
        UNKNOWN
    }

}
