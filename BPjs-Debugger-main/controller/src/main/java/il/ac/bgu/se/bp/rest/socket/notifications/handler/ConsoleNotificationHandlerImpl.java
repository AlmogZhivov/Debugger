package il.ac.bgu.se.bp.rest.socket.notifications.handler;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import static il.ac.bgu.se.bp.rest.utils.Endpoints.CONSOLE_UPDATE;


@Service
@Qualifier("consoleNotificationHandlerImpl")
public class ConsoleNotificationHandlerImpl extends AbstractNotificationHandler {

    @Override
    protected String getUpdateURI() {
        return CONSOLE_UPDATE;
    }
}
