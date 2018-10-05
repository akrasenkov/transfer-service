package me.akrasenkov.transfer.model.domain;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Funds transfer receipt domain representation.
 */
@Data
@Builder
public class TransferReceipt {

    private final String senderId;
    private final String receiverId;
    private final BigDecimal amount;

    public static TransferReceiptBuilder from(Transfer transfer) {
        return builder()
                .senderId(transfer.getSenderId())
                .receiverId(transfer.getReceiverId())
                .amount(transfer.getAmount());
    }
}
