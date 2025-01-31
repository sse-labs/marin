package org.tudo.sse.resolution.releases;

import org.tudo.sse.model.ArtifactIdent;

import java.io.IOException;
import java.util.List;

public interface IReleaseListProvider {

    /**
     * Gets the ordered list of releases (i.e. version numbers) for the given identifier. The identifier's version is
     * irrelevant, only the GA tuple is used to obtain a release list.
     *
     * @param identifier Identifier to obtain the release list for (GA-Tuple)
     * @return List of version numbers as ordered by the underlying source
     */
    List<String> getReleases(ArtifactIdent identifier) throws IOException;

}
