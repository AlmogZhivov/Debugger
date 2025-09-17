package il.ac.bgu.se.bp.rest.socket.notifications.handler;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import static il.ac.bgu.se.bp.rest.utils.Endpoints.STATE_UPDATE;


@Service
@Qualifier("stateNotificationHandlerImpl")
public class StateNotificationHandlerImpl extends AbstractNotificationHandler {

    protected StateNotificationHandlerImpl() {
        super();
    }

    @Override
    protected String getUpdateURI() {
        return STATE_UPDATE;
    }
}
