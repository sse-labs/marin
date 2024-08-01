package org.tudo.sse;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.tudo.sse.model.Artifact;
import org.tudo.sse.model.ArtifactIdent;
import org.tudo.sse.model.index.IndexInformation;
import org.tudo.sse.multithreading.IdentPlusMCA;
import org.tudo.sse.multithreading.IndexProcessingMessage;
import org.tudo.sse.resolution.ResolverFactory;
import org.tudo.sse.utils.IndexIterator;
import org.tudo.sse.multithreading.QueueActor;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The MavenCentralAnalysis enables analysis of artifacts on the maven central repository for jobs of any size.
 * Takes in cli to be configured to the specific job desired.
 *
 */
public abstract class MavenCentralAnalysis {
    private final CliInformation setupInfo;
    private ActorRef queueActorRef;
    private ResolverFactory resolverFactory;
    protected boolean resolveIndex;
    protected boolean resolvePom;
    protected boolean processTransitives;
    protected boolean resolveJar;


    public static final Logger log = LogManager.getLogger(MavenCentralAnalysis.class);

    public MavenCentralAnalysis()  {
        setupInfo = new CliInformation();
        resolveIndex = false;
        resolvePom = false;
        processTransitives = false;
        resolveJar = false;
    }


    public abstract void analyzeArtifact(Artifact current);

    /**
     * This method handles parsing the command line arguments and stores it into a CliInformation object.
     *
     * @param args - cli to be parsed
     * @see CliInformation when there is an issue processing the cli passed to the program
     */
    public void parseCmdLine(String[] args) {
        boolean flagSet1 = false;
        boolean flagSet2 = false;
        boolean flagSet3 = false;
        try {
            for (int i = 0; i < args.length; i += 2) {
                switch (args[i]) {
                    case "-st":
                        checkConflict(flagSet1, args[i]);
                        long[] parsed = parseLong(args, i);
                        setupInfo.setSkip(parsed[0]);
                        setupInfo.setTake(parsed[1]);
                        flagSet1 = true;
                        break;
                    case "-su":
                        checkTwoConflicts(flagSet1, flagSet2, args[i]);
                        long[] parsed1 = parseLong(args, i);
                        setupInfo.setSince(parsed1[0]);
                        setupInfo.setUntil(parsed1[1]);
                        flagSet1 = true;
                        flagSet2 = true;
                        break;
                    case "-ip":
                        checkConflict(flagSet1, args[i]);
                        setupInfo.setToIndexPos(parsePathName(args, i));
                        flagSet1 = true;
                        break;
                    case "--coordinates":
                        checkConflict(flagSet2, args[i]);
                        checkConflict(flagSet3, args[i]);
                        setupInfo.setToCoordinates(parsePathName(args, i));
                        flagSet2 = true;
                        flagSet3 = true;
                        break;
                    case "--name":
                        if(i + 1 < args.length) {
                            setupInfo.setName(parsePathName(args, i));
                        }
                        break;
                    case "--multi":
                        setupInfo.setMulti(true);
                        setupInfo.setThreads(parseInt(args, i));
                        break;
                    case "--output":
                        setupInfo.setOutput(true);
                        setupInfo.setToOutputDirectory(parsePathName(args, i));
                        break;
                    default:
                        throw new CLIException(args[i]);
                }
            }
        } catch(CLIException e) {
            throw new RuntimeException(e);
        }
    }

    public CliInformation getSetupInfo() {
        return setupInfo;
    }

    private void checkTwoConflicts(boolean checkConflict1, boolean checkConflict2, String flag) throws CLIException {
        if(checkConflict1 || checkConflict2) {
            throw new CLIException("Other flag already set.", flag);
        }
    }

    private void checkConflict(boolean checkConflict, String flag) throws CLIException {
        if(checkConflict) {
            throw new CLIException("Other flag already set.", flag);
        }
    }

    private long[] parseLong(String[] args, int i) throws CLIException {
        long[] toReturn = new long[2];
        if(i + 1 < args.length) {
            String[] ints = args[i + 1].split(":");
            if(ints.length == 2) {
                try {
                    toReturn[0] = Long.parseLong(ints[0]);
                    toReturn[1] = Long.parseLong(ints[1]);
                } catch(NumberFormatException e) {
                    throw(new CLIException(args[i], e.getMessage()));
                }
            } else {
                throw(new CLIException(args[i], "Correct format: first:second"));
            }
        } else {
            throw(new CLIException(args[i], "Missing argument: first:second"));
        }
        return toReturn;
    }

    private int parseInt(String[] args, int i) throws CLIException {
        if(i + 1 < args.length) {
            try{
                return Integer.parseInt(args[i + 1]);
            } catch(NumberFormatException e) {
                throw new CLIException(args[i], e.getMessage());
            }
        } else {
            throw new CLIException(args[i]);
        }
    }

