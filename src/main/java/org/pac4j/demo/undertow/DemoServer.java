package org.pac4j.demo.undertow;

import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.handlers.PathHandler;
import io.undertow.server.handlers.resource.ClassPathResourceManager;

import org.pac4j.core.config.Config;
import org.pac4j.undertow.SecurityMechanism;
import org.pac4j.undertow.handlers.ApplicationLogoutHandler;
import org.pac4j.undertow.handlers.CallbackHandler;
import org.pac4j.undertow.session.UndertowSessionStore;

/**
 * Undertow demo server demonstrating how to integrate pac4j.
 * 
 * @author Michael Remond
 * @since 1.0.0
 */
public class DemoServer {

    public final static String JWT_SALT = "12345678901234567890123456789012";

    public static void main(final String[] args) {

        final Config config = new DemoConfigFactory().build();
        final UndertowSessionStore sessionStore = new UndertowSessionStore();
        config.setSessionStore(sessionStore);

        PathHandler path = new PathHandler();

        path.addExactPath("/", new ErrorHandler(DemoHandlers.indexHandler(config)));

        path.addExactPath("/facebook/notprotected.html",
                new ErrorHandler(DemoHandlers.authenticatedHandler));
        path.addPrefixPath("/facebook/",
                new ErrorHandler(SecurityMechanism.build(DemoHandlers.authenticatedHandler, config, "FacebookClient")));
        path.addExactPath("/facebookadmin/index.html",
                new ErrorHandler(SecurityMechanism.build(DemoHandlers.authenticatedHandler, config, "FacebookClient", "admin")));
        path.addExactPath("/facebookcustom/index.html",
                new ErrorHandler(SecurityMechanism.build(DemoHandlers.authenticatedHandler, config, "FacebookClient", "custom")));
        path.addExactPath("/twitter/index.html",
                new ErrorHandler(SecurityMechanism.build(DemoHandlers.authenticatedHandler, config, "TwitterClient,FacebookClient")));
        path.addExactPath("/form/index.html",
                new ErrorHandler(SecurityMechanism.build(DemoHandlers.authenticatedHandler, config, "FormClient")));
        path.addExactPath("/form/index.html.json",
                new ErrorHandler(SecurityMechanism.build(DemoHandlers.authenticatedJsonHandler, config, "FormClient")));
        path.addExactPath("/basicauth/index.html", new ErrorHandler(SecurityMechanism.build(
                DemoHandlers.authenticatedHandler, config, "IndirectBasicAuthClient")));
        path.addExactPath("/cas/index.html",
                new ErrorHandler(SecurityMechanism.build(DemoHandlers.authenticatedHandler, config, "CasClient")));
        path.addExactPath("/saml2/index.html",
                new ErrorHandler(SecurityMechanism.build(DemoHandlers.authenticatedHandler, config, "SAML2Client")));
        path.addExactPath("/oidc/index.html",
                new ErrorHandler(SecurityMechanism.build(DemoHandlers.authenticatedHandler, config, "OidcClient")));
        path.addExactPath("/protected/index.html",
                new ErrorHandler(SecurityMechanism.build(DemoHandlers.authenticatedHandler, config)));

        path.addExactPath("/dba/index.html",
                new ErrorHandler(SecurityMechanism.build(DemoHandlers.authenticatedHandler, config, "DirectBasicAuthClient,ParameterClient")));
        path.addExactPath("/rest-jwt/index.html",
                new ErrorHandler(SecurityMechanism.build(DemoHandlers.authenticatedHandler, config, "ParameterClient")));

        path.addExactPath("/callback",new ErrorHandler(CallbackHandler.build(config)));
        path.addExactPath("/logout", new ErrorHandler(new ApplicationLogoutHandler(config)));

        path.addPrefixPath("/assets/js",
                new ErrorHandler(Handlers.resource(new ClassPathResourceManager(DemoServer.class.getClassLoader()))));

        path.addExactPath("/loginForm.html", new ErrorHandler(DemoHandlers.formHandler(config)));
        path.addExactPath("/jwt.html", new ErrorHandler(DemoHandlers.jwtHandler()));

        Undertow server = Undertow.builder().addHttpListener(8080, "localhost")
                .setHandler(sessionStore.addSessionHandler(path)).build();
        server.start();
    }
}
