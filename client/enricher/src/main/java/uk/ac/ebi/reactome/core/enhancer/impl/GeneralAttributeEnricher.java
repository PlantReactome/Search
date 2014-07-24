package uk.ac.ebi.reactome.core.enhancer.impl;

import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.reactome.core.enhancer.exception.EnricherException;
import uk.ac.ebi.reactome.core.model.result.EnrichedEntry;
import uk.ac.ebi.reactome.core.model.result.submodels.Disease;
import uk.ac.ebi.reactome.core.model.result.submodels.Literature;
import uk.ac.ebi.reactome.core.model.result.submodels.Node;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

/**
 * Queries the MySql database and converts entry to a local object
 *
 * @author Florian Korninger (fkorn@ebi.ac.uk)
 * @version 1.0
 */
public class GeneralAttributeEnricher extends Enricher{

    private static final Logger logger = LoggerFactory.getLogger(GeneralAttributeEnricher.class);
    private static final String PUBMED_URL = "http://www.ncbi.nlm.nih.gov/pubmed/";
    private static final String PATHWAY_BROWSER = "/PathwayBrowser/#";

    public void setGeneralAttributes(GKInstance instance, EnrichedEntry enrichedEntry) throws EnricherException {

        try {
            List<String> names = getAttributes(instance, ReactomeJavaConstants.name);
            if (names!= null && !names.isEmpty()) {
                if (names.size() >= 1) {
                    enrichedEntry.setName(names.get(0));
                    if (names.size() > 1) {
                        enrichedEntry.setSynonyms(names.subList(1, names.size() - 1));
                    }
                }else {
                    enrichedEntry.setName(instance.getDisplayName());
                }
            }
            enrichedEntry.setStId(getAttributeDisplayName(instance, ReactomeJavaConstants.stableIdentifier));
            enrichedEntry.setSpecies(getAttributeDisplayName(instance,ReactomeJavaConstants.species));
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

            String url = PATHWAY_BROWSER;

            List<String> loc = new ArrayList<String>();
                recursion(instance, instance.getDisplayName() + "#" + instance.getDBID().toString(), loc);
            if (hasValue(instance, ReactomeJavaConstants.species)) {
                GKInstance species = (GKInstance) instance.getAttributeValue(ReactomeJavaConstants.species);
                if (species != null && !species.getDisplayName().contains("Homo sapiens")) {
                    url += "SPECIES=" + species.getDBID();

                }
            }
            enrichedEntry.setRoot(makeUrl(loc, url));


        } catch (Exception e) {
            logger.error("Error occurred when trying to set general Attributes", e);
            throw new EnricherException("Error occurred when trying to set general Attributes", e);
        }
    }

