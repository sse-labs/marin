package org.tudo.sse.resolution;

import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.tudo.sse.model.pom.LocalPomInformation;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

/**
 * Class providing static utility methods to build LocalPomFileInformation for pom.xml files not hosted on any repository.
 */
public class LocalPomInformationFactory {

    private static final PomResolver defaultTransitiveResolver = new PomResolver(true);
    private static final PomResolver defaultDirectResolver = new PomResolver(false);

    private LocalPomInformationFactory() {}

    /**
     * Load a local pom.xml file from the given input stream, using the given resolver and resolution mode.
     *
     * @param pomInputStream InputStream containing the pom.xml file contents
     * @param loadTransitiveDependencies Whether to transitively load all referenced pom files
     * @param theResolver PomResolver to used for resolution
     * @return LocalPomInformation object representing the pom.xml file
     * @throws IOException If accessing the stream fails
     */
    public static LocalPomInformation loadLocalPom(InputStream pomInputStream,
                                                   boolean loadTransitiveDependencies,
                                                   PomResolver theResolver) throws IOException {
        final LocalPomInformation pomInfo = new LocalPomInformation(pomInputStream, loadTransitiveDependencies);

        try {
            pomInfo.resolveLocalPom(theResolver);
            return pomInfo;
        } catch (XmlPullParserException xppx){
            throw new RuntimeException(xppx);
        }
    }

    /**
     * Load a local pom.xml file from the given input stream, using the given resolution mode and a default resolver.
     *
     * @param pomInputStream InputStream containing the pom.xml file contents
     * @param loadTransitiveDependencies Whether to transitively load all referenced pom files
     * @return LocalPomInformation object representing the pom.xml file
     * @throws IOException If accessing the stream fails
     */
    public static LocalPomInformation loadLocalPom(InputStream pomInputStream,
                                                   boolean loadTransitiveDependencies) throws IOException {
        return loadLocalPom(pomInputStream, loadTransitiveDependencies, getDefaultResolver(loadTransitiveDependencies));
    }

    /**
     * Load a local pom.xml file from the given path, using the given resolver and resolution mode.
     *
     * @param pomPath Local path to pom.xml file
     * @param loadTransitiveDependencies Whether to transitively load all referenced pom files
     * @param theResolver PomResolver to used for resolution
     * @return LocalPomInformation object representing the pom.xml file
     * @throws IOException If accessing the file contents fails
     */
    public static LocalPomInformation loadLocalPom(Path pomPath,
                                                   boolean loadTransitiveDependencies,
                                                   PomResolver theResolver) throws IOException {
        final LocalPomInformation pomInfo = new LocalPomInformation(pomPath, loadTransitiveDependencies);

        try {
            pomInfo.resolveLocalPom(theResolver);
            return pomInfo;
        } catch (XmlPullParserException xppx){
            throw new RuntimeException(xppx);
        }
    }

    /**
     * Load a local pom.xml file from the given path, using the given resolution mode and a default resolver.
     *
     * @param pomPath Local path to pom.xml file
     * @param loadTransitiveDependencies Whether to transitively load all referenced pom files
     * @return LocalPomInformation object representing the pom.xml file
     * @throws IOException If accessing the file contents fails
     */
    public static LocalPomInformation loadLocalPom(Path pomPath,
                                                   boolean loadTransitiveDependencies) throws IOException {
        return loadLocalPom(pomPath, loadTransitiveDependencies, getDefaultResolver(loadTransitiveDependencies));
    }

    /**
     * Load a local pom.xml file from the given File, using the given resolver and resolution mode.
     *
     * @param pomFile File object representing a pom.xml file
     * @param loadTransitiveDependencies Whether to transitively load all referenced pom files
     * @param theResolver PomResolver to used for resolution
     * @return LocalPomInformation object representing the pom.xml file
     * @throws IOException If accessing the file contents fails
     */
    public static LocalPomInformation loadLocalPom(File pomFile,
                                                   boolean loadTransitiveDependencies,
                                                   PomResolver theResolver) throws IOException {
        return loadLocalPom(pomFile.toPath(), loadTransitiveDependencies, theResolver);
    }

    /**
     * Load a local pom.xml file from the given File, using the given resolution mode and a default resolver.
     *
     * @param pomFile File object representing a pom.xml file
     * @param loadTransitiveDependencies Whether to transitively load all referenced pom files
     * @return LocalPomInformation object representing the pom.xml file
     * @throws IOException If accessing the file contents fails
     */
    public static LocalPomInformation loadLocalPom(File pomFile,
                                                   boolean loadTransitiveDependencies) throws IOException {
        return loadLocalPom(pomFile.toPath(), loadTransitiveDependencies, getDefaultResolver(loadTransitiveDependencies));
    }

    private static PomResolver getDefaultResolver(boolean loadTransitives){
        if(loadTransitives)
            return defaultTransitiveResolver;
        else
            return defaultDirectResolver;
    }

}
