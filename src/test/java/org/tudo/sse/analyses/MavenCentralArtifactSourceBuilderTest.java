package org.tudo.sse.analyses;

import org.apache.pekko.Done;
import org.apache.pekko.actor.ActorSystem;
import org.apache.pekko.japi.Pair;
import org.apache.pekko.japi.function.Procedure;
import org.apache.pekko.stream.UniqueKillSwitch;
import org.apache.pekko.stream.javadsl.Keep;
import org.apache.pekko.stream.javadsl.Sink;
import org.apache.pekko.stream.javadsl.Source;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.opalj.log.GlobalLogContext$;
import org.opalj.log.OPALLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tudo.sse.analyses.config.ArtifactAnalysisConfig;
import org.tudo.sse.analyses.config.ArtifactAnalysisConfigBuilder;
import org.tudo.sse.analyses.config.InvalidConfigurationException;
import org.tudo.sse.model.Artifact;
import org.tudo.sse.utils.MarinOpalLogger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.tudo.sse.utils.TestUtilities.testResource;

public class MavenCentralArtifactSourceBuilderTest {

    private final Logger log = LoggerFactory.getLogger(this.getClass());
    private final ActorSystem system = ActorSystem.create("marin-test-system");

    private final Path gavInputList = testResource("artifact-names-valid.txt");

    @BeforeAll
    static void setup() {
        OPALLogger.updateLogger(GlobalLogContext$.MODULE$, MarinOpalLogger.getGlobalLogger());
    }

    @Test
    @DisplayName("Sources must respect take values in single-threaded mode")
    void takeWithIndex() throws InvalidConfigurationException, ExecutionException, InterruptedException, TimeoutException {
        final AtomicInteger count = new AtomicInteger(0);
        final ArtifactAnalysisConfig config = new ArtifactAnalysisConfigBuilder()
                .withTake(10)
                .withNumberOfThreads(1)
                .build();

        final Source<Artifact, UniqueKillSwitch> source =
                new MavenCentralArtifactSourceBuilder(config).buildSource();

        blockingRunSource(source, a -> {
            count.incrementAndGet();
            log.info("Processing artifact #{}: {}", count.get(), a.getIdent());

            assertTrue(a.hasIndexInformation());
            assertFalse(a.hasPomInformation());
            assertFalse(a.hasJarInformation());
        });

        assertEquals(10, count.get());
    }

    @Test
    @DisplayName("Sources must respect take values in multi-threaded mode")
    void takeWithIndexMulti() throws InvalidConfigurationException, ExecutionException, InterruptedException, TimeoutException {
        final AtomicInteger count = new AtomicInteger(0);
        final ArtifactAnalysisConfig config = new ArtifactAnalysisConfigBuilder()
                .withTake(100)
                .withNumberOfThreads(4)
                .build();

        final Source<Artifact, UniqueKillSwitch> source =
                new MavenCentralArtifactSourceBuilder(config).buildSource();

        blockingRunSource(source, a -> {
            count.incrementAndGet();
            log.info("Processing artifact #{}: {}", count.get(), a.getIdent());

            assertTrue(a.hasIndexInformation());
            assertFalse(a.hasPomInformation());
            assertFalse(a.hasJarInformation());
        });

        assertEquals(100, count.get());
    }



    @Test
    @DisplayName("Sources must close when the kill switch is used")
    void killSwitchMulti() throws InvalidConfigurationException, InterruptedException {
        final ArtifactAnalysisConfig config = new ArtifactAnalysisConfigBuilder()
                .withNumberOfThreads(4)
                .build();

        final Source<Artifact, UniqueKillSwitch> source =
                new MavenCentralArtifactSourceBuilder(config).buildSource();

        final Pair<UniqueKillSwitch, CompletionStage<Done>> result = runSource(source, a -> {
            assertTrue(a.hasIndexInformation());
            assertFalse(a.hasPomInformation());
            assertFalse(a.hasJarInformation());
        });

        Thread.sleep(500);

        result.first().shutdown();

        Thread.sleep(500);

        assertTrue(result.second().toCompletableFuture().isDone());
    }

    @Test
    @DisplayName("Sources must enrich artifacts correctly")
    void takeWithJarAndIndex() throws InvalidConfigurationException, ExecutionException, InterruptedException, TimeoutException {
        final AtomicInteger count = new AtomicInteger(0);
        final ArtifactAnalysisConfig config = new ArtifactAnalysisConfigBuilder()
                .withTake(10)
                .withNumberOfThreads(4)
                .build();

        final Source<Artifact, UniqueKillSwitch> source = new MavenCentralArtifactSourceBuilder(config)
                .withJarInfo()
                .withPomInfo()
                .buildSource();

        blockingRunSource(source, a -> {
            count.incrementAndGet();
            log.info("Processing artifact #{}: {}", count.get(), a.getIdent());

            assertTrue(a.hasIndexInformation());
            assertTrue(a.hasPomInformation());
            assertTrue(a.hasJarInformation());
        });

        assertEquals(10, count.get());
    }

    @Test
    @DisplayName("Sources must work with custom input files")
    void sourceFromFile() throws InvalidConfigurationException, ExecutionException, InterruptedException, TimeoutException, IOException {
        final Set<String> expected = new HashSet<>(Files.readAllLines(gavInputList).subList(2, 7));

        final AtomicInteger count = new AtomicInteger(0);
        final ArtifactAnalysisConfig config = new ArtifactAnalysisConfigBuilder()
                .withSkip(2)
                .withTake(5)
                .withInputList(gavInputList)
                .withNumberOfThreads(2)
                .build();

        final Source<Artifact, UniqueKillSwitch> source = new MavenCentralArtifactSourceBuilder(config)
                .withPomInfo()
                .buildSource();

        blockingRunSource(source, a -> {
            count.incrementAndGet();
            log.info("Processing artifact #{}: {}", count.get(), a.getIdent());

            assertFalse(a.hasIndexInformation());
            assertTrue(a.hasPomInformation());
            assertFalse(a.hasJarInformation());

            assertTrue(expected.contains(a.getIdent().toString()));
        });

        assertEquals(5, count.get());
    }

    private Pair<UniqueKillSwitch, CompletionStage<Done>> blockingRunSource(Source<Artifact, UniqueKillSwitch> source,
                                                                    Procedure<Artifact> p) throws ExecutionException, InterruptedException, TimeoutException {
        final Sink<Artifact, CompletionStage<Done>> sink = Sink.foreach(p);
        final Pair<UniqueKillSwitch, CompletionStage<Done>> result = source.toMat(sink, Keep.both()).run(system);

        result.second().toCompletableFuture().get(1, TimeUnit.MINUTES);

        return result;
    }
    
    private Pair<UniqueKillSwitch, CompletionStage<Done>> runSource(Source<Artifact, UniqueKillSwitch> source,
                                                                    Procedure<Artifact> p){
        final Sink<Artifact, CompletionStage<Done>> sink = Sink.foreach(p);
        return source.toMat(sink, Keep.both()).run(system);
    }


}
