package me.akrasenkov.transfer;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import me.akrasenkov.transfer.exception.AccountBlockedException;
import me.akrasenkov.transfer.exception.impl.AccountNotFoundException;
import me.akrasenkov.transfer.exception.impl.NotEnoughFundsException;
import me.akrasenkov.transfer.exception.TransferServiceException;
import me.akrasenkov.transfer.model.domain.AccountState;
import me.akrasenkov.transfer.model.domain.ExceptionMessage;
import me.akrasenkov.transfer.model.domain.Transfer;
import me.akrasenkov.transfer.model.domain.TransferReceipt;
import me.akrasenkov.transfer.provider.AccountStateProvider;
import me.akrasenkov.transfer.provider.TransferServiceProvider;
import spark.Request;
import spark.Response;

import java.math.BigDecimal;
import javax.inject.Inject;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.lang.String.format;
import static spark.Spark.before;
import static spark.Spark.exception;
import static spark.Spark.get;
import static spark.Spark.path;
import static spark.Spark.port;
import static spark.Spark.post;

/**
 * Application RESTful API.
 */
public class TransferServiceRestApi {

    private static final int HTTP_CREATED = 201;
    private static final int HTTP_BAD_REQUEST = 400;
    private static final int HTTP_FORBIDDEN = 403;
    private static final int HTTP_NOT_FOUND = 404;

    private static final String HEADER_LOCATION = "Location";
    private static final String APPLICATION_JSON_TYPE = "application/json";

    private final TransferServiceProvider transferServiceProvider;
    private final AccountStateProvider accountStateProvider;

    private final Gson gson = new GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .create();

    @Inject
    public TransferServiceRestApi(TransferServiceProvider transferServiceProvider,
                                  AccountStateProvider accountStateProvider) {
        this.transferServiceProvider = transferServiceProvider;
        this.accountStateProvider = accountStateProvider;
    }


    /**
     * RESTful API initialization. Ignites SparkJava and API routes.
     *
     * @param port application port to serve API
     */
    public void init(int port) {
        port(port);

        before((req, res) -> {
            res.type(APPLICATION_JSON_TYPE);
        });
        post("/transfer/:senderId/to/:receiverId", this::performTransfer, gson::toJson);
        path("/account", () -> {
            post("/", this::createAccount, gson::toJson);
            get("/:accountId", this::getAccount, gson::toJson);
        });

        exception(AccountNotFoundException.class, this::mapAccountNotFoundException);
        exception(AccountBlockedException.class, this::mapAccountBlockedException);
        exception(NotEnoughFundsException.class, this::mapNotEnoughFundsException);
        exception(IllegalArgumentException.class, this::mapIllegalArgumentException);
    }

    /**
     * Handle `GET /account/:accountId` request.
     * Retrieve account state with provided ID.
     *
     * @param rq request
     * @param rp response
     * @return account state with provided ID
     * @throws AccountNotFoundException if account with this ID was not found
     */
    private AccountState getAccount(Request rq, Response rp) throws AccountNotFoundException {
        String accountId = rq.params("accountId");
        return accountStateProvider.getAccountState(accountId);
    }

    /**
     * Handle `POST /account/` request.
     * Create new account with provided parameters.
     *
     * @param rq request
     * @param rp response
     * @return created account state
     * @throws TransferServiceException if an exception occurred during the account creation
     */
    private AccountState createAccount(Request rq, Response rp) throws TransferServiceException {
        AccountState newState = gson.fromJson(rq.body(), AccountState.class);
        AccountState createdState = accountStateProvider.saveAccountState(newState);
        rp.status(HTTP_CREATED);
        rp.header(HEADER_LOCATION, format("/account/%s", createdState.getAccountId()));
        return createdState;
    }

    /**
     * Handle `POST /transfer/:senderId/to/:receiverId/` request.
     * Perform a funds transfer between two accounts with provided IDs and transfer amount.
     *
     * @param rq request
     * @param rp response
     * @return receipt for performed funds transfer
     * @throws TransferServiceException if an exception occurred during the transfer
     */
    private TransferReceipt performTransfer(Request rq, Response rp) throws TransferServiceException {
        BigDecimal amount;
        String senderId = rq.params("senderId");
        String receiverId = rq.params("receiverId");
        String amountStr = rq.queryParams("amount");
        if (isNullOrEmpty(amountStr)){
            throw new IllegalArgumentException("amount");
        }
        try {
            amount = new BigDecimal(amountStr);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("amount");
        }

        return transferServiceProvider.performTransfer(Transfer.builder()
                .amount(amount)
                .receiverId(receiverId)
                .senderId(senderId)
                .build());
    }

    private void mapIllegalArgumentException(IllegalArgumentException ex, Request rq, Response rp) {
        ExceptionMessage message = ExceptionMessage.builder()
                .reason(ExceptionMessage.Reason.INVALID_PARAM)
                .value(ex.getMessage())
                .build();
        rp.body(gson.toJson(message));
        rp.status(HTTP_BAD_REQUEST);
    }

    private void mapAccountNotFoundException(AccountNotFoundException ex, Request rq, Response rp) {
        ExceptionMessage message = ExceptionMessage.builder()
                .reason(ExceptionMessage.Reason.ACCOUNT_NOT_FOUND)
                .value(ex.getAccountId())
                .build();
        rp.body(gson.toJson(message));
        rp.status(HTTP_NOT_FOUND);
    }

    private void mapAccountBlockedException(AccountBlockedException ex, Request rq, Response rp) {
        ExceptionMessage message = ExceptionMessage.builder()
                .reason(ExceptionMessage.Reason.ACCOUNT_IS_BLOCKED)
                .value(ex.getAccountId())
                .build();
        rp.body(gson.toJson(message));
        rp.status(HTTP_FORBIDDEN);
    }

    private void mapNotEnoughFundsException(NotEnoughFundsException ex, Request rq, Response rp) {
        ExceptionMessage message = ExceptionMessage.builder()
                .reason(ExceptionMessage.Reason.NOT_ENOUGH_FUNDS)
                .value(ex.getAmountAvailable().toString())
                .build();
        rp.body(gson.toJson(message));
        rp.status(HTTP_BAD_REQUEST);
    }
}
