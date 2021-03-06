package org.reactome.server.tools.search.database;

import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.MySQLAdaptor;
import org.reactome.server.tools.search.domain.*;
import org.reactome.server.tools.search.exception.EnricherException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Queries the MySql database and converts entry to a local object
 *
 * @author Florian Korninger (fkorn@ebi.ac.uk)
 * @version 1.0
 */
public class Enricher implements IEnricher  {
    private static MySQLAdaptor dba;
    protected static final Logger logger = LoggerFactory.getLogger(Enricher.class);

    /**
     * Constructor to make this class expandable
     */
    public Enricher() {}

    /**
     * Constructor that sets up a database connection
     * @param host,database,user,password,port parameters to set up connection
     * @throws EnricherException
     */
    public Enricher(String host, String database, String user, String password, Integer port) throws EnricherException {
        try {
            dba = new MySQLAdaptor(host,database, user, password, port);
        } catch (SQLException e) {
            logger.error("Could not initiate MySQLAdapter", e);
            throw new EnricherException("Could not initiate MySQLAdapter", e);
        }
    }

    /**
     * Only public method available to generate a entry from the database
     * @param id MySql Adapter will fetch instance for given StId old_StId or DbId
     * @return EnrichedEntry
     * @throws EnricherException
     */
    @Override
    public EnrichedEntry enrichEntry(String id, boolean interactor) throws EnricherException {
        try {
            GKInstance instance = getInstance(id);
            if (instance != null) {
                EnrichedEntry enrichedEntry = new EnrichedEntry();
                new GeneralAttributeEnricher().setGeneralAttributes(instance, enrichedEntry, interactor);
                if (instance.getSchemClass().isa(ReactomeJavaConstants.Event)) {
                    new EventAttributeEnricher().setEventAttributes(instance, enrichedEntry);
                } else if (instance.getSchemClass().isa(ReactomeJavaConstants.PhysicalEntity)) {
                    new PhysicalEntityAttributeEnricher().setPhysicalEntityAttributes(instance, enrichedEntry);
                } else if (instance.getSchemClass().isa(ReactomeJavaConstants.Regulation)) {
                    enrichedEntry.setRegulation(getRegulation(instance));
                } else {
                    logger.warn("Unexpected schema class found");
                }
                return enrichedEntry;
            }
        } catch (Exception e) {
            logger.error("Error occurred when trying to fetch Instance by dbId");
//            throw new EnricherException("Error occurred when trying to fetch Instance by dbId", e);
        }
        return null;
    }

    /**
     * Method that returns a list of cross references
     * @param instance GkInstance
     * @param fieldName Name of the database field
     * @return List of cross references
     * @throws EnricherException
     */
    protected Map<String, List<CrossReference>> getCrossReferences(GKInstance instance, String fieldName, Map<String, List<CrossReference>> map) throws EnricherException {
        if (hasValues(instance, fieldName)){
            try {
                if (map == null) {
                    map = new HashMap<>();
                }
                List<?> crossReferenceInstanceList = instance.getAttributeValuesList(fieldName);
                for (Object crossReferenceInstance : crossReferenceInstanceList) {

                    String identifier = getAttributeString((GKInstance) crossReferenceInstance, ReactomeJavaConstants.identifier);
                    Database database = getDatabase((GKInstance) crossReferenceInstance, identifier);
                    if(database != null) {
                        CrossReference crossReference = new CrossReference(identifier, database);
                        if (map.containsKey(database.getName())) {
                            map.get(database.getName()).add(crossReference);
                        } else {
                            List<CrossReference> list = new ArrayList<>();
                            list.add(crossReference);
                            map.put(database.getName(), list);
                        }
                    }
                }
                return map;
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
                throw new EnricherException(e.getMessage() , e);
            }
        }
        return null;
    }

