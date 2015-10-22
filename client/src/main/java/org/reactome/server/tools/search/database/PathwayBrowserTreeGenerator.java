package org.reactome.server.tools.search.database;

import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.reactome.server.tools.search.domain.Node;
import org.reactome.server.tools.search.exception.EnricherException;

import java.util.*;

/**
 * Class for Generating Trees containing all possible Links of an Entry to the Pathway Browser
 * 22 October 2015
 * @author Florian Korninger (fkorn@ebi.ac.uk)
 * @author Guilherme Viteri  - gviteri@ebi.ac.uk
 * @version 1.1
 */
public class PathwayBrowserTreeGenerator extends Enricher {

    /**
     * Lookup map used for generating the initial Graph
     */
    private Map<String, Node> nodeMap = new HashMap<>();

    private static final String PATHWAY_BROWSER_URL = "/PathwayBrowser/#/";
    private static final String SEL = "&amp;SEL=";
    private static final String PATH = "&amp;PATH=";

    /**
     * Main method for generating a Graph representing the different locations of an Entry in the Pathway Browser
     * This Graph will be used to generate multiple Trees where roots represent Top level pathways
     * @param instance GKInstance
     * @return Set of Trees, each root represents a TopLevelPathway
     * @throws Exception
     */
    public Set<Node> generateGraphForGivenGkInstance(GKInstance instance) throws Exception {
        /*Node is called graph because its structure is building the graph*/
        Node graph = new Node();
        graph.setStId(getStableIdentifier(instance));
        graph.setName(instance.getDisplayName());
        graph.setType(instance.getSchemClass().getName());
        graph.setSpecies(getAttributeDisplayName(instance, ReactomeJavaConstants.species));
        graph.setDiagram(hasDiagram(instance.getDBID()));
        recursion(instance, graph);
        Set<Node> leaves = graph.getLeaves();
        return buildTreesFromLeaves(leaves);
    }

    /**
     * Recursion through all possible referrers of this entry
     * @param instance GKInstance
     * @param node current Node
     * @throws EnricherException
     */
    private void recursion(GKInstance instance, Node node) throws EnricherException {
        try {
            nodeFromReference(instance, node, ReactomeJavaConstants.hasComponent);
            nodeFromReference(instance, node, ReactomeJavaConstants.repeatedUnit);
            nodeFromReference(instance, node, ReactomeJavaConstants.hasCandidate);
            nodeFromReference(instance, node, ReactomeJavaConstants.hasMember);
            nodeFromReference(instance, node, ReactomeJavaConstants.input);
            nodeFromReference(instance, node, ReactomeJavaConstants.output);
            nodeFromReference(instance, node, ReactomeJavaConstants.hasEvent);
            nodeFromReference(instance, node, ReactomeJavaConstants.activeUnit);
            nodeFromReference(instance, node, ReactomeJavaConstants.catalystActivity);
            skipNodes(instance, node, ReactomeJavaConstants.regulator);
            skipNodes(instance, node, ReactomeJavaConstants.physicalEntity);
            nodeFromAttributes(instance, node, ReactomeJavaConstants.regulatedEntity);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new EnricherException(e.getMessage(), e);
        }
    }

    /**
     *
     * @param instance GKInstance
     * @param node current Node
     * @param fieldName ReactomeJavaConstant
     * @throws Exception
     */
    private void nodeFromReference(GKInstance instance, Node node, String fieldName) throws Exception {
        Collection<?> components = instance.getReferers(fieldName);
        if (components != null && !components.isEmpty()) {
            for (Object entryObject : components) {
                GKInstance entry = (GKInstance) entryObject;
                Node newNode = getOrCreateNode(entry);
                node.addChild(newNode);
                newNode.addParent(node);
                recursion(entry, newNode);
            }
        }
    }

    /**
     *
     * @param instance GKInstance
     * @param node current Node
     * @param fieldName ReactomeJavaConstant
     * @throws Exception
     */
    private void nodeFromAttributes(GKInstance instance, Node node, String fieldName) throws Exception {
        if (hasValues(instance, fieldName)) {
            GKInstance regulatedEntityInstance = (GKInstance) instance.getAttributeValue(fieldName);
            if (regulatedEntityInstance != null) {
                Node newNode = getOrCreateNode(regulatedEntityInstance);
                node.addChild(newNode);
                newNode.addParent(node);
                recursion(regulatedEntityInstance, newNode);
            }
        }
    }

    /**
     * Node will not be added to the Graph, a Entity "Regulation" does not represent a viewable item in the Pathway Browser.
     * Nevertheless Regulations can contain "regulated Entities" (Events) that can be shown in the Pathway Browser
     * @param instance GKInstance
     * @param node current Node
     * @param fieldName ReactomeJavaConstant
     * @throws Exception
     */
    private void skipNodes(GKInstance instance, Node node, String fieldName) throws Exception {
        Collection<?> regulator = instance.getReferers(fieldName);
        if (regulator != null && !regulator.isEmpty()) {
            for (Object entryObject : regulator) {
                GKInstance entry = (GKInstance) entryObject;
                recursion(entry, node);
            }
        }
    }

