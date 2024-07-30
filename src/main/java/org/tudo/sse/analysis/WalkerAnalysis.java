package org.tudo.sse.analysis;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;

public class WalkerAnalysis {
    private static final Logger log = LogManager.getLogger(WalkerAnalysis.class);

    //Field counts
    static long descriptCount = 0;
    static long classifierCount = 0;
    static long jarCount = 0;
    static long pomCount = 0;
    static long aarCount = 0;
    static long xmlCount = 0;
    static long lastModifiedCount = 0;
    static long infoCount = 0;
    static long nameCount = 0;
    static long sha1Count = 0;
    static long jarContentCount = 0;
    static long mavenPluginCount = 0;

    //Analysis function for checking which fields are present in the maven index
    public static void checkFields(Map<String, String> item, long index) {
        if(item.get("i") != null) {
            String[] parts = item.get("i").split("\\|");
            switch (parts[0]) {
                case "jar":
                    jarCount++;
                    break;
                case "pom":
                    pomCount++;
                    break;
                case "aar":
                    aarCount++;
                    break;
                case "xml":
                    xmlCount++;
                    break;
            }
            infoCount++;
        }

        if(item.get("m") != null) {
            lastModifiedCount++;
        }

        if(item.get("l") != null) {
            classifierCount++;
        }

        if(item.get("1") != null) {
            sha1Count++;
        }

        if(item.get("n") != null) {
            nameCount++;
        }

        if(item.get("d") != null) {
            descriptCount++;
        }

        if(item.get("classnames") != null) {
            jarContentCount++;
        }

        if(item.get("px") != null) {
            mavenPluginCount++;
        }

        if(index % 100000 == 0) {
            double indexCount = index;
            log.info(indexCount + " entries collected");
            infoAnalysis(indexCount);
        }

        if(index == 20000000) {
            System.exit(0);
        }
    }

    //Analysis function for checking which fields are present in the maven index
    public static void infoAnalysis(double indexCount) {
        log.info("Percentage of info present: " + (infoCount / indexCount) * 100);
        log.info("Percentage of jars present: " + (jarCount / indexCount) * 100);
        log.info("Percentage of poms present: " + (pomCount / indexCount) * 100);
        log.info("Percentage of aars present: " + (aarCount / indexCount) * 100);
        log.info("Percentage of xmls present: " + (xmlCount / indexCount) * 100);
        log.info("Percentage of classifier present: " + (classifierCount / indexCount) * 100);
        log.info("Percentage of lastModified present: " + (lastModifiedCount / indexCount) * 100);
        log.info("Percentage of sha1CheckSum present: " + (sha1Count / indexCount) * 100);
        log.info("Percentage of artifact names present: " + (nameCount / indexCount) * 100);
        log.info("Percentage of descriptions present: " + (descriptCount / indexCount) * 100);
        log.info("Percentage of jarContent present: " + (jarContentCount / indexCount) * 100);
        log.info("Percentage of mavenPlugins present: " + (mavenPluginCount / indexCount) * 100);
    }

}
