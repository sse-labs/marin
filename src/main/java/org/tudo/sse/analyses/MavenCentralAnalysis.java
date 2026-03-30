package org.tudo.sse.analyses;

import org.opalj.log.GlobalLogContext$;
import org.opalj.log.OPALLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tudo.sse.utils.MarinOpalLogger;

/**
 * An analysis that processes entities (artifacts or libraries) from the Maven Central ecosystem.
 *
 * @author Johannes Düsing
 */
public abstract class MavenCentralAnalysis {

    /**
     * Defines whether this analysis requires artifacts to have index information annotated.
     */
    protected final boolean resolveIndex;

    /**
     * Defines whether this analysis requires artifacts to have pom information annotated.
     */
    protected final boolean resolvePom;

    /**
     * Defines whether this analysis requires artifacts to have resolved transitive pom information.
     */
    protected final boolean processTransitives;

    /**
     * Defines whether this analysis requires artifacts to have jar information annotated.
     */
    protected final boolean resolveJar;

    /**
     * Logger instance for subclasses
     */
    protected final Logger log = LoggerFactory.getLogger(getClass());


    /**
     * Creates a new Maven Central Analysis with the given configuration options.
     *
     * @param requiresIndex Whether this analysis requires index information
     * @param requiresPom Whether this analysis requires POM information
     * @param requiresTransitives Whether this analysis requires transitive POM information. If true, "requiresPOM" will
     *                            be set to true no matter its given parameter value.
     * @param requiresJar Whether this analysis requires JAR information
     */
    protected MavenCentralAnalysis(boolean requiresIndex,
                                   boolean requiresPom,
                                   boolean requiresTransitives,
                                   boolean requiresJar){
        if(!requiresIndex && !requiresPom && !requiresTransitives && !requiresJar){
            log.warn("Potential misconfiguration - no data sources (index, POM, JAR) are required by this analysis");
        } else if(requiresTransitives && !requiresPom){
            log.warn("Potential misconfiguration - analysis requires transitive information but no POM information. " +
                    "POM information will also be collected to provide transitive information.");
        }

        // Use global OPAL Logger - will only forward OPAL messages with level error or fatal
        OPALLogger.updateLogger(GlobalLogContext$.MODULE$, MarinOpalLogger.getGlobalLogger());

        resolveIndex = requiresIndex;
        resolvePom = requiresPom || requiresTransitives; // Cannot have transitive information without POM information
        processTransitives = requiresTransitives;
        resolveJar = requiresJar;
    }

    /**
     * Analysis lifecycle hook that is executed before the actual analysis run is started, but after command line
     * parameters and resolvers have been initialized. This method is called exactly once per analysis run.
     */
    protected void beforeRunStart(){
        log.debug("Starting Maven Central analysis run.");
    }


    /**
     * Analysis lifecycle hook that is executed after the analysis run has completed, immediately before the end of
     * {@link #runAnalysis runAnalysis}. This method is called exactly once per analysis run.
     */
    protected void afterRunEnd(){
        log.debug("Finished Maven Central analysis run.");
    }

    /**
     *  Starts a new analysis run. Will parse the given command line arguments and create a matching analysis
     *  configuration. Depending on the arguments provided, the analysis run will use single- or multithreaded execution.
     *
     * @param args Command line arguments to parse - usually taken directly from the main method's arguments.
     */
    public abstract void runAnalysis(String[] args);

}
