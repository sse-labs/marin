package org.tudo.sse.multiThreading;

import org.tudo.sse.model.ArtifactIdent;
import org.tudo.sse.CliInformation;

import java.util.List;

/**
 * This class is a message to be sent between the Index and Queue actors, allowing the config information for the resolvers to be accessible as well as the list of identifiers to resolve.
 */
public class ConfigPlusList {
    private final List<ArtifactIdent> idents;
    private final CliInformation setUpInfo;

    public ConfigPlusList(List<ArtifactIdent> idents, CliInformation setUpInfo) {
        this.idents = idents;
        this.setUpInfo = setUpInfo;
    }

    public List<ArtifactIdent> getIdents() {
        return idents;
    }

    public CliInformation getSetUpInfo() {
        return setUpInfo;
    }
}
