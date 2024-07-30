package org.tudo.sse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.URISyntaxException;

public class Main {

   public static final Logger log = LogManager.getLogger(Main.class);

    public static void main(String[] args) throws URISyntaxException {
        if(args.length == 0) {
            log.error("Incorrect Usage: see github for different configurations");
            System.exit(1);
        }

        OwnImplementation imp = new OwnImplementation();
        try {
            imp.runAnalysis(args);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}