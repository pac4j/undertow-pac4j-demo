package org.pac4j.demo.undertow;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HttpString;

import org.pac4j.core.config.Config;
import org.pac4j.core.profile.ProfileManager;
import org.pac4j.core.profile.UserProfile;
import org.pac4j.http.client.indirect.FormClient;
import org.pac4j.jwt.profile.JwtGenerator;
import org.pac4j.undertow.UndertowWebContext;
import org.pac4j.undertow.utils.ExchangeHelper;

/**
 * A collection of basic handlers printing dynamic html for the demo application.
 * 
 * @author Michael Remond
 * @since 1.0.0
 */
public class DemoHandlers {

    public static HttpHandler indexHandler(final Config config) {

        return new HttpHandler() {
            @Override
            public void handleRequest(HttpServerExchange exchange) throws Exception {
                StringBuilder sb = new StringBuilder();

                sb.append("<h1>index</h1>");
                sb.append("<a href='facebook/index.html'>Protected url by Facebook: facebook/index.html</a> (use a real account)<br />");
                sb.append("<a href='facebook/notprotected.html'>Not protected page: facebook/notprotected.html</a> (no authentication required)<br />");
                sb.append("<a href='facebookadmin/index.html'>Protected url by Facebook with ROLE_ADMIN: facebookadmin/index.html</a> (use a real account)<br />");
                sb.append("<a href='facebookcustom/index.html'>Protected url by Facebook with custom authorizer (= must be a <em>HttpProfile</em> where the username starts with 'jle'): facebookcustom/index.html</a> (login with form or basic authentication before with jle* username)<br />");
                sb.append("<a href='twitter/index.html'>Protected url by Twitter: twitter/index.html</a> or <a href='twitter/index.html?client_name=FacebookClient'>by Facebook: twitter/index.html?client_name=FacebookClient</a> (use a real account)<br />");
                sb.append("<a href='form/index.html'>Protected url by form authentication: form/index.html</a> (use login = pwd)<br />");
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
                sb.append("<a href='/logout?url=/'>logout</a>");
                sb.append("<br /><br />");
                sb.append("profile: ");
                sb.append(getProfile(exchange));
                sb.append("<script src=\"http://ajax.googleapis.com/ajax/libs/jquery/1.9.1/jquery.min.js\"></script>");
                sb.append("<script src=\"assets/js/app.js\"></script>");

                exchange.getResponseSender().send(sb.toString());
            }
        };

    }

    public static HttpHandler authenticatedHandler = new HttpHandler() {
        @Override
        public void handleRequest(HttpServerExchange exchange) throws Exception {
            StringBuilder sb = new StringBuilder();
            sb.append("<h1>protected area</h1>");
            sb.append("<a href=\"..\">Back</a><br />");
            sb.append("<br /><br />");
            sb.append("profile : ");
            sb.append(getProfile(exchange));
            sb.append("<br />");

            exchange.getResponseSender().send(sb.toString());
        }
    };

    public static HttpHandler authenticatedJsonHandler = new HttpHandler() {
        @Override
        public void handleRequest(HttpServerExchange exchange) throws Exception {
            StringBuilder sb = new StringBuilder();
            sb.append("{\"username\":\"");
            sb.append(getProfile(exchange).getId());
            sb.append("\"}");

            exchange.getResponseHeaders().add(HttpString.tryFromString("Content-Type"), "application/json");
            ExchangeHelper.ok(exchange, sb.toString());
        }
    };

    private static UserProfile getProfile(final HttpServerExchange exchange) {
        final UndertowWebContext context = new UndertowWebContext(exchange);
        final ProfileManager manager = new ProfileManager(context);
        return manager.get(true);
    }

    public static HttpHandler formHandler(final Config config) {
        return new HttpHandler() {
            @Override
            public void handleRequest(HttpServerExchange exchange) throws Exception {
                FormClient formClient = (FormClient) config.getClients().findClient("FormClient");
                StringBuilder sb = new StringBuilder();
                sb.append("<html><body>");
                sb.append("<form action=\"").append(formClient.getCallbackUrl()).append("\" method=\"POST\">");
                sb.append("<input type=\"text\" name=\"username\" value=\"\" />");
                sb.append("<p />");
                sb.append("<input type=\"password\" name=\"password\" value=\"\" />");
                sb.append("<p />");
                sb.append("<input type=\"submit\" name=\"submit\" value=\"Submit\" />");
                sb.append("</form>");
                sb.append("</body></html>");

                exchange.getResponseSender().send(sb.toString());
            }
        };
    }

    public static HttpHandler jwtHandler() {
        return new HttpHandler() {
            @Override
            public void handleRequest(HttpServerExchange exchange) throws Exception {
                final UserProfile profile = getProfile(exchange);
                String token = "";
                if (profile != null) {
                    final JwtGenerator jwtGenerator = new JwtGenerator(DemoServer.JWT_SALT);
                    token = jwtGenerator.generate(profile);
                }
                StringBuilder sb = new StringBuilder();
                sb.append("<h1>Generate JWT token</h1>");
                sb.append("<a href='..'>Back</a><br />");
                sb.append("<br /><br />");
                sb.append("token:");
                sb.append(token);
                exchange.getResponseSender().send(sb.toString());
            }
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
