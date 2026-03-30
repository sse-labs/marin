package org.tudo.sse.analyses.config.parsing;

import org.tudo.sse.CLIException;
import org.tudo.sse.analyses.config.InvalidConfigurationException;
import org.tudo.sse.analyses.config.LibraryAnalysisConfig;
import org.tudo.sse.analyses.config.LibraryAnalysisConfigBuilder;

/**
 * Parser for {@link LibraryAnalysisConfig} objects, i.e. configurations of the {@link org.tudo.sse.analyses.MavenCentralLibraryAnalysis}.
 * Configuration is obtained by parsing command line arguments provided as an array of strings.
 */
public class LibraryAnalysisConfigParser implements CLIParsingUtilities {

    /**
     * Creates a new library config parsers instance.
     */
    public LibraryAnalysisConfigParser() { super(); }

    /**
     * Parses a given array containing JVM command line arguments into an {@link LibraryAnalysisConfig} objects. Throws an exception
     * if the given arguments are not valid.
     * @param args Array of command line arguments, as passed by the JVM
     * @return Corresponding {@link LibraryAnalysisConfig} if parsing was successful
     * @throws CLIException If parsing failed. Contains details about the specific argument that was invalid.
     */
    public LibraryAnalysisConfig parseCommonConfig(String[] args) throws CLIException {
        final LibraryAnalysisConfigBuilder configBuilder = new LibraryAnalysisConfigBuilder();


        for(int i = 0; i < args.length; i += 2){
            handleParameter(args, i, configBuilder);
        }

        return configBuilder.build();
    }

    /**
     * Parses a given parameter from the array of CLI arguments and applies it to the config object.
     * @param args CLI arguments array
     * @param i Current parsing position
     * @param configBuilder Current configuration object
     * @throws CLIException If parsing the current argument fails.
     */
    protected void handleParameter(String[] args, int i, LibraryAnalysisConfigBuilder configBuilder) throws CLIException {
        try {
            switch (args[i]){
                case "-st":
                case "--skip-take":
                    final long[] skipTake = nextArgAsLongPair(args, i);
                    configBuilder
                            .withSkip(skipTake[0])
                            .withTake(skipTake[1]);
                    break;
                case "-prf":
                case "--progress-restore-file":
                    configBuilder.withProgressRestoreFile(nextArgAsPath(args, i));
                    break;
                case "-spi":
                case "--save-progress-interval":
                    configBuilder.withProgressWriteInterval(nextArgAsInt(args, i));
                    break;
                case "-pof":
                case "--progress-output-file":
                    configBuilder.withProgressOutputFile(nextArgAsPath(args, i));
                    break;
                case "-i":
                case "--inputs":
                    configBuilder.withInputList(nextArgAsRegularFileReference(args, i));
                    break;
                case "-t":
                case "--threads":
                    final int threads = nextArgAsInt(args, i);
                    configBuilder.withNumberOfThreads(threads);
                    break;
                default:
                    throw new CLIException(args[i]);
            }
        } catch (InvalidConfigurationException icx) {
            CLIException wrapped = new CLIException(icx.getMessage(), icx.getAttributeName());
            wrapped.initCause(icx);
            throw wrapped;
        }
    }


}
