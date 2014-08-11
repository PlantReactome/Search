package uk.ac.ebi.reactome.solr.indexer;

import com.martiansoftware.jsap.*;
import uk.ac.ebi.reactome.solr.indexer.exception.IndexerException;

/**
 * Created by flo on 4/30/14.
 */
public class IndexerTool {

    public static void main(String[] args) throws JSAPException {
        SimpleJSAP jsap = new SimpleJSAP(
                IndexerTool.class.getName(),
                "A tool for ", //TODO
                new Parameter[] {
                        new FlaggedOption( "host", JSAP.STRING_PARSER, "localhost", JSAP.NOT_REQUIRED, 'h', "host",
                        "The database host")
                        ,new FlaggedOption( "database", JSAP.STRING_PARSER, JSAP.NO_DEFAULT, JSAP.REQUIRED, 'd', "database",
                        "The reactome database name to connect to")
                        ,new FlaggedOption( "username", JSAP.STRING_PARSER, JSAP.NO_DEFAULT, JSAP.REQUIRED, 'u', "username",
                        "The database user")
                        ,new FlaggedOption( "password", JSAP.STRING_PARSER, JSAP.NO_DEFAULT, JSAP.REQUIRED, 'p', "password",
                        "The password to connect to the database")
                        ,new FlaggedOption( "input", JSAP.STRING_PARSER, JSAP.NO_DEFAULT, JSAP.REQUIRED, 'c', "cv",
                        "" ) //TODO
                        ,new FlaggedOption( "output", JSAP.STRING_PARSER, JSAP.NO_DEFAULT, JSAP.REQUIRED, 'o', "output",
                        "XML file for the EBeye" ) //TODO
                        ,new QualifiedSwitch( "verbose", JSAP.BOOLEAN_PARSER, null, JSAP.NOT_REQUIRED, 'v', "verbose",
                        "Requests verbose output." )
                }
        );
        JSAPResult config = jsap.parse(args);
        if( jsap.messagePrinted() ) System.exit( 1 );

//        MySQLAdaptor dba = new MySQLAdaptor(
//                config.getString("host"),
//                config.getString("database"),
//                config.getString("username"),
//                config.getString("password")
//        );

        try {
            uk.ac.ebi.reactome.solr.indexer.impl.Indexer indexer = new uk.ac.ebi.reactome.solr.indexer.impl.Indexer();
            long startTime = System.currentTimeMillis();
            indexer.index();
            long stopTime = System.currentTimeMillis();
            long ms =  stopTime-startTime;
            long seconds = ms / 1000;
            long minutes =  seconds / 60;
            long hours =  minutes / 60;
            System.out.println("Indexing was successful within: " + hours +"hours " + minutes + "minutes " + seconds + "seconds " );
        } catch (IndexerException e) {
            e.printStackTrace();
        }
    }
}
