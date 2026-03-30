package org.tudo.sse.analyses;

import org.apache.pekko.actor.typed.ActorSystem;
import org.tudo.sse.CLIException;
import org.tudo.sse.analyses.config.LibraryAnalysisConfig;
import org.tudo.sse.model.Artifact;
import org.tudo.sse.model.ArtifactIdent;
import org.tudo.sse.model.LibraryResolutionContext;
import org.tudo.sse.multithreading.WorkItem;
import org.tudo.sse.multithreading.WorkloadIsFinalMessage;
import org.tudo.sse.multithreading.ProcessLibraryMessage;
import org.tudo.sse.multithreading.QueueActor;
import org.tudo.sse.resolution.ResolverFactory;
import org.tudo.sse.resolution.releases.DefaultMavenReleaseListProvider;
import org.tudo.sse.resolution.releases.IReleaseListProvider;
import org.tudo.sse.analyses.config.parsing.LibraryAnalysisConfigParser;
import org.tudo.sse.utils.LibraryIndexIterator;
import org.tudo.sse.utils.MavenCentralRepository;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Base class for analyses that process <b>libraries</b> hosted on Maven Central. For each library, all artifacts (releases)
 * are aggregated and enriched with pom / JAR information as specified by the implementing class.
 */
public abstract class MavenCentralLibraryAnalysis extends MavenCentralAnalysis {

    private final IReleaseListProvider releaseListProvider;
    private final ResolverFactory resolverFactory;

    /**
     * The configuration for this analysis instance. Only available after runAnalysis has been called.
     */
    protected LibraryAnalysisConfig config;

    private ActorSystem<WorkItem> system;
    private long lastPositionSaved;

    /**
     * Creates a new Maven Central Analysis with the given configuration options.
     *
     * @param requiresPom         Whether this analysis requires POM information
     * @param requiresTransitives Whether this analysis requires transitive POM information. If true, "requiresPOM" will
     *                            be set to true no matter its given parameter value.
     * @param requiresJar         Whether this analysis requires JAR information
     */
    protected MavenCentralLibraryAnalysis(boolean requiresPom, boolean requiresTransitives, boolean requiresJar) {
        super(false, requiresPom, requiresTransitives, requiresJar);

        this.releaseListProvider = DefaultMavenReleaseListProvider.getInstance();
        this.resolverFactory = new ResolverFactory(processTransitives);
        this.lastPositionSaved = -1;
    }

    @Override
    public final void runAnalysis(String[] args){
        // Obtain CLI arguments
        try {
            this.config = new LibraryAnalysisConfigParser().parseCommonConfig(args);
            printRunInfo();
        } catch(CLIException clix){
            throw new RuntimeException(clix);
        }

        long currentPosition = 0L;
        // Get input iterator - this will either be from a configured input file or from the Maven Central Index. The
        // iterator will produce strings of form <GroupID>:<ArtifactID>.
        Iterator<String> gaIterator = getGaIterator();

        // We first start by restoring progress if possible
        if(config.progressRestoreFile != null){
            final long startingPosition = getProgressFromRestoreFile();
            log.info("Restoring previous progress (position {})", startingPosition);
            for(int i = 0; i < startingPosition; i++){
                if(!gaIterator.hasNext()){
                    log.warn("No more GA values available while restoring previous progress");
                    break;
                } else gaIterator.next();
                currentPosition += 1L;
            }
            // Notify users that skip will not be applied
            if(config.hasSkip())
                log.info("Not applying skip value because progress was restored from previous run");
        } else if(config.hasSkip()){
            // We only apply a skip if we did not restore previous progress
            log.info("Skipping {} library names", config.skip);
            for(int i = 0; i < config.skip; i++){
                if(!gaIterator.hasNext()){
                    log.warn("No more GA values available while skipping to desired position");
                    break;
                } else gaIterator.next();
                currentPosition += 1L;
            }
        }

        // If we want to use multiple threads, we initialize the queue actor that managers workers
        if(this.config.multipleThreads){
            this.system = ActorSystem.create(QueueActor.create(config.threadCount, currentPosition, config.progressWriteInterval,
                    config.progressOutputFile), "marin-actors");
        }

        long entriesTaken = 0L;

        if(config.hasTake())
            log.info("Taking {} library names", config.take);

        // Invoke the beforeRunStart lifecycle hook
        try {
            this.beforeRunStart();
        } catch (Exception x){
            log.error("Unexpected exception in analysis lifecycle hook beforeRunStart", x);
            return;
        }

        // If specified, we only take the configured amount of entries. If not, we process as long as the iterator
        // provides new entries
        while((!config.hasTake()|| entriesTaken < config.take) && gaIterator.hasNext()){
            processEntry(gaIterator.next());

            entriesTaken += 1L;
            currentPosition += 1L;

            writeProgressIfNeeded(currentPosition);
        }

        // At this point all libraries are queued (multithreaded) or processed (single-threaded)
        if(config.multipleThreads)
            log.info("Queued a total of {} library names for processing", entriesTaken);
        else
            log.info("Processed a total of {} library names", entriesTaken);

        // Close index iterator if it was used
        if(gaIterator instanceof LibraryIndexIterator){
            LibraryIndexIterator libraryIndexIterator = (LibraryIndexIterator)gaIterator;
            try {
                libraryIndexIterator.close();
            } catch (Exception x) { log.warn("Error closing index iterator: {}", x.getMessage());}
        }

        // Close actor system if it was used
        if(this.system != null){
            try {
                // Tell the queue that no more work items will follow
                this.system.tell(WorkloadIsFinalMessage.getInstance());
                this.system.getWhenTerminated().toCompletableFuture().get();
                this.system.close();
            } catch (Exception x) { log.warn("Error closing actor system: {}", x.getMessage());}
        }

        // Invoke the afterRunEnd lifecycle hook
        try {
            this.afterRunEnd();
        } catch(Exception x){
            log.error("Unexpected exception in analysis lifecycle hook afterRunEnd", x);
        }
    }

