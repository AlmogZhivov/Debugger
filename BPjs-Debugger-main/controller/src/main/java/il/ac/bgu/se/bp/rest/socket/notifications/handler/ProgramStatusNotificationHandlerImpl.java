package il.ac.bgu.se.bp.rest.socket.notifications.handler;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import static il.ac.bgu.se.bp.rest.utils.Endpoints.PROGRAM_UPDATE;


@Service
@Qualifier("programStatusNotificationHandlerImpl")
public class ProgramStatusNotificationHandlerImpl extends AbstractNotificationHandler {

    @Override
    protected String getUpdateURI() {
        return PROGRAM_UPDATE;
    }
}
