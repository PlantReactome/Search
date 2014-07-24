package uk.ac.ebi.reactome.solr.indexer.impl;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import uk.ac.ebi.reactome.solr.indexer.model.IndexDocument;

import java.io.*;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * All Exceptions that are caught here are Exceptions thrown by the GkInstanceMySQL Adapter
 * All of them should never occur because hasValue checks if the attribute is valid before I
 * access it
 * Created by flo on 3/27/14.
 */

public class Converter {

    private static final Logger logger = Logger.getRootLogger();
    private final List<String> keywords;
    /**
     * Constructor
     */
    public Converter() {
        logger.setLevel(Level.ERROR);
        logger.addAppender(new ConsoleAppender(new PatternLayout("%-6r [%p] %c - %m%n")));

        keywords = loadFile("controlledvocabulary.csv");
    }

    /**
     * Converts GKInstance to a flat IndexDocument (Bean that later is indexed in Solr)
     * @param instance GkInstance
     * @return IndexDocument
     */
    public IndexDocument buildDocumentFromGkInstance (GKInstance instance) {
        if (instance != null && instance.getDBID()!= null){
            IndexDocument document = new IndexDocument();

            setGeneralAttributes(document, instance);

            if(instance.getSchemClass().isa(ReactomeJavaConstants.Event)){
                setEventAttributes(document,instance);
            }
            if(instance.getSchemClass().isa(ReactomeJavaConstants.PhysicalEntity)){
                setPhysicalEntityAttributes(document,instance);
            }
            else if (instance.getSchemClass().isa(ReactomeJavaConstants.Regulation)){
                setRegulationAttributes(document, instance);
            }
            logger.info("Document was successfully created");
            return document;
        }
        logger.error("Instance should not be null!");
        return null;
    }

