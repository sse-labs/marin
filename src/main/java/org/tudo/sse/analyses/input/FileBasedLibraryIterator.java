package org.tudo.sse.analyses.input;

import java.nio.file.Path;

/**
 * Iterator that reads Maven Central library identifiers from a text file. Expects one colon-separated tuple of
 * groupID:artifactID per line.
 */
public class FileBasedLibraryIterator extends AbstractInputFileIterator<String>{

    /**
     * Creates a new library identifier iterator for the given input file.
     * @param gaList Path to input list
     */
    public FileBasedLibraryIterator(Path gaList) {
        super(gaList);
    }

    @Override
    protected boolean isValidLine(String line) {
        var parts = line.split(":");

        if(parts.length != 2)
            return false;

        for(String part: parts){
            if(part.isBlank())
                return false;
        }

        return true;
    }

    @Override
    protected String parseLine(String line) {
        return line;
    }
}
