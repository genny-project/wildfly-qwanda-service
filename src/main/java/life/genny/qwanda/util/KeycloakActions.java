package life.genny.qwanda.util;

import java.io.IOException;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Base64.Decoder;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import javax.inject.Inject;
import javax.persistence.NoResultException;
import javax.ws.rs.core.MultivaluedMap;
import org.jboss.resteasy.specimpl.MultivaluedMapImpl;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.keycloak.KeycloakSecurityContext;
import life.genny.qwanda.attribute.Attribute;
import life.genny.qwanda.attribute.AttributeLink;
import life.genny.qwanda.attribute.AttributeText;
import life.genny.qwanda.entity.BaseEntity;
import life.genny.qwanda.entity.Group;
import life.genny.qwanda.entity.Person;
import life.genny.qwanda.exception.BadDataException;
import life.genny.qwanda.model.Setup;
import life.genny.qwanda.service.BaseEntityService;
import life.genny.qwanda.service.KeycloakService;

public class KeycloakActions {

  @Inject
  private static BaseEntityService service;

  public static Long importKeycloakUsers(final String keycloakUrl, final String realm,
      final String username, final String password, final String clientId,
      final Integer maxReturned, final String parentGroupCodes) {
    Long count = 0L;

    AttributeLink linkAttribute = service.findAttributeLinkByCode("LNK_CORE");
    List<BaseEntity> parentGroupList = new ArrayList<BaseEntity>();

    String[] parentCodes = parentGroupCodes.split(",");
    for (String parentCode : parentCodes) {
      BaseEntity group = null;

      try {
        group = service.findBaseEntityByCode(parentCode); // careful as GRPUSERS needs to
        parentGroupList.add(group);
      } catch (NoResultException e) {
        System.out.println("Group Code does not exist :" + parentCode);
      }

    }

    KeycloakService ks;
    final Map<String, Map<String, Object>> usersMap = new HashMap<String, Map<String, Object>>();

    try {
      ks = new KeycloakService(keycloakUrl, realm, username, password, clientId);
      final List<LinkedHashMap> users = ks.fetchKeycloakUsers(maxReturned);
      for (final Object user : users) {
        final LinkedHashMap map = (LinkedHashMap) user;
        final Map<String, Object> userMap = new HashMap<String, Object>();
        for (final Object key : map.keySet()) {
          // System.out.println(key + ":" + map.get(key));
          userMap.put((String) key, map.get(key));

        }
        usersMap.put((String) userMap.get("username"), userMap);
        System.out.println();
      }

      System.out.println("finished");
    } catch (final IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

    for (final String kcusername : usersMap.keySet()) {
      final MultivaluedMap params = new MultivaluedMapImpl();
      params.add("PRI_USERNAME", kcusername);
      final Map<String, Object> userMap = usersMap.get(kcusername);

      final List<BaseEntity> users = service.findBaseEntitysByAttributeValues(params, true, 0, 1);
      if (users.isEmpty()) {
        final String code = "PER_CH40_" + kcusername.toUpperCase().replaceAll("\\ ", "")
            .replaceAll("\\.", "").replaceAll("\\&", "");
        System.out.println("New User Code = " + code);
        String firstName = (String) userMap.get("firstName");
        firstName = firstName.replaceAll("\\.", " "); // replace dots
        firstName = firstName.replaceAll("\\_", " "); // replace dots
        String lastName = (String) userMap.get("lastName");
        lastName = lastName.replaceAll("\\.", " "); // replace dots
        lastName = lastName.replaceAll("\\_", " "); // replace dots
        String name = firstName + " " + lastName;

        final String email = (String) userMap.get("email");
        final String id = (String) userMap.get("id");
        final Long unixSeconds = (Long) userMap.get("createdTimestamp");
        final Date date = new Date(unixSeconds); // *1000 is to convert seconds to milliseconds
        final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z"); // the format of
        // your date
        sdf.setTimeZone(TimeZone.getTimeZone("GMT+10")); // give a timezone reference for formating
        sdf.format(date);
        final Attribute firstNameAtt = service.findAttributeByCode("PRI_FIRSTNAME");
        final Attribute lastNameAtt = service.findAttributeByCode("PRI_LASTNAME");
        final Attribute nameAtt = service.findAttributeByCode("PRI_NAME");
        final Attribute emailAtt = service.findAttributeByCode("PRI_EMAIL");
        final Attribute uuidAtt = service.findAttributeByCode("PRI_UUID");
        final Attribute usernameAtt = service.findAttributeByCode("PRI_USERNAME");

        try {
          final BaseEntity user = new BaseEntity(code, name);

          user.addAttribute(firstNameAtt, 0.0, firstName);
          user.addAttribute(lastNameAtt, 0.0, lastName);
          user.addAttribute(nameAtt, 0.0, name);
          user.addAttribute(emailAtt, 0.0, email);
          user.addAttribute(uuidAtt, 0.0, id);
          user.addAttribute(usernameAtt, 0.0, kcusername);
          service.insert(user);

          // Now link to groups
          for (final BaseEntity parent : parentGroupList) {
            if (!parent.containsTarget(user.getCode(), linkAttribute.getCode())) {
              parent.addTarget(user, linkAttribute, 1.0);
            }
          }
          count++;
          System.out.println("BE:" + user);
        } catch (final BadDataException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }

      } else {
        users.get(0);
      }

    }

    // now save the parents
    for (BaseEntity parent : parentGroupList) {
      service.update(parent);
    }

    return count;
  }

  public static void importKeycloakUsers(final List<Group> parentGroupList,
      final AttributeLink linkAttribute, final Integer maxReturned)
      throws IOException, BadDataException {

    final Map<String, String> envParams = System.getenv();
    String keycloakUrl = envParams.get("KEYCLOAKURL");
    System.out.println("Keycloak URL=[" + keycloakUrl + "]");
    keycloakUrl = keycloakUrl.replaceAll("'", "");
    final String realm = envParams.get("KEYCLOAK_REALM");
    final String username = envParams.get("KEYCLOAK_USERNAME");
    final String password = envParams.get("KEYCLOAK_PASSWORD");
    final String clientid = envParams.get("KEYCLOAK_CLIENTID");
    final String secret = envParams.get("KEYCLOAK_SECRET");

    System.out.println("Realm is :[" + realm + "]");

    final KeycloakService kcs =
        new KeycloakService(keycloakUrl, realm, username, password, clientid, secret);
    final List<LinkedHashMap> users = kcs.fetchKeycloakUsers(maxReturned);
    for (final LinkedHashMap user : users) {
      final String name = user.get("firstName") + " " + user.get("lastName");
      final Person newUser = new Person(name);
      final String keycloakUUID = (String) user.get("id");
      newUser.setCode(Person.getDefaultCodePrefix() + keycloakUUID.toUpperCase());
      newUser.setName(name);
      newUser.addAttribute(service.createAttributeText("NAME"), 1.0, name);
      newUser.addAttribute(service.createAttributeText("FIRSTNAME"), 1.0, user.get("firstName"));
      newUser.addAttribute(service.createAttributeText("LASTNAME"), 1.0, user.get("lastName"));
      newUser.addAttribute(service.createAttributeText("UUID"), 1.0, user.get("id"));
      newUser.addAttribute(service.createAttributeText("EMAIL"), 1.0, user.get("email"));
      newUser.addAttribute(service.createAttributeText("USERNAME"), 1.0, user.get("username"));
      System.out.println("Code=" + newUser.getCode());;
      service.insert(newUser);
      // Now link to groups
      for (final Group parent : parentGroupList) {
        if (!parent.containsTarget(newUser.getCode(), linkAttribute.getCode())) {
          parent.addTarget(newUser, linkAttribute, 1.0);

        }
      }
    }
    // now save the parents
    for (Group parent : parentGroupList) {
      service.update(parent);
    }
    System.out.println(users);
  }

  public static Setup setup(final KeycloakSecurityContext kContext) {
    final Setup setup = new Setup();
    String bearerToken = null;
    String decodedJson = null;
    JSONObject jsonObj;
    try {
      bearerToken = kContext.getTokenString();
      System.out.println("bearerToken:" + bearerToken);
      final String[] jwtToken = bearerToken.split("\\.");
      System.out.println("jwtToken:" + jwtToken);
      final Decoder decoder = Base64.getDecoder();
      final byte[] decodedClaims = decoder.decode(jwtToken[1]);
      decodedJson = new String(decodedClaims);
      System.out.println("decodedJson:" + decodedJson);
      jsonObj = new JSONObject(decodedJson);
      final String userUUID = jsonObj.getString("sub");
      System.out.println("UserId=" + userUUID);
      final JSONObject realm_access = (JSONObject) jsonObj.get("realm_access");
      final JSONArray realm_roles = (JSONArray) realm_access.get("roles");
      final JSONObject resource_access = (JSONObject) jsonObj.get("resource_access");
      final JSONObject qwandaService = (JSONObject) resource_access.get("qwanda-service");
      final JSONArray resource_roles = (JSONArray) qwandaService.get("roles");

      System.out.println("Roles:" + resource_roles + "," + realm_roles + "!");

      final BaseEntity be =
          service.findUserByAttributeValue(AttributeText.getDefaultCodePrefix() + "UUID", userUUID);
      be.getBaseEntityAttributes();
      setup.setUser(be);

      //
      // {"jti":"1ae163f0-5495-4466-b224-de35a1f5794b","exp":1494376724,"nbf":0,"iat":1494376424,"iss":"http://bouncer.outcome-hub.com/auth/realms/genny","aud":"qwanda-service","sub":"81ef02bd-9976-4ce4-9fb4-17f30b416e06","typ":"Bearer","azp":"qwanda-service","auth_time":1494376102,"session_state":"4153d350-e9e9-4a00-85de-2def89427f4e","acr":"0","client_session":"307f1c32-c7ea-4408-816c-12a189c09081","allowed-origins":["*"],"realm_access":{"roles":["uma_authorization","user"]},"resource_access":{"qwanda-service":{"roles":["admin"]},"account":{"roles":["manage-account","manage-account-links","view-profile"]}},"name":"Bob
      //
      // Console","preferred_username":"adamcrow63+bobconsole@gmail.com","given_name":"Bob","family_name":"Console","email":"adamcrow63+bobconsole@gmail.com"}
    } catch (final JSONException e1) {
      // log.error("bearerToken=" + bearerToken + " decodedJson=" + decodedJson + ":"
      // +
      // e1.getMessage());
    }

    setup.setLayout("layout1");

    return setup;
  }
}
