package org.tudo.sse.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.tudo.sse.model.Artifact;
import org.tudo.sse.model.ArtifactIdent;
import org.tudo.sse.resolution.FileNotFoundException;
import org.tudo.sse.resolution.PomResolutionException;
import org.tudo.sse.resolution.PomResolver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class GAUtils {
    private static final MavenCentralRepository MavenRepo = MavenCentralRepository.getInstance();
    public static final Logger log = LogManager.getLogger(GAUtils.class);

    public static List<Artifact> retrieveAllVersions(String groupId, String artifactId) {
        try {
            PomResolver pomResolver = new PomResolver(false);
            List<Artifact> toReturn = new ArrayList<>();
            Metadata meta = getVersions(groupId, artifactId);

            if(meta.getVersioning() == null) {
                return toReturn;
            }

            List<String> versions = meta.getVersioning().getVersions();

            for(String version : versions) {
                try {
                    toReturn.add(pomResolver.resolveArtifact(new ArtifactIdent(groupId, artifactId, version)));
                } catch (PomResolutionException | FileNotFoundException e) {
                    log.error(e);
                }
            }
            return toReturn;
        } catch (FileNotFoundException | IOException | XmlPullParserException e) {
            throw new RuntimeException(e);
        }
    }

    public static List<String> getReleasesFromMetadata(ArtifactIdent identifier){
        try{
            Metadata meta = getVersions(identifier.getGroupID(), identifier.getArtifactID());

            if(meta.getVersioning() == null){
                throw new RuntimeException("Invalid versioning in metadata: null");
            }

            return meta.getVersioning().getVersions();
        } catch(FileNotFoundException | IOException | XmlPullParserException x){
            throw new RuntimeException(x);
        }
    }

    public static Artifact getLastModifiedVersion(String groupId, String artifactId) throws PomResolutionException {
        try {
            Metadata meta = getVersions(groupId, artifactId);

            if(meta.getVersioning() == null) {
                return null;
            }

            String latestVersion;
            if(meta.getVersioning().getRelease() != null) {
                latestVersion = meta.getVersioning().getRelease();
            } else {
              long latestLastModified = 0;
              List<String> versions = meta.getVersioning().getVersions();
              latestVersion = versions.get(0);
                for(String version : versions) {
                    long currentLastModified = MavenRepo.getLastModified(new ArtifactIdent(groupId, artifactId, version));
                  if(currentLastModified > latestLastModified) {
                      latestLastModified = currentLastModified;
                      latestVersion = version;
                  }
                }
            }

            PomResolver pomResolver = new PomResolver(false);
            Artifact toReturn = pomResolver.resolveArtifact(new ArtifactIdent(groupId, artifactId, latestVersion));
            return toReturn;
        } catch (FileNotFoundException | IOException | XmlPullParserException e) {
            throw new RuntimeException(e);
        }
    }

    public static Artifact getHighestVersion(String groupId, String artifactId) throws PomResolutionException {
        try {
            Metadata meta = getVersions(groupId, artifactId);

            if(meta.getVersioning() == null) {
                return null;
            }

            String highestVersion;
            if(meta.getVersioning().getRelease() != null) {
                highestVersion = meta.getVersioning().getLatest();
            } else {
                List<String> versions = meta.getVersioning().getVersions();
                highestVersion = versions.get(versions.size() - 1);
            }

            PomResolver pomResolver = new PomResolver(false);
            Artifact toReturn = pomResolver.resolveArtifact(new ArtifactIdent(groupId, artifactId, highestVersion));
            return toReturn;
        } catch (FileNotFoundException | IOException | XmlPullParserException e) {
            throw new RuntimeException(e);
        }
    }

    private static Metadata getVersions(String groupId, String artifactId) throws FileNotFoundException, IOException, XmlPullParserException {
        BufferedReader versionings = new BufferedReader(new InputStreamReader(MavenRepo.openXMLFileInputStream(new ArtifactIdent(groupId, artifactId, null))));
        MetadataXpp3Reader reader = new MetadataXpp3Reader();
        Metadata toReturn = reader.read(versionings);
        versionings.close();
        return toReturn;
    }

}
