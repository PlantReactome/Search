package org.reactome.server.tools.search.service;

import org.reactome.server.tools.interactors.exception.InvalidInteractionResourceException;
import org.reactome.server.tools.interactors.model.Interaction;
import org.reactome.server.tools.interactors.service.InteractionService;
import org.reactome.server.tools.search.database.Enricher;
import org.reactome.server.tools.search.database.IEnricher;
import org.reactome.server.tools.search.database.InteractorEnricher;
import org.reactome.server.tools.search.domain.*;
import org.reactome.server.tools.search.exception.EnricherException;
import org.reactome.server.tools.search.exception.SearchServiceException;
import org.reactome.server.tools.search.exception.SolrSearcherException;
import org.reactome.server.tools.search.solr.ISolrConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Search Service acts as api between the Controller and Solr / Database
 *
 * @author Florian Korninger (fkorn@ebi.ac.uk)
 * @version 1.0
 */
@Service
public class SearchService {
    private static final Logger logger = LoggerFactory.getLogger(SearchService.class);

    @Autowired
    private ISolrConverter solrConverter;

    private static String host;
    private static String database;
    private static String currentDatabase;
    private static String user;
    private static String password;
    private static Integer port;

    /**
     * Constructor for Spring Dependency Injection and loading MavenProperties
     *
     * @throws SearchServiceException
     */
    public SearchService() throws SearchServiceException {
        loadProperties();
    }

    private void loadProperties() throws SearchServiceException {

        try {
            Properties databaseProperties = new Properties();
            databaseProperties.load(getClass().getResourceAsStream("/web.properties"));
            host = databaseProperties.getProperty("database_host");
            database = databaseProperties.getProperty("database_name");
            currentDatabase = databaseProperties.getProperty("database_currentDatabase");
            user = databaseProperties.getProperty("database_user");
            password = databaseProperties.getProperty("database_password");
            port = Integer.valueOf(databaseProperties.getProperty("database_port"));
        } catch (IOException e) {
            logger.error("Error when loading Database Properties ", e);
            throw new SearchServiceException("Error when loading Database Properties ", e);
        }
    }

    public List<Interaction> getInteractions(String intactId){
        InteractionService interactionService = InteractionService.getInstance();
        try {
            Map<String, List<Interaction>> interactionMap = interactionService.getInteractionsByIntactId(intactId, "intact");
            List<Interaction> interactions = interactionMap.get(intactId);

            InteractorEnricher interactorEnricher = new InteractorEnricher(host, database, user, password, port);
            interactorEnricher.notclear(interactions);
            // InteractorA is the one we are querying -- always the same.

        } catch (InvalidInteractionResourceException | SQLException e) {
            e.printStackTrace();
        } catch (EnricherException e) {
            e.printStackTrace();
        }
        return null;
    }



