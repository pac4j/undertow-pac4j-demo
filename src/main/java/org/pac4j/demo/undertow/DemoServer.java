package org.pac4j.demo.undertow;

import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.handlers.PathHandler;
import io.undertow.server.handlers.resource.ClassPathResourceManager;

import io.undertow.server.session.InMemorySessionManager;
import io.undertow.server.session.SessionAttachmentHandler;
import io.undertow.server.session.SessionCookieConfig;
import lombok.val;
import org.pac4j.core.config.Config;
import org.pac4j.undertow.handler.LogoutHandler;
import org.pac4j.undertow.handler.CallbackHandler;
import org.pac4j.undertow.handler.SecurityHandler;

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

        val path = new PathHandler();

        path.addExactPath("/", SecurityHandler.build(DemoHandlers.indexHandler(), config, "AnonymousClient"));
        path.addExactPath("/index.html", SecurityHandler.build(DemoHandlers.indexHandler(), config, "AnonymousClient"));

        path.addExactPath("/facebook/notprotected.html", DemoHandlers.protectedIndex);
        path.addExactPath("/facebook/index.html", SecurityHandler.build(DemoHandlers.protectedIndex, config, "FacebookClient"));
        path.addExactPath("/facebook/notprotected.html", SecurityHandler.build(DemoHandlers.notProtectedIndex, config, "AnonymousClient"));
        path.addExactPath("/facebookadmin/index.html", SecurityHandler.build(DemoHandlers.protectedIndex, config, "FacebookClient", "admin"));
        path.addExactPath("/facebookcustom/index.html", SecurityHandler.build(DemoHandlers.protectedIndex, config, "FacebookClient", "custom"));
        path.addExactPath("/twitter/index.html", SecurityHandler.build(DemoHandlers.protectedIndex, config, "TwitterClient,FacebookClient"));
        path.addExactPath("/form/index.html", SecurityHandler.build(DemoHandlers.protectedIndex, config, "FormClient"));
        path.addExactPath("/form/index.html.json", SecurityHandler.build(DemoHandlers.authenticatedJsonHandler, config, "FormClient"));
        path.addExactPath("/basicauth/index.html", SecurityHandler.build(DemoHandlers.protectedIndex, config, "IndirectBasicAuthClient"));
        path.addExactPath("/cas/index.html", SecurityHandler.build(DemoHandlers.protectedIndex, config, "CasClient"));
        path.addExactPath("/saml2/index.html", SecurityHandler.build(DemoHandlers.protectedIndex, config, "SAML2Client"));
        path.addExactPath("/oidc/index.html", SecurityHandler.build(DemoHandlers.protectedIndex, config, "OidcClient"));
        path.addExactPath("/protected/index.html", SecurityHandler.build(DemoHandlers.protectedIndex, config));

        path.addExactPath("/dba/index.html", SecurityHandler.build(DemoHandlers.protectedIndex, config, "DirectBasicAuthClient,ParameterClient"));
        path.addExactPath("/rest-jwt/index.html", SecurityHandler.build(DemoHandlers.protectedIndex, config, "ParameterClient"));

        path.addExactPath("/callback", CallbackHandler.build(config, null));
        path.addExactPath("/logout", new LogoutHandler(config, "/?defaulturlafterlogout"));

        path.addPrefixPath("/assets/js", Handlers.resource(new ClassPathResourceManager(DemoServer.class.getClassLoader())));

        path.addExactPath("/loginForm.html", DemoHandlers.loginFormHandler(config));
        path.addExactPath("/jwt.html", SecurityHandler.build(DemoHandlers.jwtHandler(), config, "AnonymousClient"));
        path.addExactPath("/forceLogin", DemoHandlers.forceLoginHandler(config));

        val server = Undertow.builder().addHttpListener(8080, "localhost")
                .setHandler(new SessionAttachmentHandler(new ErrorHandler(path), new InMemorySessionManager("SessionManager"), new SessionCookieConfig())).build();
        server.start();
    }
}