    private Path parsePathName(String[] args, int i) throws CLIException {
        if(i + 1 < args.length) {
            if(Files.isRegularFile(Paths.get(args[i + 1])) || args[i].equals("--name")) {
                return Paths.get(args[i + 1]);
            } else if(args[i].equals("--output") && Files.isDirectory(Paths.get(args[i + 1]))) {
                return Paths.get(args[i + 1]);
            } else {
                throw new CLIException(args[i], "Invalid path");
            }
        } else {
            throw(new CLIException(args[i], "Missing argument: path/to/file"));
        }
    }

    /**
     *  This method is the driver code for the analysis being done on Maven Central.
     *  It can be configured using different command line arguments being passed to it.
     *  There's a single-threaded implementation contained in this class, as well as a multithreaded one called here but defined in different actor classes.
     *
     * @param args cli passed to the program to configure the run
     * @return A map of all artifacts collected during the run
     * @throws URISyntaxException when there is an issue with the url built
     * @throws IOException when there is an issue opening a file
     */
    public Map<ArtifactIdent, Artifact> runAnalysis(String[] args) throws URISyntaxException, IOException {
        parseCmdLine(args);
        if(setupInfo.isOutput()) {
            resolverFactory = new ResolverFactory(setupInfo.isOutput(), setupInfo.getToOutputDirectory(), processTransitives);
        } else {
            resolverFactory = new ResolverFactory(processTransitives);
        }

        if(setupInfo.isMulti()) {
            ActorSystem system = ActorSystem.create("my-system");
            queueActorRef = system.actorOf(QueueActor.props(setupInfo.getThreads(), system), "queueActor");

            if(setupInfo.getToCoordinates() == null) {
                indexProcessor();
            } else {
                readIdentsIn();
            }

            try {
                system.getWhenTerminated().toCompletableFuture().get();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            if(setupInfo.getToCoordinates() == null) {
                indexProcessor();
            } else {
                readIdentsIn();
            }
        }

        return ArtifactFactory.artifacts;
    }

    /**
     * Handles walking the maven central index, choosing how to do so based on the configuration.
     *
     * @throws URISyntaxException when there is an issue with the url built
     * @throws IOException        when there is an issue opening a file
     * @see ArtifactIdent
     */
    public void indexProcessor() throws URISyntaxException, IOException {
        String base = "https://repo1.maven.org/maven2/";
        IndexIterator indexIterator;

        //set up indexIterator here (skip to a position or start from the start)
        if (setupInfo.getToIndexPos() != null) {
            indexIterator = new IndexIterator(new URI(base), getStartingPos());
        } else if(setupInfo.getSkip() != -1) {
            indexIterator = new IndexIterator(new URI(base), setupInfo.getSkip());
        } else {
            indexIterator = new IndexIterator(new URI(base));
        }

        if (resolveIndex) {
            if (setupInfo.getSkip() != -1 && setupInfo.getTake() != -1) {
                walkPaginated(setupInfo.getTake(), indexIterator);
            } else if (setupInfo.getSince() != -1 && setupInfo.getUntil() != -1) {
                walkDates(setupInfo.getSince(), setupInfo.getUntil(), indexIterator);
            } else {
                walkAllIndexes(indexIterator);
            }
        } else if (setupInfo.getSkip() != -1 && setupInfo.getTake() != -1) {
            lazyWalkPaginated(setupInfo.getTake(), indexIterator);
        } else if (setupInfo.getSince() != -1 && setupInfo.getUntil() != -1) {
            lazyWalkDates(setupInfo.getSince(), setupInfo.getUntil(), indexIterator);
        } else {
            lazyWalkAllIndexes(indexIterator);
        }

        writeLastProcessed(indexIterator.getIndex(), setupInfo.getName());
    }

    public void processIndex(Artifact current) {
        if(setupInfo.isMulti()) {
            queueActorRef.tell(new IdentPlusMCA(current.getIdent(), this), ActorRef.noSender());
        } else {
            callResolver(current.getIdent());
            analyzeArtifact(current);
        }
    }

    /**
     * Iterates over all indexes in the maven central index and creates an artifact with the metadata collected.
     *
     * @param indexIterator an iterator for traversing the maven central index
     * @return a list of artifacts containing the maven central index metadata
     * @see Artifact
     * @see IndexInformation
     * @throws IOException when there is an issue opening a file
     */
    public List<Artifact> walkAllIndexes(IndexIterator indexIterator) throws IOException {
        List<Artifact> artifacts = new ArrayList<>();

        while(indexIterator.hasNext()) {
            Artifact current = ArtifactFactory.createArtifact(indexIterator.next());
            artifacts.add(current);
            processIndex(current);
            if(artifacts.size() % 500000 == 0) {
                log.info("{} artifacts have been parsed.", artifacts.size());
            }
        }

        log.info("{} artifacts collected.", artifacts.get(artifacts.size() - 1).getIndexInformation().getIndex());
        log.info("{} unique Identifiers.", artifacts.size());

        if(setupInfo.isMulti()) {
            queueActorRef.tell(new IndexProcessingMessage("Finished"), ActorRef.noSender());
        }

        indexIterator.closeReader();
        return artifacts;
    }

    /**
     * Iterates over a given number of indexes from the maven central index, and creates an artifact with the metadata collected.
     *
     * @param take number of artifacts from the starting point to capture
     * @param indexIterator an iterator for traversing the maven central index
     * @return a list of artifacts containing the maven central index metadata
     * @see Artifact
     * @see IndexInformation
     * @throws IOException when there is an issue opening a file
     */
    public List<Artifact> walkPaginated(long take, IndexIterator indexIterator) throws IOException {
        List<Artifact> artifacts = new ArrayList<>();

        take += indexIterator.getIndex();
        while(indexIterator.hasNext() && indexIterator.getIndex() < take) {
            Artifact current = ArtifactFactory.createArtifact(indexIterator.next());
            artifacts.add(current);
            processIndex(current);
        }

        if(setupInfo.isMulti()) {
            queueActorRef.tell(new IndexProcessingMessage("Finished"), ActorRef.noSender());
        }

        indexIterator.closeReader();
        return artifacts;
    }

    /**
     * Iterates over all indexes in the maven central index.
     * It collects a list of artifacts that are within the range of since and until.
     *
     * @param since lower bound of dates of artifacts to collect
     * @param until upper bound of dates of artifacts to collect
     * @param indexIterator an iterator for traversing the maven central index
     * @return a list of artifacts containing the maven central index metadata
     * @see Artifact
     * @see IndexInformation
     * @throws IOException when there is an issue opening a file
     */
    public List<Artifact> walkDates(long since, long until, IndexIterator indexIterator) throws IOException {
        List<Artifact> artifacts = new ArrayList<>();

        long currentToSince;
        while(indexIterator.hasNext()) {
            IndexInformation temp = indexIterator.next();
            currentToSince = temp.getLastModified();
            if(currentToSince >= since && currentToSince < until) {
                Artifact current = ArtifactFactory.createArtifact(indexIterator.next());
                artifacts.add(current);
                processIndex(current);
            }
        }

        if(setupInfo.isMulti()) {
            queueActorRef.tell(new IndexProcessingMessage("Finished"), ActorRef.noSender());
        }

        indexIterator.closeReader();
        return artifacts;
    }

    public void processIndexIdentifier(ArtifactIdent ident) {
        if(setupInfo.isMulti()){
            queueActorRef.tell(new IdentPlusMCA(ident, this), ActorRef.noSender());
        } else {
            callResolver(ident);
            if(ArtifactFactory.getArtifact(ident) != null) {
                analyzeArtifact(ArtifactFactory.getArtifact(ident));
            }
        }
    }

    /**
     * Iterates over all the indexes in the maven central index and just collects the identifiers
     * @param indexIterator an iterator for traversing the maven central index
     * @return a list of artifact identifiers
     * @see ArtifactIdent
     * @throws IOException when there is an issue opening a file
     */
    public List<ArtifactIdent> lazyWalkAllIndexes(IndexIterator indexIterator) throws IOException {
        List<ArtifactIdent> idents = new ArrayList<>();
        while(indexIterator.hasNext()) {
            ArtifactIdent ident = indexIterator.next().getIdent();
            idents.add(ident);
            processIndexIdentifier(ident);
        }

        if(setupInfo.isMulti()) {
            queueActorRef.tell(new IndexProcessingMessage("Finished"), ActorRef.noSender());
        }

        indexIterator.closeReader();
        return idents;
    }

    /**
     * Iterates over a given number of indexes from the maven central index, and collects just the identifiers.
     * @param take how many indexes from the starting position to traverse
     * @param indexIterator an iterator for traversing the maven central index
     * @return a list of artifact identifiers
     * @see ArtifactIdent
     * @throws IOException when there is an issue opening a file
     */
    public List<ArtifactIdent> lazyWalkPaginated(long take, IndexIterator indexIterator) throws IOException{
        List<ArtifactIdent> idents = new ArrayList<>();

        take += indexIterator.getIndex();
        while(indexIterator.hasNext() && indexIterator.getIndex() < take) {
            ArtifactIdent ident = indexIterator.next().getIdent();
            idents.add(ident);
            processIndexIdentifier(ident);
        }

        if(setupInfo.isMulti()) {
            queueActorRef.tell(new IndexProcessingMessage("Finished"), ActorRef.noSender());
        }
        indexIterator.closeReader();
        return idents;
    }

    /**
     * Iterates over all index in the maven central index.
     * It collects a list of artifact identifiers that are within the range of since and until.
     *
     * @param since lower bound of dates of artifacts to collect
     * @param until upper bound of dates of artifacts to collect
     * @param indexIterator an iterator for traversing the maven central index
     * @return a list of artifact identifiers within since and until
     * @see ArtifactIdent
     * @throws IOException when there is an issue opening a file
     */
    public List<ArtifactIdent> lazyWalkDates(long since, long until, IndexIterator indexIterator) throws IOException{
        List<ArtifactIdent> idents = new ArrayList<>();

        long currentToSince;
        while(indexIterator.hasNext()) {
            IndexInformation temp = indexIterator.next();
            currentToSince = temp.getLastModified();
            if(currentToSince >= since && currentToSince < until) {
                ArtifactIdent ident = temp.getIdent();
                idents.add(ident);
                processIndexIdentifier(ident);
            }
        }

        if(setupInfo.isMulti()) {
            queueActorRef.tell(new IndexProcessingMessage("Finished"), ActorRef.noSender());
        }

        indexIterator.closeReader();

        return idents;
    }

    private void writeLastProcessed(long lastIndexProcessed, Path name) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(name.toFile()));
        writer.write(String.valueOf(lastIndexProcessed));
        writer.close();
    }

    /**
     * Reads in identifiers from a file, using the configuration passed into it.
     *
     * @return a list of identifiers collected from the file
     */
    public List<ArtifactIdent> readIdentsIn() {
        Path toCoordinates = setupInfo.getToCoordinates();

        BufferedReader coordinatesReader;
        List<ArtifactIdent> identifiers = new ArrayList<>();
        try{
            coordinatesReader = new BufferedReader(new FileReader(toCoordinates.toFile()));
            String line = coordinatesReader.readLine();

            int i = -1;
            if(setupInfo.getSkip() != -1 && setupInfo.getTake() != -1) {
                int toSkip = 0;
                int curTake = 0;
                while(line != null && curTake < setupInfo.getTake()) {
                    if(toSkip >= setupInfo.getSkip()) {
                        String[] parts = line.split(":");
                        if(parts.length == 3) {
                            ArtifactIdent current = new ArtifactIdent(parts[0], parts[1], parts[2]);
                            identifiers.add(current);
                            processIndexIdentifier(current);
                            curTake++;
                        } else {
                            log.error("unable to process Artifact Identifier {} at position {}", line, i);
                        }
                    }
                    line = coordinatesReader.readLine();
                    toSkip++;
                    i++;
                }
            } else if(setupInfo.getToIndexPos() != null) {
                long start = getStartingPos();
                int curPos = 0;
                while(line != null) {
                    if(curPos >= start) {
                        String[] parts = line.split(":");
                        if(parts.length == 3) {
                            ArtifactIdent current = new ArtifactIdent(parts[0], parts[1], parts[2]);
                            identifiers.add(current);
                            processIndexIdentifier(current);
                        } else {
                            log.error("unable to process Artifact Identifier {} at position {}", line, i);
                        }
                    }
                    line = coordinatesReader.readLine();
                    curPos++;
                    i++;
                }
            } else {
                while(line != null) {
                    String[] parts = line.split(":");
                    if(parts.length == 3) {
                        ArtifactIdent current = new ArtifactIdent(parts[0], parts[1], parts[2]);
                        identifiers.add(current);
                        processIndexIdentifier(current);
                    } else {
                        log.error("unable to process Artifact Identifier {} at position {}", line, i);
                    }
                    line = coordinatesReader.readLine();
                    i++;
                }
            }

            if(setupInfo.isMulti()) {
                queueActorRef.tell(new IndexProcessingMessage("Finished"), ActorRef.noSender());
            }

            writeLastProcessed(i, setupInfo.getName());
            coordinatesReader.close();
        } catch(IOException e) {
            throw new RuntimeException(e);
        }
        return identifiers;
    }

    private long getStartingPos() {
        BufferedReader indexReader;
        try {
            indexReader = new BufferedReader(new FileReader(setupInfo.getToIndexPos().toFile()));
            String line = indexReader.readLine();
            return Integer.parseInt(line);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void callResolver(ArtifactIdent identifier) {
        if(resolvePom && resolveJar) {
            resolverFactory.runBoth(identifier);
        } else if(resolvePom) {
            resolverFactory.runPom(identifier);
        } else if(resolveJar) {
            resolverFactory.runJar(identifier);
        }
    }
}
