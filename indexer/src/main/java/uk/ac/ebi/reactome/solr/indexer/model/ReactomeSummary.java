package uk.ac.ebi.reactome.solr.indexer.model;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Guilherme S Viteri <gviteri@ebi.ac.uk>
 */
public class ReactomeSummary {

    private List<String> reactomeId;
    private List<String> reactomeName;

    public ReactomeSummary() {

    }

    public List<String> getReactomeName() {
        return reactomeName;
    }

    public void setReactomeName(List<String> reactomeName) {
        this.reactomeName = reactomeName;
    }

    public List<String> getReactomeId() {
        return reactomeId;
    }

    public void setReactomeId(List<String> reactomeId) {
        this.reactomeId = reactomeId;
    }

    public void addId(String id){
        if(reactomeId == null){
            reactomeId = new ArrayList<>();
        }
        reactomeId.add(id);
    }


    public void addName(String name){
        if(reactomeName == null){
            reactomeName = new ArrayList<>();
        }
        reactomeName.add(name);
    }

    /**
     * Saving the list into a multivalue field. Calling toString the final result will
     * be a comma-separated list. When parsing this list in the client (split by ,) then it can
     * split the reactome name as well.
     * e.g
     *   "[NUDC [cytosol], NUDC [nucleoplasm], p-S274,S326-NUDC [nucleoplasm]]"
     *   This multivalued field has 3 values, but splitting them by comma will result in
     *   4 values.
     *
     * This parse will save the list using another delimiter in place of the comma.
     */
    public static String parseList(List<String> list){
        String delim = "";
        StringBuffer sb = new StringBuffer();
        for (String s : list) {
            sb.append(delim);
            delim = "#";
            sb.append(s);
        }
        return sb.toString();
    }
}
