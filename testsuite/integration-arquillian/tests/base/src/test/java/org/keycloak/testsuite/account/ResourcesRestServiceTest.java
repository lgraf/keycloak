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
package org.keycloak.testsuite.account;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.Test;
import org.keycloak.admin.client.resource.AuthorizationResource;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.ClientsResource;
import org.keycloak.authorization.client.AuthzClient;
import org.keycloak.authorization.client.Configuration;
import org.keycloak.broker.provider.util.SimpleHttp;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.models.AccountRoles;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ErrorRepresentation;
import org.keycloak.representations.idm.OAuth2ErrorRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.idm.authorization.PermissionTicketRepresentation;
import org.keycloak.representations.idm.authorization.ResourceRepresentation;
import org.keycloak.representations.idm.authorization.ScopeRepresentation;
import org.keycloak.services.resources.account.resources.AbstractResourceService;
import org.keycloak.services.resources.account.resources.AbstractResourceService.Permission;
import org.keycloak.services.resources.account.resources.AbstractResourceService.Resource;
import org.keycloak.testsuite.util.ClientBuilder;
import org.keycloak.testsuite.util.UserBuilder;
import org.keycloak.util.JsonSerialization;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class ResourcesRestServiceTest extends AbstractRestServiceTest {

    private AuthzClient authzClient;
    private List<String> userNames = new ArrayList<>(Arrays.asList("alice", "jdoe", "bob"));

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
        super.configureTestRealm(testRealm);
        RealmRepresentation realmRepresentation = testRealm;

        realmRepresentation.setUserManagedAccessAllowed(true);

        testRealm.getUsers().add(createUser("alice", "password"));
        testRealm.getUsers().add(createUser("jdoe", "password"));
        testRealm.getUsers().add(createUser("bob", "password"));

        ClientRepresentation client = ClientBuilder.create()
                .clientId("my-resource-server")
                .authorizationServicesEnabled(true)
                .serviceAccountsEnabled(true)
                .secret("secret")
                .name("My Resource Server")
                .baseUrl("http://resourceserver.com")
                .directAccessGrants().build();

        testRealm.getClients().add(client);
    }

    @Override
    public void before() {
        super.before();
        ClientResource resourceServer = getResourceServer();
        authzClient = createAuthzClient(resourceServer.toRepresentation());
        AuthorizationResource authorization = resourceServer.authorization();

        for (int i = 0; i < 30; i++) {
            ResourceRepresentation resource = new ResourceRepresentation();

            resource.setOwnerManagedAccess(true);

            try {
                resource.setOwner(
                        JsonSerialization.readValue(new JWSInput(tokenUtil.getToken()).getContent(), AccessToken.class)
                                .getSubject());
            } catch (Exception cause) {
                throw new RuntimeException("Failed to parse access token", cause);
            }

            resource.setName("Resource " + i);
            resource.setDisplayName("Display Name " + i);
            resource.setIconUri("Icon Uri " + i);
            resource.addScope("Scope A", "Scope B", "Scope C", "Scope D");
            resource.setUri("http://resourceServer.com/resources/" + i);

            try (Response response1 = authorization.resources().create(resource)) {
                resource.setId(response1.readEntity(ResourceRepresentation.class).getId());
            }

            for (String scope : Arrays.asList("Scope A", "Scope B")) {
                PermissionTicketRepresentation ticket = new PermissionTicketRepresentation();

                ticket.setGranted(true);
                ticket.setOwner(resource.getOwner().getId());
                ticket.setRequesterName(userNames.get(i % userNames.size()));
                ticket.setResource(resource.getId());
                ticket.setScopeName(scope);

                authzClient.protection("test-user@localhost", "password").permission().create(ticket);
            }
        }
    }

    private ClientResource getResourceServer() {
        ClientsResource clients = testRealm().clients();
        return clients.get(clients.findByClientId("my-resource-server").get(0).getId());
    }

    @Override
    public void after() {
        super.after();
        ClientResource resourceServer = getResourceServer();
        ClientRepresentation representation = resourceServer.toRepresentation();
        representation.setAuthorizationServicesEnabled(false);
        resourceServer.update(representation);
        representation.setAuthorizationServicesEnabled(true);
        resourceServer.update(representation);
    }

    @Test
    public void testGetMyResources() {
        List<Resource> resources = getMyResources();

        assertEquals(30, resources.size());

        for (int i = 0; i < 30; i++) {
            String uri = "http://resourceServer.com/resources/" + i;
            Resource resource = resources.stream()
                    .filter(rep -> rep.getUris().stream().anyMatch(resourceUri -> resourceUri.equals(uri))).findAny()
                    .get();

            assertNotNull(resource.getId());
            assertEquals("Resource " + i, resource.getName());
            assertEquals("Display Name " + i, resource.getDisplayName());
            assertEquals("Icon Uri " + i, resource.getIconUri());
            assertEquals("my-resource-server", resource.getClient().getClientId());
            assertEquals("My Resource Server", resource.getClient().getName());
            assertEquals("http://resourceserver.com", resource.getClient().getBaseUrl());
        }
    }

    @Test
    public void testGetSharedWithMe() {
        for (String userName : userNames) {
            List<AbstractResourceService.ResourcePermission> resources = getSharedWithMe(userName);

            assertEquals(10, resources.size());

            for (AbstractResourceService.ResourcePermission resource : resources) {
                String uri = resource.getUri();
                int id = Integer.parseInt(uri.substring(uri.lastIndexOf('/') + 1));
                assertNotNull(resource.getId());
                assertEquals("Resource " + id, resource.getName());
                assertEquals("Display Name " + id, resource.getDisplayName());
                assertEquals("Icon Uri " + id, resource.getIconUri());
                assertEquals("my-resource-server", resource.getClient().getClientId());
                assertEquals("My Resource Server", resource.getClient().getName());
                assertEquals("http://resourceserver.com", resource.getClient().getBaseUrl());
                assertEquals(2, resource.getScopes().size());
            }
        }
    }

    @Test
    public void testGetSharedWithOthers() {
        List<AbstractResourceService.ResourcePermission> resources = doGet("/shared-with-others",
                new TypeReference<List<AbstractResourceService.ResourcePermission>>() {
                });

        assertEquals(30, resources.size());

        for (AbstractResourceService.ResourcePermission resource : resources) {
            String uri = resource.getUri();
            int id = Integer.parseInt(uri.substring(uri.lastIndexOf('/') + 1));
            assertNotNull(resource.getId());
            assertEquals("Resource " + id, resource.getName());
            assertEquals("Display Name " + id, resource.getDisplayName());
            assertEquals("Icon Uri " + id, resource.getIconUri());
            assertEquals("my-resource-server", resource.getClient().getClientId());
            assertEquals("My Resource Server", resource.getClient().getName());
            assertEquals("http://resourceserver.com", resource.getClient().getBaseUrl());
            assertEquals(1, resource.getPermissions().size());
            Permission user = resource.getPermissions().iterator().next();
            
            assertTrue(userNames.contains(user.getUsername()));
            
            assertEquals(2, user.getScopes().size());
        }
    }

    @Test
    public void testGetResource() {
        Resource resource = doGet("/" + getMyResources().get(0).getId(), Resource.class);

        String uri = resource.getUri();
        int id = Integer.parseInt(uri.substring(uri.lastIndexOf('/') + 1));
        assertNotNull(resource.getId());
        assertEquals("Resource " + id, resource.getName());
        assertEquals("Display Name " + id, resource.getDisplayName());
        assertEquals("Icon Uri " + id, resource.getIconUri());
        assertEquals("my-resource-server", resource.getClient().getClientId());
        assertEquals("My Resource Server", resource.getClient().getName());
        assertEquals("http://resourceserver.com", resource.getClient().getBaseUrl());
        assertEquals(4, resource.getScopes().size());

        OAuth2ErrorRepresentation response = doGet("/invalid_resource", OAuth2ErrorRepresentation.class);
        assertEquals("resource_not_found", response.getError());

        response = doGet("/" + getMyResources().get(0).getId(), authzClient.obtainAccessToken("jdoe", "password").getToken(), OAuth2ErrorRepresentation.class);
        assertEquals("invalid_resource", response.getError());
    }

    @Test
    public void testGetPermissions() throws Exception {
        Resource resource = getMyResources().get(0);
        List<Permission> shares = doGet("/" + resource.getId() + "/permissions", new TypeReference<List<Permission>>() {});

        assertEquals(1, shares.size());

        Permission firstShare = shares.get(0);
        List<Permission> permissions = new ArrayList<>();
        
        assertTrue(userNames.contains(firstShare.getUsername()));
        assertEquals(2, firstShare.getScopes().size());
        
        List<String> users = new ArrayList<>(userNames);
        
        users.remove(firstShare.getUsername());
        
        for (String userName : users) {
            Permission permission = new Permission();

            permission.setUsername(userName);
            permission.addScope("Scope D");

            permissions.add(permission);
        }

        SimpleHttp.doPut(getAccountUrl("resources/" + resource.getId() + "/permissions"), httpClient)
                .auth(tokenUtil.getToken())
                .json(permissions).asResponse();

        shares = doGet("/" + resource.getId() + "/permissions", new TypeReference<List<Permission>>() {});

        assertEquals(3, shares.size());

        for (Permission user : shares) {
            assertTrue(userNames.contains(user.getUsername()));
            
            if (firstShare.getUsername().equals(user.getUsername())) {
                assertEquals(2, user.getScopes().size());
            } else {
                assertEquals(1, user.getScopes().size());
            }
        }
    }

    @Test
    public void testShareResource() throws Exception {
        List<String> users = Arrays.asList("jdoe", "alice");
        List<Permission> permissions = new ArrayList<>();
        AbstractResourceService.ResourcePermission sharedResource = null;

        for (String user : users) {
            sharedResource = getSharedWithMe(user).get(0);

            assertNotNull(sharedResource);
            assertEquals(2, sharedResource.getScopes().size());
        }

        permissions.add(new Permission(users.get(0), "Scope C", "Scope D"));
        permissions.add(new Permission(users.get(users.size() - 1), "Scope A", "Scope B", "Scope C", "Scope D"));
        
        String resourceId = sharedResource.getId();
        SimpleHttp.Response response = SimpleHttp.doPut(getAccountUrl("resources/" + resourceId + "/permissions"), httpClient)
                .auth(tokenUtil.getToken())
                .json(permissions).asResponse();

        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());
        
        for (String user : users) {
            sharedResource = getSharedWithMe(user).stream()
                    .filter(resource1 -> resource1.getId().equals(resourceId)).findAny().orElse(null);

            assertNotNull(sharedResource);
            
            if (user.equals(users.get(users.size() - 1))) {
                assertEquals(4, sharedResource.getScopes().size());
            } else {
                assertEquals(2, sharedResource.getScopes().size());
            }
        }
    }

    @Test
    public void failShareResourceInvalidPermissions() throws Exception {
        List<Permission> permissions = new ArrayList<>();

        SimpleHttp.Response response = SimpleHttp.doPut(getAccountUrl("resources/" + getMyResources().get(0).getId() + "/permissions"), httpClient)
                .auth(tokenUtil.getToken())
                .json(permissions).asResponse();

        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    @Test
    public void testRevokePermission() throws Exception {
        List<String> users = Arrays.asList("jdoe", "alice");
        List<Permission> permissions = new ArrayList<>();
        AbstractResourceService.ResourcePermission sharedResource = null;

        for (String user : users) {
            sharedResource = getSharedWithMe(user).get(0);

            assertNotNull(sharedResource);
            assertEquals(2, sharedResource.getScopes().size());
        }

        permissions.add(new Permission(users.get(0), "Scope C"));
        permissions.add(new Permission(users.get(users.size() - 1), "Scope B", "Scope D"));

        String resourceId = sharedResource.getId();
        SimpleHttp.Response response = SimpleHttp.doPut(getAccountUrl("resources/" + resourceId + "/permissions"), httpClient)
                .auth(tokenUtil.getToken())
                .json(permissions).asResponse();

        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());

        for (String user : users) {
            sharedResource = getSharedWithMe(user).stream()
                    .filter(resource1 -> resource1.getId().equals(resourceId)).findAny().orElse(null);

            assertNotNull(sharedResource);

            if (user.equals(users.get(users.size() - 1))) {
                assertEquals(2, sharedResource.getScopes().size());
            } else {
                assertEquals(1, sharedResource.getScopes().size());
            }
        }
    }

    @Test
    public void testGetPermissionRequests() {
        Resource resource = getMyResources().get(0);
        List<Permission> requests = doGet("/" + resource.getId() + "/permissions/requests",
                new TypeReference<List<Permission>>() {});
        
        assertTrue(requests.isEmpty());

        for (String userName : userNames) {
            List<String> scopes = new ArrayList<>();
            
            if ("bob".equals(userName)) {
                scopes.add("Scope D");    
            } else if ("alice".equals(userName)) {
                scopes.add("Scope C");
            } else if ("jdoe".equals(userName)) {
                scopes.add("Scope C");
                scopes.add("Scope D");
            }

            for (String scope : scopes) {
                PermissionTicketRepresentation ticket = new PermissionTicketRepresentation();

                ticket.setGranted(false);
                ticket.setOwner("test-user@localhost");
                ticket.setRequesterName(userName);
                ticket.setResource(resource.getId());
                ticket.setScopeName(scope);

                authzClient.protection("test-user@localhost", "password").permission().create(ticket);       
            }
        }

        requests = doGet("/" + resource.getId() + "/permissions/requests",
                new TypeReference<List<Permission>>() {});
        
        assertEquals(3, requests.size());

        Iterator<Permission> iterator = requests.iterator();

        while (iterator.hasNext()) {
            Permission permission = iterator.next();
            String username = permission.getUsername();
            List<String> scopes = permission.getScopes();

            if ("bob".equals(username)) {
                assertEquals(1, scopes.size());
                assertTrue(scopes.contains("Scope D"));
                iterator.remove();
            } else if ("alice".equals(username)) {
                assertEquals(1, scopes.size());
                assertTrue(scopes.contains("Scope C"));
                iterator.remove();
            } else if ("jdoe".equals(username)) {
                assertEquals(2, scopes.size());
                assertTrue(scopes.contains("Scope C"));
                assertTrue(scopes.contains("Scope D"));
                iterator.remove();
            }
        }
        
        assertTrue(requests.isEmpty());
    }

    @Test
    public void testApprovePermissionRequest() throws IOException {
        Resource resource = getMyResources().get(0);
        List<Permission> requests = doGet("/" + resource.getId() + "/permissions/requests",
                new TypeReference<List<Permission>>() {});

        assertTrue(requests.isEmpty());

        for (String userName : userNames) {
            List<String> scopes = new ArrayList<>();

            if ("bob".equals(userName)) {
                scopes.add("Scope D");
            } else if ("alice".equals(userName)) {
                scopes.add("Scope C");
            } else if ("jdoe".equals(userName)) {
                scopes.add("Scope C");
                scopes.add("Scope D");
            }

            for (String scope : scopes) {
                PermissionTicketRepresentation ticket = new PermissionTicketRepresentation();

                ticket.setGranted(false);
                ticket.setOwner("test-user@localhost");
                ticket.setRequesterName(userName);
                ticket.setResource(resource.getId());
                ticket.setScopeName(scope);

                authzClient.protection("test-user@localhost", "password").permission().create(ticket);
            }
        }

        requests = doGet("/" + resource.getId() + "/permissions/requests",
                new TypeReference<List<Permission>>() {});

        assertEquals(3, requests.size());

        Iterator<Permission> iterator = requests.iterator();

        while (iterator.hasNext()) {
            Permission permission = iterator.next();
            String username = permission.getUsername();
            List<String> scopes = permission.getScopes();

            if ("bob".equals(username)) {
                scopes.clear();
            } else if ("jdoe".equals(username)) {
                scopes.remove("Scope C");
            }
        }

        SimpleHttp.doPut(getAccountUrl("resources/" + resource.getId() + "/permissions"), httpClient)
                .auth(tokenUtil.getToken())
                .json(requests).asResponse();

        requests = doGet("/" + resource.getId() + "/permissions/requests",
                new TypeReference<List<Permission>>() {});
        
        assertTrue(requests.isEmpty());

        for (String user : Arrays.asList("alice", "jdoe")) {
            AbstractResourceService.ResourcePermission sharedResource = getSharedWithMe(user).stream()
                    .filter(resource1 -> resource1.getId().equals(resource.getId())).findAny().orElse(null);

            assertNotNull(sharedResource);

            Set<ScopeRepresentation> scopes = sharedResource.getScopes();

            if ("alice".equals(user)) {
                assertEquals(1, scopes.size());
                assertTrue(scopes.stream().anyMatch(scope -> "Scope C".equals(scope.getName())));
            } else if ("jdoe".equals(user)) {
                assertEquals(1, scopes.size());
                assertTrue(scopes.stream().anyMatch(scope -> "Scope D".equals(scope.getName())));
            }
        }
    }

    private List<AbstractResourceService.ResourcePermission> getSharedWithMe(String userName) {
        return doGet("/shared-with-me", authzClient.obtainAccessToken(userName, "password").getToken(),
                new TypeReference<List<AbstractResourceService.ResourcePermission>>() {});
    }

    private <R> R doGet(String resource, TypeReference<R> typeReference) {
        return doGet(resource, tokenUtil.getToken(), typeReference);
    }

    private <R> R doGet(String resource, Class<R> type) {
        return doGet(resource, tokenUtil.getToken(), type);
    }

    private <R> R doGet(String resource, String token, TypeReference<R> typeReference) {
        try {
            return get(resource, token).asJson(typeReference);
        } catch (IOException cause) {
            throw new RuntimeException("Failed to fetch resource", cause);
        }
    }

    private <R> R doGet(String resource, String token, Class<R> type) {
        try {
            return get(resource, token).asJson(type);
        } catch (IOException cause) {
            throw new RuntimeException("Failed to fetch resource", cause);
        }
    }

    private SimpleHttp get(String resource, String token) {
        return SimpleHttp.doGet(getAccountUrl("resources" + resource), httpClient).auth(token);
    }

    private AuthzClient createAuthzClient(ClientRepresentation client) {
        Map<String, Object> credentials = new HashMap<>();

        credentials.put("secret", "secret");

        return AuthzClient
                .create(new Configuration(suiteContext.getAuthServerInfo().getContextRoot().toString() + "/auth",
                        suiteContext.getAuthServerInfo().getContextRoot().toString() + "/auth",
                        testRealm().toRepresentation().getRealm(), client.getClientId(),
                        credentials, httpClient));
    }

    private UserRepresentation createUser(String userName, String password) {
        return UserBuilder.create()
                .username(userName)
                .enabled(true)
                .password(password)
                .role("account", AccountRoles.MANAGE_ACCOUNT)
                .build();
    }

    private List<Resource> getMyResources() {
        return doGet("", new TypeReference<List<Resource>>() {});
    }
}
