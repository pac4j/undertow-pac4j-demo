package org.pac4j.demo.undertow;

import io.undertow.server.DefaultResponseListener;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;

public class ErrorHandler implements HttpHandler {

    private final static String ERROR_401 = DemoHandlers.error401Page();
    private final static String ERROR_403 = DemoHandlers.error403Page();
    private final static String ERROR_500 = DemoHandlers.error500Page();

    private final HttpHandler next;

    public ErrorHandler(final HttpHandler next) {
        this.next = next;
    }

    @Override
    public void handleRequest(final HttpServerExchange exchange) throws Exception {
        exchange.addDefaultResponseListener(new DefaultResponseListener() {
            @Override
            public boolean handleDefaultResponse(final HttpServerExchange exchange) {
                if (!exchange.isResponseChannelAvailable()) {
                    return false;
                }
                final int code = exchange.getStatusCode();
                if (code == 401) {
                    exchange.getResponseSender().send(ERROR_401);
                    return true;
                } else if (code == 403) {
                    exchange.getResponseSender().send(ERROR_403);
                    return true;
                } else if (code == 500) {
                    exchange.getResponseSender().send(ERROR_500);
                    return true;
                }
                return false;
            }
        });
        next.handleRequest(exchange);
    }
}
