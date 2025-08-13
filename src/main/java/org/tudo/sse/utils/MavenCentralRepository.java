package org.tudo.sse.utils;

import org.tudo.sse.model.ArtifactIdent;
import org.tudo.sse.resolution.FileNotFoundException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.nio.charset.StandardCharsets;

/**
 * This class manages all the url building and http requests for retrieving artifacts from the maven central repository,
 * as well as the secondary repositories that artifacts may be on.
 */
public final class MavenCentralRepository {

    private static final String RepoBasePath = "https://repo1.maven.org/maven2/";

    private static MavenCentralRepository theInstance = null;

    private MavenCentralRepository() {

    }

    /**
     * Opens an input stream to the version list of the given library.
     * @param ident Artifact identifier that references a library (only group ID and artifact ID are used)
     * @return An input stream for the library's version list XML file
     * @throws IOException If accessing the resource fails
     * @throws FileNotFoundException If the library does not exist / does not have a version list file
     */
    public InputStream openXMLFileInputStream(ArtifactIdent ident) throws IOException, FileNotFoundException {
        return ResourceConnections.openInputStream(ident.getMavenCentralXMLUri());
    }

    /**
     * Opens an input stream to the POM file of the given artifact.
     * @param ident Artifact identifier for which to open the POM file input stream
     * @return An input stream for the artifact's POM file
     * @throws FileNotFoundException If the artifact does not exist
     * @throws IOException If accessing the resource fails
     */
    public InputStream openPomFileInputStream(ArtifactIdent ident) throws FileNotFoundException, IOException {
        return ResourceConnections.openInputStream(ident.getMavenCentralPomUri());
    }

    /**
     * Opens an input stream to the JAR file of the given artifact. Uses the local Maven cache to avoid unnecessary
     * downloads.
     * @param ident Artifact identifier for which to open the JAR file input stream
     * @return An input stream for the artifact's JAR file
     * @throws IOException If accessing the resource fails
     * @throws FileNotFoundException If the artifact does not exist / does not have a JAR file
     */
    public InputStream openJarFileInputStream(ArtifactIdent ident) throws IOException, FileNotFoundException {
        //add thing here to check m2 cache for the jar that's being looked for
        String m2Repo = System.getProperty("user.home") + File.separator + ".m2" + File.separator + "repository";

        // Combine to get the full path
        String fullPath;
        try {
            fullPath = m2Repo + File.separator + buildJarFileURI(ident);
            // Create a File object
            File artifactFile = new File(fullPath);
            if(artifactFile.exists()) {
                return new FileInputStream(artifactFile);
            }

        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }

        return ResourceConnections.openInputStream(ident.getMavenCentralJarUri());

    }

    /**
     * Returns the one instance of this Maven Central repository
     * @return The instance (singleton)
     */
    public static MavenCentralRepository getInstance(){
        if(theInstance == null)
            theInstance = new MavenCentralRepository();

        return theInstance;
    }

    /**
     * Builds the URI that references an artifacts POM file.
     * @param artifact The artifact identifier
     * @return Fully encoded URI referencing the artifact's POM file
     * @throws URISyntaxException If the URI is invalid
     */
    public static URI buildPomFileURI(ArtifactIdent artifact)
            throws URISyntaxException {
        return buildArtifactBaseURI(artifact)
                .resolve(encode(artifact.getArtifactID()) + "-" + encode(artifact.getVersion()) + ".pom");
    }

    /**
     * Builds the URI that references an artifacts POM file within a secondary repository.
     * @param artifact The artifact identifier
     * @param secondaryRepoPath The base url of the secondary repository (that is not Maven Central)
     * @return Fully encoded URI referencing the artifact's POM file within a secondary repository
     * @throws URISyntaxException If the URI is invalid
     */
    public static URI buildSecondaryPomFileURI(ArtifactIdent artifact, String secondaryRepoPath)
            throws URISyntaxException {
        return buildSecondaryArtifactBaseURI(artifact, secondaryRepoPath)
                .resolve(encode(artifact.getArtifactID()) + "-" + encode(artifact.getVersion()) + ".pom");
    }

    /**
     * Builds the URI that references an artifacts default JAR file.
     * @param artifact The artifact identifier
     * @return Fully encoded URI referencing the artifact's default JAR file
     * @throws URISyntaxException If the URI is invalid
     */
    public static URI buildJarFileURI(ArtifactIdent artifact)
            throws URISyntaxException {
        return buildArtifactBaseURI(artifact)
                .resolve(encode(artifact.getArtifactID()) + "-" + encode(artifact.getVersion()) + ".jar");
    }

    /**
     * Builds the URI that references a library's version list (maven-metadata.xml) file.
     * @param artifact The artifact identifier identifying a library (only group ID and artifact ID are used)
     * @return Fully encoded URI referencing the library's version list
     * @throws URISyntaxException If the URI is invalid
     */
    public static URI buildVersionsFileURI(ArtifactIdent artifact)
            throws URISyntaxException {
        return buildLibraryBaseURI(artifact)
                .resolve(encode("maven-metadata") + ".xml");
    }

    /**
     * Builds the URI that references an artifact base directory on the Central repository
     * @param artifact The artifact identifier
     * @return Fully encoded URI referencing the artifact base directory
     * @throws URISyntaxException If the URI is invalid
     */
    public static URI buildArtifactBaseURI(ArtifactIdent artifact)
            throws URISyntaxException {
        return new URI(RepoBasePath)
                .resolve(encode(artifact.getGroupID()).replace(".", "/") + "/")
                .resolve(encode(artifact.getArtifactID()) + "/")
                .resolve(encode(artifact.getVersion()) + "/");
    }

    /**
     * Builds the URI that references an artifact base directory on a secondary repository
     * @param artifact The artifact identifier
     * @param secondaryRepoPath The base url of the secondary repository (that is not Maven Central)
     * @return Fully encoded URI referencing the artifact base directory within the secondary repository
     * @throws URISyntaxException If the URI is invalid
     */
    public static URI buildSecondaryArtifactBaseURI(ArtifactIdent artifact, String secondaryRepoPath)
            throws URISyntaxException {
        return new URI(secondaryRepoPath)
                .resolve(encode(artifact.getGroupID()).replace(".", "/") + "/")
                .resolve(encode(artifact.getArtifactID()) + "/")
                .resolve(encode(artifact.getVersion()) + "/");
    }

    /**
     * Builds the URI that references a library's base directory on the Central repository
     * @param artifact The artifact identifier identifying a library (only group ID and artifact ID are used)
     * @return Fully encoded URI referencing the library base directory
     * @throws URISyntaxException If the URI is invalid
     */
    public static URI buildLibraryBaseURI(ArtifactIdent artifact)
            throws URISyntaxException {
        return new URI(RepoBasePath)
                .resolve(encode(artifact.getGroupID()).replace(".", "/") + "/")
                .resolve(encode(artifact.getArtifactID()) + "/");
    }

    private static String encode(String path) {
        return URLEncoder.encode(path, StandardCharsets.UTF_8);
    }
}
