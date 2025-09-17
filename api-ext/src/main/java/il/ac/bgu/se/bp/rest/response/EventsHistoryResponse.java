package il.ac.bgu.se.bp.rest.response;

import il.ac.bgu.se.bp.socket.state.EventInfo;

import java.io.Serializable;
import java.util.Objects;
import java.util.SortedMap;

public class EventsHistoryResponse implements Serializable {

    private static final long serialVersionUID = 2461424967451748099L;

    private SortedMap<Long, EventInfo> eventsHistory;

    public EventsHistoryResponse() {
    }

    public EventsHistoryResponse(SortedMap<Long, EventInfo> eventsHistory) {
        this.eventsHistory = eventsHistory;
    }

    public SortedMap<Long, EventInfo> getEventsHistory() {
        return eventsHistory;
    }

    public void setEventsHistory(SortedMap<Long, EventInfo> eventsHistory) {
        this.eventsHistory = eventsHistory;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        EventsHistoryResponse that = (EventsHistoryResponse) o;
        return Objects.equals(eventsHistory, that.eventsHistory);
    }

    @Override
    public int hashCode() {
        return Objects.hash(eventsHistory);
    }

    @Override
    public String toString() {
        return "EventsHistoryResponse{" +
                "eventsHistory=" + eventsHistory +
                '}';
    }
}
