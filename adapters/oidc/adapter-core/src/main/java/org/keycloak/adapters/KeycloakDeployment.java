/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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

import org.apache.http.client.HttpClient;
import org.jboss.logging.Logger;
import org.keycloak.adapters.authentication.ClientCredentialsProvider;
import org.keycloak.adapters.authorization.PolicyEnforcer;
import org.keycloak.adapters.rotation.PublicKeyLocator;
import org.keycloak.common.enums.RelativeUrlsUsed;
import org.keycloak.common.enums.SslRequired;
import org.keycloak.common.util.KeycloakUriBuilder;
import org.keycloak.enums.TokenStore;
import org.keycloak.representations.adapters.config.AdapterConfig;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @author <a href="mailto:brad.culley@spartasystems.com">Brad Culley</a>
 * @author <a href="mailto:john.ament@spartasystems.com">John D. Ament</a>
 * @version $Revision: 1 $
 */
public class KeycloakDeployment {

    private static final Logger log = Logger.getLogger(KeycloakDeployment.class);

    protected String realm;
    protected PublicKeyLocator publicKeyLocator;
    protected KeycloakUrls urls = KeycloakUrls.unresolved();
    protected String principalAttribute = "sub";

    protected String resourceName;
    protected boolean bearerOnly;
    protected boolean autodetectBearerOnly;
    protected boolean enableBasicAuth;
    protected boolean publicClient;
    protected Map<String, Object> resourceCredentials = new HashMap<>();
    protected ClientCredentialsProvider clientAuthenticator;
    protected Callable<HttpClient> client;

    protected String scope;
    protected SslRequired sslRequired = SslRequired.ALL;
    protected int confidentialPort = -1;
    protected TokenStore tokenStore = TokenStore.SESSION;
    protected String adapterStateCookiePath = "";
    protected String stateCookieName = "OAuth_Token_Request_State";
    protected boolean useResourceRoleMappings;
    protected boolean cors;
    protected int corsMaxAge = -1;
    protected String corsAllowedHeaders;
    protected String corsAllowedMethods;
    protected String corsExposedHeaders;
    protected boolean exposeToken;
    protected boolean alwaysRefreshToken;
    protected boolean registerNodeAtStartup;
    protected int registerNodePeriod;
    protected boolean turnOffChangeSessionIdOnLogin;

    protected volatile int notBefore;
    protected int tokenMinimumTimeToLive;
    protected int minTimeBetweenJwksRequests;
    protected int publicKeyCacheTtl;
    protected Callable<PolicyEnforcer> policyEnforcer;

    // https://tools.ietf.org/html/rfc7636
    protected boolean pkce = false;
    protected boolean ignoreOAuthQueryParameter;

    protected Map<String, String> redirectRewriteRules;

    protected boolean delegateBearerErrorResponseSending = false;
    protected boolean verifyTokenAudience = false;

    public KeycloakDeployment() {
    }

    public boolean isConfigured() {
        return getRealm() != null && getPublicKeyLocator() != null && (isBearerOnly() || getAuthServerBaseUrl() != null);
    }

    public String getResourceName() {
        return resourceName;
    }

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    public PublicKeyLocator getPublicKeyLocator() {
        return publicKeyLocator;
    }

    public void setPublicKeyLocator(PublicKeyLocator publicKeyLocator) {
        this.publicKeyLocator = publicKeyLocator;
    }

    public String getAuthServerBaseUrl() {
        return urls.getFrontChannelBaseUrl();
    }

    public void setAuthServerBaseUrl(AdapterConfig config) {
        setAuthServerBaseUrl(config.getAuthServerUrl());
    }

    public void setAuthServerBaseUrl(String authServerBaseUrl) {
        this.urls = new KeycloakUrlResolver(getRealm())
                .withFrontChannelUrl(authServerBaseUrl)
                .resolve();
    }

    public void setAuthServerBaseUrls(String authServerBaseUrl, String authServerBackChannelBaseUrl) {
        urls = new KeycloakUrlResolver(getRealm())
                .withFrontChannelUrl(authServerBaseUrl)
                .withBackChannelUrl(authServerBackChannelBaseUrl)
                .resolve();
    }

    public String getAuthServerBackChannelBaseUrl() {
        return urls.getBackChannelBaseUrl();
    }

    /**
     * @param authUrlBuilder absolute URI
     * @deprecated use {@link KeycloakDeployment#setAuthServerBaseUrl(String)} or {@link KeycloakDeployment#setAuthServerBaseUrls(String, String)} instead
     */
    protected void resolveUrls(KeycloakUriBuilder authUrlBuilder) {
        urls = new KeycloakUrlResolver(getRealm())
                .withFrontChannelUrl(authUrlBuilder.build().toString())
                .resolve();
    }

