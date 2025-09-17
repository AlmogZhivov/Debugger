package il.ac.bgu.se.bp.rest.request;

import java.io.Serializable;
import java.util.Objects;

public class ExternalEventRequest implements Serializable {
    private static final long serialVersionUID = -2924530817256942988L;

    private String externalEvent;
    private boolean addEvent;

    public ExternalEventRequest() {
    }

    public String getExternalEvent() {
        return externalEvent;
    }

    public void setExternalEvent(String externalEvent) {
        this.externalEvent = externalEvent;
    }

    public boolean isAddEvent() {
        return addEvent;
    }

    public void setAddEvent(boolean addEvent) {
        this.addEvent = addEvent;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ExternalEventRequest that = (ExternalEventRequest) o;
        return addEvent == that.addEvent &&
                Objects.equals(externalEvent, that.externalEvent);
    }

    @Override
    public int hashCode() {
        return Objects.hash(externalEvent, addEvent);
    }

    @Override
    public String toString() {
        return "ExternalEventRequest{" +
                "externalEvent='" + externalEvent + '\'' +
                ", addEvent=" + addEvent +
                '}';
    }
}
