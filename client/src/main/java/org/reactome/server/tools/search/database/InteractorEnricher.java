package org.reactome.server.tools.search.database;

import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.MySQLAdaptor;
import org.reactome.server.tools.interactors.model.Interaction;
import org.reactome.server.tools.search.exception.EnricherException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.Collection;
import java.util.List;

/**
 * @author Guilherme S Viteri <gviteri@ebi.ac.uk>
 */

public class InteractorEnricher {

    private static MySQLAdaptor dba;
    protected static final Logger logger = LoggerFactory.getLogger(Enricher.class);

    /**
     * Constructor to make this class expandable
     */
    public InteractorEnricher() {}

    /**
     * Constructor that sets up a database connection
     * @param host,database,user,password,port parameters to set up connection
     * @throws EnricherException
     */
    public InteractorEnricher(String host, String database, String user, String password, Integer port) throws EnricherException {
        try {
            dba = new MySQLAdaptor(host,database, user, password, port);
        } catch (SQLException e) {
            logger.error("Could not initiate MySQLAdapter", e);
            throw new EnricherException("Could not initiate MySQLAdapter", e);
        }
    }

    public void notclear(List<Interaction> interactions) {
        for (Interaction interaction : interactions) {

            try {
                Collection<?> instances = dba.fetchInstanceByAttribute(ReactomeJavaConstants.ReferenceEntity, ReactomeJavaConstants.identifier, "=", interaction.getInteractorB().getAcc());
                for (Object object : instances) {
                    GKInstance instance = (GKInstance)object;
                    if (interaction.getInteractorB().getAcc().equals(instance.getAttributeValue(ReactomeJavaConstants.identifier))) {
                        GKInstance instance1 = (GKInstance) instance.getReferers(ReactomeJavaConstants.referenceEntity);
                        System.out.println(instance1.getDBID());
                    }

                }

            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }
}
