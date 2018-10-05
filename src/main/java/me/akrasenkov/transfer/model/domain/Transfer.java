package me.akrasenkov.transfer.model.domain;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Funds transfer domain representation.
 */
@Data
@Builder
public class Transfer {
    private final String senderId;
    private final String receiverId;
    private final BigDecimal amount;
}
