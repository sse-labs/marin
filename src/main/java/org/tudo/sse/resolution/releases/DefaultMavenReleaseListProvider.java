package org.tudo.sse.resolution.releases;

import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.Versioning;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tudo.sse.model.ArtifactIdent;
import org.tudo.sse.resolution.FileNotFoundException;
import org.tudo.sse.utils.MavenCentralRepository;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * This default implementation of IReleaseListProvider obtains a list of releases for a given GA-Tuple by
 * querying the maven-metadata.xml file hosted on Maven Central. If the file it not present, it will attempt
 * to obtain a list of release versions from the HTML list provided by Maven Central. If both attempts fail,
 * an IOException will be thrown.
 *
 * @author Johannes Düsing
 */
public class DefaultMavenReleaseListProvider implements IReleaseListProvider{

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private final MetadataXpp3Reader reader = new MetadataXpp3Reader();
    private final MavenCentralRepository mavenRepo = MavenCentralRepository.getInstance();

    // Singleton pattern implementation
    private final static DefaultMavenReleaseListProvider instance = new DefaultMavenReleaseListProvider();

    /**
     * Access the singleton instance of DefaultMavenReleaseListProvider
     * @return The only instance of this class
     */
    public static DefaultMavenReleaseListProvider getInstance() {
        return instance;
    }

    // Singleton pattern implementation
    private DefaultMavenReleaseListProvider(){}

    @Override
    public List<String> getReleases(String groupId, String artifactId) throws IOException {
        try(InputStream xmlInputStream = mavenRepo.openXMLFileInputStream(groupId, artifactId)) {
            Metadata versionListData = reader.read(new BufferedReader(new InputStreamReader(xmlInputStream)));

            Versioning versioning = versionListData.getVersioning();

            if (versioning != null) {
                List<String> versions = new ArrayList<>();
                for (Object version : versioning.getVersions()) {
                    versions.add((String) version);
                }
                return versions;
            } else {
                return List.of();
            }
        }
        catch(FileNotFoundException fnfx){
            log.warn("No `maven-metadata.xml` found for {}:{}, using fallback HTML-based version list provider",groupId,artifactId);
            final IReleaseListProvider fallbackProvider = HTMLBasedMavenReleaseListProvider.getInstance();

            try {
                return fallbackProvider.getReleases(groupId, artifactId);
            } catch (IOException iox){
                throw new IOException("Failed to obtain version list for " +  groupId + ":" + artifactId, iox);
            }
        }
        catch (XmlPullParserException | IOException | URISyntaxException x) {
            throw new IOException("Failed to obtain version list for " + groupId + ":" + artifactId, x);
        }
    }

}
