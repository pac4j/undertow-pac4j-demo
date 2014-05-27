/*
  Copyright 2014 - 2014 Michael Remond

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */
package org.pac4j.undertow;

import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.security.api.AuthenticationMechanism;
import io.undertow.security.api.AuthenticationMode;
import io.undertow.security.handlers.AuthenticationCallHandler;
import io.undertow.security.handlers.AuthenticationConstraintHandler;
import io.undertow.security.handlers.AuthenticationMechanismsHandler;
import io.undertow.security.handlers.SecurityInitialHandler;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.PathHandler;
import io.undertow.server.handlers.form.EagerFormParsingHandler;
import io.undertow.server.handlers.form.FormEncodedDataDefinition;
import io.undertow.server.handlers.form.FormParserFactory;
import io.undertow.server.handlers.resource.ClassPathResourceManager;
import io.undertow.server.session.SessionAttachmentHandler;

import java.util.Collections;
import java.util.List;

import org.pac4j.cas.client.CasClient;
import org.pac4j.core.client.Clients;
import org.pac4j.http.client.BasicAuthClient;
import org.pac4j.http.client.FormClient;
import org.pac4j.http.credentials.SimpleTestUsernamePasswordAuthenticator;
import org.pac4j.oauth.client.FacebookClient;
import org.pac4j.oauth.client.TwitterClient;
import org.pac4j.saml.client.Saml2Client;

/**
 * Undertow demo server demonstrating how to integrate pac4j.
 * 
 * @author Michael Remond
 * @since 1.0.0
 *
 */
public class DemoServer {

    public static void main(final String[] args) {

        Config.setClients(buildClients());

        PathHandler path = new PathHandler();

        path.addExactPath("/", DemoHandlers.indexHandler);

        path.addExactPath("/facebook/index.html",
                addSecurity(DemoHandlers.authenticatedHandler, "FacebookClient", false));
        path.addExactPath("/twitter/index.html", addSecurity(DemoHandlers.authenticatedHandler, "TwitterClient", false));
        path.addExactPath("/form/index.html", addSecurity(DemoHandlers.authenticatedHandler, "FormClient", false));
        path.addExactPath("/form/index.html.json",
                addSecurity(DemoHandlers.authenticatedJsonHandler, "FormClient", true));
        path.addExactPath("/basicauth/index.html",
                addSecurity(DemoHandlers.authenticatedHandler, "BasicAuthClient", false));
        path.addExactPath("/cas/index.html", addSecurity(DemoHandlers.authenticatedHandler, "CasClient", false));
        path.addExactPath("/saml2/index.html", addSecurity(DemoHandlers.authenticatedHandler, "Saml2Client", false));

        path.addExactPath("/callback", addFormParsing(new CallbackHandler()));
        path.addExactPath("/logout", new LogoutHandler());
        ;
        path.addPrefixPath("/assets/js",
                Handlers.resource(new ClassPathResourceManager(DemoServer.class.getClassLoader())));

        path.addExactPath("/theForm.html", DemoHandlers.formHandler);

        Undertow server = Undertow.builder().addListener(8080, "localhost").setHandler(addSession(path)).build();
        server.start();
    }

    private static HttpHandler addFormParsing(final HttpHandler toWrap) {
        HttpHandler handler = toWrap;
        FormParserFactory factory = FormParserFactory.builder().addParser(new FormEncodedDataDefinition()).build();
        EagerFormParsingHandler formHandler = new EagerFormParsingHandler(factory);
        formHandler.setNext(handler);
        handler = formHandler;
        return handler;
    }

    private static HttpHandler addSession(final HttpHandler toWrap) {
        return new SessionAttachmentHandler(toWrap, Config.getSessionManager(), Config.getSessioncookieconfig());
    }

    private static HttpHandler addSecurity(final HttpHandler toWrap, final String clientName, boolean isAjax) {
        HttpHandler handler = toWrap;
        // protect resource
        handler = new AuthenticationCallHandler(handler);
        // set authentication required
        handler = new AuthenticationConstraintHandler(handler);
        List<AuthenticationMechanism> mechanisms = Collections
                .<AuthenticationMechanism> singletonList(new ClientAuthenticationMechanism(clientName, isAjax));
        // use pac4j as authentication mechanism
        handler = new AuthenticationMechanismsHandler(handler, mechanisms);
        // put security context in exchange
        handler = new SecurityInitialHandler(AuthenticationMode.PRO_ACTIVE, null, handler);
        return handler;
    }

    private static Clients buildClients() {
        final Saml2Client saml2Client = new Saml2Client();
        saml2Client.setKeystorePath("resource:samlKeystore.jks");
        saml2Client.setKeystorePassword("pac4j-demo-passwd");
        saml2Client.setPrivateKeyPassword("pac4j-demo-passwd");
        saml2Client.setIdpMetadataPath("resource:testshib-providers.xml");

        final FacebookClient facebookClient = new FacebookClient("145278422258960", "be21409ba8f39b5dae2a7de525484da8");
        final TwitterClient twitterClient = new TwitterClient("CoxUiYwQOSFDReZYdjigBA",
                "2kAzunH5Btc4gRSaMr7D7MkyoJ5u1VzbOOzE8rBofs");
        // HTTP
        final FormClient formClient = new FormClient("http://localhost:8080/theForm.html",
                new SimpleTestUsernamePasswordAuthenticator());
        final BasicAuthClient basicAuthClient = new BasicAuthClient(new SimpleTestUsernamePasswordAuthenticator());

        // CAS
        final CasClient casClient = new CasClient();
        // casClient.setGateway(true);
        casClient.setCasLoginUrl("https://freeuse1.casinthecloud.com/leleujgithub/login");

        final Clients clients = new Clients("http://localhost:8080/callback", saml2Client, facebookClient,
                twitterClient, formClient, basicAuthClient, casClient);

        return clients;
    }
}
