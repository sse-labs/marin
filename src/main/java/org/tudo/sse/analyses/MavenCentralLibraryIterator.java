package org.tudo.sse.analyses;

import org.tudo.sse.analyses.config.LibraryAnalysisConfig;
import org.tudo.sse.analyses.input.FileBasedLibraryIterator;
import org.tudo.sse.model.Artifact;
import org.tudo.sse.model.ArtifactIdent;
import org.tudo.sse.model.LibraryResolutionContext;
import org.tudo.sse.resolution.releases.DefaultMavenReleaseListProvider;
import org.tudo.sse.resolution.releases.IReleaseListProvider;
import org.tudo.sse.utils.LibraryIndexIterator;
import org.tudo.sse.utils.MavenCentralRepository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

/**
 * Iterator that produces libraries that are enriched with index-, pom- or jar information, as configured. This is the
 * iterator equivalent to the {@link MavenCentralLibraryAnalysis}. Only supports single-threaded resolution.
 *
 * @author Johannes Düsing
 */
public final class MavenCentralLibraryIterator extends AbstractEntityIterator<String, LibraryResolutionContext> {

    private final IReleaseListProvider releaseListProvider;
    private Consumer<FailedLibraryEvent> failedLibraryCallback = null;

    /**
     * Creates a new iterator instance with the given configuration values.
     * @param resolvePom Whether information on artifact pom files shall be resolved
     * @param resolveTransitivePoms Whether transitive poms should also be resolved
     * @param resolveJar Whether information on jar files shall be resolved
     * @param config The analysis configuration, see {@link org.tudo.sse.analyses.config.LibraryAnalysisConfigBuilder}
     */
    public MavenCentralLibraryIterator(boolean resolvePom,
                                       boolean resolveTransitivePoms,
                                       boolean resolveJar,
                                       LibraryAnalysisConfig config) {
        super(resolvePom, resolveTransitivePoms, resolveJar, config);

        if(this.baseConfig.multipleThreads){
            log.warn("Library iterator does no support multiple threads - using a single thread for resolution");
        }

        this.releaseListProvider = DefaultMavenReleaseListProvider.getInstance();
    }

    @Override
    protected LibraryResolutionContext buildEntity(String ga){
        final String[] gaParts = ga.split(":"); // Valid tuple, ensured by source
        final LibraryResolutionContext ctx = LibraryResolutionContext.newInstance(ga);

        // Get list of all library releases
        final List<ArtifactIdent> allReleases = getReleaseIdentifiers(gaParts[0], gaParts[1]);

        if(allReleases != null){
            // For each release, build artifact and enrich with data
            for(ArtifactIdent release : allReleases){
                final Artifact artifact = ctx.createArtifact(release);
                ctx.addLibraryArtifact(artifact);

                this.enrichArtifact(artifact, ctx);
            }
        } else {
            // If we failed to obtain a release list, the respective callback has been invoked. Here, we just return an
            // empty context.
            log.warn("No releases for library {}, returning empty context", ga);
        }
        return ctx;
    }

    /**
     * Sets the callback that is invoked when obtaining a release list for a given library fails.
     * @param failedLibraryCallback Callback on failed libraries
     */
    public void setFailedLibraryCallback(Consumer<FailedLibraryEvent> failedLibraryCallback){
        this.failedLibraryCallback = failedLibraryCallback;
    }

    private List<ArtifactIdent> getReleaseIdentifiers(String groupId, String artifactId){
        try {
            final List<String> libraryVersions = releaseListProvider.getReleases(groupId, artifactId);
            List<ArtifactIdent> identifiers = new ArrayList<>();
            for(String libraryVersion : libraryVersions){
                identifiers.add(new ArtifactIdent(groupId, artifactId, libraryVersion));
            }
            return identifiers;
        } catch (Exception x) {
            log.warn("Failed to obtain version list for library {}:{}", groupId, artifactId, x);

            if(this.failedLibraryCallback != null){
                final var event = new FailedLibraryEvent(groupId, artifactId, x);
                try { this.failedLibraryCallback.accept(event); } catch (Exception ex) {
                    log.warn("Unexcepted exception in failed analysis callback for library {}:{}", groupId, artifactId, ex);
                }
            }

            return null;
        }
    }

    @Override
    protected Iterator<String> buildSource(){
        if(this.baseConfig.hasInputList()){
            try {
                var iterator = new FileBasedLibraryIterator(this.baseConfig.inputListFile);
                iterator.validateInput();
                return iterator;
            } catch (IOException | IllegalArgumentException x){
                log.error("The given input list file is invalid", x);
                this.badSource = true;
            }
        } else {
            try {
                return new LibraryIndexIterator(MavenCentralRepository.RepoBaseURI);
            } catch (IOException iox){
                log.error("Failed to access Maven Central Index", iox);
                this.badSource = true;
            }
        }
        return null;
    }

    /**
     * Event that is thrown when obtaining a library's release list fails.
     */
    public static class FailedLibraryEvent {

        private final String groupId;
        private final String artifactId;
        private final Throwable cause;

        /**
         * Creates a new event instance.
         * @param groupId The library group id
         * @param artifactId The library artifact id
         * @param cause The throwable that caused the failure
         */
        FailedLibraryEvent(String groupId, String artifactId, Throwable cause){
            this.groupId = groupId;
            this.artifactId = artifactId;
            this.cause = cause;
        }

        /**
         * Get the failed library's groupID
         * @return Maven groupID
         */
        public String getGroupId() {
            return this.groupId;
        }

        /**
         * Get the failed library's artifactID
         * @return Maven artifactID
         */
        public String getArtifactId() {
            return this.artifactId;
        }

        /**
         * Get the failure cause
         * @return Throwable that caused the failure
         */
        public Throwable getCause() {
            return this.cause;
        }
    }

}
