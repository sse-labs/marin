package org.tudo.sse.model;

import java.net.URI;
import java.util.Objects;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.tudo.sse.utils.MavenCentralRepository;


/**
 * The ArtifactIdent holds the gav triple for each artifact on the maven central repository.
 * As well as methods for retrieving different file types from the maven central repository.
 */
public class ArtifactIdent {
    /**
     * The group section of the identifier.
     */
    private String groupID;

    /**
     * The specific artifact identifier.
     */
    private String artifactID;
    /**
     * The version of the artifact.
     */
    private String version;
    /**
     * The repository where this artifact can be found.
     */
    private String repository;

    private static final Logger log = LogManager.getLogger(ArtifactIdent.class);

    public ArtifactIdent(String groupID, String artifactID, String version) {
        this.artifactID = artifactID;
        this.groupID = groupID;
        this.version = version;
        this.repository = "https://repo1.maven.org/maven2/";
    }

    public ArtifactIdent(ArtifactIdent toCopy) {
        this.groupID = toCopy.groupID;
        this.artifactID = toCopy.artifactID;
        this.version = toCopy.version;
        this.repository = toCopy.repository;
    }

    /**
     * Gets the groupID.
     * @return groupID
     */
    public String getGroupID() {
        return groupID;
    }

    /**
     * This updates the groupID value
     * @param groupID new groupID value
     */
    public void setGroupID(String groupID) {
        this.groupID = groupID;
    }

    /**
     * Gets the artifactID.
     * @return artifactID
     */
    public String getArtifactID() {
        return artifactID;
    }

    /**
     * This updates the artifactID value
     * @param artifactID new artifactID value
     */
    public void setArtifactID(String artifactID) {
        this.artifactID = artifactID;
    }

    /**
     * Gets the version.
     * @return version
     */
    public String getVersion() {
        return version;
    }

    /**
     * This updates the version value
     * @param version new version value
     */
    public void setVersion(String version) {
        this.version = version;
    }

    /**
     * Gets the repository where this artifact can be found.
     * @return repository
     */
    public String getRepository() {
        return repository;
    }

    /**
     * Sets the repository for where this artifact can be found.
     * @param repository new repository value
     */
    public void setRepository(String repository) {
        this.repository = repository;
    }

    /**
     * Gets the coordinates, the full built identifier for a maven artifact.
     * @return full g:a:v value
     */
    public String getCoordinates() {
        return groupID + ":" + artifactID + ":" + version;
    }

    /**
     * Gets the url for the pom file on maven central
     * @return pom URI
     */
    public URI getMavenCentralPomUri() {
        try {
            if(repository.equals("https://repo1.maven.org/maven2/")) {
                return MavenCentralRepository.buildPomFileURI(this);
            } else {
                return MavenCentralRepository.buildSecondaryPomFileURI(this, getRepository());
            }
        } catch(Exception x){
            log.error("Failed to build artifact pom url: {}", x.getMessage());
            return null;
        }
    }

    /**
     * Gets the url for the jar file on maven central
     * @return jar URI
     */
    public URI getMavenCentralJarUri() {
        try {
            return MavenCentralRepository.buildJarFileURI(this);
        } catch(Exception x){
            log.error("Failed to build artifact jar url: {}", x.getMessage());
            return null;
        }
    }

    /**
     * Gets the url for the versions file on maven central
     * @return versions.xml URI
     */
    public URI getMavenCentralXMLUri() {
        try {
            return MavenCentralRepository.buildVersionsFileURI(this);
        } catch(Exception x){
            log.error("Failed to build maven-metadata url: {}", x.getMessage());
            return null;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ArtifactIdent that = (ArtifactIdent) o;
        return Objects.equals(groupID, that.groupID) &&
                Objects.equals(artifactID, that.artifactID) &&
                Objects.equals(version, that.version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(groupID, artifactID, version);
    }

}
