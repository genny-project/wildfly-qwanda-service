package life.genny.qwanda.service;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.*;
import javax.annotation.PostConstruct;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.validation.constraints.NotNull;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.core.MultivaluedMap;

import life.genny.qwanda.*;
import life.genny.qwanda.attribute.AttributeLink;
import life.genny.qwanda.attribute.EntityAttribute;
import life.genny.qwanda.message.*;
import life.genny.qwanda.validation.Validation;
import org.apache.logging.log4j.Logger;
import io.vertx.core.json.JsonObject;
import life.genny.qwanda.attribute.Attribute;
import life.genny.qwanda.entity.BaseEntity;
import life.genny.qwanda.entity.EntityEntity;
import life.genny.qwanda.exception.BadDataException;
import life.genny.qwanda.util.PersistenceHelper;
import life.genny.qwandautils.GennySettings;
import life.genny.qwandautils.JsonUtils;
import life.genny.security.SecureResources;
import life.genny.services.BaseEntityService2;
import life.genny.utils.VertxUtils;
import life.genny.bootxport.bootx.QwandaRepository;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

@RequestScoped

public class Service extends BaseEntityService2 implements QwandaRepository {

    @Override
    public void setRealm(String realm) {
        // TODO Auto-generated method stub

    }

    @Override
    public <T> void delete(T entity) {
        getEntityManager().remove(entity);
    }

