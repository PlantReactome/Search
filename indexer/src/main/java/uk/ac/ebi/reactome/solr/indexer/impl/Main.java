package uk.ac.ebi.reactome.solr.indexer.impl;

import uk.ac.ebi.reactome.solr.indexer.exception.IndexerException;

/**
 * Created by flo on 4/30/14.
 */
public class Main {

    public static void main(String[] args)  {

        try {
            Indexer indexer = new Indexer();
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
