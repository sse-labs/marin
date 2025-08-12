package org.tudo.sse.utils;

import org.apache.maven.index.reader.ResourceHandler;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.TimeUnit;

/**
 * An implementation of the ResourceHandler class required for processing the Maven Central Index. This specific class
 * uses HTTP resources to access the index.
 */
public class HttpResourceHandler implements ResourceHandler {

    private final URI root;

    private final int BUFFER_SIZE = 10 * 1024 * 1024; // Buffer 10MB

    /**
     * Creates a new HttpResourceHandler for the given root URI
     * @param root The root URI, typically the Maven Central Index URI
     */
    public HttpResourceHandler(URI root) {
        this.root = root;
    }

    @Override
    public Resource locate(String name) {
        return new HttpResource(name);
    }

    /**
     * HTTP based implementation of the Resource class required for processing the Maven Central Index.
     */
    public class HttpResource implements Resource {

        private final String name;

        /**
         * Creates a new resource with the given name.
         * @param name Resource name
         */
        public HttpResource(String name) {
            this.name = name;
        }

        @Override
        public InputStream read() throws IOException {
            URL target = root.resolve(name).toURL();

            HttpURLConnection conn = (HttpURLConnection) target.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Connection", "keep-alive");
            conn.connect();

            return new BufferedInputStream(conn.getInputStream(), BUFFER_SIZE);
        }

    }

}
