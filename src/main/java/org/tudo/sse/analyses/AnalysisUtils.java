package org.tudo.sse.analyses;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tudo.sse.analyses.config.LibraryAnalysisConfig;
import org.tudo.sse.analyses.config.LibraryAnalysisConfigBuilder;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Iterator;

/**
 * Utility methods for general analysis implementations.
 *
 * @author Johannes Düsing
 */
final class AnalysisUtils {

    private static final Logger log = LoggerFactory.getLogger(AnalysisUtils.class);

    /**
     * Skips the correct amount of entities for the given iterator, based on the given configuration. The number of
     * entities that are skipped are returned by this method.
     * @param it The iterator to skip on
     * @param config The current configuration
     * @return The amount of entities that have been skipped
     */
    static long skipInitial(Iterator<?> it, LibraryAnalysisConfig config) {
        long entitiesSkipped = 0L;

        final long entitiesToSkip = AnalysisUtils.getInitialPosition(config);

        for(int i = 0; i < entitiesToSkip && it.hasNext(); i++){
            it.next();
            entitiesSkipped += 1L;
        }

        log.debug("Successfully skipped to position {}", entitiesSkipped);

        if(!it.hasNext()){
            log.warn("Reached end of input source while skipping to position {}", entitiesToSkip);
        }

        return entitiesSkipped;
    }

    /**
     * Retrieves the initial position to start analysis from, based on the current configuration.
     * @param config The current analysis configuration
     *
     * @return Initial number of entities to skip before starting
     */
    static long getInitialPosition(LibraryAnalysisConfig config) {
        if(config.progressRestoreFile != null) return getRestoreProgressValue(config);
        else if(config.hasSkip()) return config.skip;
        else return 0L;
    }

    /**
     * Retrieves the last progress value to start from based on a previous run.
     * @param config The current analysis configuration
     *
     * @return Progress value
     */
    static long getRestoreProgressValue(LibraryAnalysisConfig config) {
        BufferedReader indexReader;
        try {
            indexReader = new BufferedReader(new FileReader(config.progressRestoreFile.toFile()));
            String line = indexReader.readLine();
            return Integer.parseInt(line);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Writes the current position to the specified progress output file.
     * @param currentPosition The current progress to write
     * @param config The current analysis configuration
     */
    static void writePosition(long currentPosition, LibraryAnalysisConfig config) {
        try(BufferedWriter writer = Files.newBufferedWriter(config.progressOutputFile)) {
            writer.write(Long.toString(currentPosition));
        } catch(IOException ignored) {}
    }
}
