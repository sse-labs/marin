package org.tudo.sse.analyses.input;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;

/**
 * An iterator that wraps an underlying file that contains line-wise representations of entities. This wrapper produces
 * one entity per line, with parsing logic defined by concrete subclasses.
 *
 * @param <T> The type that entities are parsed to
 *
 * @author Johannes Düsing
 */
abstract class AbstractInputFileIterator<T> implements Iterator<T> {

    /**
     * Checks whether a given line within the input file represents a valid entity
     * @param line The line to check
     * @return True is the line represents a valid entity, false otherwise
     */
    protected abstract boolean isValidLine(String line);

    /**
     * Parses a given line into its representing entity. Will only ever be called after isValidLine returned true.
     * @param line The line to parse
     * @return The resulting entity
     */
    protected abstract T parseLine(String line);

    private final Path inputPath;

    private Iterator<String> lineIterator;

    /**
     * Creates a new iterator instance for the given file path.
     * @param input Path to a text file containing one entity per line
     */
    protected AbstractInputFileIterator(Path input){
        this.inputPath = input;
    }

    /**
     * Checks whether the underlying file exists and contains only valid entities.
     * @throws IOException If accessing the underlying file fails
     * @throws IllegalArgumentException If the underlying file contents are not valid
     */
    public void validateInput() throws IOException, IllegalArgumentException {
        var lines = Files.readAllLines(inputPath);
        int lineCnt = 0;

        for(String line: lines){
            if(!isValidLine(line)){
                throw new IllegalArgumentException("Not a valid entity: " + line + " (" + inputPath.getFileName() + ":" + lineCnt + ")");
            }

            lineCnt += 1;
        }

        this.lineIterator = lines.iterator();
    }

    @Override
    public boolean hasNext() {
        if(this.lineIterator == null){
            try {
                var lines = Files.readAllLines(this.inputPath);
                this.lineIterator = lines.iterator();
            } catch (IOException iox){
                throw new IllegalArgumentException("Failed to access input file", iox);
            }
        }

        return this.lineIterator.hasNext();
    }

    @Override
    public T next() {
        if(!hasNext())
            throw new IllegalStateException("Next on empty iterator");

        String line = this.lineIterator.next();

        if(isValidLine(line))
            return parseLine(line);
        else
            throw new IllegalStateException("Not a valid entity: " + line);
    }

}
