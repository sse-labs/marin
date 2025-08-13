package org.tudo.sse;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * This class holds the configuration information for the MavenCentralAnalysis class.
 * This allows for easier setup for different run configurations.
 */
public class CliInformation {
    private long skip;
    private long take;
    private long since;
    private long until;
    private Path name;
    private Path toCoordinates;
    private Path toIndexPos;
    private boolean output;
    private Path toOutputDirectory;
    private boolean multi;
    private int threads;
    private int writeProcessedIndexes;

    /**
     * Initializes a new CliInformation object with all parameters set to default values (where appropriate)
     */
    public CliInformation() {
        skip = -1;
        take = -1;
        since = -1;
        until = -1;
        name = Paths.get("lastIndexProcessed");
        writeProcessedIndexes = 1000;
        toCoordinates = null;
        toIndexPos = null;
        toOutputDirectory = null;
        output = false;
        multi = false;
    }

    /**
     * Retrieves the number of artifacts to skip from the index (pagination).
     * @return The number of artifacts to skip
     */
    public long getSkip() {
        return skip;
    }

    /**
     * Sets the number of artifacts to skip from the index (pagination).
     * @param skip The number of artifacts to skip
     */
    public void setSkip(long skip) {
        this.skip = skip;
    }

    /**
     * Retrieves the number of artifacts to process from the index (pagination).
     * @return The number of artifacts to process from the index. A value of -1 indicates no limit.
     */
    public long getTake() {
        return take;
    }

    /**
     * Sets the number of artifacts to process from the index (pagination).
     * @param take The number of artifacts to process from the index. A value of -1 indicates no limit.
     */
    public void setTake(long take) {
        this.take = take;
    }

    /**
     * Retrieves the timestamp to use as a lower bound in terms of release time when processing artifacts.
     * @return The timestamp to use as a lower bound
     */
    public long getSince() {
        return since;
    }

    /**
     * Sets the timestamp to use as a lower bound in terms of release time when processing artifacts.
     * @param since The timestamp to use as a lower bound
     */
    public void setSince(long since) {
        this.since = since;
    }

    /**
     * Retrieves the timestamp to use as an upper bound in terms of release time when processing artifacts.
     * @return The timestamp to use as an upper bound
     */
    public long getUntil() {
        return until;
    }

    /**
     * Sets the timestamp to use as an upper bound in terms of release time when processing artifacts.
     * @param until The timestamp to use as an upper bound
     */
    public void setUntil(long until) {
        this.until = until;
    }

    /**
     * Get the path to the text file containing a list of GAV triples to process.
     * @return Path to input list file
     */
    public Path getToCoordinates() {
        return toCoordinates;
    }

    /**
     * Sets the path to the text file containing a list of GAV triples to process.
     * @param toCoordinates Path to input list file
     */
    public void setToCoordinates(Path toCoordinates) {
        this.toCoordinates = toCoordinates;
    }

    /**
     * Retrieves the path from which to restore progress
     * @return The progress restore path
     */
    public Path getToIndexPos() {
        return toIndexPos;
    }

    /**
     * Sets the path from which to restore progress
     * @param toIndexPos The progress restore path
     */
    public void setToIndexPos(Path toIndexPos) {
        this.toIndexPos = toIndexPos;
    }

    /**
     * Retrieves the file path to write progress information to.
     * @return The progress file path
     */
    public Path getName() {
        return name;
    }

    /**
     * Sets the file path to write progress information to.
     * @param name The progress file path
     */
    public void setName(Path name) {
        this.name = name;
    }

    /**
     * Retrieves whether the application is to be run in multithreaded mode.
     * @return True if multithreading is enabled, false otherwise
     */
    public boolean isMulti() {
        return multi;
    }

    /**
     * Sets whether the application is to be run in multithreaded mode.
     * @param multi True for multithreading, false otherwise
     */
    public void setMulti(boolean multi) {
        this.multi = multi;
    }

    /**
     * Retrieves the number of threads configured for multithreading.
     * @return The number of threads configured
     */
    public int getThreads() {
        return threads;
    }

    /**
     * Sets the number of threads for multithreading
     * @param threads The number of threads to use
     */
    public void setThreads(int threads) {
        this.threads = threads;
    }

    /**
     * Retrieves whether all artifacts processed should be persisted to an output directory.
     * @return True if artifacts are persisted, false otherwise
     */
    public boolean isOutput() {
        return output;
    }

    /**
     * Sets whether all artifacts processed should be persisted to an output directory.
     * @param output True for persisting artifacts, false otherwise
     */
    public void setOutput(boolean output) {
        this.output = output;
    }

    /**
     * Retrieves the path under which to persist artifacts that have been processed.
     * @return The output path
     */
    public Path getToOutputDirectory() {
        return toOutputDirectory;
    }

    /**
     * Sets the path under which to persist artifacts that have been processed.
     * @param toOutputDirectory Path to use for artifact output
     */
    public void setToOutputDirectory(Path toOutputDirectory) {
        this.toOutputDirectory = toOutputDirectory;
    }

    /**
     * Gets the number of artifacts after which to write progress (to the progress file).
     * @return The number of artifacts after which progress is saved
     */
    public int getWriteProcessedIndexes() { return writeProcessedIndexes; }

    /**
     * Sets the number of artifacts after which to write progress to the progress file.
     * @param writeProcessedIndexes The number of artifacts after which progress is saved
     */
    public void setWriteProcessedIndexes(int writeProcessedIndexes) {this.writeProcessedIndexes = writeProcessedIndexes;}
}
