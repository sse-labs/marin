package org.tudo.sse.analyses.config;

import java.nio.file.Path;

/**
 * Class representing the configuration of a {@link org.tudo.sse.analyses.MavenCentralArtifactAnalysis} instance.
 *
 * @author Johannes Düsing
 */
public class ArtifactAnalysisConfig extends LibraryAnalysisConfig {

    static final long DEFAULT_VALUE_SINCE = -1L;
    static final long DEFAULT_VALUE_UNTIL = -1L;
    static final Path DEFAULT_VALUE_OUTPUT = null;

    /**
     * UNIX timestamp (in milliseconds) before which artifacts shall be excluded from analysis, or -1 if disabled.
     */
    public long since;

    /**
     * UNIX timestamp (in milliseconds) after which artifacts shall be excluded from analysis, or -1 if disabled.
     */
    public long until;

    /**
     * Directory to write file outputs to, or null if disabled.
     */
    public Path outputDirectory;

    /**
     * Whether files shall be written to the output directory.
     */
    public boolean outputEnabled;

    /**
     * Creates a new configuration object with default values.
     */
    ArtifactAnalysisConfig(){
        super();

        since = DEFAULT_VALUE_SINCE;
        until = DEFAULT_VALUE_UNTIL;
        outputEnabled = false;
        outputDirectory = DEFAULT_VALUE_OUTPUT;
    }

    /**
     * Checks whether there is a custom time range to filter artifacts for.
     * @return True if time based filtering is enabled
     */
    public boolean hasTimeBasedFiltering(){
        return this.since != DEFAULT_VALUE_SINCE && this.until != DEFAULT_VALUE_UNTIL;
    }


}
