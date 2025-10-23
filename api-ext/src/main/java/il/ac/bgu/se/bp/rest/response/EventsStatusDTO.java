package il.ac.bgu.se.bp.rest.response;

import java.util.Set;

/**
 * Serialization-safe DTO for events status.
 */
public class EventsStatusDTO {
    private Set<EventInfoDTO> requested;
    private Set<EventInfoDTO> blocked;
    private Set<EventInfoDTO> wait;

    public EventsStatusDTO() {
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
        return "EventsStatusDTO{" +
                "requested=" + requested +
                ", blocked=" + blocked +
                ", wait=" + wait +
                '}';
    }
}
