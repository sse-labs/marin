package org.tudo.sse.utils;

import org.tudo.sse.resolution.FileNotFoundException;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;

/**
 * This class handles opening connections for pom and jar resolution. This is done through the implementation of HttpURLConnection.
 */
public class ResourceConnections {

    /**
     * This method attempts to open a connection to a given url, handling fileNotFound and redirect response codes.
     *
     * @param toOpen url to open connection
     * @return a connection to the requested resource
     * @throws IOException when there is an issue opening a file
     * @throws FileNotFoundException
     * @throws NullPointerException
     */
    public static HttpURLConnection openConnection(URI toOpen) throws IOException, FileNotFoundException, NullPointerException {

        HttpURLConnection conn = (HttpURLConnection) toOpen.toURL().openConnection();

        if (conn.getResponseCode() == HttpURLConnection.HTTP_NOT_FOUND) {
            throw new FileNotFoundException(toOpen.toURL());
        }

        while (conn.getResponseCode() == 308) {
            System.out.println(conn.getHeaderField("Location"));
            conn = (HttpURLConnection) new URL(conn.getHeaderField("Location")).openConnection();
        }

        conn.connect();
        return conn;
    }

    /**
     * This method attempts to open an inputStream from the connection opened in the openConnection method.
     * @param toOpen url to open
     * @return an inputStream to the requested resource
     * @throws IOException when there is an issue opening a file
     * @throws FileNotFoundException
     */
    public static InputStream openInputStream(URI toOpen) throws IOException, FileNotFoundException {
        try {
            return openConnection(toOpen).getInputStream();
        } catch(NullPointerException e) {
            return null;
        }
    }
}
