package org.tudo.sse.analyses;

import org.apache.pekko.actor.typed.ActorSystem;
import org.tudo.sse.CLIException;
import org.tudo.sse.analyses.config.ArtifactAnalysisConfig;
import org.tudo.sse.analyses.config.ArtifactAnalysisConfigBuilder;
import org.tudo.sse.model.Artifact;
import org.tudo.sse.model.ArtifactIdent;
import org.tudo.sse.model.index.IndexInformation;
import org.tudo.sse.model.ArtifactResolutionContext;
import org.tudo.sse.model.ResolutionContext;
import org.tudo.sse.multithreading.ProcessIdentifierMessage;
import org.tudo.sse.multithreading.WorkItem;
import org.tudo.sse.multithreading.WorkloadIsFinalMessage;
import org.tudo.sse.resolution.ResolverFactory;
import org.tudo.sse.analyses.config.parsing.ArtifactAnalysisConfigParser;
import org.tudo.sse.analyses.input.FileBasedArtifactIterator;
import org.tudo.sse.utils.IndexIterator;
import org.tudo.sse.multithreading.QueueActor;
import org.tudo.sse.utils.MavenCentralRepository;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;

/**
 * The MavenCentralAnalysis enables analysis of artifacts on the maven central repository for jobs of any size.
 * Takes in cli to be configured to the specific job desired.
 */
public abstract class MavenCentralArtifactAnalysis extends MavenCentralAnalysis {

    private ArtifactAnalysisConfig artifactConfig;
    private ActorSystem<WorkItem> system;
    private ResolverFactory resolverFactory;


    private long currentPosition = 0;
    private long lastPositionSaved = 0;

    /**
     * Creates a new Maven Central Analysis with the given configuration options.
     *
     * @param requiresIndex Whether this analysis requires index information
     * @param requiresPom Whether this analysis requires POM information
     * @param requiresTransitives Whether this analysis requires transitive POM information. If true, "requiresPOM" will
     *                            be set to true no matter its given parameter value.
     * @param requiresJar Whether this analysis requires JAR information
     */
    protected MavenCentralArtifactAnalysis(boolean requiresIndex,
                                           boolean requiresPom,
                                           boolean requiresTransitives,
                                           boolean requiresJar)  {
        super(requiresIndex, requiresPom, requiresTransitives, requiresJar);

        // Initialize with default config, update later
        artifactConfig = new ArtifactAnalysisConfigBuilder().build();
    }


    /**
     * Main analysis implementation. Defines how a single artifact shall be analyzed. Artifacts will have information
     * annotated corresponding to the protected attributes' values.
     * @param current The artifact to analyze
     */
    public abstract void analyzeArtifact(Artifact current);

    /**
     * Returns the CLI configuration for this analysis.
     * @return CLI information for this analysis
     */
    public ArtifactAnalysisConfig getSetupInfo() {
        return this.artifactConfig;
    }

    private void printRunInfo(){
        log.info("Running a Maven Central Analysis Implementation:");
        if(resolveIndex) log.info      ("\t - The analysis requires index information");
        if(resolvePom) log.info        ("\t - The analysis requires pom information");
        if(processTransitives) log.info("\t - The analysis requires transitive pom dependencies");
        if(resolveJar)log.info        ("\t - The analysis requires jar information");

        log.info("The current run has been configured as follows:");

        if(this.artifactConfig.multipleThreads){
            log.info("\t - Using " + this.artifactConfig.threadCount + " threads");
        } else {
            log.info("\t - Using one thread");
        }

        if(this.artifactConfig.inputListFile == null){
            log.info("\t - Reading artifacts from Maven Central index");
            if(this.artifactConfig.progressRestoreFile != null) log.info("\t - Restoring last index position from " + this.artifactConfig.progressRestoreFile);
            if(this.artifactConfig.progressOutputFile != null)       log.info("\t - Writing last index position to " + this.artifactConfig.progressOutputFile);
            if(this.artifactConfig.hasSkip())          log.info("\t - Skipping " + this.artifactConfig.skip + " artifacts");
            if(this.artifactConfig.hasTake())          log.info("\t - Taking " + this.artifactConfig.take + " artifacts");
            if(this.artifactConfig.since >= 0)         log.info("\t - Skipping artifacts before " + this.artifactConfig.since);
            if(this.artifactConfig.until >= 0)         log.info("\t - Taking artifacts until " + this.artifactConfig.until);

        } else {
            log.info("\t - Reading artifacts from GAV-list at " + this.artifactConfig.inputListFile);
        }
    }