    protected String getStableIdentifier(GKInstance instance) throws Exception {
        if (hasValue(instance, ReactomeJavaConstants.stableIdentifier)){
            return (String) ((GKInstance) instance.getAttributeValue(ReactomeJavaConstants.stableIdentifier)).getAttributeValue(ReactomeJavaConstants.identifier);
        }else{
            logger.error("No StableIdentifier for " + instance.getDBID() + " [" + instance.getDisplayName() + "]");
            return instance.getDBID() + "";
        }
    }

    /**
     * This method provides a list of referenced entities identified by the field name
     * @param instance GkInstance
     * @param fieldName name of the InstanceField
     * @return List of referenced entries
     * @throws EnricherException
     */
    protected List<EntityReference> getEntityReferences(GKInstance instance, String fieldName) throws EnricherException {
        if(hasValues(instance, fieldName)) {
            try {
                List<EntityReference> entityReferenceList = new ArrayList<>();
                List<?> componentsList = instance.getAttributeValuesList(fieldName);
                for (Object gkInstance : componentsList) {
                    entityReferenceList.add(getEntityReferenceHelper((GKInstance) gkInstance));
                }
                return entityReferenceList;
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
                throw new EnricherException(e.getMessage() , e);
            }
        }
        return null;
    }

    /**
     * Helper method to determine if a instance contains a diagram
     * @param dbId id
     * @return boolean
     * @throws EnricherException
     */
    protected boolean hasDiagram(long dbId) throws EnricherException {
        try {
            GKInstance inst = dba.fetchInstance(dbId);
            if (inst.getSchemClass().isa(ReactomeJavaConstants.Pathway)) {
                Collection<?> diagrams = inst.getReferers(ReactomeJavaConstants.representedPathway);
                if (diagrams != null && diagrams.size() > 0) {
                    for (Object diagram1 : diagrams) {
                        GKInstance diagram = (GKInstance) diagram1;
                        if (diagram.getSchemClass().isa(ReactomeJavaConstants.PathwayDiagram)) {
                            return true;
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new EnricherException(e.getMessage() , e);
        }
        return false;
    }

    /**
     * Method that returns an entityReference for a given field
     * @param instance GkInstance
     * @param fieldName name of the InstanceField
     * @return EntityReference
     * @throws EnricherException
     */
    protected EntityReference getEntityReference(GKInstance instance, String fieldName) throws EnricherException {
        if (hasValue(instance, fieldName)) {
            try {
                GKInstance physicalEntityInstance = (GKInstance) instance.getAttributeValue(fieldName);
                return getEntityReferenceHelper(physicalEntityInstance);
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
                throw new EnricherException(e.getMessage() , e);
            }
        }
        return null;
    }

    /**
     *
     * @param dbID Id
     * @param instance GkInstance
     * @return list of entityReference
     * @throws EnricherException
     */
    protected List<EntityReference> getReferedEntityReferences(Long dbID, GKInstance instance) throws EnricherException {

        try {
            Collection<?> derivedInstanceList = instance.getReferers(ReactomeJavaConstants.referenceEntity);
            if (derivedInstanceList != null && !derivedInstanceList.isEmpty()) {
                List<EntityReference> entityReferenceList = new ArrayList<>();
                for (Object derivedObject : derivedInstanceList) {
                    GKInstance derivedInstance = (GKInstance) derivedObject;
                    if (!dbID.equals(derivedInstance.getDBID())) {
                        entityReferenceList.add(getEntityReferenceHelper(derivedInstance));
                    }
                }
                return entityReferenceList;
            }
            return null;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new EnricherException(e.getMessage() , e);
        }
    }

    /**
     * Method that returns a List of GoTerms, depending on the fieldName.
     * The GoTerms can be compartments, goMolecularFunctions, goBiologicalProcesses
     * @param instance GkInstance
     * @param fieldName name of the InstanceField
     * @return List of GoTerms
     * @throws EnricherException
     */
    protected List<GoTerm> getGoTerms (GKInstance instance, String fieldName) throws EnricherException {
        if (hasValues(instance, fieldName)) {
            List<GoTerm> goTermList = new ArrayList<>();
            try {
                List<?> goTermInstanceList = instance.getAttributeValuesList(fieldName);
                for (Object goTermInstance : goTermInstanceList) {
                    goTermList.add(getGoTerm((GKInstance) goTermInstance));
                }
                return goTermList;
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
                throw new EnricherException(e.getMessage() , e);
            }
        }
        return null;
    }

    /**
     * Method that returns a single Go Term (Name, Accession, URL)
     * @param instance GkInstance
     * @return GoTerm
     * @throws EnricherException
     */
    protected GoTerm getGoTerm (GKInstance instance) throws EnricherException {
        GoTerm goTerm = new GoTerm();
        goTerm.setName(instance.getDisplayName());
        try {
            goTerm.setAccession((String) instance.getAttributeValue(ReactomeJavaConstants.accession));
            goTerm.setDatabase(getDatabase(instance,goTerm.getAccession()));
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new EnricherException(e.getMessage() , e);
        }
        return goTerm;
    }

    /**
     * Returns a Sting value for a given field
     * @param instance GkInstance
     * @param fieldName name of the InstanceField
     * @return String
     * @throws EnricherException
     */
    protected String getAttributeString(GKInstance instance, String fieldName) throws EnricherException {
        if (hasValue(instance,fieldName)) {
            try {
                return instance.getAttributeValue(fieldName).toString();
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
                throw new EnricherException(e.getMessage() , e);
            }
        }
        return null;
    }

    /**
     * Returns an integer value for a given field
     * @param instance GkInstance
     * @param fieldName name of the InstanceField
     * @return Integer
     * @throws EnricherException
     */
    protected Integer getAttributeInteger(GKInstance instance, String fieldName) throws EnricherException {
        if (hasValue(instance,fieldName)) {
            try {
                return (Integer) instance.getAttributeValue(fieldName);
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
                throw new EnricherException(e.getMessage() , e);
            }
        }
        return null;
    }

    /**
     * Returns a List of Strings for a given field
     * Adding the single objects to a list of strings is necessary to work around a unchecked assignment warning
     * @param instance GkInstance
     * @param fieldName name of the InstanceField
     * @return List of String
     * @throws EnricherException
     */
    protected List<String> getAttributes (GKInstance instance, String fieldName) throws EnricherException {
        if (hasValues(instance, fieldName)) {
            try {
                List<?> list = instance.getAttributeValuesList(fieldName);
                List<String> attributes = new ArrayList<>();
                for (Object object : list) {
                    attributes.add(object.toString());
                }
                return attributes;
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
                throw new EnricherException(e.getMessage() , e);
            }
        }
        return null;
    }

    /**
     * Checks if Field is a valid attribute and if the attribute is not null
     * @param instance GkInstance
     * @param fieldName name of the InstanceField
     * @return true if field has values
     */
    protected boolean hasValue(GKInstance instance, String fieldName) throws EnricherException {
        if(instance!= null && instance.getSchemClass().isValidAttribute(fieldName)) {
            try {
                if (instance.getAttributeValue(fieldName) != null) {
                    return true;
                }
            } catch (Exception e) { // will never happen because i check it above
                logger.error(e.getMessage(), e);
                throw new EnricherException(e.getMessage() , e);
            }
        }
        return false;
    }

    /**
     * Checks if Field is a valid attribute and if the attribute list is not null or empty
     * @param instance GkInstance
     * @param fieldName name of the InstanceField
     * @return true if field has values
     */
    protected boolean hasValues(GKInstance instance, String fieldName) throws EnricherException {
        if(instance!= null && instance.getSchemClass().isValidAttribute(fieldName)) {
            try {
                if (instance.getAttributeValuesList(fieldName) != null && ! (instance.getAttributeValuesList(fieldName).isEmpty())) {
                    return true;
                }
            } catch (Exception e) { // will never happen because i check it above
                logger.error(e.getMessage(), e);
                throw new EnricherException(e.getMessage() , e);
            }
        }
        return false;
    }

    /**
     * Returns a database object that contains name and url
     * @param instance GkInstance
     * @param identifier external reference identifier, to be replaced in the url
     * @return Database
     * @throws EnricherException
     */
    protected Database getDatabase (GKInstance instance, String identifier) throws EnricherException {
        if (instance != null && identifier != null) {
            if (hasValue(instance, ReactomeJavaConstants.referenceDatabase)) {
                try {
                    GKInstance database = (GKInstance) instance.getAttributeValue(ReactomeJavaConstants.referenceDatabase);
                    String name = (String) database.getAttributeValue(ReactomeJavaConstants.name);
                    String accessUrl = (String) database.getAttributeValue(ReactomeJavaConstants.accessUrl);
                    return new Database(name, accessUrl.replace("###ID###", identifier));
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                    throw new EnricherException(e.getMessage(), e);
                }
            }
        }
        return null;
    }

    /**
     * Returns a List of Display Names for given field
     * @param instance GkInstance
     * @param fieldName name of the InstanceField
     * @return List of Display Names
     * @throws EnricherException
     */
    protected List<String> getAttributesDisplayNames(GKInstance instance, String fieldName) throws EnricherException {
        if (hasValues(instance, fieldName)) {
            try {
                List<String> list = new ArrayList<>();
                List<?> instanceList = instance.getAttributeValuesList(fieldName);
                for (Object attributeObject : instanceList) {
                    if(attributeObject instanceof GKInstance) {
                        GKInstance attributeInstance = (GKInstance) attributeObject;
                        list.add(attributeInstance.getDisplayName());
                    } else if (attributeObject instanceof String){
                        list.add((String) attributeObject);
                    } else {
                        logger.error(instance.getSchemClass().getName() + " field type unknown for " + fieldName);
                    }
                }
                return list;
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
                throw new EnricherException(e.getMessage(), e);
            }
        }
        return null;
    }

    /**
     * Returns a display name for given field
     * @param instance GkInstance
     * @param fieldName name of the InstanceField
     * @return String DisplayName
     * @throws EnricherException
     */
    protected String getAttributeDisplayName(GKInstance instance, String fieldName) throws EnricherException {
        if (hasValues(instance, fieldName)) {
            try {
                GKInstance attributeInstance = (GKInstance) instance.getAttributeValue(fieldName);
                return attributeInstance.getDisplayName();
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
                throw new EnricherException(e.getMessage(), e);
            }
        }
        return null;
    }

    /**
     * Returns a list of regulations for given field
     * @param instance GkInstance
     * @param fieldName name of the InstanceField
     * @return List of Regulation
     * @throws EnricherException
     */
    protected Map<String,List<Regulation>> getRegulations(GKInstance instance, String fieldName) throws EnricherException {
        if (instance != null && fieldName!=null) {
            try {
                Map<String,List<Regulation>> map = new HashMap<>();
                Collection<?> regulationInstanceList = instance.getReferers(fieldName);
                if (regulationInstanceList!=null && !regulationInstanceList.isEmpty()) {
                    for (Object regulationObject : regulationInstanceList) {
                        Regulation regulation = getRegulation((GKInstance) regulationObject);
                        if (map.containsKey(regulation.getRegulationType())){
                            map.get(regulation.getRegulationType()).add(regulation);
                        } else {
                            List<Regulation> list = new ArrayList<>();
                            list.add(regulation);
                            map.put(regulation.getRegulationType(), list);
                        }
                    }
                }
                return map;
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
                throw new EnricherException(e.getMessage(), e);
            }
        }
        return null;
    }

    /**
     * Returns a regulation object
     * @param instance GkInstance
     * @return Regulation
     * @throws EnricherException
     */
    private Regulation getRegulation(GKInstance instance) throws EnricherException {
        try {
            Regulation regulation = new Regulation();
            regulation.setRegulator(getEntityReference(instance, ReactomeJavaConstants.regulator));
            regulation.setRegulatedEntity(getEntityReference(instance, ReactomeJavaConstants.regulatedEntity));

            if (hasValues(instance, ReactomeJavaConstants.regulationType)) {
                GKInstance regulationType = (GKInstance) instance.getAttributeValue(ReactomeJavaConstants.regulationType);
                if (hasValue(regulationType, ReactomeJavaConstants.name)) {
                    regulation.setRegulationType((String) regulationType.getAttributeValue(ReactomeJavaConstants.name));
                } else {
                    regulation.setRegulationType(regulationType.getDisplayName());
                }
            } else {
                regulation.setRegulationType(instance.getSchemClass().getName());
            }
            return regulation;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new EnricherException(e.getMessage(), e);
        }
    }

    /**
     * Helper method that creates an entityReference
     * @param instance GkInstance
     * @return EntityReference
     * @throws EnricherException
     */
    protected EntityReference getEntityReferenceHelper(GKInstance instance) throws EnricherException {
        if (instance != null) {
            try {
                EntityReference physicalEntity = new EntityReference();
                physicalEntity.setStId(getStableIdentifier(instance));
                physicalEntity.setName(getAttributeString(instance, ReactomeJavaConstants.name));
                if (hasValue(instance, ReactomeJavaConstants.species)) {
                    physicalEntity.setSpecies(((GKInstance) instance.getAttributeValue(ReactomeJavaConstants.species)).getDisplayName());
                }
                if (hasValue(instance, ReactomeJavaConstants.compartment)) {
                    physicalEntity.setCompartment(((GKInstance) instance.getAttributeValue(ReactomeJavaConstants.compartment)).getDisplayName());
                }
                return physicalEntity;
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
                throw new EnricherException(e.getMessage() , e);
            }
        }
        return null;
    }

    private GKInstance getInstance(String identifier) throws Exception {
        identifier = identifier.trim().split("\\.")[0];
        if (identifier.startsWith("REACT")){
            return getInstance(dba.fetchInstanceByAttribute(ReactomeJavaConstants.StableIdentifier, "oldIdentifier", "=", identifier));
        }else if (identifier.startsWith("R-")) {
            return getInstance(dba.fetchInstanceByAttribute(ReactomeJavaConstants.StableIdentifier, ReactomeJavaConstants.identifier, "=", identifier));
        } else {
            return dba.fetchInstance(Long.parseLong(identifier));
        }
    }

    private GKInstance getInstance(Collection<GKInstance> target) throws Exception {
        if(target == null) throw new Exception("No entity found");
        if(target.size()!=1) throw new Exception("Many options have been found for the specified identifier");
        GKInstance stId = target.iterator().next();
        return (GKInstance) dba.fetchInstanceByAttribute(ReactomeJavaConstants.DatabaseObject, ReactomeJavaConstants.stableIdentifier, "=", stId).iterator().next();
    }

    /**
     * The FrontPage data schema holds all the TopLevelPathways.
     * In the search only TopLevelPathways should be listed
     *
     * @return Set of FrontPages.
     * @throws Exception
     */
    protected Set<String> loadFrontPage() throws Exception {
        Set<String> topLevelPathway = new HashSet<>();
        Collection<?> frontPage = dba.fetchInstancesByClass(ReactomeJavaConstants.FrontPage);
        GKInstance instance = (GKInstance) frontPage.iterator().next();
        Collection<GKInstance> instances = instance.getAttributeValuesList(ReactomeJavaConstants.frontPageItem);

        /**
         * Matching only the number in the stable identifier, because in the
         * FrontPage model there aren't top level pathways for all species.
         * The IDs are the same for the species.
         */
        Pattern p = Pattern.compile("([0-9]+)");

        for (GKInstance gki : instances) {
            String stId = getStableIdentifier(gki);
            Matcher m = p.matcher(stId);

            if (m.find()) {
                String s = m.group(1);
                topLevelPathway.add(s);
            }
        }

        return topLevelPathway;
    }

}