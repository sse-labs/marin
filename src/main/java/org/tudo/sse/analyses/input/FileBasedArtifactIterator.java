package org.tudo.sse.analyses.input;

import org.tudo.sse.model.ArtifactIdent;

import java.nio.file.Path;

/**
 * Iterator that reads Maven Central artifact identifiers from a text file. Expects one colon-separated triple of
 * groupID:artifactID:version per line.
 */
public class FileBasedArtifactIterator extends AbstractInputFileIterator<ArtifactIdent> {

    /**
     * Creates a new artifact identifier iterator for the given input file.
     * @param gavList Path to input file
     */
    public FileBasedArtifactIterator(Path gavList) {
        super(gavList);
    }

    @Override
    protected boolean isValidLine(String line) {
        var parts = line.split(":");

        if(parts.length != 3)
            return false;

        for(String part: parts){
            if(part.isBlank())
                return false;
        }

        return true;
    }

    @Override
    protected ArtifactIdent parseLine(String line) {
        var parts = line.split(":");

        return new ArtifactIdent(parts[0], parts[1], parts[2]);
    }
}
