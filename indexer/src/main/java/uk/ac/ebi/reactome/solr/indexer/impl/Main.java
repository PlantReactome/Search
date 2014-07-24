package uk.ac.ebi.reactome.solr.indexer.impl;

import uk.ac.ebi.reactome.solr.indexer.exception.IndexerException;

/**
 * Created by flo on 4/30/14.
 */
public class Main {

    public static void main(String[] args)  {

        try {
            Indexer i = new Indexer();
            long startTime = System.currentTimeMillis();

            i.index();

            long stopTime = System.currentTimeMillis();
            System.out.println("Time elapsed :" + (stopTime-startTime));
        } catch (IndexerException e) {
            e.printStackTrace();
        }

    }
}
