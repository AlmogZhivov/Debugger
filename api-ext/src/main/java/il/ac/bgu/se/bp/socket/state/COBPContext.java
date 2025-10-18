package il.ac.bgu.se.bp.socket.state;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

public class COBPContext implements Serializable {
    private static final long serialVersionUID = 1L;

    private List<COBPEntity> entities;
    private Map<String, List<COBPEntity>> queryResults;
    private String currentBThreadContext; // e.g., "Piece.All"
    private Map<String, COBPEntity> bThreadBoundContext; // e.g., "piece": {COBPEntity}

    public COBPContext(List<COBPEntity> entities, Map<String, List<COBPEntity>> queryResults, String currentBThreadContext, Map<String, COBPEntity> bThreadBoundContext) {
        this.entities = entities;
        this.queryResults = queryResults;
        this.currentBThreadContext = currentBThreadContext;
        this.bThreadBoundContext = bThreadBoundContext;
    }

    public List<COBPEntity> getEntities() {
        return entities;
    }

    public void setEntities(List<COBPEntity> entities) {
        this.entities = entities;
    }

    public Map<String, List<COBPEntity>> getQueryResults() {
        return queryResults;
    }

    public void setQueryResults(Map<String, List<COBPEntity>> queryResults) {
        this.queryResults = queryResults;
    }

    public String getCurrentBThreadContext() {
        return currentBThreadContext;
    }

    public void setCurrentBThreadContext(String currentBThreadContext) {
        this.currentBThreadContext = currentBThreadContext;
    }

    public Map<String, COBPEntity> getBThreadBoundContext() {
        return bThreadBoundContext;
    }

    public void setBThreadBoundContext(Map<String, COBPEntity> bThreadBoundContext) {
        this.bThreadBoundContext = bThreadBoundContext;
    }

    @Override
    public String toString() {
        return "COBPContext{" +
                "entities=" + entities +
                ", queryResults=" + queryResults +
                ", currentBThreadContext='" + currentBThreadContext + '\'' +
                ", bThreadBoundContext=" + bThreadBoundContext +
                '}';
    }
}