    private void setRegulationAttributes(IndexDocument document, GKInstance instance) {
        try {
            document.setType(ReactomeJavaConstants.Regulation);
            document.setExactType(instance.getSchemClass().getName());
            if (hasValues(instance, ReactomeJavaConstants.regulatedEntity)) {
                GKInstance regulatedEntity = (GKInstance) instance.getAttributeValue(ReactomeJavaConstants.regulatedEntity);
                document.setRegulatedEntityId(String.valueOf(regulatedEntity.getDBID()));
                if (hasValue(regulatedEntity, ReactomeJavaConstants.name)) {
                    document.setRegulatedEntity((String) regulatedEntity.getAttributeValue(ReactomeJavaConstants.name));
                } else {
                    document.setRegulatedEntity(regulatedEntity.getDisplayName());
                }

            }
            if (hasValues(instance, ReactomeJavaConstants.regulator)) {
                GKInstance regulator = (GKInstance) instance.getAttributeValue(ReactomeJavaConstants.regulator);
                document.setRegulatorId(String.valueOf(regulator.getDBID()));
                if (hasValue(regulator, ReactomeJavaConstants.name)) {
                    document.setRegulator((String) regulator.getAttributeValue(ReactomeJavaConstants.name));
                } else {
                    document.setRegulator(regulator.getDisplayName());
                }
            }

        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }
    private List<String> loadFile(String fileName) {
        try {
            List<String> list = new ArrayList<String>();
            InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(fileName);
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            String line = "";
            while ((line = bufferedReader.readLine()) != null) {
                list.add(line);
            }
            bufferedReader.close();
            return list;
        } catch (FileNotFoundException e) {
            logger.error("" , e);
        } catch (IOException e) {
            logger.error("" , e);
        }
        return null;
    }


    /**
     * Sets all general attributes (attributes that should occur in events and physical entities
     * @param document IndexDocument
     * @param instance GkInstance
     */
    private void setGeneralAttributes(IndexDocument document, GKInstance instance) {

        document.setDbId(instance.getDBID());
        setNameAndSynonyms(document, instance);
        document.setKeywords(getKeywordsFromName(document.getName()));



        if (hasValues(instance, ReactomeJavaConstants.summation)){

            String summation = (concatenateSummations(instance));
            if (!summation.contains("This event has been computationally inferred from an event that has been demonstrated in another species")){
                document.setSummation(summation);
            } else {
                document.setInferredSummation(summation);
            }
        }
        if(hasValues(instance, ReactomeJavaConstants.disease)){
            document.setIsDisease(true);
        } else {
            document.setIsDisease(false);
        }


        if (hasValue(instance, ReactomeJavaConstants.compartment)){
            setCompartmentAttributes(document, instance);
        }

        if (hasValue(instance, ReactomeJavaConstants.crossReference)) {
            setCrossReference(document, instance);
        }

        try {
            if (hasValues(instance, ReactomeJavaConstants.stableIdentifier)){
                document.setStId(((GKInstance) instance.getAttributeValue(ReactomeJavaConstants.stableIdentifier)).getDisplayName());
            }

            if (hasValue(instance, ReactomeJavaConstants.species)){
                document.setSpecies(((GKInstance) instance.getAttributeValue(ReactomeJavaConstants.species)).getDisplayName());
            } else {
                document.setSpecies("Entries without species");
            }

            if (hasValues(instance, ReactomeJavaConstants.literatureReference)){

                List<GKInstance> literature = instance.getAttributeValuesList(ReactomeJavaConstants.literatureReference);
                document.setLiteratureReferenceTitle(getLiteratureAttributes(literature, ReactomeJavaConstants.title));
                document.setLiteratureReferencePubMedId(getLiteratureAttributes(literature, ReactomeJavaConstants.pubMedIdentifier));
                document.setLiteratureReferenceIsbn(getLiteratureAttributes(literature, ReactomeJavaConstants.ISBN));

                document.setLiteratureReferenceAuthor(getLiteratureAuthors(literature));
            }
        } catch (Exception e) {
            logger.error(e.getMessage(),e);
        }


    }

    private List<String> getKeywordsFromName(String name) {
        List<String> list = new ArrayList<>();
        for (String keyword : keywords) {
            if (name.toLowerCase().contains(keyword.toLowerCase())) {
                list.add(keyword);
            }
        }
        return list;
    }

    public void setCrossReference(IndexDocument document, GKInstance instance)  {
        try {
            List<GKInstance> crossReferenceInstanceList = instance.getAttributeValuesList(ReactomeJavaConstants.crossReference);
            List<String> identifiers = new ArrayList<>();
            List<String> databases = new ArrayList<>();
            for (GKInstance crossReferenceInstance : crossReferenceInstanceList) {
                String id = (String) crossReferenceInstance.getAttributeValue(ReactomeJavaConstants.identifier);
                String db = ((GKInstance) crossReferenceInstance.getAttributeValue(ReactomeJavaConstants.referenceDatabase)).getDisplayName();
                identifiers.add(id);
                identifiers.add(db + ":" + id);
                databases.add(db);
            }
            document.setCrossReferences(identifiers);
            document.setCrossReferenceDatabase(databases);
        }catch (Exception e) {
            e.printStackTrace();  // rework
        }
    }

    /**
     * sets all event attributes
     * @param document IndexDocument
     * @param instance GkInstance
     */
    private void setEventAttributes (IndexDocument document, GKInstance instance) {

        document.setExactType(instance.getSchemClass().getName());

        if (instance.getSchemClass().isa(ReactomeJavaConstants.Pathway)) {
            document.setType(instance.getSchemClass().getName());
        } else {
            document.setType(ReactomeJavaConstants.Reaction);
        }

        if (hasValue(instance, ReactomeJavaConstants.catalystActivity)){
            setCatalystActivityAttributes(document, instance);
        }

        if (hasValue(instance, ReactomeJavaConstants.goBiologicalProcess)){
            document.setGoBiologicalProcessAccessions(getGoTermAccession(instance, ReactomeJavaConstants.goBiologicalProcess));
            document.setGoBiologicalProcessName(getGoTermName(instance, ReactomeJavaConstants.goBiologicalProcess));
        }
    }

    /**
     * sets all physical entity attributes
     * @param document IndexDocument
     * @param instance GkInstance
     */
    private void setPhysicalEntityAttributes (IndexDocument document, GKInstance instance) {

        if (hasValue(instance, ReactomeJavaConstants.goCellularComponent)){
            document.setGoCellularComponentAccessions(getGoTermAccession(instance, ReactomeJavaConstants.goCellularComponent));
            document.setGoCellularComponentName(getGoTermName(instance, ReactomeJavaConstants.goCellularComponent));
        }

        if (hasValue(instance, ReactomeJavaConstants.referenceEntity)){
            setReferenceEntityAttributes(document, instance);
        } else {
            document.setExactType(instance.getSchemClass().getName());

            if (instance.getSchemClass().isa(ReactomeJavaConstants.EntitySet)) {
                document.setType("Set");
            } else if (instance.getSchemClass().isa(ReactomeJavaConstants.GenomeEncodedEntity)) {
                document.setType("Genes and Transcripts");
            } else {
                document.setType(instance.getSchemClass().getName());
            }
        }
    }

    /**
     * returns the concatenated summation (will be better to display as a result when searching
     * @param instance GkInstance
     * @return String (concatenated summation
     */
    private String concatenateSummations(GKInstance instance) {
        String summation = "";
        try {
            List<GKInstance> summationsList = instance.getAttributeValuesList(ReactomeJavaConstants.summation);
            boolean first = true;
            for (GKInstance summationInstance : summationsList) {
                if (first) {
                    summation = summationInstance.getAttributeValue(ReactomeJavaConstants.text).toString();
                } else {
                    summation = summation + "<br>" + summationInstance.getAttributeValue(ReactomeJavaConstants.text).toString();
                }
//                buffer = buffer.replaceAll("(?i)(\\<p\\>)|(\\</p\\>)|(\\<br\\>)", " ");
//                buffer = buffer.replaceAll("(?i)(\\<ul\\>)|(\\</ul\\>)", " ");
//                buffer = buffer.replaceAll("(?i)(\\<li\\>)|(\\</li\\>)", ", ");


            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return summation;
    }


    /**
     * Sets the name and if name has multiple entries synonyms
     * @param document IndexDocument
     * @param instance GkInstance
     */
    private void setNameAndSynonyms(IndexDocument document, GKInstance instance) {
        try {
            if (hasValues(instance, ReactomeJavaConstants.name)){
                List<String> list =  instance.getAttributeValuesList(ReactomeJavaConstants.name);
                if (list == null || list.size()<1) {
                    document.setName(instance.getDisplayName());
                } else {
                    document.setName(list.get(0));
                    if (list.size()>1){
                        document.setSynonyms(list.subList(1,list.size()-1));
                    }
                }
            }
            else {
                logger.warn(instance.getDBID() + " has no ordinary Name");
                document.setName(instance.getDisplayName());
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    /**
     * Sets compartment accessions and names
     * @param document IndexDocument
     * @param instance GkInstance
     */
    private void setCompartmentAttributes (IndexDocument document, GKInstance instance) {
        try {
            List<GKInstance> compartments = (List<GKInstance>) instance.getAttributeValuesList(ReactomeJavaConstants.compartment);
            List<String> compartmentAccession = new ArrayList<>();
            List<String> compartmentName = new ArrayList<>();
            for (GKInstance compartment : compartments) {
                compartmentAccession.add(compartment.getAttributeValue(ReactomeJavaConstants.accession).toString());
                compartmentName.add(compartment.getDisplayName());
            }
            document.setCompartmentAccession(compartmentAccession);
            document.setCompartmentName(compartmentName);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }

    }

    /**
     * returns a list of all authors for multiple literature references
     * @param literature GKInstances
     * @return list of Strings (Authors)
     */
    private List<String> getLiteratureAuthors(List<GKInstance> literature) {
        List<String> list = new ArrayList<>();
        for (GKInstance gkInstance : literature) {
            if (hasValues(gkInstance, ReactomeJavaConstants.author)) {
                try {
                    List<GKInstance> authors = (List<GKInstance>) gkInstance.getAttributeValuesList(ReactomeJavaConstants.author);
                    for (GKInstance author : authors) {
                        list.add(author.getDisplayName());
                    }
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
            }
        }
        return list;
    }

    /**
     * returns a list of attribute values of a given field for literature references
     * @param literature GkInstances
     * @param fieldName name of the attribute field
     * @return list of String (attributes)
     */
    private List<String> getLiteratureAttributes(List<GKInstance> literature, String fieldName)  {

        List<String> list = new ArrayList<>();
        for (GKInstance gkInstance : literature) {
            if (hasValue(gkInstance, fieldName)) {
                try {
                    list.add (gkInstance.getAttributeValue(fieldName).toString());
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
            }
        }
        return list;
    }

    /**
     * Sets all reference entity attributes
     * @param document IndexDocument
     * @param instance GkInstance
     */
    private void setReferenceEntityAttributes(IndexDocument document, GKInstance instance) {

        try {


            GKInstance referenceEntity = (GKInstance) instance.getAttributeValue(ReactomeJavaConstants.referenceEntity);

            document.setType(setReferenceTypes(referenceEntity));
            document.setExactType(referenceEntity.getSchemClass().getName());

            setReferenceNameAndSynonyms(document, referenceEntity);

            if (hasValue(referenceEntity, ReactomeJavaConstants.crossReference)) {
                setReferenceCrossReference (document, referenceEntity);
            }

            if (hasValues(referenceEntity, ReactomeJavaConstants.geneName)){
                document.setReferenceGeneNames(referenceEntity.getAttributeValuesList(ReactomeJavaConstants.geneName));
            }

            GKInstance referenceDatabase = (GKInstance) referenceEntity.getAttributeValue(ReactomeJavaConstants.referenceDatabase);
            String id = null;
            String db = (String) referenceDatabase.getAttributeValue(ReactomeJavaConstants.name);
            if (hasValue(referenceEntity, ReactomeJavaConstants.variantIdentifier)) {
                id = (String) referenceEntity.getAttributeValue(ReactomeJavaConstants.variantIdentifier);
            }
            else if (hasValue(referenceEntity, ReactomeJavaConstants.identifier)){
                id = (String) referenceEntity.getAttributeValue(ReactomeJavaConstants.identifier);
            }
            if(id!=null) {
                List<String> referenceIdentifiers = new LinkedList<String>();
                referenceIdentifiers.add(id);
                referenceIdentifiers.add(db + ":" + id);
                document.setReferenceIdentifiers(referenceIdentifiers);
            }

            if (hasValues(referenceEntity, ReactomeJavaConstants.otherIdentifier)){
                document.setReferenceOtherIdentifier(referenceEntity.getAttributeValuesList(ReactomeJavaConstants.otherIdentifier));
            }
            if (hasValue(referenceEntity, ReactomeJavaConstants.referenceDatabase)) {
                GKInstance database = (GKInstance) referenceEntity.getAttributeValue(ReactomeJavaConstants.referenceDatabase);
                document.setDatabaseName((String) database.getAttributeValue(ReactomeJavaConstants.name));
                String url = (String) database.getAttributeValue(ReactomeJavaConstants.accessUrl);
                if(id!=null) {
                    document.setReferenceURL(url.replace("###ID###", id));
                }
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    private void setReferenceCrossReference(IndexDocument document, GKInstance instance) {
        try {
            List<GKInstance> crossReferenceInstanceList = instance.getAttributeValuesList(ReactomeJavaConstants.crossReference);
            List<String> identifier = new ArrayList<>();
            List<String> database = new ArrayList<>();
            for (GKInstance crossReferenceInstance : crossReferenceInstanceList) {
                identifier.add((String) crossReferenceInstance.getAttributeValue(ReactomeJavaConstants.identifier));
                database.add(((GKInstance) crossReferenceInstance.getAttributeValue(ReactomeJavaConstants.referenceDatabase)).getDisplayName());
            }
            document.setReferenceCrossReferences(identifier);
            document.setReferenceCrossReferenceDatabase(database);
        }catch (Exception e) {
            e.printStackTrace();  // rework
        }
    }

    /**
     * Renames the Types according to their Schema Class
     * @param instance GkInstance
     */
    private String setReferenceTypes (GKInstance instance) {
        if (instance.getSchemClass().isa(ReactomeJavaConstants.ReferenceGeneProduct) || instance.getSchemClass().isa(ReactomeJavaConstants.ReferenceIsoform) ) {
            return "Protein";

        } else if (instance.getSchemClass().isa(ReactomeJavaConstants.ReferenceDNASequence)) {
            return "DNA Sequence";
        } else if (instance.getSchemClass().isa(ReactomeJavaConstants.ReferenceRNASequence)) {
            return "RNA Sequence";
        } else if (instance.getSchemClass().isa(ReactomeJavaConstants.ReferenceMolecule) || instance.getSchemClass().isa(ReactomeJavaConstants.ReferenceGroup)) {
            return "Chemical Compound";
        } else {
            return instance.getSchemClass().getName();
        }
    }

    /**
     * Sets GoMolecularFunction accession-numbers and names for multiple Catalyst Activities
     * @param document IndexDocument
     * @param instance GkInstance
     */
    private void setCatalystActivityAttributes (IndexDocument document, GKInstance instance) {
        try {

            List<GKInstance> catalystActivity = (List<GKInstance>) instance.getAttributeValuesList(ReactomeJavaConstants.catalystActivity);
            List<String> goMolecularFunctionAccession = new ArrayList<>();
            List<String> goMolecularFunctionName = new ArrayList<>();
            for (GKInstance gkInstance : catalystActivity) {
                goMolecularFunctionAccession.addAll(getGoTermAccession(gkInstance, ReactomeJavaConstants.activity));
                goMolecularFunctionName.add(getGoTermName(gkInstance, ReactomeJavaConstants.activity));
            }
            document.setGoMolecularFunctionAccession(goMolecularFunctionAccession);
            document.setGoMolecularFunctionName(goMolecularFunctionName);

        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    /**
     * Sets a reference entity name and if name has multiple values synonyms
     * @param document IndexDocument
     * @param instance GkInstance
     */
    private void setReferenceNameAndSynonyms (IndexDocument document, GKInstance instance) {
        try {
            if (hasValues(instance, ReactomeJavaConstants.name)){
                List<String> list =  instance.getAttributeValuesList(ReactomeJavaConstants.name);
                if (list == null || list.size() < 1) {
                    document.setReferenceName(instance.getDisplayName());
                } else {
                    document.setReferenceName(list.get(0));
                    if (list.size()>1){
                        document.setReferenceSynonyms(list.subList(1, list.size()));
                    }
                }
            }
//            Does not make sense after testing :/
//            else {
//                document.setReferenceName(instance.getDisplayName());
//            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    /**
     * gets a goTermAccessionNumber
     * @param instance GkInstance
     * @param fieldName name of the InstanceField
     * @return String goTermAccessionNumber
     */
    private List<String> getGoTermAccession(GKInstance instance, String fieldName) {
        try {
            List<String> rtn = new LinkedList<String>();
            GKInstance goTerm = (GKInstance) instance.getAttributeValue(fieldName);
            if (hasValue(goTerm, ReactomeJavaConstants.accession)){
                String go = goTerm.getAttributeValue(ReactomeJavaConstants.accession).toString();
                rtn.add(go);
                rtn.add("go:" + go);
                return  rtn;
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        logger.error("Go Term: " + instance.getDBID() + "has no accession number");
        return null;
    }

    /**
     * gets a goTermAccessionName
     * @param instance GkInstance
     * @param fieldName name of the InstanceField
     * @return String goTermAccessionNumber
     */
    private String getGoTermName(GKInstance instance, String fieldName) {
        try {
            GKInstance goTerm = (GKInstance) instance.getAttributeValue(fieldName);
            return goTerm.getDisplayName();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        logger.error("Go Term: " + instance.getDBID() + "has no name");
        return null;
    }

    /**
     * Checks if Field is a valid attribute and if the attribute is not null
     * @param instance GkInstance
     * @param fieldName name of the InstanceField
     * @return true if field has values
     */
    private boolean hasValue(GKInstance instance, String fieldName)  {
        if(instance.getSchemClass().isValidAttribute(fieldName)) {
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

    /**
     * Checks if Field is a valid attribute and if the attribute list is not null or empty
     * @param instance GkInstance
     * @param fieldName name of the InstanceField
     * @return true if field has values
     */
    private boolean hasValues(GKInstance instance, String fieldName) {
        if(instance.getSchemClass().isValidAttribute(fieldName)) {
            try {
                if (instance.getAttributeValuesList(fieldName) != null && ! (instance.getAttributeValuesList(fieldName).isEmpty())) {
                    return true;
                }
            } catch (Exception e) {
                // will never happen because i check it above
                logger.error(e.getMessage(), e);
            }
        }
        logger.info(instance.getDBID() + " has no values: " + fieldName);
        return false;
    }
}
