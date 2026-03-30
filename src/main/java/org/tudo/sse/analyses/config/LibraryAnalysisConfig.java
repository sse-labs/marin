package org.tudo.sse.analyses.config;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Class representing the configuration of a {@link org.tudo.sse.analyses.MavenCentralLibraryAnalysis}.
 *
 * @author Johannes Düsing
 */
public class LibraryAnalysisConfig {

    static final long DEFAULT_VALUE_SKIP = -1L;
    static final long DEFAULT_VALUE_TAKE = -1L;
    static final Path DEFAULT_VALUE_INPUT_LIST = null;
    static final Path DEFAULT_VALUE_PROGRESS_OUTPUT_FILE = Paths.get("marin-progress");
    static final Path DEFAULT_VALUE_PROGRESS_RESTORE_FILE = null;
    static final int DEFAULT_NUM_THREADS = 1;
    static final int DEFAULT_PROGRESS_WRITE_INTERVAL = 100;

    /**
     * Number of artifacts to skip from the underlying source, or -1 if disabled.
     */
    public long skip;

    /**
     * Number of artifacts to take from the underlying source, or -1 if no limit shall be applied.
     */
    public long take;

    /**
     * Path to read inputs from, instead of accessing the Maven Central index. Null if the index shall be used.
     */
    public Path inputListFile;

    /**
     * File path to output analysis progress to.
     */
    public Path progressOutputFile;

    /**
     * Path to read previous progress from, in order to restore it. Null if disabled.
     */
    public Path progressRestoreFile;

    /**
     * True if multiple threads shall be used, false for single-threaded analysis.
     */
    public boolean multipleThreads;

    /**
     * Number of threads to use.
     */
    public int threadCount;

    /**
     * Interval of entities after which progress shall be saved. Defaults to 100.
     */
    public int progressWriteInterval;

    /**
     * Creates a new configuration with default values.
     */
    LibraryAnalysisConfig() {
        skip = DEFAULT_VALUE_SKIP;
        take = DEFAULT_VALUE_TAKE;
        inputListFile = DEFAULT_VALUE_INPUT_LIST;
        progressOutputFile = DEFAULT_VALUE_PROGRESS_OUTPUT_FILE;
        progressRestoreFile = DEFAULT_VALUE_PROGRESS_RESTORE_FILE;
        multipleThreads = DEFAULT_NUM_THREADS == 1 ? false : true;
        threadCount = DEFAULT_NUM_THREADS;
        progressWriteInterval = DEFAULT_PROGRESS_WRITE_INTERVAL;
    }

    /**
     * Checks whether there is a non-default amount of entities to skip
     * @return True if there are entities to skip
     */
    public boolean hasSkip() {
        return this.skip != DEFAULT_VALUE_SKIP;
    }

    /**
     * Checks whether there is a non-default amount of entities to take
     * @return True if there are limited entities to take
     */
    public boolean hasTake() {
        return this.take != DEFAULT_VALUE_TAKE;
    }

    /**
     * Checks whether there is a custom input list of entities to process.
     * @return True if there is an input list specified
     */
    public boolean hasInputList() {
        return this.inputListFile != DEFAULT_VALUE_INPUT_LIST;
    }
}
