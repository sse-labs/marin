package org.tudo.sse.utils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import org.tudo.sse.resolution.FileNotFoundException;

/**
 * This class handles opening connections for pom and jar resolution.
 * This is done through the implementation of HttpURLConnection.
 */
public class ResourceConnections {

    /**
     * This method attempts to open a connection to a given url, handling fileNotFound and redirect response codes.
     *
     * @param toOpen url to open connection
     * @return a connection to the requested resource
     * @throws IOException when there is an issue opening a file
     * @throws FileNotFoundException handles errors that occur when the file to process isn't found
     * @throws NullPointerException handles errors that occur when a null pointer is accessed
     */
    public static HttpURLConnection openConnection(final URI toOpen) throws IOException, FileNotFoundException, NullPointerException {
        HttpURLConnection conn = (HttpURLConnection) toOpen.toURL().openConnection();
        conn.setConnectTimeout(2 * 1000);
        conn.setReadTimeout(5 * 1000);

        while (conn.getResponseCode() == 308) {
            conn = (HttpURLConnection) new URL(conn.getHeaderField("Location")).openConnection();
        }

        if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
            conn.disconnect();
            if(conn.getResponseCode() == HttpURLConnection.HTTP_NOT_FOUND)
                throw new FileNotFoundException(toOpen.toURL());
            else
                throw new IOException("Error accessing resource: " + conn.getURL().toString() + " (Code " + conn.getResponseCode() + ")");
        }


        conn.connect();
        return conn;
    }

    /**
     * This method attempts to open an inputStream
     * from the connection opened in the openConnection method.
     *
     * @param toOpen url to open
     * @return an inputStream to the requested resource
     * @throws IOException when there is an issue opening a file
     * @throws FileNotFoundException handles errors that occur when the file to process isn't found*/
    public static InputStream openInputStream(final URI toOpen) throws IOException, FileNotFoundException {
        try {
            HttpURLConnection con = openConnection(toOpen);
            InputStream conStream = con.getInputStream();
            byte[] allBytes = conStream.readAllBytes();
            conStream.close();
            con.disconnect();
            return new ByteArrayInputStream(allBytes);
        } catch (NullPointerException e) {
            return null;
        }
    }
}
