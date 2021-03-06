package uk.ac.ebi.reactome.solr.indexer.impl;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.MySQLAdaptor;
import org.json.JSONException;
import org.json.JSONObject;
import org.reactome.server.tools.interactors.database.InteractorsDatabase;
import org.reactome.server.tools.interactors.exception.InvalidInteractionResourceException;
import org.reactome.server.tools.interactors.model.Interaction;
import org.reactome.server.tools.interactors.model.InteractionDetails;
import org.reactome.server.tools.interactors.model.Interactor;
import org.reactome.server.tools.interactors.service.InteractionService;
import org.reactome.server.tools.interactors.service.InteractorService;
import org.reactome.server.tools.interactors.util.InteractorConstant;
import org.reactome.server.tools.interactors.util.Toolbox;
import uk.ac.ebi.reactome.solr.indexer.exception.IndexerException;
import uk.ac.ebi.reactome.solr.indexer.model.IndexDocument;
import uk.ac.ebi.reactome.solr.indexer.model.InteractorSummary;
import uk.ac.ebi.reactome.solr.indexer.model.ReactomeSummary;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
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

    private InteractorService interactorService;
    private InteractionService interactionService;

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

    public Indexer(MySQLAdaptor dba, SolrClient solrClient, int addInterval, File ebeye, String release, Boolean verbose, InteractorsDatabase interactorsDatabase) {

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

        this.interactorService = new InteractorService(interactorsDatabase);
        this.interactionService = new InteractionService(interactorsDatabase);
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
     * Queries gk_instance and create a list of accessions that are not in reactome (accessionsNoReactome) and
     * also a map with the accession +information (stIds,names) in reactome (accessionMap).
     *
     * @param accessionList all unique accessions from Interactors Database excluding those that start with EBI-. They are provided by IntAct but does not have accession.
     * @throws IndexerException
     */
    private void createAccessionSet(List<String> accessionList) throws IndexerException {
        int progress = 0;

        /**
         * Making a copy of the original accession list. Accessions that exist in Reactome will be removed from this
         * collection. The final collection will hold those accessions that are not present in Reactome.
         */
        accessionsNoReactome.addAll(accessionList);

        Collection<?> instances;
        try {

            /**
             * Get all ReferenceEntities in Reactome Database. We have around 350000. These are the objects where we have the accession.
             * Then we check if the given ref. identifier is in the accessionList (which has all the accessions from intact).
             */
            instances = dba.fetchInstancesByClass(ReactomeJavaConstants.ReferenceEntity);
            logger.info("Retrieving accessions from Reactome -- Accession list has [" + accessionList.size() + "] entries and [" + instances.size() + "] ReferenceEntities");
            for (Object object : instances) {

                progress++;
                if(progress % 15000 == 0){
                    logger.info("  >> querying accessions in GKInstance [" + progress + "]");
                }

                GKInstance instance = (GKInstance) object;
                if (!instance.getSchemClass().isValidAttribute(ReactomeJavaConstants.identifier)) continue;

                String identifier = (String) instance.getAttributeValue(ReactomeJavaConstants.identifier);

                if (identifier == null || identifier.isEmpty()) continue;

                if (!accessionList.contains(identifier)) continue;

                /**
                 * Removing the identifier that exists in Reactome.
                 * Remember, the final collection will hold those accessions that are not present in Reactome.
                 * At, we are going to get the Interactions using the accessions that are not in reactome.
                 */
                accessionsNoReactome.remove(identifier);

                /** Get the referenceEntity in the referers, this instance has the accession we are interested in **/
                Collection<?> referenceEntity = instance.getReferers(ReactomeJavaConstants.referenceEntity);
                if (referenceEntity == null) continue;

                for (Object o : referenceEntity) {
                    GKInstance gkInstance = (GKInstance)o;

                    if (isDirectlyAssociatedTo(gkInstance)) {
                        /**
                         * accessionMap is a map that has the accession as the Key
                         * and ReactomeSummary as the value. ReactomeSummary holds a list
                         * of ids (StId) and names that are refer to the accession.
                         */
                        if (accessionMap.containsKey(identifier)) {
                            /**
                             * If the accession is in the map, we get it and add the Id and Name into the list
                             */
                            ReactomeSummary summary = accessionMap.get(identifier);
                            summary.addId(getReactomeId(gkInstance));
                            summary.addName(gkInstance.getDisplayName());
                        } else {
                            /**
                             * Otherwise create a new one and add to the map
                             */
                            ReactomeSummary summary = new ReactomeSummary();
                            summary.addId(getReactomeId(gkInstance));
                            summary.addName(gkInstance.getDisplayName());
                            accessionMap.put(identifier, summary);
                        }

                    }
                }
            }

            logger.info("  >> querying accessions in GKInstance [" + progress + "]");

        } catch (Exception e) {
            logger.error("Fetching Instances by ClassName from the Database caused an error", e);
            throw new IndexerException("Fetching Instances by ClassName from the Database caused an error", e);
        }

    }

    /**
     * Check if the instance is referred to a Reaction.
     *
     * @return true if is associated
     * @throws Exception
     */
    private boolean isDirectlyAssociatedTo(GKInstance gkInstance) throws Exception {

        List<String> fieldReferers = new ArrayList<>();
        fieldReferers.add(ReactomeJavaConstants.input);
        fieldReferers.add(ReactomeJavaConstants.output);
        fieldReferers.add(ReactomeJavaConstants.regulator);
        fieldReferers.add(ReactomeJavaConstants.physicalEntity);

        /** To be discussed, I added but the number of interactor wasn't changed at the end. **/
        //fieldReferers.add(ReactomeJavaConstants.entityFunctionalStatus);

        for (String field : fieldReferers) {
            Collection<?> collection = gkInstance.getReferers(field);

            if (collection != null && !collection.isEmpty()) {
                for (Object entryObject : collection) {
                    GKInstance entry = (GKInstance) entryObject;

                    if(entry.getSchemClass().isa(ReactomeJavaConstants.Reaction)
                            || entry.getSchemClass().isa(ReactomeJavaConstants.CatalystActivity)
                            || entry.getSchemClass().isa(ReactomeJavaConstants.BlackBoxEvent)
                            || entry.getSchemClass().isa(ReactomeJavaConstants.Depolymerisation)
                            || entry.getSchemClass().isa(ReactomeJavaConstants.FailedReaction)
                            || entry.getSchemClass().isa(ReactomeJavaConstants.Polymerisation)
                            || entry.getSchemClass().isa(ReactomeJavaConstants.ReactionlikeEvent)){
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /**
     * Save a document containing an interactor that IS NOT in Reactome and a List of Interactions
     * with Reactome proteins
     *
     * @throws IndexerException
     */
    private int indexInteractors() throws IndexerException {
        logger.info("Start adding interactor into Solr");

        int numberOfDocuments = 0;
        try {
            List<IndexDocument> collection = new ArrayList<>();

            /**
             * Querying interactor database and retrieve all unique accession identifiers of intact-micluster file
             */
            logger.info("Getting all accessions from Interactors Database");
            List<String> accessionsList = interactorService.getAllAccessions();

            logger.info("Creating taxonomy map querying Reactome Database");
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

            /**
             * Queries gk_instance and create a list of accessions that are not in reactome and
             * also a map with the accession +information (stIds,names) in reactome
             */
            createAccessionSet(accessionsList);

            /**
             * Get Interactions for all accessions that are NOT in Reactome.
             * Keep in mind that we are only saving interactions having score higher than InteractorConstant.MINIMUM_VALID_SCORE
             *
             * The result of this query is Map having the accession as the key and a list of interactions. Take into account the
             * Interaction domain has InteractorA and InteractorB where interactorA is ALWAYS the same as the map key.
             * e.g map K=q13501, interactorA=q13501, interactorB=p12345 (this is the interaction)
             */
            Map<String, List<Interaction>> interactions = interactionService.getInteractions(accessionsNoReactome, InteractorConstant.STATIC);

            logger.info("Preparing SolR documents for Interactors [" + interactions.size() + "]");

            int preparingSolrDocuments = 0;
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
                        /** get reactome information from the map based on interactor B. Interactor A is the one we are creating the document **/
                        summary.setReactomeSummary(accessionMap.get(interaction.getInteractorB().getAcc()));
                        summary.setAccession(interaction.getInteractorB().getAcc());
                        summary.setScore(interaction.getIntactScore());

                        for(InteractionDetails interactionDetails : interaction.getInteractionDetailsList()){
                            summary.addInteractionEvidences(interactionDetails.getInteractionAc());
                        }

                        interactorSummarySet.add(summary);
                    }
                }

                if (!interactorSummarySet.isEmpty()) {
                    /**
                     * Create index document based on interactor A and the summary based on Interactor B.
                     */
                    IndexDocument indexDocument = createDocument(interactions.get(accKey).get(0).getInteractorA(), interactorSummarySet);
                    collection.add(indexDocument);

                    numberOfDocuments++;
                }

                preparingSolrDocuments++;
                if(preparingSolrDocuments % 1000 == 0){
                    logger.info("  >> preparing interactors Solr Documents [" + preparingSolrDocuments + "]");
                }
            }

            logger.info("  >> preparing interactors Solr Documents [" + preparingSolrDocuments + "]");

            /**
             * Save the indexDocument into Solr.
             */
            addDocumentsToSolrServer(collection);

            logger.info(numberOfDocuments + " Interactor(s) have now been added to Solr");

        } catch (InvalidInteractionResourceException | SQLException e) {
            throw new IndexerException(e);
        }

        return numberOfDocuments;
    }

    /**
     * Get all species available in Reactome and add them to taxonomy map
     */
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
     * Creating interactor document, where the Interactor A is the base and the B is the interactor which has
     * the reactome information.
     */
    private IndexDocument createDocument(Interactor interactorA, Set<InteractorSummary> interactorSummarySet) {

        IndexDocument document = new IndexDocument();
        document.setDbId(interactorA.getAcc());

        /**
         * In the interactors.db we are saving the alias null if it is the same as the accession
         * Just assigning the acc in the name which is required here
         */
        if(interactorA.getAlias() != null) {
            document.setName(interactorA.getAliasWithoutSpecies(false));
        }else {
            document.setName(interactorA.getAcc());
        }

        document.setType("Interactor");
        document.setExactType("Interactor");

        document.setSynonyms(Collections.singletonList(interactorA.getAlias()));
        document.setReferenceIdentifiers(Collections.singletonList(interactorA.getAcc()));
        document.setReferenceURL(Toolbox.getAccessionURL(interactorA.getAcc(), InteractorConstant.STATIC));
        document.setDatabaseName(Toolbox.getDatabaseName(interactorA.getAcc()));

        String species;
        if (taxonomyMap.containsKey(interactorA.getTaxid())) {
            species = taxonomyMap.get(interactorA.getTaxid());
        } else {
            species = getTaxonomyLineage(interactorA.getTaxid());
        }
        document.setSpecies(species);

        List<String> interactionIds = new ArrayList<>();
        List<String> accessions = new ArrayList<>();
        List<String> reactomeIds = new ArrayList<>();
        List<String> reactomeNames = new ArrayList<>();
        List<Double> scores = new ArrayList<>();

        for (InteractorSummary interactorSummary : interactorSummarySet) {
            reactomeIds.add(parseList(interactorSummary.getReactomeSummary().getReactomeId()));
            reactomeNames.add(parseList(interactorSummary.getReactomeSummary().getReactomeName()));

            interactionIds.add(parseList(interactorSummary.getInteractionEvidences()));

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
     * Query Ensembl REST API in order to get the taxonomy lineage
     * and then get the parent.
     *
     * Once we found the species we add it to the global map, it will
     * reduce the amount of queries to an external resource.
     *
     * @return the species
     */
    private String getTaxonomyLineage(Integer taxId){
        if(taxId == 1 || taxId == 0 || taxId == -1){
            return "Entries without species";
        }

        InputStream response;
        try {
            String urlString = "http://rest.ensembl.org/taxonomy/id/" + taxId;
            URL url = new URL(urlString);
            URLConnection conn = url.openConnection();

            conn.setRequestProperty("Content-Type", "application/json");
            response = conn.getInputStream();
            String StringFromInputStream = IOUtils.toString(response, "UTF-8");
            JSONObject jsonObject = new JSONObject(StringFromInputStream);

            int parentTaxid = jsonObject.getJSONObject("parent").getInt("id");

            if (taxonomyMap.containsKey(parentTaxid)){
                String species = taxonomyMap.get(parentTaxid);
                taxonomyMap.put(taxId, species);
                return species;
            }

            response.close();
            /**
             * taking too long to execute.
             */
//            getTaxonomyLineage(parentTaxid,original);

        }catch (IOException | JSONException e){
            e.printStackTrace();
        }
        return "Entries without species";

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
            logger.error("Fetching Instances by ClassName from the Database caused an error", e);
            throw new IndexerException("Fetching Instances by ClassName from the Database caused an error", e);
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
                logger.info(documents.size() + " Documents successfully added to Solr");
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
                logger.error("Could not add document", e);
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
            logger.error("an error occurred while cleaning the SolrServer", e);
            throw new IndexerException("an error occurred while cleaning the SolrServer", e);
        }
    }

    /**
     * Closes connection to Solr Server
     */
    private static void closeSolrServer() {
        try {
            solrClient.close();
            logger.info("SolrServer shutdown");
        } catch (IOException e) {
            logger.error("an error occurred while closing the SolrServer", e);
        }
    }

    /**
     * Commits Data that has been added till now to Solr Server
     *
     * @throws IndexerException not committing could mean that this Data will not be added to Solr
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

    /**
     * Saving a List into a multivalue field in SolR, but calling toString the final result will
     * be a comma-separated list. When parsing this List in the client (splitting by comma) then it may split
     * other identifiers which has comma as part of its name.
     * e.g Reactome names has comma on it.
     *   "[NUDC [cytosol], NUDC [nucleoplasm], p-S274,S326-NUDC [nucleoplasm]]"
     *   This multivalued field has 3 values, but splitting them by comma will result in
     *   4 values.
     *
     * This parser retrieve the list as String using # as delimiter.
     */
    private String parseList(List<String> list){
        String delim = "";
        StringBuilder sb = new StringBuilder();
        for (String s : list) {
            sb.append(delim);
            delim = "#";
            sb.append(s);
        }
        return sb.toString();
    }
}


