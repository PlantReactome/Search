package org.reactome.server.tools.search.domain;

import java.util.List;

/**
 * @author Guilherme S Viteri <gviteri@ebi.ac.uk>
 */

public class Interaction implements Comparable<Interaction> {

    private List<InteractorReactomeEntry> interactorReactomeEntries;
    private Double score;
    private String interactionId;
    private String accession;

    public List<InteractorReactomeEntry> getInteractorReactomeEntries() {
        return interactorReactomeEntries;
    }

    public void setInteractorReactomeEntries(List<InteractorReactomeEntry> interactorReactomeEntries) {
        this.interactorReactomeEntries = interactorReactomeEntries;
    }

    public Double getScore() {
        return score;
    }

    public void setScore(Double score) {
        this.score = score;
    }

    public String getInteractionId() {
        return interactionId;
    }

    public void setInteractionId(String interactionId) {
        this.interactionId = interactionId;
    }

    public String getAccession() {
        return accession;
    }

    public void setAccession(String accession) {
        this.accession = accession;
    }

    @Override
    public int compareTo(Interaction otherInteraction) {
        return this.score.compareTo(otherInteraction.getScore());
    }
}
