package org.pac4j.demo.undertow;

import io.undertow.security.api.SecurityContext;
import io.undertow.security.idm.Account;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import io.undertow.util.HttpString;

import org.pac4j.core.client.Client;
import org.pac4j.core.config.Config;
import org.pac4j.core.exception.http.HttpAction;
import org.pac4j.core.util.Pac4jConstants;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.http.client.indirect.FormClient;
import org.pac4j.jwt.profile.JwtGenerator;
import org.pac4j.undertow.account.Pac4jAccount;
import org.pac4j.undertow.context.UndertowWebContext;

import java.util.List;
import org.pac4j.jwt.config.signature.SecretSignatureConfiguration;
import org.pac4j.undertow.http.UndertowHttpActionAdapter;

/**
 * A collection of basic handlers printing dynamic html for the demo application.
 * 
 * @author Michael Remond
 * @since 1.0.0
 */
public class DemoHandlers {

    public static HttpHandler indexHandler() {

        return exchange -> {
            StringBuilder sb = new StringBuilder();

            sb.append("<h1>index</h1>");
            sb.append("<a href='facebook/index.html'>Protected url by Facebook: facebook/index.html</a> (use a real account)<br />");
            sb.append("<a href='facebook/notprotected.html'>Not protected page: facebook/notprotected.html</a> (no authentication required)<br />");
            sb.append("<a href='facebookadmin/index.html'>Protected url by Facebook with ROLE_ADMIN: facebookadmin/index.html</a> (use a real account)<br />");
            sb.append("<a href='facebookcustom/index.html'>Protected url by Facebook with custom authorizer (= must be a <em>CommonProfile</em> where the username starts with 'jle'): facebookcustom/index.html</a> (login with form or basic authentication before with jle* username)<br />");
            sb.append("<a href='twitter/index.html'>Protected url by Twitter: twitter/index.html</a> or <a href='twitter/index.html?client_name=FacebookClient'>by Facebook: twitter/index.html?client_name=FacebookClient</a> (use a real account)<br />");
            sb.append("<a href='form/index.html'>Protected url by form authentication: form/index.html</a> (use login = pwd)<br />");
            sb.append("<a href=\"javascript:ajaxClick();\">Click here to send AJAX request after performing form authentication</a><br />");
            sb.append("<a href='basicauth/index.html'>Protected url by indirect basic auth: basicauth/index.html</a> (use login = pwd)<br />");
            sb.append("<a href='cas/index.html'>Protected url by CAS: cas/index.html</a> (use login = pwd)<br />");
            sb.append("<a href='saml2/index.html'>Protected url by SAML2: saml2/index.html</a> (use testpac4j at gmail.com / Pac4jtest)<br />");
            sb.append("<a href='oidc/index.html'>Protected url by OpenID Connect: oidc/index.html</a> (use a real account)<br />");
            sb.append("<a href='protected/index.html'>Protected url: protected/index.html</a> (won't start any login process)<br />");
            sb.append("<br />");
            sb.append("<a href='jwt.html'>Generate a JWT token</a> (after being authenticated)<br />");
            sb.append("<a href='/dba/index.html'>Protected url by DirectBasicAuthClient: /dba/index.html</a> (POST the <em>Authorization</em> header with value: <em>Basic amxlbGV1OmpsZWxldQ==</em>) then by <a href='/dba/index.html'>ParameterClient: /dba/index.html</a> (with request parameter: token=<em>jwt_generated_token</em>)<br />");
            sb.append("<a href='/rest-jwt/index.html'>Protected url by ParameterClient: /rest-jwt/index.html</a> (with request parameter: token=<em>jwt_generated_token</em>)<br />");
            sb.append("<br />");
            sb.append("<a href='/forceLogin?client_name=FacebookClient'>Force Facebook login</a> (even if already authenticated)<br />");
            sb.append("<br />");
            sb.append("<a href='/logout?url=/?forcepostlogouturl'>logout</a>");
            sb.append("<br /><br />");
            sb.append("profiles: ");
            sb.append(getProfiles(exchange));
            sb.append("<br /><br />");
            sb.append("<script src=\"http://ajax.googleapis.com/ajax/libs/jquery/1.9.1/jquery.min.js\"></script>");
            sb.append("<script src=\"assets/js/app.js\"></script>");

            sendEnd(exchange, sb);
        };
    }

    private static void sendEnd(final HttpServerExchange exchange, final StringBuilder sb) {
        exchange.getResponseHeaders().add(Headers.CONTENT_TYPE, "text/html; charset=utf-8");
        exchange.getResponseSender().send(sb.toString());
        exchange.endExchange();
    }

