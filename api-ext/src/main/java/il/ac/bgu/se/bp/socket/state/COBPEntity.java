package il.ac.bgu.se.bp.socket.state;

import java.io.Serializable;
import java.util.Map;

public class COBPEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String type;
    private Map<String, Object> properties;

    public COBPEntity(String id, String type, Map<String, Object> properties) {
        this.id = id;
        this.type = type;
        this.properties = properties;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, Object> properties) {
        this.properties = properties;
    }

    @Override
    public String toString() {
        return "COBPEntity{" +
                "id='" + id + '\'' +
                ", type='" + type + '\'' +
                ", properties=" + properties +
                '}';
    }
}
