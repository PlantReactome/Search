package org.reactome.server.tools.search.database;

import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.reactome.server.tools.search.domain.Disease;
import org.reactome.server.tools.search.domain.EnrichedEntry;
import org.reactome.server.tools.search.domain.EntityReference;
import org.reactome.server.tools.search.domain.Literature;
import org.reactome.server.tools.search.exception.EnricherException;

import java.util.ArrayList;
import java.util.List;

/**
 * Queries the MySql database and converts entry to a local object
 *
 * @author Florian Korninger (fkorn@ebi.ac.uk)
 * @version 1.0
 */
public class GeneralAttributeEnricher extends Enricher {

    private static final String PUBMED_URL = "http://www.ncbi.nlm.nih.gov/pubmed/";

    public void setGeneralAttributes(GKInstance instance, EnrichedEntry enrichedEntry) throws EnricherException {


        try {
            List<String> names = getAttributes(instance, ReactomeJavaConstants.name);
            if (names != null && !names.isEmpty()) {
                if (names.size() >= 1) {
                    enrichedEntry.setName(names.get(0));
                    if (names.size() > 1) {
                        enrichedEntry.setSynonyms(names.subList(1, names.size() - 1));
                    }
                }
            } else {
                enrichedEntry.setName(instance.getDisplayName());
            }
            if (hasValue(instance, ReactomeJavaConstants.stableIdentifier)) {
                enrichedEntry.setStId((String) ((GKInstance) instance.getAttributeValue(ReactomeJavaConstants.stableIdentifier)).getAttributeValue(ReactomeJavaConstants.identifier));
            }
            enrichedEntry.setSpecies(getAttributeDisplayName(instance, ReactomeJavaConstants.species));
            List<?> summationInstances = instance.getAttributeValuesList(ReactomeJavaConstants.summation);
            List<String> summations = new ArrayList<String>();
            for (Object summationInstance : summationInstances) {
                GKInstance summation = (GKInstance) summationInstance;
                summations.add((String) summation.getAttributeValue(ReactomeJavaConstants.text));
            }
            enrichedEntry.setSummations(summations);
            enrichedEntry.setCompartments(getGoTerms(instance, ReactomeJavaConstants.compartment));
            enrichedEntry.setInferredFrom(getEntityReferences(instance, ReactomeJavaConstants.inferredFrom));
            enrichedEntry.setOrthologousEvents(getEntityReferences(instance, ReactomeJavaConstants.orthologousEvent));
            enrichedEntry.setCrossReferences(getCrossReferences(instance, ReactomeJavaConstants.crossReference, null));
            enrichedEntry.setDiseases(getDiseases(instance));
            enrichedEntry.setLiterature(setLiteratureReferences(instance));

            List<List<EntityReference>> list = new ArrayList<List<EntityReference>>();
            List<EntityReference> path = new ArrayList<EntityReference>();
            PathwayBrowserTreeGenerator pathwayBrowserTreeGenerator = new PathwayBrowserTreeGenerator();
            enrichedEntry.setLocationsPathwayBrowser(pathwayBrowserTreeGenerator.generateGraphForGivenGkInstance(instance));


        } catch (Exception e) {
            logger.error("Error occurred when trying to set general Attributes", e);
            throw new EnricherException("Error occurred when trying to set general Attributes", e);
        }
    }

    /**
     * Returns a list of literature for a given instance
     *
     * @param instance GkInstance
     * @return List of Literature Objects
     * @throws EnricherException
     */
    private List<Literature> setLiteratureReferences(GKInstance instance) throws EnricherException {
        if (hasValues(instance, ReactomeJavaConstants.literatureReference)) {
            List<Literature> literatureList = new ArrayList<Literature>();
            try {
                List<?> literatureInstanceList = instance.getAttributeValuesList(ReactomeJavaConstants.literatureReference);
                for (Object literatureObject : literatureInstanceList) {
                    GKInstance literatureInstance = (GKInstance) literatureObject;
                    Literature literature = new Literature();
                    literature.setTitle(getAttributeString(literatureInstance, ReactomeJavaConstants.title));
                    literature.setJournal(getAttributeString(literatureInstance, ReactomeJavaConstants.journal));
                    literature.setPubMedIdentifier(getAttributeString(literatureInstance, ReactomeJavaConstants.pubMedIdentifier));
                    literature.setYear(getAttributeInteger(literatureInstance, ReactomeJavaConstants.year));
                    if (literature.getPubMedIdentifier() != null) {
                        literature.setUrl(PUBMED_URL + literature.getPubMedIdentifier());
                    }
                    literatureList.add(literature);
                }
                return literatureList;
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
                throw new EnricherException(e.getMessage(), e);
            }
        }
        return null;
    }

    /**
     * Returns a list of disease information related to an instance
     *
     * @param instance GkInstance
     * @return List of Disease Objects
     * @throws EnricherException
     */
    private List<Disease> getDiseases(GKInstance instance) throws EnricherException {
        if (hasValues(instance, ReactomeJavaConstants.disease)) {
            try {
                List<Disease> diseases = new ArrayList<Disease>();
                List<?> diseaseInstanceList = instance.getAttributeValuesList(ReactomeJavaConstants.disease);
                for (Object diseaseObject : diseaseInstanceList) {
                    Disease disease = new Disease();
                    GKInstance diseaseInstance = (GKInstance) diseaseObject;
                    disease.setName(getAttributeString(diseaseInstance, ReactomeJavaConstants.name));
                    disease.setSynonyms(getAttributes(diseaseInstance, ReactomeJavaConstants.synonym));
                    disease.setIdentifier(getAttributeString(diseaseInstance, ReactomeJavaConstants.identifier));
                    disease.setDatabase(getDatabase(diseaseInstance, disease.getIdentifier()));
                    diseases.add(disease);
                }
                return diseases;
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
                throw new EnricherException(e.getMessage(), e);
            }
        }
        return null;
    }
}