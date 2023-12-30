package com.newrelic.example;

import java.io.IOException;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Collection;
import java.util.Collections;

// New Relic API imports
import com.newrelic.api.agent.DatastoreParameters;
import com.newrelic.api.agent.HeaderType;
import com.newrelic.api.agent.Headers;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.TracedMethod;
import com.newrelic.api.agent.Transaction;
import com.newrelic.api.agent.TransportType;

import fi.iki.elonen.NanoHTTPD;

public class NewRelicApiServer extends NanoHTTPD {

    public NewRelicApiServer() throws IOException, URISyntaxException {
        super(8081);

        // Set Dispatcher name and version
        NewRelic.setServerInfo("NanoHttp", "2.3.1");
        // Set Appserver port for jvm identification
        NewRelic.setAppServerPort(8081);
        // Set JVM instance name
        NewRelic.setInstanceName("Server");

        start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
        System.out.println("\nRunning on http://localhost:8081/ \n");
    }

    public static void main(String[] args) throws URISyntaxException {
        try {
            new NewRelicApiServer();
        } catch (IOException ioe) {
            System.err.println("Unable to start the server:\n" + ioe);
        }
    }

    @Trace(dispatcher = true)
    @Override
    public Response serve(IHTTPSession session) {
        // Obtain a reference to the current Transaction
        Transaction tx = NewRelic.getAgent().getTransaction();
        // Set the name of the current transaction
        NewRelic.setTransactionName("Custom", "ExternalHTTPServer");

        // Wrap the Request object
        Headers req = new HeadersWrapper(session);

        // Set the request for the current transaction and convert it into a web transaction
        tx.acceptDistributedTraceHeaders(TransportType.HTTP, req);

        queryDB();

        return newFixedLengthResponse("<html><body><h1>SuccessfulResponse</h1>\n</body></html>\n");
    }

    // Implement Headers interface to create a wrapper for the outgoing Request/incoming Response headers
    static class HeadersWrapper implements Headers {
        private final IHTTPSession delegate;

        public HeadersWrapper(IHTTPSession request) {
            this.delegate = request;
        }

        @Override
        public HeaderType getHeaderType() {
            return HeaderType.HTTP;
        }

        @Override
        public String getHeader(String name) {
            return delegate.getHeaders().get(name);
        }

        @Override
        public Collection<String> getHeaders(String name) {
            return Collections.singletonList(getHeader(name));
        }

        @Override
        public void setHeader(String name, String value) {
            delegate.getHeaders().put(name, value);
        }

        @Override
        public void addHeader(String name, String value) {
            delegate.getHeaders().put(name, value);
        }

        @Override
        public Collection<String> getHeaderNames() {
            return delegate.getHeaders().keySet();
        }

        @Override
        public boolean containsHeader(String name) {
            return delegate.getHeaders().containsKey(name);
        }
    }

    @Trace
    public void queryDB() {
        Connection c = null;
        Statement stmt = null;
        try {
            Class.forName("org.sqlite.JDBC");
            c = DriverManager.getConnection("jdbc:sqlite:/tmp/test.db");
            c.setAutoCommit(false);
            System.out.println("Opened database successfully");

            stmt = c.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM COMPANY;");
            while (rs.next()) {
                int id = rs.getInt("id");
                String name = rs.getString("name");
                int age = rs.getInt("age");
                String address = rs.getString("address");
                float salary = rs.getFloat("salary");
                System.out.println("ID = " + id);
                System.out.println("NAME = " + name);
                System.out.println("AGE = " + age);
                System.out.println("ADDRESS = " + address);
                System.out.println("SALARY = " + salary);
                System.out.println();
            }
            rs.close();
            stmt.close();
            c.close();
        } catch (Exception e) {
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
            System.exit(0);
        }
        // Obtain a reference to the method currently being traced
        TracedMethod method = NewRelic.getAgent().getTracedMethod();

        // Create a DatastoreParameters object and report a call to an external datastore service
        method.reportAsExternal(
                DatastoreParameters
                        .product("sqlite")
                        .collection("test.db")
                        .operation("select")
                        .instance("localhost", 8080)
                        .databaseName("test.db")
                        .build());
    }
}
