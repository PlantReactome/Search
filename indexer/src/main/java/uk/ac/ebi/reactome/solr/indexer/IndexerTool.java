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
import uk.ac.ebi.reactome.solr.indexer.exception.IndexerException;
import uk.ac.ebi.reactome.solr.indexer.impl.Indexer;
import uk.ac.ebi.reactome.solr.indexer.util.PreemptiveAuthInterceptor;

import java.io.File;
import java.sql.SQLException;

/**
 * Creates the Solr documents and the ebeye.xml file
 * Created by flo on 4/30/14.
 */
public class IndexerTool {

    private static final Logger logger = Logger.getLogger(IndexerTool.class);

    public static void main(String[] args) throws JSAPException, IndexerException, SQLException {
        long startTime = System.currentTimeMillis();

        SimpleJSAP jsap = new SimpleJSAP(
                IndexerTool.class.getName(),
                "A tool for generating a Solr Index", //TODO
                new Parameter[] {
                        new FlaggedOption( "host", JSAP.STRING_PARSER, "localhost", JSAP.NOT_REQUIRED, 'h', "host",
                                "The database host")
                        ,new FlaggedOption( "database", JSAP.STRING_PARSER, JSAP.NO_DEFAULT, JSAP.REQUIRED, 'd', "database",
                        "The reactome database name to connect to")
                        ,new FlaggedOption( "dbuser", JSAP.STRING_PARSER, JSAP.NO_DEFAULT, JSAP.REQUIRED, 'u', "dbuser",
                        "The database user")
                        ,new FlaggedOption( "dbpassword", JSAP.STRING_PARSER, JSAP.NO_DEFAULT, JSAP.REQUIRED, 'p', "dbpassword",
                        "The password to connect to the database")
                        ,new FlaggedOption( "solruser", JSAP.STRING_PARSER, JSAP.NO_DEFAULT, JSAP.REQUIRED, 'e', "solruser",
                        "The solr user")
                        ,new FlaggedOption( "solrpassword", JSAP.STRING_PARSER, JSAP.NO_DEFAULT, JSAP.REQUIRED, 'a', "solrpassword",
                        "The password to connect to solr")
                        ,new FlaggedOption( "solrurl", JSAP.STRING_PARSER, "localhost:8983/solr/reactome", JSAP.REQUIRED, 's', "solrurl",
                        "Url of the running Solr server")
                        ,new FlaggedOption( "output", JSAP.STRING_PARSER, JSAP.NO_DEFAULT, JSAP.NOT_REQUIRED, 'o', "output",
                        "XML output file for the EBeye" )
                        ,new FlaggedOption( "release", JSAP.STRING_PARSER, JSAP.NO_DEFAULT, JSAP.NOT_REQUIRED, 'r', "release",
                        "Release version number" )
                        ,new QualifiedSwitch( "verbose", JSAP.BOOLEAN_PARSER, null, JSAP.NOT_REQUIRED, 'v', "verbose",
                        "Requests verbose output." )
                        ,new FlaggedOption( "addInterval", JSAP.INTEGER_PARSER, "100", JSAP.NOT_REQUIRED, 'i', "addInterval",
                        "Release version number" )
                }
        );

        JSAPResult config = jsap.parse(args);
        if( jsap.messagePrinted() ) System.exit( 1 );

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
        if (config.getString("outout") != null) {
            output = new File(config.getString("output"));
        }

        SolrClient solrClient = getSolrClient(user,password,url);

        Indexer indexer = new Indexer(dba, solrClient, addInterval, output, release, verbose);

        indexer.index();
        long stopTime = System.currentTimeMillis();
        long ms =  stopTime-startTime;
        int seconds = (int) (ms / 1000) % 60 ;
        int minutes = (int) ((ms / (1000*60)) % 60);

        if (verbose) {
            System.out.println("Indexing was successful within: " + minutes + "minutes " + seconds + "seconds ");
        }
    }

    private static SolrClient getSolrClient(String user, String password, String url) {

        if(user!=null && !user.isEmpty() && password!=null && !password.isEmpty()) {
            HttpClientBuilder builder = HttpClientBuilder.create().addInterceptorFirst(new PreemptiveAuthInterceptor());
            CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(user, password);
            credentialsProvider.setCredentials(AuthScope.ANY, credentials);
            HttpClient client = builder.setDefaultCredentialsProvider(credentialsProvider).build();
            return new HttpSolrClient(url,client);
        }
        return new  HttpSolrClient(url);
    }
}
