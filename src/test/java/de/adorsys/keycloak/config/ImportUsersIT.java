package de.adorsys.keycloak.config;

import de.adorsys.keycloak.config.util.KeycloakAuthentication;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ImportUsersIT extends AbstractImportTest {
    private static final String REALM_NAME = "realmWithUsers";

    ImportUsersIT() {
        this.resourcePath = "import-files/users";
    }

    @Test
    public void integrationTests() {
        shouldCreateRealmWithUser();
        shouldUpdateRealmWithAddingClientUser();
        shouldUpdateRealmWithChangedClientUserPassword();
        shouldUpdateRealmWithUserThatUsernameMatchExisting();
        shouldCreateRealmWithUsersAndUpdateSingleUserCorrect();
    }

    private void shouldCreateRealmWithUser() {
        doImport("0_create_realm_with_user.json");

        RealmRepresentation createdRealm = keycloakProvider.get().realm(REALM_NAME).toRepresentation();

        assertThat(createdRealm.getRealm(), is(REALM_NAME));
        assertThat(createdRealm.isEnabled(), is(true));

        UserRepresentation createdUser = keycloakRepository.getUser(REALM_NAME, "myuser");

        assertThat(createdUser.getUsername(), is("myuser"));
        assertThat(createdUser.getEmail(), is("myuser@mail.de"));
        assertThat(createdUser.isEnabled(), is(true));
        assertThat(createdUser.getFirstName(), is("My firstname"));
        assertThat(createdUser.getLastName(), is("My lastname"));
    }

    private void shouldUpdateRealmWithAddingClientUser() {
        doImport("1_update_realm_add_clientuser.json");

        RealmRepresentation createdRealm = keycloakProvider.get().realm(REALM_NAME).toRepresentation();

        assertThat(createdRealm.getRealm(), is(REALM_NAME));
        assertThat(createdRealm.isEnabled(), is(true));

        UserRepresentation createdUser = keycloakRepository.getUser(REALM_NAME, "myclientuser");

        assertThat(createdUser.getUsername(), is("myclientuser"));
        assertThat(createdUser.getEmail(), is("myclientuser@mail.de"));
        assertThat(createdUser.isEnabled(), is(true));
        assertThat(createdUser.getFirstName(), is("My clientuser's firstname"));
        assertThat(createdUser.getLastName(), is("My clientuser's lastname"));

        // check if login with password is successful
        KeycloakAuthentication.AuthenticationToken token = keycloakAuthentication.login(
                REALM_NAME,
                "moped-client",
                "my-special-client-secret",
                "myclientuser",
                "myclientuser123"
        );

        assertThat(token.getAccessToken(), is(not(nullValue())));
        assertThat(token.getRefreshToken(), is(not(nullValue())));
        assertThat(token.getExpiresIn(), is(greaterThan(0)));
        assertThat(token.getRefreshExpiresIn(), is(greaterThan(0)));
        assertThat(token.getTokenType(), is("bearer"));
    }

    private void shouldUpdateRealmWithChangedClientUserPassword() {
        doImport("2_update_realm_change_clientusers_password.json");

        RealmRepresentation createdRealm = keycloakProvider.get().realm(REALM_NAME).toRepresentation();

        assertThat(createdRealm.getRealm(), is(REALM_NAME));
        assertThat(createdRealm.isEnabled(), is(true));

        UserRepresentation user = keycloakRepository.getUser(REALM_NAME, "myclientuser");

        assertThat(user.getUsername(), is("myclientuser"));
        assertThat(user.getEmail(), is("myclientuser@mail.de"));
        assertThat(user.isEnabled(), is(true));
        assertThat(user.getFirstName(), is("My clientuser's firstname"));
        assertThat(user.getLastName(), is("My clientuser's lastname"));

        // check if login with old password fails
        assertThrows(KeycloakAuthentication.AuthenticationException.class, () ->
                keycloakAuthentication.login(
                        REALM_NAME,
                        "moped-client",
                        "my-special-client-secret",
                        "myclientuser",
                        "myclientuser123"
                )
        );

        // check if login with new password is successful
        KeycloakAuthentication.AuthenticationToken token = keycloakAuthentication.login(
                REALM_NAME,
                "moped-client",
                "my-special-client-secret",
                "myclientuser",
                "changedclientuser123"
        );

        assertThat(token.getAccessToken(), is(not(nullValue())));
        assertThat(token.getRefreshToken(), is(not(nullValue())));
        assertThat(token.getExpiresIn(), is(greaterThan(0)));
        assertThat(token.getRefreshExpiresIn(), is(greaterThan(0)));
        assertThat(token.getTokenType(), is("bearer"));
    }

    /**
     * https://github.com/adorsys/keycloak-config-cli/issues/51
     */
    private void shouldUpdateRealmWithUserThatUsernameMatchExisting() {
        doImport("3_update_realm_with_new_user.json");

        RealmRepresentation createdRealm = keycloakProvider.get().realm(REALM_NAME).toRepresentation();

        assertThat(createdRealm.getRealm(), is(REALM_NAME));
        assertThat(createdRealm.isEnabled(), is(true));

        UserRepresentation user = keycloakRepository.getUser(REALM_NAME, "myuser");

        assertThat(user.getUsername(), is("myuser"));
        assertThat(user.getEmail(), is("myuser@mail.de"));
        assertThat(user.isEnabled(), is(true));
        assertThat(user.getFirstName(), is("My firstname"));
        assertThat(user.getLastName(), is("My lastname"));

        UserRepresentation createdUser = keycloakRepository.getUser(REALM_NAME, "my");

        assertThat(createdUser.getUsername(), is("my"));
        assertThat(createdUser.getEmail(), is("my@mail.de"));
        assertThat(createdUser.isEnabled(), is(true));
        assertThat(createdUser.getFirstName(), is("My firstname"));
        assertThat(createdUser.getLastName(), is("My lastname"));
    }

    /**
     * https://github.com/adorsys/keycloak-config-cli/issues/51
     */
    private void shouldCreateRealmWithUsersAndUpdateSingleUserCorrect() {

        doImport("4_1_create_realm_with_users_to_check_update.json");

        RealmResource realmResource = keycloakProvider.get().realm(REALM_NAME);
        final RealmRepresentation createdRealm = realmResource.toRepresentation();
        assertThat(createdRealm.getRealm(), is(REALM_NAME));
        assertThat(createdRealm.isEnabled(), is(true));
        assertThat(realmResource.users().list().size(), is(5));

        // act -> update realm with a single user to change
        doImport("4_2_create_realm_with_users_to_check_update.json");

        realmResource = keycloakProvider.get().realm(REALM_NAME);
        assertThat(realmResource.users().list().size(), is(6));

        // assert -> check whether only the "user1" was updated or not
        final UserRepresentation updatedUser = keycloakRepository.getUser(REALM_NAME, "user");
        assertThat(updatedUser.getEmail(), is("user@mail.de"));
        assertThat(updatedUser.getLastName(), is("lastName"));
        assertThat(updatedUser.getFirstName(), is("firstName"));

        final UserRepresentation updatedUser1 = keycloakRepository.getUser(REALM_NAME, "user1");
        assertThat(updatedUser1.getEmail(), is("user1@mail.de"));
        assertThat(updatedUser1.getLastName(), is("lastName1"));
        assertThat(updatedUser1.getFirstName(), is("firstName1"));

        final UserRepresentation updatedUser2 = keycloakRepository.getUser(REALM_NAME, "user2");
        assertThat(updatedUser2.getEmail(), is("user2@mail.de"));
        assertThat(updatedUser2.getLastName(), is("lastName2"));
        assertThat(updatedUser2.getFirstName(), is("firstName2"));
    }
}