    /**
     * If a Node is not already present in the Graph (checked with the NodeMap) it will be Created from the GKInstance
     * Otherwise the old Node will be returned
     * @param instance GKInstance
     * @return Node
     * @throws Exception
     */
    private Node getOrCreateNode(GKInstance instance) throws Exception {
        Node node = nodeMap.get(getStableIdentifier(instance));
        if (node == null) {
            node = new Node();
            node.setName(instance.getDisplayName());
            node.setStId(getStableIdentifier(instance));
            node.setSpecies(getAttributeDisplayName(instance,ReactomeJavaConstants.species));
            node.setType(instance.getSchemClass().getName());
            node.setDiagram(hasDiagram(instance.getDBID()));
            nodeMap.put(node.getStId(), node);
        }
        return node;
    }

    /**
     * Rotating the graph using the leaves as roots of individual trees
     * @param leaves of the Graph represent the TopLevelPathways in Reactome
     * @return a Set of Trees, where each Tree Root is a different TopLevelPathway
     * @throws EnricherException
     */
    private Set<Node> buildTreesFromLeaves(Set<Node> leaves) throws EnricherException {
        Set<Node> topLvlTrees = new TreeSet<>();
        for (Node leaf : leaves) {
            Node tree = getTreeFromGraphLeaf(leaf, "", "", "", "");
            if (tree != null) {
                topLvlTrees.add(tree);
            } else {
                logger.error("Could no process tree for " + leaf.getName());
            }
        }
        return topLvlTrees;
    }

    /**
     * Generating individual Trees from a leaf
     * Url linking to the Pathway browser will be set
     * URL consists of 3 Attributes PATH, SEL, MAIN
     * MAIN = main URL parameter (required)
     * @param leaf of the Graph represent the TopLevelPathways in Reactome
     * @param sel URL parameter to select Reactions or Physical Entities (optional)
     * @param path URL parameter to identify a unique "Path" to this entry
     * @param shortPath URL parameter to identify a unique "Path" to this entry
     * @param lastNodeWithDiagram  saves STID of the Last Pathway in the Diagram
     * @return generated Tree
     */
    private Node getTreeFromGraphLeaf(Node leaf, String sel, String path, String shortPath, String lastNodeWithDiagram) {
        /* */
        Node tree = new Node();
        tree.setStId(leaf.getStId());
        tree.setName(leaf.getName());
        tree.setSpecies(leaf.getSpecies());
        tree.setType(leaf.getType());
        boolean isPathway = leaf.getType().equals("Pathway");
        boolean hasDiagram = leaf.hasDiagram();
        leaf.setUnique(false);

        /*Setting main Url attributes*/
        String main;
        if (isPathway){
            main = leaf.getStId();
        } else {
            sel = leaf.getStId();
            main = lastNodeWithDiagram;
        }

        /*Check if Pathway is a unique pathway*/
        Set<Node> children = leaf.getChildren();
        if(isPathway) {
            if (children == null) {
                leaf.setUnique(true);
            } else if (children.size() == 1) {
                if (children.iterator().next().isUnique()) {
                    leaf.setUnique(true);
                }
            }
        }

        /*Building the Url for the current entry*/
        StringBuilder url = new StringBuilder();
        url.append(PATHWAY_BROWSER_URL);
        if (leaf.isUnique()){
            url.append(leaf.getStId());
        } else {
            url.append(main);
            if (!sel.isEmpty()) {
                url.append(SEL);
                url.append(sel);
            }

            if (isPathway) {
                if (!path.isEmpty()) {
                    url.append(PATH);
                    url.append(path);
                } else {
                    url.append(path);
                }
            } else {
                if (!shortPath.isEmpty()) {
                    url.append(PATH);
                    url.append(shortPath);
                } else {
                    url.append(shortPath);
                }
            }
        }
        tree.setUrl(url.toString());

        /*Building Path for next entry*/
        if (isPathway) {
            if (hasDiagram) {
                if (shortPath.isEmpty()) {
                    shortPath +=  lastNodeWithDiagram;
                } else {
                    shortPath+="," + lastNodeWithDiagram;
                }
            } else {
                if (path.isEmpty()) {
                    path += leaf.getStId();
                } else {
                    path += "," + leaf.getStId();
                }
            }
        }
        if (hasDiagram){
            lastNodeWithDiagram = leaf.getStId();
        }

        /*Continue in the recursion */
        Set<Node> parents = leaf.getParent();
        if (parents!=null) {
            for (Node node : parents) {
                tree.addChild(getTreeFromGraphLeaf(node, sel, path, shortPath, lastNodeWithDiagram));
            }
        }
        return tree;
    }
}
