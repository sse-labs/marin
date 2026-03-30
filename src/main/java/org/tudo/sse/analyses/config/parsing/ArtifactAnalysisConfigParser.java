package org.tudo.sse.analyses.config.parsing;

import org.tudo.sse.CLIException;
import org.tudo.sse.analyses.config.ArtifactAnalysisConfig;
import org.tudo.sse.analyses.config.ArtifactAnalysisConfigBuilder;
import org.tudo.sse.analyses.config.InvalidConfigurationException;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Date;

/**
 * Parser for creating {@link ArtifactAnalysisConfig} objects from CLI parameters.
 * Configuration is obtained by parsing command line arguments provided as an array of strings.
 */
public class ArtifactAnalysisConfigParser extends LibraryAnalysisConfigParser implements CLIParsingUtilities {

    private static final ZoneId GMT_ZONE = ZoneId.of("GMT");

    /**
     * Creates a new artifact config parser instance.
     */
    public ArtifactAnalysisConfigParser() { super(); }

    /**
     * Parses a given array containing JVM command line arguments into an {@link org.tudo.sse.analyses.config.ArtifactAnalysisConfig} object. Throws an exception
     * if the given arguments are not valid.
     * @param args Array of command line arguments, as passed by the JVM
     * @return Corresponding {@link ArtifactAnalysisConfig} if parsing was successful
     * @throws CLIException If parsing failed. Contains details about the specific argument that was invalid.
     */
    public ArtifactAnalysisConfig parseArtifactConfig(String[] args) throws CLIException {
        final ArtifactAnalysisConfigBuilder configBuilder = new ArtifactAnalysisConfigBuilder();

        for(int i = 0; i < args.length; i += 2) {
            handleArtifactParameter(args, i, configBuilder);
        }

        return configBuilder.build();
    }


    private void handleArtifactParameter(String[] args, int i, ArtifactAnalysisConfigBuilder configBuilder) throws CLIException {
        try {

            switch(args[i]) {
                case "-su":
                case "--since-until":
                    final String[] sinceUntil = nextArgAsStringPair(args, i);

                    long since = toUnixTimeStampMillis(sinceUntil[0], "since", false);
                    long until = toUnixTimeStampMillis(sinceUntil[1], "until", true);

                    configBuilder.withSinceUtil(since, until);
                    break;
                case "-o":
                case "--output":
                    configBuilder.withOutputDirectory(nextArgAsDirectoryReference(args, i));
                    break;
                default:
                    super.handleParameter(args, i , configBuilder);

            }

        } catch (InvalidConfigurationException icx) {
            CLIException wrapped = new CLIException(icx.getMessage(), icx.getAttributeName());
            wrapped.initCause(icx);
            throw wrapped;
        }
    }

    private long toUnixTimeStampMillis(String value, String attrName, boolean useEndOfDay) throws CLIException {
        ZonedDateTime date;

        try {
            long timestamp = Long.parseLong(value);
            Instant s = Instant.ofEpochSecond(timestamp);
            date = s.atZone(GMT_ZONE);
        } catch (NumberFormatException ignored){
            date = parseYYYYMMDD(value, attrName);
            // Parsing a date will create a (local) time of 00:00:00 - we want to add one day if requested
            if(useEndOfDay)
                date = date.plusHours(23).plusMinutes(59).plusSeconds(59);
        } catch (DateTimeException dtx){
            throw new CLIException("Not a valid UNIX timestamp", attrName);
        }

        // Do a plausibility check on the given time stamp
        if(date.getYear() < 1950 || date.getYear() > 2100)
            throw new CLIException("Cutoff dates must fall between the years 1950 and 2100");

        return date.toInstant().toEpochMilli();
    }

    private ZonedDateTime parseYYYYMMDD(String value, String attrName) throws CLIException {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("uuuu-MM-dd");

        try {
            return LocalDate.parse(value, dtf).atStartOfDay(GMT_ZONE);
        } catch (DateTimeParseException dtpx) {
            var exception = new CLIException("Not a valid date of format YYYY-MM-DD", attrName);
            exception.initCause(dtpx);
            throw exception;
        }
    }

}
