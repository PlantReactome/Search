package org.reactome.server.tools.search.database;

import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.reactome.server.tools.search.domain.Node;
import org.reactome.server.tools.search.exception.EnricherException;

/**
 * Class for Generating Interactor Trees containing all possible Links of an Entry to the Pathway Browser
 * that is directly associated to a Reaction.
 *
 * @author Florian Korninger (fkorn@ebi.ac.uk)
 * @author Guilherme Viteri  - gviteri@ebi.ac.uk
 */
public class InteractorPathwayBrowserTreeGenerator extends PathwayBrowserTreeGenerator {

    /**
     * Recursion through all possible referrers of this entry, taking into account we
     * want to show those proteins that are directly connected to a Reaction.
     *
     * The use case is specific for the interactor intermediate page => As an user I can search for proteins e.g FUND1
     * that are not in Reactome, but interacts with another protein in Reactome and open its detail page.
     * When I check the Locations in The Pathway Browser Tree I should only see those protein directly connected into a Reaction.
     * It means, I will see the interactors corona when I reach the PathwayBrowser.
     *
     * @param instance GKInstance
     * @param node     current Node
     * @throws EnricherException
     */
    protected void recursion(GKInstance instance, Node node) throws EnricherException {
        try {
            nodeFromReference(instance, node, ReactomeJavaConstants.input);
            nodeFromReference(instance, node, ReactomeJavaConstants.output);
            nodeFromReference(instance, node, ReactomeJavaConstants.hasEvent);
            nodeFromReference(instance, node, ReactomeJavaConstants.catalystActivity);
            skipNodes(instance, node, ReactomeJavaConstants.regulator);
            skipNodes(instance, node, ReactomeJavaConstants.activeUnit);
            nodeFromAttributes(instance, node, ReactomeJavaConstants.regulatedEntity);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new EnricherException(e.getMessage(), e);
        }
    }
}