    /**
     * Returns a list of literature for a given instance
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
                    throw new EnricherException(e.getMessage() , e);
                }
            }
        return null;
    }

    @SuppressWarnings("ConstantConditions")
    private void recursion(GKInstance instance, String path, List<String> list)  throws EnricherException {
        try {
            Collection<?> components = instance.getReferers(ReactomeJavaConstants.hasComponent);
            if (components != null && components.size() > 0) {
                for (Object entryObject : components) {
                    GKInstance entry = (GKInstance) entryObject;
                    recursion(entry, entry.getDisplayName()+"#"+entry.getDBID().toString() + "//" + path,  list);
                }
            }
            Collection<?> repeatedUnit = instance.getReferers(ReactomeJavaConstants.repeatedUnit);
            if (repeatedUnit != null && repeatedUnit.size() > 0) {
                for (Object entryObject : repeatedUnit) {
                    GKInstance entry = (GKInstance) entryObject;
                    recursion(entry, entry.getDisplayName()+"#"+entry.getDBID().toString() + "//" + path,  list);
                }
            }
            Collection<?> hasCandidate = instance.getReferers(ReactomeJavaConstants.hasCandidate);
            if (hasCandidate != null && hasCandidate.size() > 0) {
                for (Object entryObject : hasCandidate) {
                    GKInstance entry = (GKInstance) entryObject;
                    recursion(entry, entry.getDisplayName()+"#"+entry.getDBID().toString() + "//" + path,  list);
                }
            }
            Collection<?> hasMember = instance.getReferers(ReactomeJavaConstants.hasMember);
            if (hasMember != null && hasMember.size() > 0) {
                for (Object entryObject : hasMember) {
                    GKInstance entry = (GKInstance) entryObject;
                    recursion(entry, entry.getDisplayName()+"#"+entry.getDBID().toString() + "//" + path,  list);
                }
            }
            Collection<?> input = instance.getReferers(ReactomeJavaConstants.input);
            if (input != null && input.size() > 0) {
                for (Object entryObject : input) {
                    GKInstance entry = (GKInstance) entryObject;
                    recursion(entry, entry.getDisplayName()+"#"+entry.getDBID().toString() + "//" + path,  list);
                }
            }
            Collection<?> output = instance.getReferers(ReactomeJavaConstants.output);
            if (output != null && output.size() > 0) {
                for (Object entryObject : output) {
                    GKInstance entry = (GKInstance) entryObject;
                    recursion(entry, entry.getDisplayName()+"#"+entry.getDBID().toString() + "//" + path,  list);
                }
            }
            Collection<?> hasEvent = instance.getReferers(ReactomeJavaConstants.hasEvent);
            if (hasEvent != null && hasEvent.size() > 0) {
                for (Object entryObject : hasEvent) {
                    GKInstance entry = (GKInstance) entryObject;
                    recursion(entry, entry.getDisplayName()+"#"+entry.getDBID().toString() + "//" + path,  list);
                }
            }
            Collection<?> activeUnit = instance.getReferers(ReactomeJavaConstants.activeUnit);
            if (activeUnit != null && activeUnit.size() > 0) {
                for (Object entryObject : activeUnit) {
                    GKInstance entry = (GKInstance) entryObject;
                    recursion(entry, entry.getDisplayName()+"#"+entry.getDBID().toString() + "//" + path,  list);
                }
            }
            Collection<?> catalystActivity = instance.getReferers(ReactomeJavaConstants.catalystActivity);
            if (catalystActivity != null && catalystActivity.size() > 0) {
                for (Object entryObject : catalystActivity) {
                    GKInstance entry = (GKInstance) entryObject;
                    recursion(entry, entry.getDisplayName()+"#"+entry.getDBID().toString() + "//" + path,  list);
                }
            }
//            Collection<?> regulatedEntity = instance.getReferers(ReactomeJavaConstants.regulatedEntity);
//            if (regulatedEntity != null && regulatedEntity.size() > 0) {
//                for (Object entryObject : regulatedEntity) {
//                    GKInstance entry = (GKInstance) entryObject;
//                    recursion(entry, path,  list);
//                }
//            }
            Collection<?> regulator = instance.getReferers(ReactomeJavaConstants.regulator);
            if (regulator != null && regulator.size() > 0) {
                for (Object entryObject : regulator) {
                    GKInstance entry = (GKInstance) entryObject;
                    recursion(entry, path,  list);
                }
            }
            Collection<?> physicalEntity = instance.getReferers(ReactomeJavaConstants.physicalEntity);
            if (physicalEntity != null && physicalEntity.size() > 0) {
                for (Object entryObject : physicalEntity) {
                    GKInstance entry = (GKInstance) entryObject;
                    recursion(entry,  path,  list);
                }
            }
            GKInstance regulatedEntityInstance = null;
            if (hasValues(instance, ReactomeJavaConstants.regulatedEntity)) {
                regulatedEntityInstance = (GKInstance) instance.getAttributeValue(ReactomeJavaConstants.regulatedEntity);
                if (regulatedEntityInstance != null) {
                    recursion(regulatedEntityInstance, regulatedEntityInstance.getDisplayName() + "#" + regulatedEntityInstance.getDBID().toString() + "//" + path, list);
                }
            }
            if (components == null
                    && repeatedUnit == null
                    && hasCandidate == null
                    && hasMember==null
                    && input == null
                    && output == null
                    && hasEvent ==null
                    && catalystActivity == null
//                    && regulatedEntity == null
                    && regulator ==null
                    && physicalEntity==null
                    && regulatedEntityInstance == null) {
                list.add(path);
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new EnricherException(e.getMessage() , e);
        }
    }

    private Node makeUrl (List<String> loc, String url) throws EnricherException {

        Node root = new Node();
        root.setChildren(new HashMap<String, Node>());
//        String regex = ".*?(?=,198014)";
//        String text = "http://localhost:8080/PathwayBrowser/#DIAGRAM=1980143&ID=1980122&PATH=162582,157118,1980143,2122948";
//        String[] a = text.split(regex);

        for (String singlePath : loc){
            Node current = root;
            String id;
            String diagram = "";
            String path = "";
            String[] arrayPath = singlePath.split("//");
            for (String s : arrayPath) {
                String[] nameId = s.split("#");
                Node node1  = new Node();
                node1.setName(nameId[0]);
                node1.setDbId(Long.parseLong(nameId[nameId.length-1]));
                if (current.getChildren() == null) {
                    current.setChildren(new HashMap<String, Node>());


                    current.getChildren().put(node1.getName(),node1);
                }
                else if (!current.getChildren().containsKey(node1.getName())){

                    current.getChildren().put(node1.getName(), node1);
                }
                String newUrl = url;
                String dbId = String.valueOf(current.getDbId());

                if (current.getName() != null) {

                    if(hasDiagram(current.getDbId())){

                        diagram = String.valueOf(current.getDbId());
                        id = "";

                    } else {
                        id = String.valueOf(current.getDbId());
                    }

                    if (!diagram.isEmpty()) {

                        if ( newUrl.endsWith("#")) {
                            newUrl += "DIAGRAM=" + diagram;
                        } else {
                            newUrl += "&amp;DIAGRAM=" + diagram;
                        }
                    }
                    if (!id.isEmpty() && !diagram.isEmpty()) {
                        newUrl += "&amp;ID=" + id;
                    }
                    if (!id.isEmpty() && diagram.isEmpty()) {
                        if ( newUrl.endsWith("#")) {
                            newUrl += "DIAGRAM=" + id;
                        } else {
                            newUrl += "&amp;DIAGRAM=" + id;
                        }
                    }
                    if (!path.isEmpty()) {
                        newUrl += "&amp;PATH=" + path;

                        path += "," + dbId;

                    } else {

                        path = dbId;
                    }

                    if (newUrl.contains("DIAGRAM")) {
                        newUrl = newUrl.replaceAll("," + diagram + ".*$", "");
                        current.setUrl(newUrl);
                    }
                }

                current = current.getChildren().get(node1.getName());
            }
            String newUrl = url;

            if (current.getName() != null) {
                if(hasDiagram(current.getDbId())){

                    diagram = String.valueOf(current.getDbId());
                    id = "";

                } else {
                    id = String.valueOf(current.getDbId());
                }

                if (!diagram.isEmpty()) {

                    if ( newUrl.endsWith("#")) {
                        newUrl += "DIAGRAM=" + diagram;
                    } else {
                        newUrl += "&amp;DIAGRAM=" + diagram;
                    }
                }
                if (!id.isEmpty() && !diagram.isEmpty()) {
                    newUrl += "&amp;ID=" + id;
                }
                if (!id.isEmpty() && diagram.isEmpty()) {
                    if ( newUrl.endsWith("#")) {
                        newUrl += "DIAGRAM=" + id;
                    } else {
                        newUrl += "&amp;DIAGRAM=" + id;
                    }
                }
                if (!path.isEmpty()) {
                    newUrl += "&amp;PATH=" + path;
                }
                if (newUrl.contains("DIAGRAM")) {
                    newUrl = newUrl.replaceAll("," + diagram + ".*$", "");
                    current.setUrl(newUrl);
                }
            }
        }
        return root;
    }
}