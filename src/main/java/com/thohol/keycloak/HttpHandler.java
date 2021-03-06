package com.thohol.keycloak;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpHeaders;
import org.apache.http.client.utils.URIBuilder;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

class HttpHandler {
    private static final Logger LOGGER = Logger.getLogger(HttpHandler.class);

    static JsonNode getJsonNode(String baseUrl, String contentType, Map<String, String> headers, Map<String, String> queryParameters, Map<String, String> formParameters, String graphQlQuery) {
        HttpResponse<String> response;
        if (graphQlQuery != null) {
            response = executeGraphQL(baseUrl, contentType, headers, queryParameters, graphQlQuery);
        } else {
            response = getResponse(baseUrl, contentType, headers, queryParameters, formParameters);
        }

        if (response.statusCode() != 200) {
            throw new JsonGraphQlRemoteClaimException("Wrong status received for remote claim - Expected: 200, Received: " + response.statusCode(), baseUrl);
        }
        try {
            return new ObjectMapper().readTree(response.body());
        } catch (IOException e) {
            throw new JsonGraphQlRemoteClaimException("Error when parsing response for remote claim", baseUrl, e);
        }
    }

    private static HttpResponse<String> getResponse(String baseUrl, String contentType, Map<String, String> headers, Map<String, String> queryParameters, Map<String, String> formParameters) {
        try {
            HttpClient httpClient = HttpClient.newHttpClient();
            URIBuilder uriBuilder = new URIBuilder(baseUrl);

            // Build queryParameters
            queryParameters.forEach(uriBuilder::setParameter);
            URI uri = uriBuilder.build();

            HttpRequest.Builder builder = HttpRequest.newBuilder().uri(uri);

            // Build formParameters
            if (formParameters != null) {
                builder.POST(Utils.getFormData(formParameters));
            }

            // Build headers
            builder.header(HttpHeaders.CONTENT_TYPE , contentType);
            headers.forEach(builder::header);

            // Call
            return httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException | IOException e) {
            throw new JsonGraphQlRemoteClaimException("Error when accessing remote claim", baseUrl, e);
        } catch (URISyntaxException e) {
            throw new JsonGraphQlRemoteClaimException("Wrong uri syntax ", baseUrl, e);
        }
    }

    private static HttpResponse<String> executeGraphQL(String baseUrl, String contentType, Map<String, String> headers, Map<String, String> queryParameters, String graphQlQuery) {
        try {
            HttpClient httpClient = HttpClient.newHttpClient();
            URIBuilder uriBuilder = new URIBuilder(baseUrl);
            URI uri = uriBuilder.build();

            HttpRequest.Builder builder = HttpRequest.newBuilder().uri(uri);
            builder.POST(Utils.getGraphQlBody(graphQlQuery, queryParameters));

            // Build headers
            builder.header(HttpHeaders.CONTENT_TYPE , contentType);
            headers.forEach(builder::header);

            // Call
            return httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException | IOException e) {
            throw new JsonGraphQlRemoteClaimException("Error when accessing remote claim", baseUrl, e);
        } catch (URISyntaxException e) {
            throw new JsonGraphQlRemoteClaimException("Wrong uri syntax ", baseUrl, e);
        }
    }
}
