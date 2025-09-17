package il.ac.bgu.se.bp.socket.state;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class BThreadScope  implements Serializable {
    private static final long serialVersionUID = 2208145820539786522L;

    private String scopeName;
    private String currentLineNumber;
    private Map<String, String> variables;

    public BThreadScope() {
    }

    public BThreadScope(String scopeName, String currentLineNumber, Map<String, String> variables) {
        this.scopeName = scopeName;
        this.currentLineNumber = currentLineNumber;
        this.variables = variables;
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
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BThreadScope that = (BThreadScope) o;
        return Objects.equals(scopeName, that.scopeName) && Objects.equals(currentLineNumber, that.currentLineNumber) && Objects.equals(variables, that.variables);
    }

    @Override
    public int hashCode() {
        return Objects.hash(scopeName, currentLineNumber, variables);
    }

    @Override
    public String toString() {

        List<String> variablesS = new LinkedList<>();

        for (Map.Entry e : variables.entrySet()) {
            variablesS.add(e.getKey() + ":" + e.getValue());
        }
        return "BThreadScope{" +
                "scopeName='" + scopeName + '\'' +
                ", currentLineNumber='" + currentLineNumber + '\'' +
                "\tvariables= [" + String.join(",", variablesS) + "],\n" +
                '}';
    }
}
