package me.akrasenkov.transfer;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;

import static com.google.common.base.Preconditions.checkNotNull;

public class App implements Runnable {

    private static App instance;

    private Injector injector;
    private int port;

    @Inject
    private TransferServiceRestApi restApi;

    public static void main(String[] args) {
        if (args.length == 0) throw new IllegalArgumentException("App port not provided");
        int port = Integer.valueOf(checkNotNull(args[0]));
        Injector injector = Guice.createInjector(new AppModule());
        instance = new App(port, injector);
        instance.run();
    }

    public App(int port, Injector injector) {
        this.port = port;
        this.injector = injector;
    }

    @Override
    public void run() {
        injector.injectMembers(this);
        restApi.init(port);
    }

}
