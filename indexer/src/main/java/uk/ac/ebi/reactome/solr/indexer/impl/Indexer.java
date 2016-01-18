package uk.ac.ebi.reactome.solr.indexer.impl;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.MySQLAdaptor;
import org.reactome.server.tools.interactors.exception.InvalidInteractionResourceException;
import org.reactome.server.tools.interactors.model.Interaction;
import org.reactome.server.tools.interactors.model.Interactor;
import org.reactome.server.tools.interactors.service.InteractionService;
import org.reactome.server.tools.interactors.service.InteractorService;
import uk.ac.ebi.reactome.solr.indexer.exception.IndexerException;
import uk.ac.ebi.reactome.solr.indexer.model.IndexDocument;
import uk.ac.ebi.reactome.solr.indexer.model.InteractorSummary;
import uk.ac.ebi.reactome.solr.indexer.model.ReactomeSummary;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;

/**
 * This class is responsible for establishing connection to Solr
 * and the MySQL adapter. It iterates through the collection of
 * GkInstances returned by the MySQL adapter for a given SchemaClass
 * and adds IndexDocuments in batches to the Solr Server
 *
 * @author Florian Korninger (fkorn@ebi.ac.uk)
 * @version 1.0
 */
public class Indexer {
    private static final Logger logger = Logger.getLogger(Indexer.class);

    private static SolrClient solrClient;
    private static MySQLAdaptor dba;
    private Converter converter;
    private Marshaller marshaller;

    private int addInterval;
    private Boolean verbose;
    private Boolean xml;
    private static final String CONTROLLED_VOCABULARY = "controlledvocabulary.csv";
    private static final String EBEYE_NAME = "Reactome";
    private static final String EBEYE_DESCRIPTION = "Reactome is a free, open-source, curated and peer reviewed pathway " +
            "database. Our goal is to provide intuitive bioinformatics tools for the visualization, interpretation and " +
            "analysis of pathway knowledge to support basic research, genome analysis, modeling, systems biology and " +
            "education.";

    private InteractorService interactorService = InteractorService.getInstance();
    private InteractionService interactionService = InteractionService.getInstance();

    /**
     * Collection that holds accessions from IntAct that are not in Reactome Data.
     * This collection will be used to keep interactions to those accession not in Reactome.
     */
    private static final Set<String> accessionsNoReactome = new HashSet<>();

    /**
     * Reactome Ids and names (ReactomeSummary) and their reference Entity accession identifier
     */
    private Map<String, ReactomeSummary> accessionMap = new HashMap<>();
    private Map<Integer, String> taxonomyMap = new HashMap<>();

    public Indexer(MySQLAdaptor dba, SolrClient solrClient, int addInterval, File ebeye, String release, Boolean verbose) {

        logger.setLevel(Level.INFO);
        Indexer.dba = dba;
        Indexer.solrClient = solrClient;
        converter = new Converter(CONTROLLED_VOCABULARY);

        this.addInterval = addInterval;
        this.verbose = verbose;
        this.xml = ebeye != null;
        if (xml) {
            marshaller = new Marshaller(ebeye, EBEYE_NAME, EBEYE_DESCRIPTION, release);
        }
    }

    public void index() throws IndexerException {

        try {
            cleanSolrIndex();
            if (xml) {
                marshaller.writeHeader();
            }
            int entriesCount = 0;
            entriesCount += indexSchemaClass(ReactomeJavaConstants.Event);
            commitSolrServer();
            entriesCount += indexSchemaClass(ReactomeJavaConstants.PhysicalEntity);
            commitSolrServer();
            entriesCount += indexSchemaClass(ReactomeJavaConstants.Regulation);
            if (xml) {
                marshaller.writeFooter(entriesCount);
            }
            commitSolrServer();

            /** Interactor **/
            indexInteractors();

            commitSolrServer();

        } catch (Exception e) {
            e.printStackTrace();
            logger.error(e);
            throw new IndexerException(e);
        } finally {
            closeSolrServer();
        }

    }

