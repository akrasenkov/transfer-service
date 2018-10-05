package me.akrasenkov.transfer;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import me.akrasenkov.transfer.model.domain.AccountState;
import me.akrasenkov.transfer.model.domain.ExceptionMessage;
import me.akrasenkov.transfer.model.domain.TransferReceipt;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

import java.io.IOException;
import java.math.BigDecimal;

import static com.google.common.truth.Truth.assertThat;

/**
 * Integration tests suite for Funds Transfer Service.
 */
public class TransferServiceRestApiTest {

    /**
     * Port for test application's API serving.
     */
    private static final int APP_PORT = 8081;

    private static TransferService transferService;
    private static Retrofit retrofit;
    private static Gson gson;
    private static App app;

    /**
     * Setting up integration tests environment.
     * We need to create a Guice injector for our App and start it manually.
     * Then we create a Retrofit client based on {@link TransferService} for our RESTful API.
     */
    @BeforeAll
    public static void setUp() {
        Injector injector = Guice.createInjector(new AppModule());
        app = new App(APP_PORT, injector);
        app.run();

        gson = new GsonBuilder()
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .create();

        retrofit = new Retrofit.Builder()
                .baseUrl("http://localhost:" + APP_PORT)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();
        transferService = retrofit.create(TransferService.class);
    }

    /**
     * Test a positive case of creating a new Account.
     * Service MUST:
     *      - return account params equal to the sent ones
     *      - return the `201 Created code`
     *      - return the `Location` header with relative path to created account
     *
     * @throws IOException Retrofit I/O exception
     */
    @Test
    public void accountCreationTest_PositiveCase() throws IOException {
        AccountState state = AccountState.builder()
                .balance(new BigDecimal("56778.456"))
                .blocked(true)
                .build();

        Call<AccountState> createRequest = transferService.createAccount(state);
        Response<AccountState> createResponse = createRequest.execute();
        assertThat(createResponse.code()).isEqualTo(201);
        assertThat(createResponse.body()).isNotNull();;

        AccountState createdState = createResponse.body();
        assertThat(createdState.getAccountId()).isNotEmpty();
        assertThat(createdState.getBalance()).isEqualTo(state.getBalance());
        assertThat(createdState.isBlocked()).isTrue();
        assertThat(createResponse.headers().get("Location"))
                .isEqualTo("/account/" + createdState.getAccountId());

        AccountState fetchedState = getAccountAndCheck(createdState.getAccountId());
        assertThat(fetchedState.getBalance()).isEqualTo(createdState.getBalance());
        assertThat(fetchedState.getAccountId()).isEqualTo(createdState.getAccountId());
        assertThat(fetchedState.isBlocked()).isTrue();
    }

    /**
     * Test a positive case of funds transfer between accounts.
     * Service MUST:
     *      - subtract transfer amount from senderId's balance
     *      - add transfer amount to receiverId's balance
     *      - return the `200 OK` code and non-empty response body
     *      - return a transfer receipt with correct amount
     *      - return correct balances for senderId and receiverId
     *
     * @throws IOException Retrofit I/O exception
     */
    @Test
    public void fundsTransfer_PositiveCase() throws IOException {
        AccountState sender = transferService
                .createAccount(AccountState.builder().balance(new BigDecimal("56778.456")).build())
                .execute()
                .body();
        AccountState receiver = transferService
                .createAccount(AccountState.builder().balance(new BigDecimal("890.789")).build())
                .execute()
                .body();
        BigDecimal amount = new BigDecimal("6770.111");

        Call<TransferReceipt> transferRequest = transferService.performTransfer(
                sender.getAccountId(),
                receiver.getAccountId(),
                amount);
        Response<TransferReceipt> transferResponse = transferRequest.execute();
        assertThat(transferResponse.code()).isEqualTo(200);
        assertThat(transferResponse.body()).isNotNull();

        TransferReceipt receipt = transferResponse.body();
        assertThat(receipt.getSenderId()).isEqualTo(sender.getAccountId());
        assertThat(receipt.getReceiverId()).isEqualTo(receiver.getAccountId());
        assertThat(receipt.getAmount()).isEqualTo(amount);

        AccountState updatedSender = getAccountAndCheck(sender.getAccountId());
        AccountState updatedReceiver = getAccountAndCheck(receiver.getAccountId());
        assertThat(updatedSender.getBalance()).isEqualTo(sender.getBalance().subtract(amount));
        assertThat(updatedReceiver.getBalance()).isEqualTo(receiver.getBalance().add(amount));
    }

    /**
     * Test a negative case of funds transfer: senderId account is blocked.
     * Service MUST:
     *      - return the `403 Forbidden`` code
     *      - return body with `reason` property set to `ACCOUNT_IS_BLOCKED`
     *      - return body with `values` property containing exactly one ID equal to senderId account ID
     *
     * @throws IOException Retrofit I/O exception
     */
    @Test
    public void fundsTransfer_SenderBlockedNegativeCase() throws IOException {
        AccountState senderBlocked = transferService
                .createAccount(AccountState.builder().balance(new BigDecimal("56778.456")).blocked(true).build())
                .execute()
                .body();
        AccountState receiver = transferService
                .createAccount(AccountState.builder().balance(new BigDecimal("890.789")).build())
                .execute()
                .body();
        BigDecimal amount = new BigDecimal("6770.111");

        ExceptionMessage message = performTransferAndCheckError(senderBlocked, receiver, amount, 403);
        assertThat(message.getReason()).isEqualTo(ExceptionMessage.Reason.ACCOUNT_IS_BLOCKED);
        assertThat(message.getValues()).containsExactly(senderBlocked.getAccountId());

        AccountState updatedSender = getAccountAndCheck(senderBlocked.getAccountId());
        AccountState updatedReceiver = getAccountAndCheck(receiver.getAccountId());
        assertThat(updatedSender.getBalance()).isEqualTo(senderBlocked.getBalance());
        assertThat(updatedReceiver.getBalance()).isEqualTo(receiver.getBalance());
    }