    public RelativeUrlsUsed getRelativeUrls() {
        return urls.getRelativeUrls();
    }

    public String getRealmInfoUrl() {
        return urls.getRealmInfoUrl();
    }

    public KeycloakUriBuilder getAuthUrl() {
        return urls.getAuthUrl();
    }

    public String getTokenUrl() {
        return urls.getTokenUrl();
    }

    public KeycloakUriBuilder getLogoutUrl() {
        return urls.getLogoutUrl();
    }

    public String getAccountUrl() {
        return urls.getAccountUrl();
    }

    public String getRegisterNodeUrl() {
        return urls.getRegisterNodeUrl();
    }

    public String getUnregisterNodeUrl() {
        return urls.getUnregisterNodeUrl();
    }

    public String getJwksUrl() {
        return urls.getJwksUrl();
    }

    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }

    public boolean isBearerOnly() {
        return bearerOnly;
    }

    public void setBearerOnly(boolean bearerOnly) {
        this.bearerOnly = bearerOnly;
    }

    public boolean isAutodetectBearerOnly() {
        return autodetectBearerOnly;
    }

    public void setAutodetectBearerOnly(boolean autodetectBearerOnly) {
        this.autodetectBearerOnly = autodetectBearerOnly;
    }

    public boolean isEnableBasicAuth() {
        return enableBasicAuth;
    }

    public void setEnableBasicAuth(boolean enableBasicAuth) {
        this.enableBasicAuth = enableBasicAuth;
    }

    public boolean isPublicClient() {
        return publicClient;
    }

    public void setPublicClient(boolean publicClient) {
        this.publicClient = publicClient;
    }

    public Map<String, Object> getResourceCredentials() {
        return resourceCredentials;
    }

    public void setResourceCredentials(Map<String, Object> resourceCredentials) {
        this.resourceCredentials = resourceCredentials;
    }

    public ClientCredentialsProvider getClientAuthenticator() {
        return clientAuthenticator;
    }

    public void setClientAuthenticator(ClientCredentialsProvider clientAuthenticator) {
        this.clientAuthenticator = clientAuthenticator;
    }

    public HttpClient getClient() {
        try {
            return client.call();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void setClient(final HttpClient client) {
        this.client = new Callable<HttpClient>() {
            @Override
            public HttpClient call() {
                return client;
            }
        };
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public SslRequired getSslRequired() {
        return sslRequired;
    }

    public void setSslRequired(SslRequired sslRequired) {
        this.sslRequired = sslRequired;
    }

    public boolean isSSLEnabled() {
        if (SslRequired.NONE == sslRequired) {
            return false;
        }
        return true;
    }

    public int getConfidentialPort() {
        return confidentialPort;
    }

    public void setConfidentialPort(int confidentialPort) {
        this.confidentialPort = confidentialPort;
    }

    public TokenStore getTokenStore() {
        return tokenStore;
    }

    public void setTokenStore(TokenStore tokenStore) {
        this.tokenStore = tokenStore;
    }

    public String getAdapterStateCookiePath() {
        return adapterStateCookiePath;
    }

    public void setAdapterStateCookiePath(String adapterStateCookiePath) {
        this.adapterStateCookiePath = adapterStateCookiePath;
    }

    public String getStateCookieName() {
        return stateCookieName;
    }

    public void setStateCookieName(String stateCookieName) {
        this.stateCookieName = stateCookieName;
    }

    public boolean isUseResourceRoleMappings() {
        return useResourceRoleMappings;
    }

    public void setUseResourceRoleMappings(boolean useResourceRoleMappings) {
        this.useResourceRoleMappings = useResourceRoleMappings;
    }

    public boolean isCors() {
        return cors;
    }

    public void setCors(boolean cors) {
        this.cors = cors;
    }

    public int getCorsMaxAge() {
        return corsMaxAge;
    }

    public void setCorsMaxAge(int corsMaxAge) {
        this.corsMaxAge = corsMaxAge;
    }

    public String getCorsAllowedHeaders() {
        return corsAllowedHeaders;
    }

    public void setCorsAllowedHeaders(String corsAllowedHeaders) {
        this.corsAllowedHeaders = corsAllowedHeaders;
    }

    public String getCorsAllowedMethods() {
        return corsAllowedMethods;
    }

    public void setCorsAllowedMethods(String corsAllowedMethods) {
        this.corsAllowedMethods = corsAllowedMethods;
    }

    public String getCorsExposedHeaders() {
        return corsExposedHeaders;
    }

    public void setCorsExposedHeaders(String corsExposedHeaders) {
        this.corsExposedHeaders = corsExposedHeaders;
    }

    public boolean isExposeToken() {
        return exposeToken;
    }

    public void setExposeToken(boolean exposeToken) {
        this.exposeToken = exposeToken;
    }

    public int getNotBefore() {
        return notBefore;
    }

    public void setNotBefore(int notBefore) {
        this.notBefore = notBefore;
    }

    public void updateNotBefore(int notBefore) {
        this.notBefore = notBefore;
        getPublicKeyLocator().reset(this);
    }

    public boolean isAlwaysRefreshToken() {
        return alwaysRefreshToken;
    }

    public void setAlwaysRefreshToken(boolean alwaysRefreshToken) {
        this.alwaysRefreshToken = alwaysRefreshToken;
    }

    public boolean isRegisterNodeAtStartup() {
        return registerNodeAtStartup;
    }

    public void setRegisterNodeAtStartup(boolean registerNodeAtStartup) {
        this.registerNodeAtStartup = registerNodeAtStartup;
    }

    public int getRegisterNodePeriod() {
        return registerNodePeriod;
    }

    public void setRegisterNodePeriod(int registerNodePeriod) {
        this.registerNodePeriod = registerNodePeriod;
    }

    public String getPrincipalAttribute() {
        return principalAttribute;
    }

    public void setPrincipalAttribute(String principalAttribute) {
        this.principalAttribute = principalAttribute;
    }

    public boolean isTurnOffChangeSessionIdOnLogin() {
        return turnOffChangeSessionIdOnLogin;
    }

    public void setTurnOffChangeSessionIdOnLogin(boolean turnOffChangeSessionIdOnLogin) {
        this.turnOffChangeSessionIdOnLogin = turnOffChangeSessionIdOnLogin;
    }

    public int getTokenMinimumTimeToLive() {
        return tokenMinimumTimeToLive;
    }

    public void setTokenMinimumTimeToLive(final int tokenMinimumTimeToLive) {
        this.tokenMinimumTimeToLive = tokenMinimumTimeToLive;
    }

    public int getMinTimeBetweenJwksRequests() {
        return minTimeBetweenJwksRequests;
    }

    public void setMinTimeBetweenJwksRequests(int minTimeBetweenJwksRequests) {
        this.minTimeBetweenJwksRequests = minTimeBetweenJwksRequests;
    }

    public int getPublicKeyCacheTtl() {
        return publicKeyCacheTtl;
    }

    public void setPublicKeyCacheTtl(int publicKeyCacheTtl) {
        this.publicKeyCacheTtl = publicKeyCacheTtl;
    }

    public void setPolicyEnforcer(Callable<PolicyEnforcer> policyEnforcer) {
        this.policyEnforcer = policyEnforcer;
    }

    public PolicyEnforcer getPolicyEnforcer() {
        if (policyEnforcer == null) {
            return null;
        }
        try {
            return policyEnforcer.call();
        } catch (Exception cause) {
            throw new RuntimeException("Failed to obtain policy enforcer", cause);
        }
    }

    // https://tools.ietf.org/html/rfc7636
    public boolean isPkce() {
        return pkce;
    }

    public void setPkce(boolean pkce) {
        this.pkce = pkce;
    }

    public void setIgnoreOAuthQueryParameter(boolean ignoreOAuthQueryParameter) {
        this.ignoreOAuthQueryParameter = ignoreOAuthQueryParameter;
    }

    public boolean isOAuthQueryParameterEnabled() {
        return !this.ignoreOAuthQueryParameter;
    }

    public Map<String, String> getRedirectRewriteRules() {
        return redirectRewriteRules;
    }

    public void setRewriteRedirectRules(Map<String, String> redirectRewriteRules) {
        this.redirectRewriteRules = redirectRewriteRules;
    }

    public boolean isDelegateBearerErrorResponseSending() {
        return delegateBearerErrorResponseSending;
    }

    public void setDelegateBearerErrorResponseSending(boolean delegateBearerErrorResponseSending) {
        this.delegateBearerErrorResponseSending = delegateBearerErrorResponseSending;
    }

    public boolean isVerifyTokenAudience() {
        return verifyTokenAudience;
    }

    public void setVerifyTokenAudience(boolean verifyTokenAudience) {
        this.verifyTokenAudience = verifyTokenAudience;
    }

    public void setClient(Callable<HttpClient> callable) {
        client = callable;
    }
}
