package com.bank.appEntry;

import com.bank.api.BankApiServer;
import com.bank.exception.ServerStartupException;

import java.util.logging.Level;
import java.util.logging.Logger;

public class Main {

    private static final Logger LOGGER =
            Logger.getLogger(Main.class.getName());

    public static void main(String[] args) {

        try {

            new BankApiServer().start();

            LOGGER.info(
                    "Mirae Bank API server started successfully."
            );

        } catch (Exception e) {

            ServerStartupException exception =
                    new ServerStartupException(
                            "Unable to initialize server.",
                            e
                    );

            LOGGER.log(
                    Level.SEVERE,
                    exception.getMessage(),
                    exception
            );
        }
    }
}