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

class KeycloakUrls {

    private String frontChannelUrl;
    private String backChannelurl;
    private RelativeUrlsUsed relativeUrlsUsed;

    private KeycloakUriBuilder authUrl;
    private String realmInfoUrl;
    private String accountUrl;
    private String tokenUrl;
    private KeycloakUriBuilder logoutUrl;
    private String registerNodeUrl;
    private String unregisterNodeUrl;
    private String jwksUrl;

    static KeycloakUrls unresolved() {
        return new KeycloakUrls();
    }

    static KeycloakUrls relative(String frontChannelUrl, String backChannelUrl, RelativeUrlsUsed relativeUrlsUsed) {
        return new KeycloakUrls(frontChannelUrl, backChannelUrl, relativeUrlsUsed);
    }

    private KeycloakUrls() {
    }

    private KeycloakUrls(String frontChannelUrl, String backChannelUrl, RelativeUrlsUsed relativeUrlsUsed) {
        this.frontChannelUrl = frontChannelUrl;
        this.backChannelurl = backChannelUrl;
        this.relativeUrlsUsed = relativeUrlsUsed;
    }

    KeycloakUrls(String frontChannelUrl, String backChannelUrl, RelativeUrlsUsed relativeUrlsUsed,
                 KeycloakUriBuilder authUrl, String realmInfoUrl,
                 String accountUrl, String tokenUrl, KeycloakUriBuilder logoutUrl,
                 String registerNodeUrl, String unregisterNodeUrl, String jwksUrl) {
        this(frontChannelUrl, backChannelUrl, relativeUrlsUsed);

        this.authUrl = authUrl;
        this.realmInfoUrl = realmInfoUrl;
        this.accountUrl = accountUrl;
        this.tokenUrl = tokenUrl;
        this.logoutUrl = logoutUrl;
        this.registerNodeUrl = registerNodeUrl;
        this.unregisterNodeUrl = unregisterNodeUrl;
        this.jwksUrl = jwksUrl;
    }


    String getFrontChannelBaseUrl() {
        return frontChannelUrl;
    }

    String getBackChannelBaseUrl() {
        return backChannelurl;
    }

    RelativeUrlsUsed getRelativeUrls() {
        return relativeUrlsUsed;
    }

    String getRealmInfoUrl() {
        return realmInfoUrl;
    }

    KeycloakUriBuilder getAuthUrl() {
        return authUrl;
    }

    String getAccountUrl() {
        return accountUrl;
    }

    String getTokenUrl() {
        return tokenUrl;
    }

    KeycloakUriBuilder getLogoutUrl() {
        return logoutUrl;
    }

    String getRegisterNodeUrl() {
        return registerNodeUrl;
    }

    String getUnregisterNodeUrl() {
        return unregisterNodeUrl;
    }

    String getJwksUrl() {
        return jwksUrl;
    }
}
