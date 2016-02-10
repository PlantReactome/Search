package uk.ac.ebi.reactome.solr.indexer;

import com.martiansoftware.jsap.*;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.gk.persistence.MySQLAdaptor;
import org.reactome.server.tools.interactors.database.InteractorsDatabase;
import uk.ac.ebi.reactome.solr.indexer.exception.IndexerException;
import uk.ac.ebi.reactome.solr.indexer.impl.Indexer;
import uk.ac.ebi.reactome.solr.indexer.util.MailUtil;
import uk.ac.ebi.reactome.solr.indexer.util.PreemptiveAuthInterceptor;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

/**
 * Creates the Solr documents and the ebeye.xml file
 * Created by flo on 4/30/14.
 */
public class IndexerTool {

    private static final Logger logger = Logger.getLogger(IndexerTool.class);
    private static final String FROM = "reactome-indexer@reactome.org";

    public static void main(String[] args) throws JSAPException, SQLException, IndexerException {
        long startTime = System.currentTimeMillis();

        SimpleJSAP jsap = new SimpleJSAP(
                IndexerTool.class.getName(),
                "A tool for generating a Solr Index", //TODO
                new Parameter[]{
                        new FlaggedOption("host", JSAP.STRING_PARSER, "localhost", JSAP.NOT_REQUIRED, 'h', "host",
                        "The database host")
                        , new FlaggedOption("database", JSAP.STRING_PARSER, JSAP.NO_DEFAULT, JSAP.REQUIRED, 'd', "database",
                        "The reactome database name to connect to")
                        , new FlaggedOption("dbuser", JSAP.STRING_PARSER, JSAP.NO_DEFAULT, JSAP.REQUIRED, 'u', "dbuser",
                        "The database user")
                        , new FlaggedOption("dbpassword", JSAP.STRING_PARSER, JSAP.NO_DEFAULT, JSAP.REQUIRED, 'p', "dbpassword",
                        "The password to connect to the database")
                        , new FlaggedOption("solruser", JSAP.STRING_PARSER, JSAP.NO_DEFAULT, JSAP.REQUIRED, 'e', "solruser",
                        "The solr user")
                        , new FlaggedOption("solrpassword", JSAP.STRING_PARSER, JSAP.NO_DEFAULT, JSAP.REQUIRED, 'a', "solrpassword",
                        "The password to connect to solr")
                        , new FlaggedOption("solrurl", JSAP.STRING_PARSER, "http://localhost:8983/solr/reactome", JSAP.REQUIRED, 's', "solrurl",
                        "Url of the running Solr server")
                        , new FlaggedOption("output", JSAP.STRING_PARSER, JSAP.NO_DEFAULT, JSAP.NOT_REQUIRED, 'o', "output",
                        "XML output file for the EBeye")
                        , new FlaggedOption("release", JSAP.STRING_PARSER, JSAP.NO_DEFAULT, JSAP.NOT_REQUIRED, 'r', "release",
                        "Release version number")
                        , new QualifiedSwitch("verbose", JSAP.BOOLEAN_PARSER, null, JSAP.NOT_REQUIRED, 'v', "verbose",
                        "Requests verbose output.")
                        , new FlaggedOption("addInterval", JSAP.INTEGER_PARSER, "1000", JSAP.NOT_REQUIRED, 'i', "addInterval",
                        "Release version number")
                        , new FlaggedOption("mail-smtp", JSAP.STRING_PARSER, "smtp.oicr.on.ca", JSAP.NOT_REQUIRED, 'm', "mail-smtp",
                        "SMTP Mail host")
                        , new FlaggedOption("mail-port", JSAP.INTEGER_PARSER, "25", JSAP.NOT_REQUIRED, 't', "mail-port",
                        "SMTP Mail port")
                        , new FlaggedOption("mail-destination", JSAP.STRING_PARSER, "reactome-developer@reactome.org", JSAP.NOT_REQUIRED, 'f', "mail-destination",
                        "Mail Destination")
                        , new FlaggedOption("interactors-database-path", JSAP.STRING_PARSER, null, JSAP.REQUIRED, 'g', "interactors-database-path",
                        "Interactor Database Path")

                }
        );

        JSAPResult config = jsap.parse(args);
        if (jsap.messagePrinted()) System.exit(1);

        String user = config.getString("solruser");
        String password = config.getString("solrpassword");
        String url = config.getString("solrurl");
        int addInterval = config.getInt("addInterval");

        String release = config.getString("release");
        Boolean verbose = config.getBoolean("verbose");

        MySQLAdaptor dba = new MySQLAdaptor(
                config.getString("host"),
                config.getString("database"),
                config.getString("dbuser"),
                config.getString("dbpassword")
        );

        File output = null;
        if (config.getString("output") != null) { // There was a typo here causing the issue on ebeye.xml wasn't created. Fixed
            output = new File(config.getString("output"));
        }

        SolrClient solrClient = getSolrClient(user, password, url);

        String database = config.getString("interactors-database-path");
        File databaseFile = new File(database);
        if(!databaseFile.exists()){
            throw new IndexerException("Interactor database does not exist");
        }

        InteractorsDatabase interactorsDatabase = null;
        try {
            interactorsDatabase = new InteractorsDatabase(database);
        } catch (SQLException e) {
            e.printStackTrace();
        }

        Indexer indexer = new Indexer(dba, solrClient, addInterval, output, release, verbose, interactorsDatabase);
        MailUtil mail = new MailUtil(config.getString("mail-smtp"), config.getInt("mail-port"));

        try {
            indexer.index();

            long stopTime = System.currentTimeMillis();
            long ms = stopTime - startTime;

            long hour = TimeUnit.MILLISECONDS.toHours(ms);
            long minutes = TimeUnit.MILLISECONDS.toMinutes(ms) -
                    TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(ms));
            long seconds = TimeUnit.MILLISECONDS.toSeconds(ms) -
                    TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(ms));

            if (verbose) {
                System.out.println("Indexing was successful within: " + hour + "hour(s) " + minutes + "minute(s) " + seconds + "second(s) ");
            }

            // Send an email by the end of indexer.
            mail.send(FROM, config.getString("mail-destination"), "[SearchIndexer] The Solr indexer has been created", "The Solr Indexer has ended successfully within: " + hour + "hour(s) " + minutes + "minute(s) " + seconds + "second(s) ");

        } catch (IndexerException e) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            String exceptionAsString = sw.toString();
            StringBuilder body = new StringBuilder();
            body.append("The Solr Indexer has not finished properly. Please check the following exception.\n\n");
            body.append("Message: ").append(e.getMessage());
            body.append("\n");
            body.append("Cause: ").append(e.getCause());
            body.append("\n");
            body.append("Stacktrace: ").append(exceptionAsString);

            // Send an error notification by the end of indexer.
            mail.send(FROM, config.getString("mail-destination"), "[SearchIndexer] The Solr indexer has thrown exception", body.toString() );
        }
    }

    private static SolrClient getSolrClient(String user, String password, String url) {

        if (user != null && !user.isEmpty() && password != null && !password.isEmpty()) {
            HttpClientBuilder builder = HttpClientBuilder.create().addInterceptorFirst(new PreemptiveAuthInterceptor());
            CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(user, password);
            credentialsProvider.setCredentials(AuthScope.ANY, credentials);
            HttpClient client = builder.setDefaultCredentialsProvider(credentialsProvider).build();
            return new HttpSolrClient(url, client);
        }
        return new HttpSolrClient(url);
    }
}
