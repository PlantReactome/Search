package uk.ac.ebi.reactome.solr.indexer.impl;

/**
 * Created by flo on 4/29/14.
 */

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.MySQLAdaptor;
import uk.ac.ebi.reactome.solr.indexer.exception.IndexerException;
import uk.ac.ebi.reactome.solr.indexer.model.IndexDocument;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Properties;

/**
 * Created by flo on 3/27/14.
 */
public class Indexer {

    private static SolrServer solrServer;
    private static MySQLAdaptor dba;
    private Converter converter;
    private Marshaller marshaller;

    private static final Logger logger = Logger.getRootLogger();
    private Properties solrProperties;
    private Properties databaseProperties;
    private Properties marshallerProperties;

    private final int commitInterval ;
    private final int numberOfTries;


    private void loadProperties() {
        solrProperties = new Properties();
        databaseProperties = new Properties();
//        marshallerProperties = new Properties();
        try {
            solrProperties.load(getClass().getResourceAsStream("/solr.properties"));
            databaseProperties.load(getClass().getResourceAsStream("/database.properties"));
//            marshallerProperties.load(getClass().getResourceAsStream("/marshaller.properties"));
        } catch (FileNotFoundException e) {
            logger.error("no property file found", e);
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
    }

    public Indexer() throws IndexerException {

        logger.setLevel(Level.WARN);
        logger.addAppender(new ConsoleAppender(new PatternLayout("%-6r [%p] %c - %m%n")));

        loadProperties();
        commitInterval = Integer.parseInt(solrProperties.getProperty("commitInterval"));
        numberOfTries = Integer.parseInt(solrProperties.getProperty("numberOfTries"));


        initializeMysql();
        initializeSolrServer();
//        initializeMarshaller();


        converter = new Converter();
    }

    public void index()  {

        try {
            cleanSolrIndex();
//            marshaller.writeHeader();
            int entriesCount = 0;
            entriesCount += indexSchemaClass(ReactomeJavaConstants.Regulation);
            entriesCount += indexSchemaClass(ReactomeJavaConstants.Event);
            entriesCount += indexSchemaClass(ReactomeJavaConstants.PhysicalEntity);
//            marshaller.writeFooter(entriesCount);
            closeSolrServer();
        } catch (Exception e) {
            logger.error(e.getMessage(),e);
        }
    }

    /**
     * Takes all Entries of a Reactome Class (Event or Physical Entity), converts and indexes it
     * @param className
     * @throws Exception
     */
    private int indexSchemaClass(String className) throws Exception {

        Collection<GKInstance> instances = dba.fetchInstancesByClass(className);

        int numberOfDocuments = 0;
        for (GKInstance instance : instances) {
            IndexDocument document = converter.buildDocumentFromGkInstance(instance);
            addDocumentToSolrServer(document);
//            marshaller.writeEntry(document);
            numberOfDocuments ++;
            if (numberOfDocuments % commitInterval == 0) {
                commitSolrServer();
//                optimizeSolrServer(); overkill
            }

        }
        commitSolrServer();
        return numberOfDocuments;
    }




    /**
     * Safely adding Document Bean to Solr Server
     * @param document
     * @throws IndexerException
     */
    private void addDocumentToSolrServer (IndexDocument document) throws IndexerException {

        if (solrServer != null && document != null) {
            try {
                solrServer.addBean(document);
            } catch (Exception e) {
                int numberOfTries = 3;
                boolean unsuccessfulAdd = true;

                while (numberOfTries <= this.numberOfTries && unsuccessfulAdd){
                    try {
                        solrServer.addBean(document);
                        unsuccessfulAdd = false;
                    } catch (Exception e2) {
                        numberOfTries++;
                    }
                }
                if (unsuccessfulAdd){
                    logger.error("Could not add document");
                    throw new IndexerException("Could not add document", e);
                }
            }
        }
        else {
            logger.error("SolrServer or Document is null");
        }
    }

    /**
     * Cleaning Solr Server (removes all current Data)
     * @throws IOException
     * @throws SolrServerException
     * @throws IndexerException
     */
    private void cleanSolrIndex() throws IndexerException {

        if (solrServer != null ){
            try {
                solrServer.deleteByQuery("*:*");
            } catch (SolrServerException e) {
                logger.error("could not Delete Solr Data solr Exception", e);

            } catch (IOException e) {
                logger.error("could not Delete Solr Data IO Exception", e);
            }
            commitSolrServer();
            optimizeSolrServer();
            logger.info("SolrServer cleaned");
        }
        else {
            logger.error("SolrServer Should not be Null");
        }
    }

    /**
     * Closes connection to Solr Server
     * @throws IndexerException (if commit was not successful)
     */
    private void closeSolrServer() throws IndexerException {

        if (solrServer != null ){
            commitSolrServer();
            optimizeSolrServer(); // Important (used for creating the dictionaries - spell checking and suggestions)
            solrServer.shutdown();
            logger.info("SolrServer shutdown");
        }
        else {
            logger.error("SolrServer Should not be Null");
        }
    }

    /**
     * Commits Data that has been added till now to Solr Server
     * @throws IndexerException not comiting could mean that this Data will not be added to Solr
     */
    private void commitSolrServer() throws IndexerException {
        if (solrServer != null ){
            try {
                solrServer.commit();
            } catch (Exception e) {
                int numberOfTries = 1;
                boolean unsuccessfulCommit = true;

                while (numberOfTries <= this.numberOfTries && unsuccessfulCommit){
                    try {
                        solrServer.commit();
                        unsuccessfulCommit = false;
                    } catch (Exception e2) {
                        numberOfTries++;
                    }
                }
                if (unsuccessfulCommit){
                    logger.error("Could not commit", e);
                    throw new IndexerException("Could not commit", e);
                }
            }
        }
    }

    /**
     * Optimizes the Solr Server (deletes unused and outdated Data)
     * throws no Exception Solr should work fine without optimization
     */
    private void optimizeSolrServer() throws IndexerException {

        if (solrServer != null){
            try {
                solrServer.optimize();
            } catch (Exception e) {
                int numberOfTries = 1;
                boolean unsuccessfulOptimize = true;
                while (numberOfTries <= this.numberOfTries && unsuccessfulOptimize){
                    try {
                        solrServer.optimize();
                        unsuccessfulOptimize = false;
                    } catch (Exception e2) {
                        numberOfTries++;
                    }
                }
                if (unsuccessfulOptimize){
                    logger.error("Could not Optimize SolrServer");
                    throw new IndexerException("Could not optimize", e);
                }
            }
        }

    }

    /**
     * InitializesSolrServer
     * 1 retry to establish connection to server (Solr documentation advices not more than 1)
     * 5 seconds to establish TCP
     * 1 seconds socket read timeout (will mess up commits and optimize because of large spellcheck dictionary!)
     * 100 Total Connections
     * will not follow redirects (default)
     */
    private void initializeSolrServer()  {

        HttpSolrServer server = new HttpSolrServer(solrProperties.getProperty("url"));
//        keep Default values do only alter if necessary!
//        server.setMaxRetries                    (Integer.parseInt(solrProperties.getProperty("maxRetries")));
//        server.setConnectionTimeout             (Integer.parseInt(solrProperties.getProperty("connectionTimeout")));
//        server.setSoTimeout                     (Integer.parseInt(solrProperties.getProperty("soTimeout")));
//        server.setDefaultMaxConnectionsPerHost  (Integer.parseInt(solrProperties.getProperty("maxConnectionsPerHost")));
//        server.setMaxTotalConnections           (Integer.parseInt(solrProperties.getProperty("maxConnectionsTotal")));
//        server.setFollowRedirects               (Boolean.parseBoolean(solrProperties.getProperty("followRedirects")));
        solrServer = server;
    }

    /**
     * Initializes the Mysql Server (via Reactome Adapter)
     * @throws IndexerException
     */
    private void initializeMysql() throws IndexerException {
        try {
            dba = new MySQLAdaptor( databaseProperties.getProperty("host"),
                                    databaseProperties.getProperty("database"),
                                    databaseProperties.getProperty("user"),
                                    databaseProperties.getProperty("password"),
                                    Integer.parseInt(databaseProperties.getProperty("port")));
        } catch (SQLException e) {
            logger.error("Could not initiate MySQLAdapter", e);
            throw new IndexerException(e.getMessage(), e);
        }
    }

    private void initializeMarshaller() {
        marshaller = new Marshaller(marshallerProperties.getProperty("fileName"),
                                    marshallerProperties.getProperty("name"),
                                    marshallerProperties.getProperty("description"),
                                    marshallerProperties.getProperty("release"));
    }

}


