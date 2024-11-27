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
public class MavenCentralRepository {

    private static final String RepoBasePath = "https://repo1.maven.org/maven2/";

    private static MavenCentralRepository theInstance = null;

    private MavenCentralRepository() {

    }

    public InputStream openXMLFileInputStream(ArtifactIdent ident) throws IOException, FileNotFoundException {
        return ResourceConnections.openInputStream(ident.getMavenCentralXMLUri());
    }

    public long getLastModified(ArtifactIdent ident) throws FileNotFoundException, IOException {
        HttpURLConnection connection = ResourceConnections.openConnection(ident.getMavenCentralPomUri());
        long toReturn = connection.getLastModified();
        connection.disconnect();
        return toReturn;
    }

    public InputStream openPomFileInputStream(ArtifactIdent ident) throws FileNotFoundException, IOException {
        return ResourceConnections.openInputStream(ident.getMavenCentralPomUri());
    }

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

    public static MavenCentralRepository getInstance(){
        if(theInstance == null)
            theInstance = new MavenCentralRepository();

        return theInstance;
    }

    public static URI buildPomFileURI(ArtifactIdent artifact)
            throws URISyntaxException {
        return buildArtifactBaseURI(artifact)
                .resolve(encode(artifact.getArtifactID()) + "-" + encode(artifact.getVersion()) + ".pom");
    }

    public static URI buildSecondaryPomFileURI(ArtifactIdent artifact, String secondaryRepoPath)
            throws URISyntaxException {
        return buildSecondaryArtifactBaseURI(artifact, secondaryRepoPath)
                .resolve(encode(artifact.getArtifactID()) + "-" + encode(artifact.getVersion()) + ".pom");
    }

    public static URI buildJarFileURI(ArtifactIdent artifact)
            throws URISyntaxException {
        return buildArtifactBaseURI(artifact)
                .resolve(encode(artifact.getArtifactID()) + "-" + encode(artifact.getVersion()) + ".jar");
    }

    public static URI buildVersionsFileURI(ArtifactIdent artifact)
            throws URISyntaxException {
        return buildVersionsBaseURI(artifact)
                .resolve(encode("maven-metadata") + ".xml");
    }

    public static URI buildArtifactBaseURI(ArtifactIdent artifact)
            throws URISyntaxException {
        return new URI(RepoBasePath)
                .resolve(encode(artifact.getGroupID()).replace(".", "/") + "/")
                .resolve(encode(artifact.getArtifactID()) + "/")
                .resolve(encode(artifact.getVersion()) + "/");
    }

    public static URI buildSecondaryArtifactBaseURI(ArtifactIdent artifact, String secondaryRepoPath)
            throws URISyntaxException {
        return new URI(secondaryRepoPath)
                .resolve(encode(artifact.getGroupID()).replace(".", "/") + "/")
                .resolve(encode(artifact.getArtifactID()) + "/")
                .resolve(encode(artifact.getVersion()) + "/");
    }

    public static URI buildVersionsBaseURI(ArtifactIdent artifact)
            throws URISyntaxException {
        return new URI(RepoBasePath)
                .resolve(encode(artifact.getGroupID()).replace(".", "/") + "/")
                .resolve(encode(artifact.getArtifactID()) + "/");
    }

    private static String encode(String path) {
        return URLEncoder.encode(path, StandardCharsets.UTF_8);
    }
}
