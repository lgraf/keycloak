/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.adapters;

import org.keycloak.common.enums.RelativeUrlsUsed;
import org.keycloak.common.util.KeycloakUriBuilder;
import org.keycloak.constants.ServiceUrlConstants;

import java.net.URI;

class KeycloakUrlResolver {

    private final String realm;

    private String frontChannelUrl;
    private String backChannelUrl;

    KeycloakUrlResolver(String realm) {
        this.realm = realm;
    }

    KeycloakUrlResolver withFrontChannelUrl(String url) {
        if (url != null) {
            this.frontChannelUrl = stripDefaultPorts(url);
        }
        return this;
    }

    KeycloakUrlResolver withBackChannelUrl(String url) {
        if (url != null) {
            this.backChannelUrl = stripDefaultPorts(url);
        }
        return this;
    }

    KeycloakUrls resolve() {
        // bearer only with realm key deployment
        if (frontChannelUrl == null) {
            return KeycloakUrls.unresolved();
        }

        // relative url deployment (urls will be resolved per request)
        RelativeUrlsUsed relativeUrlsUsed = relativeUrlUsed(frontChannelUrl);
        if (relativeUrlsUsed == RelativeUrlsUsed.ALWAYS) {
            return KeycloakUrls.relative(frontChannelUrl, backChannelUrl, relativeUrlsUsed);
        }

        KeycloakUriBuilder builder = KeycloakUriBuilder.fromUri(frontChannelUrl);

        KeycloakUriBuilder authUrl = KeycloakUriBuilder.fromUri(builder.clone().path(ServiceUrlConstants.AUTH_PATH).build(realm));
        String realmInfoUrl = builder.clone().path(ServiceUrlConstants.REALM_INFO_PATH).build(realm).toString();
        String accountUrl = builder.clone().path(ServiceUrlConstants.ACCOUNT_SERVICE_PATH).build(realm).toString();

        if (backChannelUrl != null) {
            builder = KeycloakUriBuilder.fromUri(backChannelUrl);
        }

        String tokenUrl = builder.clone().path(ServiceUrlConstants.TOKEN_PATH).build(realm).toString();
        KeycloakUriBuilder logoutUrl = KeycloakUriBuilder.fromUri(builder.clone().path(ServiceUrlConstants.TOKEN_SERVICE_LOGOUT_PATH).build(realm));
        String registerNodeUrl = builder.clone().path(ServiceUrlConstants.CLIENTS_MANAGEMENT_REGISTER_NODE_PATH).build(realm).toString();
        String unregisterNodeUrl = builder.clone().path(ServiceUrlConstants.CLIENTS_MANAGEMENT_UNREGISTER_NODE_PATH).build(realm).toString();
        String jwksUrl = builder.clone().path(ServiceUrlConstants.JWKS_URL).build(realm).toString();

        return new KeycloakUrls(frontChannelUrl, backChannelUrl, relativeUrlsUsed, authUrl, realmInfoUrl, accountUrl, tokenUrl, logoutUrl, registerNodeUrl, unregisterNodeUrl, jwksUrl);
    }

    private RelativeUrlsUsed relativeUrlUsed(String url) {
        return URI.create(url).getHost() == null ? RelativeUrlsUsed.ALWAYS : RelativeUrlsUsed.NEVER;
    }

    private String stripDefaultPorts(String url) {
        return KeycloakUriBuilder.fromUri(url).build().toString();
    }
}