    /**
     * Test a negative case of funds transfer: receiverId account is blocked.
     * Service MUST:
     *      - return the `403 Forbidden`` code
     *      - return body with `reason` property set to `ACCOUNT_IS_BLOCKED`
     *      - return body with `values` property containing exactly one ID equal to receiverId account ID
     *
     * @throws IOException Retrofit I/O exception
     */
    @Test
    public void fundsTransfer_ReceiverBlockedNegativeCase() throws IOException {
        AccountState sender = transferService
                .createAccount(AccountState.builder().balance(new BigDecimal("56778.456")).build())
                .execute()
                .body();
        AccountState receiverBlocked = transferService
                .createAccount(AccountState.builder().balance(new BigDecimal("890.789")).blocked(true).build())
                .execute()
                .body();
        BigDecimal amount = new BigDecimal("6770.111");

        ExceptionMessage message = performTransferAndCheckError(sender, receiverBlocked, amount, 403);
        assertThat(message.getReason()).isEqualTo(ExceptionMessage.Reason.ACCOUNT_IS_BLOCKED);
        assertThat(message.getValues()).containsExactly(receiverBlocked.getAccountId());

        AccountState updatedSender = getAccountAndCheck(sender.getAccountId());
        AccountState updatedReceiver = getAccountAndCheck(receiverBlocked.getAccountId());
        assertThat(updatedSender.getBalance()).isEqualTo(sender.getBalance());
        assertThat(updatedReceiver.getBalance()).isEqualTo(receiverBlocked.getBalance());
    }

    /**
     * Test a negative case of funds transfer: senderId does not have enough funds to transfer.
     * Service MUST:
     *      - return the `400 Bad Request` code
     *      - return body with `reason` property set to `NOT_ENOUGH_FUNDS`
     *      - return body with `values` property containing exactly one number equal to senderId's account balance
     *
     * @throws IOException Retrofit I/O exception
     */
    @Test
    public void fundsTransfer_NotEnoughFundsNegativeCase() throws IOException {
        AccountState sender = transferService
                .createAccount(AccountState.builder().balance(new BigDecimal("1.01")).build())
                .execute()
                .body();
        AccountState receiver = transferService
                .createAccount(AccountState.builder().balance(new BigDecimal("100")).build())
                .execute()
                .body();
        BigDecimal amount = new BigDecimal("1.02");

        ExceptionMessage message = performTransferAndCheckError(sender, receiver, amount, 400);
        assertThat(message.getReason()).isEqualTo(ExceptionMessage.Reason.NOT_ENOUGH_FUNDS);
        assertThat(message.getValues()).containsExactly(sender.getBalance().toString());
    }

    /**
     * Helper method for performing funds transfer and response check.
     *
     * @param sender        funds senderId
     * @param receiver      funds receiverId
     * @param amount        transfer amount
     * @param expectedCode  response code expected
     * @return ExceptionMessage response body
     * @throws IOException Retrofit I/O exception
     */
    private ExceptionMessage performTransferAndCheckError(
            AccountState sender, AccountState receiver, BigDecimal amount, int expectedCode)
            throws IOException {
        Call<TransferReceipt> transferRequest = transferService.performTransfer(
                sender.getAccountId(),
                receiver.getAccountId(),
                amount);
        Response<TransferReceipt> transferResponse = transferRequest.execute();
        assertThat(transferResponse.code()).isEqualTo(expectedCode);
        assertThat(transferResponse.errorBody()).isNotNull();

        return gson.fromJson(transferResponse.errorBody().string(), ExceptionMessage.class);
    }

    /**
     * Helper method for account fetching and response check.
     *
     * @param accountId account ID
     * @return account state for specified ID
     * @throws IOException Retrofit I/O exception
     */
    private AccountState getAccountAndCheck(String accountId) throws IOException {
        Call<AccountState> request = transferService.getAccount(accountId);
        Response<AccountState> response = request.execute();
        assertThat(response.code()).isEqualTo(200);
        assertThat(response.body()).isNotNull();
        return response.body();
    }

    /**
     * Retrofit RESTful interface representation for App.
     */
    private interface TransferService {

        @POST("/account/")
        Call<AccountState> createAccount(@Body AccountState state);

        @GET("/account/{accountId}")
        Call<AccountState> getAccount(@Path("accountId") String accountId);

        @POST("/transfer/{senderId}/to/{receiverId}")
        Call<TransferReceipt> performTransfer(@Path("senderId") String senderId,
                                              @Path("receiverId") String receiverId,
                                              @Query("amount") BigDecimal amount);

    }

}
