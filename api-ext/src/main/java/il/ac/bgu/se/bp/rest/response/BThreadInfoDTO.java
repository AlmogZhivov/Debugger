package il.ac.bgu.se.bp.rest.response;

import java.util.Map;
import java.util.Set;

/**
 * Serialization-safe DTO for B-thread information.
 */
public class BThreadInfoDTO {
    private String name;
    private Map<Integer, BThreadScopeDTO> env;
    private Set<EventInfoDTO> requested;
    private Set<EventInfoDTO> blocked;
    private Set<EventInfoDTO> wait;

    public BThreadInfoDTO() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<Integer, BThreadScopeDTO> getEnv() {
        return env;
    }

    public void setEnv(Map<Integer, BThreadScopeDTO> env) {
        this.env = env;
    }

    public Set<EventInfoDTO> getRequested() {
        return requested;
    }

    public void setRequested(Set<EventInfoDTO> requested) {
        this.requested = requested;
    }

    public Set<EventInfoDTO> getBlocked() {
        return blocked;
    }

    public void setBlocked(Set<EventInfoDTO> blocked) {
        this.blocked = blocked;
    }

    public Set<EventInfoDTO> getWait() {
        return wait;
    }

    public void setWait(Set<EventInfoDTO> wait) {
        this.wait = wait;
    }

    @Override
    public String toString() {
        return "BThreadInfoDTO{" +
                "name='" + name + '\'' +
                ", env=" + env +
                ", requested=" + requested +
                ", blocked=" + blocked +
                ", wait=" + wait +
                '}';
    }
}
