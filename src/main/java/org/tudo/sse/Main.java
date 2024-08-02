package org.tudo.sse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.tudo.sse.model.Artifact;
import org.tudo.sse.resolution.PomResolutionException;
import org.tudo.sse.utils.GAUtils;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

public class Main {

   public static final Logger log = LogManager.getLogger(Main.class);

    public static void main(String[] args) throws URISyntaxException {
        if(args.length == 0) {
            log.error("Incorrect Usage: see github for different configurations");
            System.exit(1);
        }

        OwnImplementation imp = new OwnImplementation(false, true, false, true);
        try {
            imp.runAnalysis(args);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}