package org.tudo.sse.analyses.config.parsing;

import org.tudo.sse.CLIException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Interface providing default implementations for parsing command line arguments.
 */
interface CLIParsingUtilities {

    /**
     * Parses the next argument from the given array of arguments as an integer value and returns it.
     *
     * @param args The CLI arguments
     * @param i The current parsing position
     * @return The <b>next</b> argument at position i+1 as an integer value
     * @throws CLIException If there is no next argument, or it is not a valid integer
     */
    default int nextArgAsInt(String[] args, int i) throws CLIException {
        if(i + 1 < args.length) {
            try{
                return Integer.parseInt(args[i + 1]);
            } catch(NumberFormatException e) {
                throw new CLIException(args[i], e.getMessage());
            }
        } else {
            throw new CLIException(args[i]);
        }
    }

    /**
     * Parses the next argument from the given array of arguments as a pair of long values, separated by a colon. The
     * pair is returned as a long-array of size 2.
     *
     * @param args The CLI arguments
     * @param i The current parsing position
     * @return The <b>next</b> argument at position i+1 as a pair of long values
     * @throws CLIException If there is no next argument, or it is not formatted as two long values separated by a colon.
     */
    default long[] nextArgAsLongPair(String[] args, int i) throws CLIException {
        long[] toReturn = new long[2];
        if(i + 1 < args.length) {
            String[] ints = args[i + 1].split(":");
            if(ints.length == 2) {
                try {
                    toReturn[0] = Long.parseLong(ints[0]);
                    toReturn[1] = Long.parseLong(ints[1]);
                } catch(NumberFormatException e) {
                    throw(new CLIException(args[i], e.getMessage()));
                }
            } else {
                throw(new CLIException("Correct format: first:second", args[i]));
            }
        } else {
            throw(new CLIException("Missing argument: first:second", args[i]));
        }
        return toReturn;
    }

    /**
     * Parses the next argument from the given array of arguments as a pair of string values, separated by a colon. The
     * pair is returned as a string-array of size 2.
     * @param args The CLI arguments
     * @param i The current parsing position
     * @return The <b>next</b> argument at position i+1 as a pair of string values
     * @throws CLIException If there is no next argument, or it is not formatted as two string values separated by a colon.
     */
    default String[] nextArgAsStringPair(String[] args, int i) throws CLIException {
        if(i + 1 < args.length) {
            String[] toReturn = args[i + 1].split(":");

            if(toReturn.length != 2) {
                throw new CLIException("Expected a colon-separated pair of strings", args[i]);
            }

            return toReturn;
        } else {
            throw(new CLIException("Missing argument: first:second", args[i]));
        }
    }

    /**
     * Parses the next argument from the given array of arguments as a Path value.
     *
     * @param args The CLI arguments
     * @param i The current parsing position
     * @return The <b>next</b> argument at position i+1 as a Path
     * @throws CLIException If there is no next argument
     */
    default Path nextArgAsPath(String[] args, int i) throws CLIException {
        if(i + 1 < args.length) {
            return Paths.get(args[i + 1]);
        } else {
            throw(new CLIException("Missing argument: path/to/file", args[i]));
        }
    }

    /**
     * Parses the next argument from the given array of arguments as a Path and ensures that it references a regular file.
     *
     * @param args The CLI arguments
     * @param i The current parsing position
     * @return The <b>next</b> argument at position i+1 as a Path that is guaranteed to point to a regular file
     * @throws CLIException If there is no next argument, or it does not point to a file (i.e. directory), or it does not exist
     */
    default Path nextArgAsRegularFileReference(String[] args, int i) throws CLIException {
        final Path path = nextArgAsPath(args, i);
        if(Files.isRegularFile(path))
            return path;
        else
            throw new CLIException("Expected an existing file but got: " + path, args[i]);
    }

    /**
     * Parses the next argument from the given array of arguments as a Path and ensures that it references a directory.
     * @param args The CLI arguments
     * @param i The current parsing position
     * @return The <b>next</b> argument at position i+1 as a Path that is guaranteed to point to a directory
     * @throws CLIException If there is no next argument, or it does not point to a directory, or it does not exist
     */
    default Path nextArgAsDirectoryReference(String[] args, int i) throws CLIException {
        final Path path = nextArgAsPath(args, i);
        if(Files.isDirectory(path))
            return path;
        else
            throw new CLIException("Expected an existing directory but got: " + path, args[i]);
    }

}