    @Override
    public final void runAnalysis(String[] args)  {
        // Obtain CLI arguments
        try {
            this.artifactConfig = new ArtifactAnalysisConfigParser().parseArtifactConfig(args);
            printRunInfo();
        } catch(CLIException clix){
            throw new RuntimeException(clix);
        }

        // Initialize resolver factory
        if(this.artifactConfig.outputEnabled) {
            resolverFactory = new ResolverFactory(this.artifactConfig.outputEnabled, this.artifactConfig.outputDirectory, processTransitives);
        } else {
            resolverFactory = new ResolverFactory(processTransitives);
        }

        if(this.artifactConfig.multipleThreads) {
            system = ActorSystem.create(QueueActor.create(this.artifactConfig.threadCount, getInitialPosition(),
                    artifactConfig.progressWriteInterval, artifactConfig.progressOutputFile), "marin-actors");

        }

        // Invoke the beforeRunStart lifecycle hook
        try {
            this.beforeRunStart();
        } catch (Exception x){
            log.error("Unexpected exception in analysis lifecycle hook beforeRunStart", x);
            return;
        }

        if(this.artifactConfig.inputListFile == null) {
            indexProcessor();
        } else {
            processArtifactsFromInputFile();
        }

        if(system != null){
            try {
                // Tell the queue that no more work items will follow
                system.tell(WorkloadIsFinalMessage.getInstance());
                system.getWhenTerminated().toCompletableFuture().get();
                system.close();
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
     * Handles walking the maven central index, choosing how to do so based on the configuration.
     * @see ArtifactIdent
     */
    public void indexProcessor() {

        try {
            final IndexIterator indexIterator = new IndexIterator(MavenCentralRepository.RepoBaseURI);

            // Skip to starting position
            final long startingPosition = getInitialPosition();
            skipN(startingPosition, indexIterator);

            if(this.artifactConfig.hasTake()){
                walkPaginated(this.artifactConfig.take, indexIterator);
            } else if(this.artifactConfig.since != -1 && this.artifactConfig.until != -1){
                walkDates(this.artifactConfig.since, this.artifactConfig.until, indexIterator);
            } else {
                walkAllIndexes(indexIterator);
            }

            // Write final position only if not in multithreaded mode - otherwise Queue actor handles progress
            if(!artifactConfig.multipleThreads) writePosition();
        } catch (IOException iox){
            throw new RuntimeException(iox);
        }

    }

    private void processIndex(Artifact current, ArtifactResolutionContext ctx) {
        if(this.artifactConfig.multipleThreads) {
            system.tell(new ProcessIdentifierMessage(ctx, this));
        } else {
            callResolver(current.getIdent(), ctx);
            analyzeArtifact(current);
        }
    }

    /**
     * Iterates over all indexes in the maven central index and creates an artifact with the metadata collected.
     *
     * @param indexIterator an iterator for traversing the maven central index
     * @see ArtifactIdent
     * @throws IOException when there is an issue opening a file
     */
    private void walkAllIndexes(IndexIterator indexIterator) throws IOException {
        long entriesTaken = 0L;

        while(indexIterator.hasNext()) {
            final IndexInformation current = indexIterator.next();
            entriesTaken += 1L;
            this.currentPosition += 1L;

            if(this.artifactConfig.outputEnabled && !resolvePom && !resolveJar) {
                Path filePath = this.artifactConfig.outputDirectory.resolve(current.getIdent().getGroupID() + "-" + current.getIdent().getArtifactID() + "-" + current.getIdent().getVersion() + ".txt");
                if(!Files.exists(filePath)) {
                    Files.createFile(filePath);
                }
            }

            final ArtifactResolutionContext ctx = ArtifactResolutionContext.newInstance(current.getIdent());

            if(resolveIndex){
                final Artifact currentArtifact = ctx.createArtifact(current.getIdent());
                currentArtifact.setIndexInformation(current);

                processIndex(currentArtifact, ctx);
            } else {
                processIndexIdentifier(current.getIdent(), ctx);
            }

            writePositionIfNeeded();
        }

        if(artifactConfig.multipleThreads)
            log.info("Queued a total of {} entries for processing.", entriesTaken);
        else
            log.info("Processed a total of {} entries.", entriesTaken);

        indexIterator.closeReader();
    }

    /**
     * Iterates over a given number of indexes from the maven central index, and creates an artifact with the metadata collected.
     *
     * @param take number of artifacts from the starting point to capture
     * @param indexIterator an iterator for traversing the maven central index
     * @see ArtifactIdent
     * @throws IOException when there is an issue opening a file
     */
    void walkPaginated(long take, IndexIterator indexIterator) throws IOException {
        long entriesTaken = 0L;

        while(indexIterator.hasNext() && entriesTaken < take) {
            final IndexInformation current = indexIterator.next();
            entriesTaken += 1;
            this.currentPosition += 1L;

            if(this.artifactConfig.outputEnabled && !resolvePom && !resolveJar) {
                Path filePath = this.artifactConfig.outputDirectory.resolve(current.getIdent().getGroupID() + "-" + current.getIdent().getArtifactID() + "-" + current.getIdent().getVersion() + ".txt");
                if(!Files.exists(filePath)) {
                    Files.createFile(filePath);
                }
            }

            final ArtifactResolutionContext ctx = ArtifactResolutionContext.newInstance(current.getIdent());

            if(resolveIndex){
                final Artifact currentArtifact = ctx.createArtifact(current.getIdent());
                currentArtifact.setIndexInformation(current);

                processIndex(currentArtifact, ctx);
            } else {
                processIndexIdentifier(current.getIdent(), ctx);
            }

            writePositionIfNeeded();
        }

        if(artifactConfig.multipleThreads)
            log.info("Queued a total of {} entries for processing.", entriesTaken);
        else
            log.info("Processed a total of {} entries.", entriesTaken);

        indexIterator.closeReader();
    }

    /**
     * Invokes analysis configuration for all artifact identifiers in the date window defined by given boundaries.
     *
     * @param since lower bound of dates of artifacts to collect
     * @param until upper bound of dates of artifacts to collect
     * @param indexIterator an iterator for traversing the maven central index
     * @see ArtifactIdent
     * @throws IOException when there is an issue opening a file
     */
    void walkDates(long since, long until, IndexIterator indexIterator) throws IOException {
        long entriesTaken = 0L;

        long currentToSince;

        while(indexIterator.hasNext()) {
            IndexInformation current = indexIterator.next();
            entriesTaken += 1L;
            this.currentPosition += 1L;

            currentToSince = current.getLastModified();

            if(currentToSince >= since && currentToSince < until) {
                if(this.artifactConfig.outputEnabled && !resolvePom && !resolveJar) {
                    Path filePath = this.artifactConfig.outputDirectory.resolve(current.getIdent().getGroupID() + "-" + current.getIdent().getArtifactID() + "-" + current.getIdent().getVersion() + ".txt");
                    if(!Files.exists(filePath)) {
                        Files.createFile(filePath);
                    }
                }

                final ArtifactResolutionContext ctx = ArtifactResolutionContext.newInstance(current.getIdent());

                if(resolveIndex){
                    // If we want to retain index information, build an artifact and attach it
                    final Artifact currentArtifact = ctx.createArtifact(current.getIdent());
                    currentArtifact.setIndexInformation(current);

                    processIndex(currentArtifact, ctx);
                } else {
                    processIndexIdentifier(current.getIdent(), ctx);
                }
            }

            writePositionIfNeeded();
        }

        if(artifactConfig.multipleThreads)
            log.info("Queued a total of {} entries for processing.", entriesTaken);
        else
            log.info("Processed a total of {} entries.", entriesTaken);

        indexIterator.closeReader();
    }

    private void processIndexIdentifier(ArtifactIdent ident, ArtifactResolutionContext ctx) {
        if(this.artifactConfig.multipleThreads){
            system.tell(new ProcessIdentifierMessage(ctx, this));
        } else {
            callResolver(ident, ctx);
            final Artifact artifact = ctx.getArtifact(ident);
            if(artifact != null) {
                analyzeArtifact(artifact);
            }
        }
    }

    /**
     * Reads in identifiers from a file, using the configuration passed into it.
     */
    void processArtifactsFromInputFile() {
        final Iterator<ArtifactIdent> fileIterator = new FileBasedArtifactIterator(this.artifactConfig.inputListFile);

        // Restore from progress file if available
        if(this.artifactConfig.progressRestoreFile != null){
            long lastProgress = getStartingPos();
            log.info("Restoring previous progress from file (position {})", lastProgress);

            this.skipN(lastProgress, fileIterator);
        } else if(this.artifactConfig.hasSkip()){
            // Skip configured values only if we did not restore from progress file
            log.info("Skipping {} entries from file", artifactConfig.skip);
            skipN(this.artifactConfig.skip, fileIterator);
        }

        // Warn if after skipping there are no entries left
        if(!fileIterator.hasNext())
            log.warn("No more contents left to process in input file: {}", this.artifactConfig.inputListFile);

        long entriesTaken = 0L;
        while ((!this.artifactConfig.hasTake() || entriesTaken < this.artifactConfig.take) && fileIterator.hasNext()) {

            ArtifactIdent current = null;

            try { current = fileIterator.next(); } catch (Exception x){
                log.error("Malformed GAV triple in input file {} line {}", this.artifactConfig.inputListFile,
                        this.currentPosition);
            }

            this.currentPosition += 1L;
            entriesTaken += 1L;

            if(current != null){
                final ArtifactResolutionContext resolutionCtx = ArtifactResolutionContext.newInstance(current);

                processIndexIdentifier(current, resolutionCtx);
            }
        }

        if(artifactConfig.multipleThreads){
            log.info("Queued a total of {} entries for processing from file {}.", entriesTaken, this.artifactConfig.inputListFile);
        } else {
            log.info("Processed a total of {} entries from file {}.", entriesTaken, this.artifactConfig.inputListFile);

            // Write position one final time - in multithreaded mode the queue worker will take care of this
            writePosition();
        }
    }

    /**
     * Invokes all resolvers as defined by the analysis configuration to enrich the given artifact identifier.
     * @param identifier Artifact identifier to enrich
     * @param ctx The current resolution context
     */
    public void callResolver(ArtifactIdent identifier, ResolutionContext ctx) {
        if(resolvePom && resolveJar) {
            resolverFactory.runBoth(identifier, ctx);
        } else if(resolvePom) {
            resolverFactory.runPom(identifier, ctx);
        } else if(resolveJar) {
            resolverFactory.runJar(identifier, ctx);
        } else {
            // If no sources are configured, we still want to have an "empty" artifact so it can be returned later
            ctx.createArtifact(identifier);
        }
    }

    private long getInitialPosition() {
        if(artifactConfig.progressRestoreFile != null) return getStartingPos();
        else if(artifactConfig.hasSkip()) return artifactConfig.skip;
        else return 0L;
    }

    private long getStartingPos() {
        BufferedReader indexReader;
        try {
            indexReader = new BufferedReader(new FileReader(this.artifactConfig.progressRestoreFile.toFile()));
            String line = indexReader.readLine();
            return Integer.parseInt(line);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void skipN(long N, Iterator<?> it){
        for(int i = 0; i < N && it.hasNext(); i++){
            it.next();
            this.currentPosition += 1L;
        }
    }

    private void writePositionIfNeeded(){
        if(!this.artifactConfig.multipleThreads &&
                this.currentPosition - this.lastPositionSaved > artifactConfig.progressWriteInterval){
            writePosition();
        }
    }

    private void writePosition() {
        try(BufferedWriter writer = Files.newBufferedWriter(this.artifactConfig.progressOutputFile)) {
            writer.write(Long.toString(this.currentPosition));
        } catch(IOException ignored) {}

        this.lastPositionSaved = currentPosition;
    }
}
