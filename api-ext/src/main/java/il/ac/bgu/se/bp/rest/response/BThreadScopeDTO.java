package il.ac.bgu.se.bp.rest.response;

import java.util.Map;

/**
 * Serialization-safe DTO for B-thread scope information.
 */
public class BThreadScopeDTO {
    private String scopeName;
    private String currentLineNumber;
    private Map<String, String> variables;

    public BThreadScopeDTO() {
    }

    public String getScopeName() {
        return scopeName;
    }

    public void setScopeName(String scopeName) {
        this.scopeName = scopeName;
    }

    public String getCurrentLineNumber() {
        return currentLineNumber;
    }

    public void setCurrentLineNumber(String currentLineNumber) {
        this.currentLineNumber = currentLineNumber;
    }

    public Map<String, String> getVariables() {
        return variables;
    }

    public void setVariables(Map<String, String> variables) {
        this.variables = variables;
    }

    @Override
    public String toString() {
        return "BThreadScopeDTO{" +
                "scopeName='" + scopeName + '\'' +
                ", currentLineNumber='" + currentLineNumber + '\'' +
                ", variables=" + variables +
                '}';
    }
}