    private boolean hasValue(GKInstance instance, String fieldName) {
        if (instance.getSchemClass().isValidAttribute(fieldName)) {
            try {
                if (instance.getAttributeValue(fieldName) != null) {
                    return true;
                }
            } catch (Exception e) {
                // will never happen because i check it above
                logger.error(e.getMessage(), e);
            }
        }
        logger.info(instance.getDBID() + " has no value: " + fieldName);
        return false;
    }

    private String getReactomeId(GKInstance instance) throws Exception {
        if (hasValue(instance, ReactomeJavaConstants.stableIdentifier)) {
            return (String) ((GKInstance) instance.getAttributeValue(ReactomeJavaConstants.stableIdentifier)).getAttributeValue(ReactomeJavaConstants.identifier);
        } else {
            logger.error("No ST_ID for " + instance.getDBID() + " >> " + instance.getDisplayName());
            return instance.getDBID().toString();
        }
    }

    /**
     * This method is populating the global attribute accessionMap.
     *
     * @param accessionList
     * @return
     * @throws IndexerException
     */
    private void createAccessionSet(List<String> accessionList) throws IndexerException {

        /**
         * Making a copy of the original accession list. Accessions that exist in Reactome will be removed from this
         * collection. The final collection will hold those accessions that are not present in Reactome.
         */
        accessionsNoReactome.addAll(accessionList);

        Collection<?> instances;
        try {
            instances = dba.fetchInstancesByClass(ReactomeJavaConstants.ReferenceEntity);
            for (Object object : instances) {
                GKInstance instance = (GKInstance) object;
                if (!instance.getSchemClass().isValidAttribute(ReactomeJavaConstants.identifier)) continue;

                String identifier = (String) instance.getAttributeValue(ReactomeJavaConstants.identifier);

                if (identifier == null || identifier.isEmpty()) continue;

                if (!accessionList.contains(identifier)) continue;

                /**
                 * removing the identifier that exists in Reactome.
                 */
                accessionsNoReactome.remove(identifier);

                Collection<?> referenceEntity = instance.getReferers(ReactomeJavaConstants.referenceEntity);
                if (referenceEntity == null) continue;

                for (Object o : referenceEntity) {
                    GKInstance gkInstance = (GKInstance)o;

                    if (accessionMap.containsKey(identifier)) {
                        ReactomeSummary summary = accessionMap.get(identifier);
                        summary.addId(getReactomeId(gkInstance));
                        summary.addName(gkInstance.getDisplayName());
                    } else {
                        ReactomeSummary summary = new ReactomeSummary();
                        summary.addId(getReactomeId(gkInstance));
                        summary.addName(gkInstance.getDisplayName());
                        accessionMap.put(identifier, summary);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Fetching Instances by ClassName from the Database caused an errer", e);
            throw new IndexerException("Fetching Instances by ClassName from the Database caused an errer", e);
        }

    }

    /**
     * Save a document containing an interactor that IS NOT in Reactome and a List of Interactions
     * with Reactome proteins
     *
     * @return
     * @throws IndexerException
     */
    private int indexInteractors() throws IndexerException {
        logger.info("Start addind interactor into Solr");

        int numberOfDocuments = 0;
        try {
            List<IndexDocument> collection = new ArrayList<>();

            /**
             * Querying interactor database and retrieve all unique accession identifiers of intact-micluster file
             */
            List<String> accessionsList = interactorService.getAllAccessions();
            createTaxonomyMap();

            /**
             * Removing accession identifier that are not Uniprot/CHEBI accession Identifier.
             * The intact file keeps the same Intact id in this case.
             */
            Iterator<String> iter = accessionsList.iterator();
            while (iter.hasNext()) {
                String str = iter.next();
                if (str.startsWith("EBI-")) {
                    iter.remove();
                }
            }

            createAccessionSet(accessionsList);

            /**
             * Get Interactions for all accessions that are not in Reactome.
             */
            Map<String, List<Interaction>> interactions = interactionService.getInteractions(accessionsNoReactome, "intact");

            logger.info("Accessions not in Reactome: " + accessionsNoReactome.size());

            for (String accKey : interactions.keySet()) {
                Set<InteractorSummary> interactorSummarySet = new HashSet<>();
                for (Interaction interaction : interactions.get(accKey)) {

                    /**
                     * Interaction --> InteractorA and InteractorB where:
                     *  InteractorA is the one being queried in the database
                     *  InteractorB is the one that Interacts with A.
                     */
                    if (accessionMap.containsKey(interaction.getInteractorB().getAcc())) {
                        InteractorSummary summary = new InteractorSummary();
                        summary.setReactomeSummary(accessionMap.get(interaction.getInteractorB().getAcc()));
                        summary.setAccession(interaction.getInteractorB().getAcc());
                        summary.setScore(interaction.getIntactScore());
                        summary.setInteractionId(interaction.getInteractionDetailsList().get(0).getInteractionAc());
                        interactorSummarySet.add(summary);
                    }
                }

                if (!interactorSummarySet.isEmpty()) {
                    IndexDocument indexDocument = createDocument(interactions.get(accKey).get(0).getInteractorA(), interactorSummarySet);
                    collection.add(indexDocument);

                    numberOfDocuments++;
                }
            }

            /**
             * Save the indexDocument into Solr.
             */
            addDocumentsToSolrServer(collection);

            logger.info(numberOfDocuments + " Interactor have now been added to Solr");

        } catch (InvalidInteractionResourceException | SQLException e) {
            throw new IndexerException(e);
        }

        return numberOfDocuments;
    }

    private void createTaxonomyMap() {

        Collection<?> instances;
        try {
            instances = dba.fetchInstancesByClass(ReactomeJavaConstants.Species);
            for (Object object : instances) {
                GKInstance instance = (GKInstance) object;
                GKInstance crossReference = (GKInstance) instance.getAttributeValue(ReactomeJavaConstants.crossReference);
                String[] name = crossReference.getDisplayName().split(":");
                taxonomyMap.put(Integer.parseInt(name[1]), instance.getDisplayName());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

        /**
     * Creating interactor document
     * @param interactorA
     * @param interactorSummarySet
     * @return
     */
    private IndexDocument createDocument(Interactor interactorA, Set<InteractorSummary> interactorSummarySet) {

        IndexDocument document = new IndexDocument();
        document.setDbId(interactorA.getAcc());
        document.setName(interactorA.getAlias());
        document.setType("Interactor");
        document.setExactType("Interactor");
        List<String> referenceIdentifiersList = new ArrayList<>(1);
        referenceIdentifiersList.add(interactorA.getAcc());
        document.setReferenceIdentifiers(referenceIdentifiersList);
        String species = "Entries without species";
        if (taxonomyMap.containsKey(interactorA.getTaxid())) {
            species = taxonomyMap.get(interactorA.getTaxid());
        }
        document.setSpecies(species);

        List<String> interactionIds = new ArrayList<>();
        List<String> accessions = new ArrayList<>();
        List<String> reactomeIds = new ArrayList<>();
        List<String> reactomeNames = new ArrayList<>();
        List<Double> scores = new ArrayList<>();

        for (InteractorSummary interactorSummary : interactorSummarySet) {
            reactomeIds.add(ReactomeSummary.parseList(interactorSummary.getReactomeSummary().getReactomeId()));
            reactomeNames.add(ReactomeSummary.parseList(interactorSummary.getReactomeSummary().getReactomeName()));
            interactionIds.add(interactorSummary.getInteractionId());
            scores.add(interactorSummary.getScore());
            accessions.add(interactorSummary.getAccession());
        }

        document.setInteractionsIds(interactionIds);
        document.setReactomeInteractorIds(reactomeIds);
        document.setReactomeInteractorNames(reactomeNames);
        document.setScores(scores);
        document.setInteractorAccessions(accessions);
        return document;

    }

    /**
     * Iterates of a Collection of GkInstances, each Instance will be converted
     * to a IndexDocument by the Converter, The IndexDocuments will be added to
     * Solr and marshaled to a xml file.
     *
     * @param className Name of the SchemaClass that should be indexed
     * @return number of Documents processed
     * @throws IndexerException
     */
    private int indexSchemaClass(String className) throws IndexerException {

        Collection<?> instances;
        try {
            instances = dba.fetchInstancesByClass(className);
        } catch (Exception e) {
            logger.error("Fetching Instances by ClassName from the Database caused an errer", e);
            throw new IndexerException("Fetching Instances by ClassName from the Database caused an errer", e);
        }
        int numberOfDocuments = 0;
        List<IndexDocument> collection = new ArrayList<>();
        for (Object object : instances) {
            GKInstance instance = (GKInstance) object;
            IndexDocument document = converter.buildDocumentFromGkInstance(instance);
            collection.add(document);
            if (xml) {
                marshaller.writeEntry(document);
            }
            numberOfDocuments++;
            if (numberOfDocuments % addInterval == 0 && !collection.isEmpty()) {
                addDocumentsToSolrServer(collection);
                collection.clear();
                if (xml) {
                    try {
                        marshaller.flush();
                    } catch (IOException e) {
                        logger.error("An error occurred when trying to flush to XML", e);
                    }
                }
                logger.info(numberOfDocuments + " " + className + " have now been added to Solr");
                if (verbose) {
                    System.out.println(numberOfDocuments + " " + className + " have now been added to Solr");
                }
            }
        }
        if (!collection.isEmpty()) {
            addDocumentsToSolrServer(collection);
            logger.info(numberOfDocuments + " " + className + " have now been added to Solr");
            if (verbose) {
                System.out.println(numberOfDocuments + " " + className + " have now been added to Solr");
            }
        }
        return numberOfDocuments;
    }

    /**
     * Safely adding Document Bean to Solr Server
     *
     * @param documents List of Documents that will be added to Solr
     *                  <p>
     *                  !!!!!!!!!!!!!!!!!!!!!! REMOTE_SOLR_EXCEPTION is a Runtime Exception !!!!!!!!!!!!!!!!!!!!!!!!!!!!!
     */
    private void addDocumentsToSolrServer(List<IndexDocument> documents) {

        if (documents != null && !documents.isEmpty()) {
            try {
                solrClient.addBeans(documents);
                logger.info(documents.size() + " Documents succsessfully added to Sorl");
            } catch (IOException | SolrServerException | HttpSolrClient.RemoteSolrException e) {
                for (IndexDocument document : documents) {
                    try {
                        solrClient.addBean(document);
                        logger.info("A single document was added to Solr");
                    } catch (IOException | SolrServerException | HttpSolrClient.RemoteSolrException e1) {
                        logger.error("Could not add document", e);
                        logger.error("Document DBID: " + document.getDbId() + " Name " + document.getName());
                    }
                }
                logger.error("Could not add documenst", e);
            }
        } else {
            logger.error("Solr Documents are null or empty");
        }
    }

    /**
     * Cleaning Solr Server (removes all current Data)
     *
     * @throws IndexerException
     */
    private void cleanSolrIndex() throws IndexerException {
        try {
            solrClient.deleteByQuery("*:*");
            commitSolrServer();
            logger.info("Solr index has been cleaned");
        } catch (SolrServerException | IOException e) {
            logger.error("an error occured while cleaning the SolrServer", e);
            throw new IndexerException("an error occured while cleaning the SolrServer", e);
        }
    }

    /**
     * Closes connection to Solr Server
     */
    public static void closeSolrServer() {
        try {
            solrClient.close();
            logger.info("SolrServer shutdown");
        } catch (IOException e) {
            logger.error("an error occured while closing the SolrServer", e);
        }
    }

    /**
     * Commits Data that has been added till now to Solr Server
     *
     * @throws IndexerException not comiting could mean that this Data will not be added to Solr
     */
    private void commitSolrServer() throws IndexerException {
        try {
            solrClient.commit();
            logger.info("Solr index has been committed and flushed to disk");
        } catch (Exception e) {
            logger.error("Error occurred while committing", e);
            throw new IndexerException("Could not commit", e);
        }
    }
}