    /**
     * Stores logger object.
     */
    protected static final Logger log = org.apache.logging.log4j.LogManager
            .getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());

    public Service() {

    }

    @Inject
    private PersistenceHelper helper;

    @Inject
    private SecurityService securityService;


    @Inject
    private Hazel inDB;

    @Inject
    private ServiceTokenService serviceTokens;

    String token;

    String currentRealm = GennySettings.mainrealm; // permit temprorary override

    @PostConstruct
    public void init() {

    }

    @Override
    public String getToken() {
        return serviceTokens.getServiceToken(getRealm());
    }

    @Override
    @javax.ejb.Asynchronous
    public void sendQEventAttributeValueChangeMessage(final QEventAttributeValueChangeMessage event) {
        // Send a vertx message broadcasting an attribute value Change
        log.info(String.format("!!ATTRIBUTE CHANGE EVENT ->%s", event.getBe().getCode()));
        VertxUtils.publish(getUser(), "events", JsonUtils.toJson(event));
    }

    @Override
    @javax.ejb.Asynchronous
    public void sendQEventLinkChangeMessage(final QEventLinkChangeMessage event) {
        // Send a vertx message broadcasting an link Change

        BaseEntity originalParent = null;
        BaseEntity targetParent = null;
        try {
            // update cache for source and target
            if (event.getOldLink() != null && event.getOldLink().getSourceCode() != null) {
                String originalParentCode = event.getOldLink().getSourceCode();
                originalParent = this.findBaseEntityByCode(originalParentCode);
                updateDDT(originalParent.getCode(), JsonUtils.toJson(originalParent));
                QEventAttributeValueChangeMessage parentEvent = new QEventAttributeValueChangeMessage(
                        originalParent.getCode(), originalParent.getCode(), originalParent, event.getToken());
                this.sendQEventAttributeValueChangeMessage(parentEvent);
            }

            if (event.getLink() != null && event.getLink().getSourceCode() != null) {
                String targetParentCode = event.getLink().getSourceCode();
                targetParent = this.findBaseEntityByCode(targetParentCode);
                updateDDT(targetParent.getCode(), JsonUtils.toJson(targetParent));
                QEventAttributeValueChangeMessage targetEvent = new QEventAttributeValueChangeMessage(
                        targetParent.getCode(), targetParent.getCode(), targetParent, event.getToken());
                this.sendQEventAttributeValueChangeMessage(targetEvent);

            }
            VertxUtils.publish(getUser(), "events", JsonUtils.toJson(event));
            log.info(String.format("!!LINK CHANGE EVENT ->%s", event));
        } catch (Exception e) {
            log.error(String.format("Error in posting link Change to JMS:%s.", e.getLocalizedMessage()));
        }
    }

    @Override
    public void sendQEventSystemMessage(final String systemCode) {
        Properties properties = new Properties();
        try {
            properties.load(Thread.currentThread().getContextClassLoader().getResource("git.properties").openStream());
        } catch (IOException e) {
            log.error(String.format("IOException:%s when load file:git.properties", e.getMessage()));
            return;
        }
        sendQEventSystemMessage(systemCode, properties, securityService.getToken());
    }

    @Override
    public void sendQEventSystemMessage(final String systemCode, final String token) {
        Properties properties = new Properties();
        try {
            properties.load(Thread.currentThread().getContextClassLoader().getResource("git.properties").openStream());
        } catch (IOException e) {
            log.error(String.format("IOException:%s when load file:git.properties", e.getMessage()));
            return;
        }
        sendQEventSystemMessage(systemCode, properties, token);
    }

    @Override
    @javax.ejb.Asynchronous
    public void sendQEventSystemMessage(final String systemCode, final Properties properties, final String token) {
        // Send a vertx message broadcasting an link Change
        log.info(String.format("!!System EVENT ->%s for realm:%s.", systemCode, this.getCurrentRealm()));
        QEventSystemMessage event = new QEventSystemMessage(systemCode, properties, token);
        VertxUtils.publish(getUser(), "events", JsonUtils.toJson(event));
    }

    @Lock(LockType.READ)
    public Long findChildrenByAttributeLinkCount(@NotNull final String sourceCode, final String linkCode,
                                                 final MultivaluedMap<String, String> params) {

        return super.findChildrenByAttributeLinkCount(sourceCode, linkCode, params, null);
    }

    @Override
    @Lock(LockType.READ)
    public Long findChildrenByAttributeLinkCount(String sourceCode, String linkCode,
                                                 MultivaluedMap<String, String> params, final String stakeholderCode) {

        return super.findChildrenByAttributeLinkCount(sourceCode, linkCode, params, stakeholderCode);
    }

    @Override
    protected String getCurrentToken() {
        return securityService.getToken();
    }

    @Override
    public EntityManager getEntityManager() {
        return helper.getEntityManager();
    }

    @Override
    public BaseEntity getUser() {
        BaseEntity user = null;

        user = super.findBaseEntityByCode(securityService.getUserCode(), true);

        return user;
    }

    @Override
    public Long insert(final BaseEntity entity) {
        if (securityService.isAuthorised()) {
            return super.insert(entity);
        } else {
            throw new NotAuthorizedException("NotAuthorized!");
        }
    }

    @Override

    public EntityEntity addLink(final String sourceCode, final String targetCode, final String linkCode,
                                final Object value, final Double weight) {
        try {
            return super.addLink(sourceCode, targetCode, linkCode, value, weight);
        } catch (IllegalArgumentException | BadDataException e) {
            log.error(String.format("Exception:%s when addLink.", e.getMessage()));
        }
        return null;
    }

    @Override
    protected String getRealm() {

        String realm = null;
        try {
            realm = securityService.getRealm();
        } catch (Exception e) {
            return currentRealm;
        }
        if (realm == null)
            return currentRealm;
        else
            return realm;

    }


    @Override
    public Boolean inRole(final String role) {
        return securityService.inRole(role);
    }

    // This was created to avoid sending large volumes of cache saves when in dev
    // mode
    @Override
    public void writeToDDT(final List<BaseEntity> bes) {
        for (BaseEntity be : bes) {
            writeToDDT(be);
        }
    }

    @Override
    public void writeQuestionsToDDT(final List<Question> qs) {
        for (Question q : qs) {
            writeToDDT(q);
        }
    }

    @Override
    @javax.ejb.Asynchronous
    public void writeToDDT(final BaseEntity be) {
        String json = JsonUtils.toJson(be);
        VertxUtils.writeCachedJson(be.getRealm(), be.getCode(), json, getToken());

        if (be.getCode().startsWith("RUL_FRM_")) {
            // write themes and frames and ASKS ands MSG
            String priAsks = be.getValue("PRI_ASKS", "");
            if (!StringUtils.isBlank(priAsks)) {
                VertxUtils.writeCachedJson(be.getRealm(), be.getCode().substring("RUL_".length()) + "_ASKS", priAsks, getToken());
            }
            String priMsg = be.getValue("PRI_MSG", "");
            if (!StringUtils.isBlank(priMsg)) {
                VertxUtils.writeCachedJson(be.getRealm(), be.getCode().substring("RUL_".length()) + "_MSG", priMsg, getToken());
            }

        }
    }


    public void clearCache() {
        VertxUtils.clearCache(getRealm(), getToken());
    }

    @Override
    @javax.ejb.Asynchronous
    public void writeToDDT(final Question q) {
        String json = JsonUtils.toJson(q);
        VertxUtils.writeCachedJson(q.getRealm(), q.getCode(), json, getToken());
    }


    @Override
    @javax.ejb.Asynchronous
    public void writeToDDT(final String key, String jsonValue) {

        VertxUtils.writeCachedJson(this.getRealm(), key, jsonValue, getToken());

    }

    @Override
    @javax.ejb.Asynchronous
    public void updateDDT(final String key, final String value) {
        writeToDDT(key, value);
    }

    @Override
    public String readFromDDT(final String key) {
        return VertxUtils.readCachedJson(this.getRealm(), key, getToken()).toString();
    }

    @Override
    @javax.ejb.Asynchronous
    public void pushAttributes() {
        if (!SecurityService.importMode) {
            pushAttributesAsync();
        }
    }

    @javax.ejb.Asynchronous
    public void pushAttributesAsync() {
        // Attributes
        final List<Attribute> entitys = findAttributes();
        Attribute[] atArr = new Attribute[entitys.size()];
        atArr = entitys.toArray(atArr);
        QDataAttributeMessage msg = new QDataAttributeMessage(atArr);
        msg.setToken(securityService.getToken());
        String json = JsonUtils.toJson(msg);
        writeToDDT("attributes", json);

    }

    public String getServiceToken(String realm) {
        return serviceTokens.getServiceToken(realm);
    }

    public String getKeycloakUrl(String realm) {
        String keycloakurl = null;

        boolean devMode = GennySettings.devMode;
        if (devMode) { // UGLY!!!
            realm = "genny";
        }
        if (SecureResources.getKeycloakJsonMap().isEmpty()) {
            SecureResources.reload();
        }
        String keycloakJson = SecureResources.getKeycloakJsonMap().get(realm + ".json");
        if (keycloakJson != null) {
            JsonObject realmJson = new JsonObject(keycloakJson);
            JsonObject secretJson = realmJson.getJsonObject("credentials");
            String secret = secretJson.getString("secret");
            log.info(String.format("secret:%s", secret));
            keycloakurl = realmJson.getString("auth-server-url").substring(0,
                    realmJson.getString("auth-server-url").length() - "/auth".length());
        }

        return keycloakurl;
    }

    /**
     * @return the currentRealm
     */
    public String getCurrentRealm() {
        return currentRealm;
    }

    /**
     * @param currentRealm the currentRealm to set
     */
    public void setCurrentRealm(String currentRealm) {
        this.currentRealm = currentRealm;
    }

    @Override
    public Long insert(Answer[] answers) {
        if (securityService.isAuthorised()) {
            String realm = getRealm();
            for (Answer answer : answers) {
                answer.setRealm(realm); // always override
            }
            return super.insert(answers);
        }
        return -1L;
    }

    @Override
    public List<Validation> queryValidation(@NotNull String realm) {
        return Collections.emptyList();
    }

    @Override
    public List<Attribute> queryAttributes(@NotNull String realm) {
        return Collections.emptyList();
    }

    @Override
    public List<BaseEntity> queryBaseEntitys(@NotNull String realm) {
        return Collections.emptyList();
    }

    @Override
    public List<EntityAttribute> queryEntityAttribute(@NotNull String realm) {
        return Collections.emptyList();
    }

    @Override
    public List<EntityEntity> queryEntityEntity(@NotNull String realm) {
        return Collections.emptyList();
    }

    @Override
    public List<Question> queryQuestion(@NotNull String realm) {
        return Collections.emptyList();
    }

    @Override
    public List<QuestionQuestion> queryQuestionQuestion(@NotNull String realm) {
        return Collections.emptyList();
    }

    @Override
    public List<Ask> queryAsk(@NotNull String realm) {
        return Collections.emptyList();
    }

    @Override
    public List<QBaseMSGMessageTemplate> queryNotification(@NotNull String realm) {
        return Collections.emptyList();
    }

    @Override
    public List<QBaseMSGMessageTemplate> queryMessage(@NotNull String realm) {
        return Collections.emptyList();
    }

    @Override
    public void insertValidations(ArrayList<Validation> validationList) {

    }

    @Override
    public void insertAttributes(ArrayList<Attribute> attributeList) {

    }

    @Override
    public void insertEntityAttribute(ArrayList<EntityAttribute> entityAttributeList) {

    }

    @Override
    public void insertBaseEntitys(ArrayList<BaseEntity> baseEntityList) {

    }

    @Override
    public void insertEntityEntitys(ArrayList<EntityEntity> entityEntityist) {

    }

    @Override
    public void insertAttributeLinks(ArrayList<AttributeLink> attributeLinkList) {

    }

    @Override
    public void insertQuestions(ArrayList<Question> questionList) {

    }

    @Override
    public void insertQuestionQuestions(ArrayList<QuestionQuestion> questionQuestionList) {

    }

    @Override
    public void insertAsks(ArrayList<Ask> askList) {

    }

    @Override
    public void inserTemplate(ArrayList<QBaseMSGMessageTemplate> messageList) {

    }
}
