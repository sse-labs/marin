package org.tudo.sse.analyses.config;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZonedDateTime;

/**
 * Configuration builder to obtain {@link ArtifactAnalysisConfig} instances programmatically.
 *
 * @author Johannes Düsing
 */
public final class ArtifactAnalysisConfigBuilder extends LibraryAnalysisConfigBuilder {

    private final ArtifactAnalysisConfig theConfig;

    private ArtifactAnalysisConfigBuilder(ArtifactAnalysisConfig theConfig) {
        super(theConfig);

        this.theConfig = theConfig;
    }

    /**
     * Creates a new builder with default configuration values.
     */
    public ArtifactAnalysisConfigBuilder(){
        this(new ArtifactAnalysisConfig());
    }

    @Override
    public ArtifactAnalysisConfigBuilder withSkip(long toSkip) throws InvalidConfigurationException {
        if(this.theConfig.since != ArtifactAnalysisConfig.DEFAULT_VALUE_SINCE ||
                this.theConfig.until != ArtifactAnalysisConfig.DEFAULT_VALUE_UNTIL)
            throw new InvalidConfigurationException("skip", "Cannot apply skip when time-based filtering is used");

        super.withSkip(toSkip);
        return this;
    }

    @Override
    public ArtifactAnalysisConfigBuilder withTake(long toTake) throws InvalidConfigurationException {
        if(this.theConfig.since != ArtifactAnalysisConfig.DEFAULT_VALUE_SINCE ||
                this.theConfig.until != ArtifactAnalysisConfig.DEFAULT_VALUE_UNTIL)
            throw new InvalidConfigurationException("take", "Cannot apply take when time-based filtering is used");

        super.withTake(toTake);
        return this;
    }

    @Override
    public ArtifactAnalysisConfigBuilder withInputList(Path inputList) throws InvalidConfigurationException {
        if(this.theConfig.since != ArtifactAnalysisConfig.DEFAULT_VALUE_SINCE ||
            this.theConfig.until != ArtifactAnalysisConfig.DEFAULT_VALUE_UNTIL)
            throw new InvalidConfigurationException("inputs", "Cannot use input list when time-based filtering is applied");

        super.withInputList(inputList);
        return this;
    }

    @Override
    public ArtifactAnalysisConfigBuilder withProgressOutputFile(Path outputFile) throws InvalidConfigurationException {
        super.withProgressOutputFile(outputFile);
        return this;
    }

    @Override
    public ArtifactAnalysisConfigBuilder withProgressRestoreFile(Path restoreFile) throws InvalidConfigurationException {
        super.withProgressRestoreFile(restoreFile);
        return this;
    }

    @Override
    public ArtifactAnalysisConfigBuilder withNumberOfThreads(int numThreads) throws InvalidConfigurationException {
        super.withNumberOfThreads(numThreads);
        return this;
    }

    @Override
    public ArtifactAnalysisConfigBuilder withProgressWriteInterval(int progressWriteInterval) throws InvalidConfigurationException {
        super.withProgressWriteInterval(progressWriteInterval);
        return this;
    }

    /**
     * Sets the output directory to store processed artifacts in. If your analysis requires IndexInformation only, it
     * will output a list of GAV triples. If pom information is required, one pom.xml file per artifact will be stored.
     * If JAR information is required, the artifact's JAR will be stored in this directory.
     *
     * @param outDir The output directory to use. Must be a valid reference to an existing directory.
     * @return The current configuration builder instance
     * @throws InvalidConfigurationException If the given configuration value is not valid
     */
    public ArtifactAnalysisConfigBuilder withOutputDirectory(Path outDir) throws InvalidConfigurationException {
        if(outDir == null || !Files.isDirectory(outDir))
            throw new InvalidConfigurationException("output", "Output directory must be a valid directory reference");

        if(this.theConfig.outputDirectory != ArtifactAnalysisConfig.DEFAULT_VALUE_OUTPUT)
            log.warn("Value 'output' is set multiple times, previous value overridden.");

        this.theConfig.outputDirectory = outDir;
        this.theConfig.outputEnabled = true;
        return this;
    }

    /**
     * Sets a time-based range to filter artifacts for. The resulting analysis will only process artifacts that have
     * been released after the timestamp given in 'since', and before 'until'.
     *
     * @param since DateTime marking the lower limit of the range
     * @param until DateTime marking the upper limit of the range
     * @return The current configuration builder instance
     * @throws InvalidConfigurationException If the given configuration values are not valid
     */
    public ArtifactAnalysisConfigBuilder withSinceUntil(ZonedDateTime since, ZonedDateTime until) throws InvalidConfigurationException {
        return this.withSinceUtil(since.toInstant().toEpochMilli(), until.toInstant().toEpochMilli());
    }

    /**
     * Sets a time-based range to filter artifacts for. The resulting analysis will only process artifacts that have
     * been released after the timestamp given in 'since', and before 'until'.
     *
     * @param since UNIX timestamp in milliseconds marking the lower limit of the range
     * @param until UNIX timestamp in milliseconds marking the upper limit of the range
     * @return The current configuration builder instance
     * @throws InvalidConfigurationException If the given configuration values are not valid
     */
    public ArtifactAnalysisConfigBuilder withSinceUtil(long since, long until) throws InvalidConfigurationException {
        if(since <= 0L || until <= since)
            throw new InvalidConfigurationException("since-until", "Since and Until must define a valid range");

        if(this.theConfig.skip != LibraryAnalysisConfig.DEFAULT_VALUE_SKIP ||
            this.theConfig.take != LibraryAnalysisConfig.DEFAULT_VALUE_TAKE)
            throw new InvalidConfigurationException("since-until", "Cannot apply time-based filtering when pagination is used");

        if(this.theConfig.inputListFile != LibraryAnalysisConfig.DEFAULT_VALUE_INPUT_LIST)
            throw new InvalidConfigurationException("since-until", "Cannot apply time-based filtering when input list is used");

        if(this.theConfig.since != ArtifactAnalysisConfig.DEFAULT_VALUE_SINCE ||
            this.theConfig.until != ArtifactAnalysisConfig.DEFAULT_VALUE_UNTIL)
            log.warn("Values 'since' and 'until' are set multiple times, previous values overridden.");

        this.theConfig.since = since;
        this.theConfig.until = until;
        return this;
    }

    @Override
    public ArtifactAnalysisConfig build(){
        return this.theConfig;
    }
}
