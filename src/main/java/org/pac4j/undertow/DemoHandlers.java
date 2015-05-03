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

import io.undertow.server.DefaultResponseListener;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import io.undertow.util.HttpString;

import org.pac4j.cas.client.CasClient;
import org.pac4j.core.client.Clients;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.exception.RequiresHttpAction;
import org.pac4j.http.client.BasicAuthClient;
import org.pac4j.http.client.FormClient;
import org.pac4j.oauth.client.FacebookClient;
import org.pac4j.oauth.client.TwitterClient;
import org.pac4j.saml.client.Saml2Client;
import org.pac4j.undertow.utils.HttpResponseHelper;
import org.pac4j.undertow.utils.StorageHelper;

/**
 * A collection of basic handlers printing dynamic html for the demo application.
 * 
 * @author Michael Remond
 * @since 1.0.0
 *
 */
public class DemoHandlers {

    public static HttpHandler buildIndexHandler(final Config config) {

        return new HttpHandler() {
            @Override
            public void handleRequest(HttpServerExchange exchange) throws Exception {
                StorageHelper.createSession(exchange);
                WebContext context = new UndertowWebContext(exchange);
                Clients client = config.getClients();
                FacebookClient fbClient = (FacebookClient) client.findClient("FacebookClient");
                TwitterClient twClient = (TwitterClient) client.findClient("TwitterClient");
                FormClient formClient = (FormClient) client.findClient("FormClient");
                BasicAuthClient baClient = (BasicAuthClient) client.findClient("BasicAuthClient");
                CasClient casClient = (CasClient) client.findClient("CasClient");
                Saml2Client saml2Client = (Saml2Client) client.findClient("Saml2Client");

                StringBuilder sb = new StringBuilder();
                sb.append("<h1>index</h1>");
                sb.append("<a href=\"facebook/index.html\">Protected url by Facebook : facebook/index.html</a><br />");
                sb.append("<a href=\"twitter/index.html\">Protected url by Twitter : twitter/index.html</a><br />");
                sb.append("<a href=\"form/index.html\">Protected url by form authentication : form/index.html</a><br />");
                sb.append("<a href=\"javascript:ajaxClick();\">Click here to send AJAX request after performing form authentication</a><br />");
                sb.append("<a href=\"basicauth/index.html\">Protected url by basic auth : basicauth/index.html</a><br />");
                sb.append("<a href=\"cas/index.html\">Protected url by CAS : cas/index.html</a><br />");
                sb.append("<a href=\"saml2/index.html\">Protected url by SAML2 : saml2/index.html</a><br />");
                sb.append("<br />");
                sb.append("<a href=\"logout\">logout</a>");
                sb.append("<br /><br />");
                sb.append("profile : ");
                sb.append(StorageHelper.getProfile(exchange));
                sb.append("<br /><br />");
                sb.append("<hr />");
                try {
                    sb.append("<a href=\"").append(fbClient.getRedirectAction(context, false, false).getLocation())
                            .append("\">Authenticate with Facebook</a><br />");
                    sb.append("<a href=\"").append(twClient.getRedirectAction(context, false, false).getLocation())
                            .append("\">Authenticate with Twitter</a><br />");
                    sb.append("<a href=\"").append(formClient.getRedirectAction(context, false, false).getLocation())
                            .append("\">Authenticate with form</a><br />");
                    sb.append("<a href=\"").append(baClient.getRedirectAction(context, false, false).getLocation())
                            .append("\">Authenticate with basic auth</a><br />");
                    sb.append("<a href=\"").append(casClient.getRedirectAction(context, false, false).getLocation())
                            .append("\">Authenticate with CAS</a><br />");
                    sb.append("<a href=\"").append(saml2Client.getRedirectAction(context, false, false).getLocation())
                            .append("\">Authenticate with SAML</a><br />");
                } catch (RequiresHttpAction e) {

                }
                sb.append("<script src=\"http://ajax.googleapis.com/ajax/libs/jquery/1.9.1/jquery.min.js\"></script>");
                sb.append("<script src=\"assets/js/app.js\"></script>");

                returnHtml(exchange, sb.toString());
            }
        };
    }

    private static void returnHtml(final HttpServerExchange exchange, final String content) {
        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/html");
        exchange.getResponseSender().send(content);
    }

    public static HttpHandler authenticatedHandler = new HttpHandler() {
        @Override
        public void handleRequest(HttpServerExchange exchange) throws Exception {

            StringBuilder sb = new StringBuilder();
            sb.append("<h1>protected area</h1>");
            sb.append("<a href=\"..\">Back</a><br />");
            sb.append("<br /><br />");
            sb.append("profile : ");
            sb.append(StorageHelper.getProfile(exchange));
            sb.append("<br />");

            returnHtml(exchange, sb.toString());
        }
    };

    public static HttpHandler authenticatedJsonHandler = new HttpHandler() {
        @Override
        public void handleRequest(HttpServerExchange exchange) throws Exception {
            ProfileWrapper profile = StorageHelper.getProfile(exchange);
            StringBuilder sb = new StringBuilder();
            sb.append("{\"username\":\"");
            sb.append(profile.getProfile().getUsername());
            sb.append("\"}");

            exchange.getResponseHeaders().add(HttpString.tryFromString("Content-Type"), "application/json");
            HttpResponseHelper.ok(exchange, sb.toString());
        }
    };

    public static HttpHandler buildFormHandler(final Config config) {
        return new HttpHandler() {
            @Override
            public void handleRequest(HttpServerExchange exchange) throws Exception {
                FormClient formClient = (FormClient) config.getClients().findClient("FormClient");
                StringBuilder sb = new StringBuilder();
                sb.append("<form action=\"").append(formClient.getCallbackUrl()).append("\" method=\"POST\">");
                sb.append("<input type=\"text\" name=\"username\" value=\"\" />");
                sb.append("<p />");
                sb.append("<input type=\"password\" name=\"password\" value=\"\" />");
                sb.append("<p />");
                sb.append("<input type=\"submit\" name=\"submit\" value=\"Submit\" />");
                sb.append("</form>");

                returnHtml(exchange, sb.toString());
            }
        };
    }

    private static void handle403(final HttpServerExchange exchange) {
        exchange.addDefaultResponseListener(new DefaultResponseListener() {
            @Override
            public boolean handleDefaultResponse(final HttpServerExchange exchange) {
                if (!exchange.isResponseChannelAvailable()) {
                    return false;
                }
                if (exchange.getResponseCode() == 403) {
                    returnHtml(exchange, "Forbidden");
                    return true;
                }
                return false;
            }
        });
    }
}