    public static HttpHandler protectedIndex = exchange -> {
        StringBuilder sb = new StringBuilder();
        sb.append("<h1>protected area</h1>");
        sb.append("<a href=\"..\">Back</a><br />");
        sb.append("<br /><br />");
        sb.append("profiles: ");
        sb.append(getProfiles(exchange));
        sb.append("<br />");

        sendEnd(exchange, sb);
    };

    public static HttpHandler notProtectedIndex = exchange -> {
        StringBuilder sb = new StringBuilder();
        sb.append("<h1>not protected area</h1>");
        sb.append("<a href=\"..\">Back</a><br />");
        sb.append("<br /><br />");
        sb.append("profiles: ");
        sb.append(getProfiles(exchange));
        sb.append("<br />");

        sendEnd(exchange, sb);
    };

    public static HttpHandler authenticatedJsonHandler = exchange -> {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"username\":\"");
        sb.append(getProfile(exchange).getId());
        sb.append("\"}");

        exchange.getResponseHeaders().add(HttpString.tryFromString("Content-Type"), "application/json");
        sendEnd(exchange, sb);
    };

    private static Pac4jAccount getAccount(final HttpServerExchange exchange) {
        final SecurityContext securityContext = exchange.getSecurityContext();
        if (securityContext != null) {
            final Account account = securityContext.getAuthenticatedAccount();
            if (account instanceof Pac4jAccount) {
                return (Pac4jAccount) account;
            }
        }
        return null;
    }

    private static List<CommonProfile> getProfiles(final HttpServerExchange exchange) {
        final Pac4jAccount account = getAccount(exchange);
        if (account != null) {
            return account.getProfiles();
        }
        return null;
    }

    private static CommonProfile getProfile(final HttpServerExchange exchange) {
        final Pac4jAccount account = getAccount(exchange);
        if (account != null) {
            return account.getProfile();
        }
        return null;
    }

    public static HttpHandler loginFormHandler(final Config config) {
        return exchange -> {
            FormClient formClient = (FormClient) config.getClients().findClient("FormClient").get();
            StringBuilder sb = new StringBuilder();
            sb.append("<html><body>");
            sb.append("<form action=\"").append(formClient.getCallbackUrl()).append("?client_name=FormClient\" method=\"POST\">");
            sb.append("<input type=\"text\" name=\"username\" value=\"\" />");
            sb.append("<p />");
            sb.append("<input type=\"password\" name=\"password\" value=\"\" />");
            sb.append("<p />");
            sb.append("<input type=\"submit\" name=\"submit\" value=\"Submit\" />");
            sb.append("</form>");
            sb.append("</body></html>");

            sendEnd(exchange, sb);
        };
    }

    public static HttpHandler jwtHandler() {
        return exchange -> {
            String token = "";
            final Pac4jAccount account = getAccount(exchange);
            if (account != null) {
                final JwtGenerator jwtGenerator = new JwtGenerator(new SecretSignatureConfiguration(DemoServer.JWT_SALT));
                token = jwtGenerator.generate(account.getProfile());
            }
            StringBuilder sb = new StringBuilder();
            sb.append("<h1>Generate JWT token</h1>");
            sb.append("<a href='..'>Back</a><br />");
            sb.append("<br /><br />");
            sb.append("token: ");
            sb.append(token);

            sendEnd(exchange, sb);
        };
    }

    public static HttpHandler forceLoginHandler(final Config config) {
        return exchange -> {
            final UndertowWebContext context = new UndertowWebContext(exchange);
            final String clientName = context.getRequestParameter(Pac4jConstants.DEFAULT_CLIENT_NAME_PARAMETER).get();
            final Client client = config.getClients().findClient(clientName).get();
            HttpAction action;
            try {
                action = (HttpAction) client.getRedirectionAction(context).get();
            } catch (final HttpAction e) {
                action = e;
            }
            UndertowHttpActionAdapter.INSTANCE.adapt(action, context);
        };
    }

    public static String error401Page() {
         StringBuilder sb = new StringBuilder();
         sb.append("<h1>unauthorized</h1>");
         sb.append("<br />");
         sb.append("<a href='/'>Home</a>");
         return sb.toString();
    }

    public static String error403Page() {
        StringBuilder sb = new StringBuilder();
        sb.append("<h1>forbidden</h1>");
        sb.append("<br />");
        sb.append("<a href='/'>Home</a>");
        return sb.toString();
    }

    public static String error500Page() {
        StringBuilder sb = new StringBuilder();
        sb.append("<h1>internal error</h1>");
        sb.append("<br />");
        sb.append("<a href='/'>Home</a>");
        return sb.toString();
    }
}