    /**
     * Analysis lifecycle hook that is executed before a library is being processed, i.e., before any releases are
     * discovered.
     * <p>
     * <b>Note:</b>This method may be called concurrently by multiple threads if the analysis uses parallel execution
     * </p>
     *
     *
     * @param groupId The library's group ID
     * @param artifactId The library's artifact ID
     */
    protected void beforeLibraryStart(String groupId, String artifactId) {
        log.debug("Start processing library {}:{}", groupId, artifactId);
    }

    /**
     * Analysis lifecycle hook that is executed after a library has been processed, i.e., after all releases have been
     * discovered and the analysis implementation has been executed.
     * <p>
     * <b>Note:</b>This method may be called concurrently by multiple threads if the analysis uses parallel execution
     * </p>
     *
     * @param groupId The library's group ID
     * @param artifactId The library's artifact ID
     * @param artifacts The set of artifacts for the given library
     */
    protected void afterLibraryEnd(String groupId, String artifactId, List<Artifact> artifacts) {
        log.debug("Finished processing {} artifacts for library {}:{}", artifacts.size(), groupId, artifactId);
    }

    /**
     * Analysis lifecycle hook that is executed when the analysis failed to obtain a version list for a given library.
     *
     * <p>
     * <b>Note:</b>This method may be called concurrently by multiple threads if the analysis uses parallel execution
     * </p>
     *
     * @param groupId The library's group ID
     * @param artifactId The library's artifact ID
     * @param cause The exception that caused the failure
     */
    protected void onVersionListError(String groupId, String artifactId, Exception cause){}

    /**
     * Main analysis implementation. Defines how a single library shall be analyzed. All artifacts for a given
     * library will have information annotated corresponding to the protected attributes' values.
     * @param libraryGA The library GA tuple (i.e., the library name)
     * @param releases A list of artifacts as ordered by Maven Central's metadata.xml file
     */
    protected abstract void analyzeLibrary(String libraryGA, List<Artifact> releases);

    private void processEntry(String ga){
        final String[] parts = ga.split(":");
        final LibraryResolutionContext resolutionCtx = LibraryResolutionContext.newInstance(ga);

        if(parts.length != 2){
            log.warn("Not a valid GA tuple (need <groupID>:<artifactID>): {}", ga);
            return;
        }

        final String groupId = parts[0];
        final String artifactId = parts[1];

        if(!config.multipleThreads){
            process(groupId, artifactId, resolutionCtx);
        } else {
            final ProcessLibraryMessage msg = new ProcessLibraryMessage(() -> process(groupId, artifactId, resolutionCtx));
            this.system.tell(msg);
        }
    }