    /**
     * Gets Faceting information for a specific query + filters.
     * This Method will query solr once again if the number of selected filters and found facets differ
     * (this will help preventing false faceting information when filter are contradictory to each other)
     *
     * @param queryObject query and filter (species types keywords compartments)
     * @return FacetMapping
     * @throws SolrSearcherException
     */
    public FacetMapping getFacetingInformation(Query queryObject) throws SolrSearcherException {
        if (queryObject != null && queryObject.getQuery() != null && !queryObject.getQuery().isEmpty()) {

            FacetMapping facetMapping = solrConverter.getFacetingInformation(queryObject);
            boolean correctFacets = true;
            // Each faceting group(species,types,keywords,compartments) is dependent from all selected filters of other faceting groups
            // This brings the risk of having filters that contradict each other. To avoid having selected facets that will cause problems
            // with the next filtering or querying it is necessary to remove those from the filtering process and repeat the faceting step
            if (queryObject.getSpecies() != null && facetMapping.getSpeciesFacet().getSelected().size() != queryObject.getSpecies().size()) {
                correctFacets = false;
                List<String> species = new ArrayList<String>();
                for (FacetContainer container : facetMapping.getSpeciesFacet().getSelected()) {
                    species.add(container.getName());
                }
                queryObject.setSpecies(species);
            }
            if (queryObject.getTypes() != null && facetMapping.getTypeFacet().getSelected().size() != queryObject.getTypes().size()) {
                correctFacets = false;
                List<String> types = new ArrayList<String>();
                for (FacetContainer container : facetMapping.getTypeFacet().getSelected()) {
                    types.add(container.getName());
                }
                queryObject.setTypes(types);
            }
            if (queryObject.getKeywords() != null && facetMapping.getKeywordFacet().getSelected().size() != queryObject.getKeywords().size()) {
                correctFacets = false;
                List<String> keywords = new ArrayList<String>();
                for (FacetContainer container : facetMapping.getKeywordFacet().getSelected()) {
                    keywords.add(container.getName());
                }
                queryObject.setKeywords(keywords);
            }
            if (queryObject.getCompartment() != null && facetMapping.getCompartmentFacet().getSelected().size() != queryObject.getCompartment().size()) {
                correctFacets = false;
                List<String> compartments = new ArrayList<String>();
                for (FacetContainer container : facetMapping.getCompartmentFacet().getSelected()) {
                    compartments.add(container.getName());
                }
                queryObject.setCompartment(compartments);
            }
            if (correctFacets) {
                return facetMapping;
            } else {
                return solrConverter.getFacetingInformation(queryObject);
            }
        }
        return null;
    }

    /**
     * Method for providing Faceting information for Species,Types,Keywords and Compartments
     *
     * @return FacetMapping
     * @throws SolrSearcherException
     */
    public FacetMapping getTotalFacetingInformation() throws SolrSearcherException {
        return solrConverter.getFacetingInformation();
    }

    /**
     * Method for providing autocomplete suggestions
     *
     * @param query Term (Snippet) you want to have autocompleted
     * @return List(String) of suggestions if solr is able to provide some
     * @throws SolrSearcherException
     */
    public List<String> getAutocompleteSuggestions(String query) throws SolrSearcherException {
        if (query != null && !query.isEmpty()) {
            return solrConverter.getAutocompleteSuggestions(query);
        }
        return null;
    }

    /**
     * Method for supplying spellcheck suggestions
     *
     * @param query Term you searched for
     * @return List(String) of suggestions if solr is able to provide some
     * @throws SolrSearcherException
     */
    public List<String> getSpellcheckSuggestions(String query) throws SolrSearcherException {
        if (query != null && !query.isEmpty()) {
            return solrConverter.getSpellcheckSuggestions(query);
        }
        return null;
    }

    /**
     * Returns one specific Entry by DbId
     *
     * @param id StId or DbId
     * @return Entry Object
     */
    public EnrichedEntry getEntryById(String id) throws EnricherException, SolrSearcherException {
        if (id != null && !id.isEmpty()) {
            IEnricher enricher = new Enricher(host, currentDatabase, user, password, port);
            return enricher.enrichEntry(id);
        }
        return null;
    }

    /**
     * Returns one specific Entry by DbId
     *
     * @param id StId or DbId
     * @return Entry Object
     */
    public EnrichedEntry getEntryById(Integer version, String id) throws EnricherException, SolrSearcherException {
        if (id != null && !id.isEmpty()) {
            IEnricher enricher = new Enricher(host, database + version, user, password, port);
            return enricher.enrichEntry(id);
        }
        return null;
    }

    /**
     * This Method gets multiple entries for a specific query while considering the filter information
     * the entries will be returned grouped into types and sorted by relevance (depending on the chosen solr properties)
     *
     * @param queryObject QueryObject (query, species, types, keywords, compartments, start, rows)
     *                    start specifies the starting point (offset) and rows the amount of entries returned in total
     * @return GroupedResult
     * @throws SolrSearcherException
     */
    public GroupedResult getEntries(Query queryObject, Boolean cluster) throws SolrSearcherException {
        if (cluster) {
            return solrConverter.getClusteredEntries(queryObject);
        } else {
            return solrConverter.getEntries(queryObject);
        }
    }


}
