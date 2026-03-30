package org.tudo.sse.analyses.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * Configuration builder to obtain {@link LibraryAnalysisConfig} instances programmatically.
 *
 * @author Johannes Düsing
 */
public class LibraryAnalysisConfigBuilder {

    /**
     * The logger for this instance.
     */
    protected final Logger log = LoggerFactory.getLogger(this.getClass());

    private final LibraryAnalysisConfig theConfig;

    /**
     * Creates a new builder with default configuration values.
     */
    public LibraryAnalysisConfigBuilder() {
        this(new LibraryAnalysisConfig());
    }

    /**
     * Creates a new builder with the given baseline configuration instance.
     *
     * @param theConfig Baseline configuration instance for this builder
     */
    protected LibraryAnalysisConfigBuilder(LibraryAnalysisConfig theConfig) {
        this.theConfig = theConfig;
    }

    /**
     * Sets the amount of entities to skip when processing analysis inputs. Can be used for pagination. Defaults to zero.
     *
     * @param toSkip Amount of entities (artifacts or libraries) to skip
     * @return The current configuration builder instance
     * @throws InvalidConfigurationException If the given configuration value is not valid.
     */
    public LibraryAnalysisConfigBuilder withSkip(long toSkip) throws InvalidConfigurationException {
        if(toSkip < 0)
            throw new InvalidConfigurationException("skip", "Skip must not be negative");

        if(this.theConfig.skip != LibraryAnalysisConfig.DEFAULT_VALUE_SKIP)
            log.warn("Value 'skip' is set multiple times, previous value overridden.");

        this.theConfig.skip = toSkip == 0 ? LibraryAnalysisConfig.DEFAULT_VALUE_SKIP : toSkip;
        return this;
    }

    /**
     * Sets the amount of entities to take when processing analysis inputs. Can be used for pagination. By default, no
     * limit is imposed and the input source is consumed fully.
     *
     * @param toTake Amount of entities (artifacts or libraries) to take
     * @return The current configuration builder instance
     * @throws InvalidConfigurationException If the given configuration value is not valid.
     */
    public LibraryAnalysisConfigBuilder withTake(long toTake) throws InvalidConfigurationException {
        if(toTake <= 0)
            throw new InvalidConfigurationException("take", "Take must be positive");

        if(this.theConfig.take != LibraryAnalysisConfig.DEFAULT_VALUE_TAKE)
            log.warn("Value 'take' is set multiple times, previous value overridden.");

        this.theConfig.take = toTake;
        return this;
    }

    /**
     * Sets the input list of entities to process as analysis inputs. This is either a list of GAV triples (artifacts)
     * or GA tuples (libraries), one entity per line. By default, the Maven Central index is used as the source of inputs.
     *
     * @param listPath Path to input file. Must be an existing regular file, one entity per line is required.
     * @return The current configuration builder instance
     * @throws InvalidConfigurationException If the given configuration value is not valid
     */
    public LibraryAnalysisConfigBuilder withInputList(Path listPath) throws InvalidConfigurationException {
        if(listPath == null || !Files.isRegularFile(listPath))
            throw new InvalidConfigurationException("inputs", "Input list file must be a valid file reference");

        if(this.theConfig.inputListFile != LibraryAnalysisConfig.DEFAULT_VALUE_INPUT_LIST)
            log.warn("Value 'inputs' is set multiple times, previous value overridden.");

        this.theConfig.inputListFile = listPath;
        return this;
    }

    /**
     * Sets the output file to regularly write the analysis progress to. By default, a file called 'marin-progress.txt'
     * is created in the current workdir.
     *
     * @param outputFile Path to the progress output file to create. Existing files will be overridden!
     * @return The current configuration builder instance
     * @throws InvalidConfigurationException If the given configuration value is not valid
     */
    public LibraryAnalysisConfigBuilder withProgressOutputFile(Path outputFile) throws InvalidConfigurationException {
        if(outputFile == null)
            throw new InvalidConfigurationException("progress-output-file", "Output file cannot be null");

        if(this.theConfig.progressOutputFile != LibraryAnalysisConfig.DEFAULT_VALUE_PROGRESS_OUTPUT_FILE)
            log.warn("Value 'progress-output-file' is set multiple times, previous value overridden.");

        this.theConfig.progressOutputFile = outputFile;
        return this;
    }

    /**
     * Sets the file to restore progress from. Should be a progress output file previously created by MARIN. By default,
     * no progress is restored.
     *
     * @param restoreFile Path to the progress restore file. Must be a valid text file.
     * @return The current configuration builder instance
     * @throws InvalidConfigurationException If the given configuration value is not valid
     */
    public LibraryAnalysisConfigBuilder withProgressRestoreFile(Path restoreFile) throws InvalidConfigurationException {

        boolean fileExists = Files.isRegularFile(restoreFile);

        if(!fileExists){
            try {
                log.info("Progress restore file does not yet exists, creating default at {}", restoreFile.toAbsolutePath());
                Files.write(restoreFile, "0".getBytes(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                fileExists = true;
            } catch(IOException iox){
                log.warn("Failed to create restore file", iox);
            }
        }

        if(!fileExists)
            throw new InvalidConfigurationException("progress-restore-file", "Restore file must be a valid file reference");

        if(this.theConfig.progressRestoreFile != LibraryAnalysisConfig.DEFAULT_VALUE_PROGRESS_RESTORE_FILE)
            log.warn("Value 'progress-restore-file' is set multiple times, previous value overridden.");

        this.theConfig.progressRestoreFile = restoreFile;
        return this;
    }

    /**
     * Sets the number of threads to use for analysis. By default, one thread is used.
     * @param numThreads Number of threads to use
     * @return The current configuration builder instance
     * @throws InvalidConfigurationException If the given configuration value is not valid
     */
    public LibraryAnalysisConfigBuilder withNumberOfThreads(int numThreads) throws InvalidConfigurationException {
        if(numThreads <= 0)
            throw new InvalidConfigurationException("threads", "Number of threads must be positive");

        if(this.theConfig.threadCount != LibraryAnalysisConfig.DEFAULT_NUM_THREADS)
            log.warn("Value 'threads' is set multiple times, previous value overridden.");

        this.theConfig.threadCount = numThreads;
        this.theConfig.multipleThreads = numThreads > 1;
        return this;
    }

    /**
     * Sets the amount of entities (artifacts or libraries) after which to write progress to the progress output file.
     * Default is 100.
     * @param progressWriteInterval Amount of entities after which to save progress
     * @return The current configuration builder instance
     * @throws InvalidConfigurationException If the given configuration value is not valid
     */
    public LibraryAnalysisConfigBuilder withProgressWriteInterval(int progressWriteInterval) throws InvalidConfigurationException {
        if(progressWriteInterval <= 0)
            throw new InvalidConfigurationException("save-progress-interval", "Progress write interval must be positive");

        if(this.theConfig.progressWriteInterval != LibraryAnalysisConfig.DEFAULT_PROGRESS_WRITE_INTERVAL)
            log.warn("Value 'progress-write-interval' is set multiple times, previous value overridden.");

        this.theConfig.progressWriteInterval = progressWriteInterval;
        return this;
    }

    /**
     * Builds the configuration as specified by all previous invocations to this instance.
     *
     * @return The configuration object that has been constructed by this builder.
     */
    public LibraryAnalysisConfig build(){
        return this.theConfig;
    }

}
