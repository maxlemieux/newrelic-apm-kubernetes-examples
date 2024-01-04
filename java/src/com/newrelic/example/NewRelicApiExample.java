package com.newrelic.example;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

// New Relic API imports
import com.newrelic.api.agent.ExternalParameters;
import com.newrelic.api.agent.HeaderType;
import com.newrelic.api.agent.Headers;
import com.newrelic.api.agent.HttpParameters;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.TracedMethod;
import com.newrelic.api.agent.Transaction;

import fi.iki.elonen.NanoHTTPD;
import org.apache.http.HttpMessage;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

public class NewRelicApiExample extends NanoHTTPD {

    public NewRelicApiExample() throws IOException, URISyntaxException {
        super(8080);

        // Set Dispatcher name and version
        NewRelic.setServerInfo("NanoHttp", "2.3.1");
        // Set Appserver port for JVM identification
        NewRelic.setAppServerPort(8080);
        // Set JVM instance name
        NewRelic.setInstanceName("Client");

        start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
        System.out.println("Running at: http://localhost:8080/");
    }

    public static void main(String[] args) throws URISyntaxException {
        try {
            new NewRelicApiExample();
        } catch (IOException ioe) {
            System.err.println("Unable to start the server:\n" + ioe);
        }
    }

    @Trace(dispatcher = true)
    @Override
    public Response serve(IHTTPSession session) {
        System.out.println("Received request for URI: " + session.getUri());
        URI uri = null;
        int status = 0;

        try {
            createDB();
            Thread.sleep(1000);
            uri = new URI("http://java-example-newrelic-example-server:8081");
            status = makeExternalCall(uri);
        } catch (URISyntaxException | InterruptedException | IOException e) {
            System.err.println("Error during request handling: " + e.getMessage());
            e.printStackTrace();
        }

        if (status == 200) {
            System.out.println("Returning successful response.");
            return newFixedLengthResponse("<html><body><h1>Successful Response</h1>\n</body></html>\n");
        } else {
            System.err.println("Returning error response with status: " + status);
            return newFixedLengthResponse("<html><body><h1>Error\n" + status + "</h1>\n</body></html>\n");
        }
    }

    @Trace
    public int makeExternalCall(URI uri) throws IOException {
        System.out.println("Making external call to URI: " + uri);
        HttpUriRequest request = RequestBuilder.get().setUri(uri).build();

        // Wrap the outbound Request object
        Headers outboundHeaders = new HeadersWrapper(request);

        // Obtain a reference to the current transaction
        Transaction transaction = NewRelic.getAgent().getTransaction();
        // Add headers for outbound external request
        transaction.insertDistributedTraceHeaders(outboundHeaders);

        CloseableHttpClient connection = HttpClientBuilder.create().build();
        CloseableHttpResponse response = connection.execute(request);

        // Wrap the incoming Response object
        Headers inboundHeaders = new HeadersWrapper(response);

        // Create an input parameter object for a call to an external HTTP service
        ExternalParameters params = HttpParameters
            .library("HttpClient")
            .uri(uri)
            .procedure("execute")
            .inboundHeaders(inboundHeaders)
            .build();

        // Obtain a reference to the method currently being traced
        TracedMethod tracedMethod = NewRelic.getAgent().getTracedMethod();
        // Report a call to an external HTTP service
        tracedMethod.reportAsExternal(params);
        int statusCode = response.getStatusLine().getStatusCode();
        System.out.println("External call to URI " + uri + " returned status code: " + statusCode);

        return statusCode;
    }

    // Implement Headers interface to create a wrapper for the outgoing Request/incoming Response headers
    static class HeadersWrapper implements Headers {
        private final HttpMessage delegate;

        public HeadersWrapper(HttpMessage request) {
            this.delegate = request;
        }

        @Override
        public void setHeader(String name, String value) {
            delegate.setHeader(name, value);
        }

        @Override
        public HeaderType getHeaderType() {
            return HeaderType.HTTP;
        }

        @Override
        public String getHeader(String name) {
            return delegate.getFirstHeader(name).getValue();
        }

        @Override
        public Collection<String> getHeaders(String name) {
            return Arrays.stream(delegate.getHeaders(name))
                .map(NameValuePair::getValue)
                .collect(Collectors.toList());
        }

        @Override
        public void addHeader(String name, String value) {
            delegate.addHeader(name, value);
        }

        @Override
        public Collection<String> getHeaderNames() {
            return Arrays.stream(delegate.getAllHeaders())
                .map(NameValuePair::getName)
                .collect(Collectors.toSet());
        }

        @Override
        public boolean containsHeader(String name) {
            return Arrays.stream(delegate.getAllHeaders())
                .map(NameValuePair::getName)
                .anyMatch(headerName -> headerName.equals(name));
        }
    }

    @Trace
    public void createDB() {
        System.out.println("Creating or resetting the database.");
        Connection c = null;
        Statement stmt = null;

        try {
            Class.forName("org.sqlite.JDBC");
            c = DriverManager.getConnection("jdbc:sqlite:/tmp/test.db");
            System.out.println("Opened database successfully");

            stmt = c.createStatement();

            String dropSql = "DROP TABLE IF EXISTS COMPANY;";
            stmt.executeUpdate(dropSql);
            System.out.println("Dropped existing COMPANY table if it existed.");

            String sql = "CREATE TABLE COMPANY " +
                    "(ID INT PRIMARY KEY     NOT NULL," +
                    " NAME           TEXT    NOT NULL, " +
                    " AGE            INT     NOT NULL, " +
                    " ADDRESS        CHAR(50), " +
                    " SALARY         REAL)";
            stmt.executeUpdate(sql);

            sql = "INSERT INTO COMPANY (ID,NAME,AGE,ADDRESS,SALARY) " +
                   "VALUES (1, 'Paul', 32, 'California', 20000.00 );";
            stmt.executeUpdate(sql);

            sql = "INSERT INTO COMPANY (ID,NAME,AGE,ADDRESS,SALARY) " +
                    "VALUES (2, 'Allen', 25, 'Texas', 15000.00 );";
            stmt.executeUpdate(sql);

            sql = "INSERT INTO COMPANY (ID,NAME,AGE,ADDRESS,SALARY) " +
            "VALUES (3, 'Teddy', 23, 'Norway', 20000.00 );";
            stmt.executeUpdate(sql);

            sql = "INSERT INTO COMPANY (ID,NAME,AGE,ADDRESS,SALARY) " +
                    "VALUES (4, 'Mark', 25, 'Rich-Mond ', 65000.00 );";
            stmt.executeUpdate(sql);
            System.out.println("Inserted sample data into COMPANY table.");

            stmt.close();
            c.close();
        } catch (Exception e) {
            System.err.println("Error creating or resetting the database: " + e.getMessage());

            System.err.println(e.getClass().getName() + ": " + e.getMessage());
            System.exit(0);
        }
    }
}
