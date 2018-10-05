package me.akrasenkov.transfer;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import me.akrasenkov.transfer.provider.AccountStateProvider;
import me.akrasenkov.transfer.provider.TransferServiceProvider;
import me.akrasenkov.transfer.provider.impl.AccountStateProviderImpl;
import me.akrasenkov.transfer.provider.impl.TransferServiceProviderImpl;
import me.akrasenkov.transfer.storage.AccountStateStorage;
import me.akrasenkov.transfer.storage.impl.InMemoryAccountStateStorage;

/**
 * Main injection module for app.
 */
public class AppModule extends AbstractModule {
    @Override
    protected void configure() {
        // We use an in-memory storage implementation, so let's bind it as Singleton.
        bind(AccountStateStorage.class).to(InMemoryAccountStateStorage.class).in(Singleton.class);

        // Service providers binding.
        bind(TransferServiceProvider.class).to(TransferServiceProviderImpl.class);
        bind(AccountStateProvider.class).to(AccountStateProviderImpl.class);

        // Application RESTful API binding.
        bind(TransferServiceRestApi.class);
    }
}
