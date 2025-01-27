package org.tudo.sse.resolution.releases;

import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.Versioning;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.tudo.sse.model.ArtifactIdent;
import org.tudo.sse.resolution.FileNotFoundException;
import org.tudo.sse.utils.MavenCentralRepository;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


public class DefaultMavenReleaseListProvider implements IReleaseListProvider{

    private final MetadataXpp3Reader reader = new MetadataXpp3Reader();
    private final MavenCentralRepository mavenRepo = MavenCentralRepository.getInstance();

    private static DefaultMavenReleaseListProvider instance = new DefaultMavenReleaseListProvider();

    public static DefaultMavenReleaseListProvider getInstance() {
        return instance;
    }

    private DefaultMavenReleaseListProvider(){}

    @Override
    public List<String> getReleases(ArtifactIdent identifier) throws IOException {
        Objects.requireNonNull(identifier);

        try(InputStream xmlInputStream = mavenRepo.openXMLFileInputStream(identifier)) {
            Metadata versionListData = reader.read(new BufferedReader(new InputStreamReader(xmlInputStream)));

            Versioning versioning = versionListData.getVersioning();

            if(versioning != null) {
                List<String> versions = new ArrayList<>();
                for(Object version : versioning.getVersions()) {
                    versions.add((String)version);
                }
                return versions;
            } else {
                return List.of();
            }

        } catch (XmlPullParserException | IOException | FileNotFoundException x) {
            throw new IOException("Failed to obtain version list for " + identifier, x);
        }
    }

}
