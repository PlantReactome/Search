package uk.ac.ebi.reactome.solr.indexer.model;

/**
 * @author Guilherme S Viteri <gviteri@ebi.ac.uk>
 */

/**
 * Helper class
 */
public class InteractorSummary {

    private ReactomeSummary reactomeSummary;
    private String accession;
    private Double score;
    private String interactionId;

    public ReactomeSummary getReactomeSummary() {
        return reactomeSummary;
    }

    public void setReactomeSummary(ReactomeSummary reactomeSummary) {
        this.reactomeSummary = reactomeSummary;
    }

    public String getAccession() {
        return accession;
    }

    public void setAccession(String accession) {
        this.accession = accession;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        InteractorSummary summary = (InteractorSummary) o;

        return !(accession != null ? !accession.equals(summary.accession) : summary.accession != null);

    }

    @Override
    public int hashCode() {
        return accession != null ? accession.hashCode() : 0;
    }
}