    private Void process(String groupId, String artifactId, LibraryResolutionContext resolutionCtx){

        try { this.beforeLibraryStart(groupId, artifactId); }
        catch(Exception x){
            log.error("Unexpected exception in analysis lifecycle hook beforeLibraryStart for library {}, {}", groupId, artifactId, x);
        }

        List<ArtifactIdent> identifiers = getReleaseIdentifiers(groupId, artifactId);

        // Abort if we find no releases, error has already been logged at this point
        if(identifiers == null) return null;

        // Resolve all information for all library releases as defined by this analysis
        for(ArtifactIdent ident : identifiers){
            Artifact a = resolutionCtx.createArtifact(ident);
            resolveDataAsNeeded(ident, resolutionCtx);
            resolutionCtx.addLibraryArtifact(a);
        }

        try {
            // Call the custom analysis implementation
            analyzeLibrary(resolutionCtx.getLibraryGA(), resolutionCtx.getLibraryArtifacts());
        } catch(Exception x){
            // Whatever the user-defined code does, we do not want to abort!
            log.error("Unknown error in analysis implementation", x);
        }

        try { this.afterLibraryEnd(groupId, artifactId, resolutionCtx.getLibraryArtifacts()); }
        catch(Exception x){
            log.error("Unexpected exception in analysis lifecycle hook afterLibraryEnd for library {}, {}", groupId, artifactId, x);
        }

        return null;
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
            log.warn("Failed to obtain version list for library {}:{} ({})", groupId, artifactId, x.getCause().getMessage());

            try { this.onVersionListError(groupId, artifactId, x); }
            catch(Exception inner) {
                log.warn("Unexcepted exception in analysis lifecycle hook onVersionListError for library {}:{}", groupId, artifactId, x);
            }

            return null;
        }
    }

    private Iterator<String> getGaIterator(){
        if(config.inputListFile != null){
            try {
                final List<String> inputs = Files.readAllLines(config.inputListFile);
                log.info("Read {} GAs from input file {}", inputs.size(), config.inputListFile);
                return inputs.iterator();
            } catch(IOException iox){
                log.error("Failed to read GA tuples from input file at {}", config.inputListFile, iox);
                throw new RuntimeException(iox);
            }
        } else {
            try {
                return new LibraryIndexIterator(new URI(MavenCentralRepository.RepoBasePath));
            } catch (URISyntaxException | IOException iox){
                log.error("Failed to create Maven Central library iterator", iox);
                throw new RuntimeException(iox);
            }

        }
    }

    private void resolveDataAsNeeded(ArtifactIdent identifier, LibraryResolutionContext resolutionCtx){
        if(resolvePom && resolveJar) {
            resolverFactory.runBoth(identifier, resolutionCtx);
        } else if(resolvePom) {
            resolverFactory.runPom(identifier, resolutionCtx);
        } else if(resolveJar) {
            resolverFactory.runJar(identifier, resolutionCtx);
        }
    }

    private void printRunInfo(){
        log.info("Running a Maven Central Library Analysis Implementation:");
        if(resolveIndex) log.info      ("\t - The analysis requires index information");
        if(resolvePom) log.info        ("\t - The analysis requires pom information");
        if(processTransitives) log.info("\t - The analysis requires transitive pom dependencies");
        if(resolveJar)log.info         ("\t - The analysis requires jar information");

        log.info("The current run has been configured as follows:");

        if(config.multipleThreads){
            log.info("\t - Using " + config.threadCount + " threads");
        } else {
            log.info("\t - Using one thread");
        }

        if(config.inputListFile == null){
            log.info("\t - Reading libraries from Maven Central index");
            if(config.progressRestoreFile != null) log.info("\t - Restoring last index position from " + config.progressRestoreFile);
            if(config.progressOutputFile != null)  log.info("\t - Writing last index position to " + config.progressOutputFile);
            if(config.hasSkip())                   log.info("\t - Skipping " + config.skip + " artifacts");
            if(config.hasTake())                   log.info("\t - Taking " + config.take + " artifacts");
        } else {
            log.info("\t - Reading libraries from GA-list at " + config.inputListFile);
        }
    }

    private long getProgressFromRestoreFile() {
        BufferedReader indexReader;
        try {
            indexReader = new BufferedReader(new FileReader(config.progressRestoreFile.toFile()));
            String line = indexReader.readLine();
            return Integer.parseInt(line);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void writeProgressIfNeeded(long currentPosition){
        if(!this.config.multipleThreads){
            if(currentPosition - this.lastPositionSaved > this.config.progressWriteInterval){
                try(BufferedWriter writer = new BufferedWriter(new FileWriter(this.config.progressOutputFile.toFile()))){
                    // Write position of last GA, as it has now been completed!
                    writer.write(String.valueOf(currentPosition));
                } catch(IOException ignored){}
                this.lastPositionSaved = currentPosition;
            }
        }
    }

}
