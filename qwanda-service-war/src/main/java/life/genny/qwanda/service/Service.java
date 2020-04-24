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
import javax.persistence.EntityTransaction;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import javax.validation.constraints.NotNull;
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

import java.util.List;

import life.genny.bootxport.bootx.QwandaRepository;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.exception.ConstraintViolationException;

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
        log.info("!!ATTRIBUTE CHANGE EVENT ->" + event.getBe().getCode());

        VertxUtils.publish(getUser(), "events", JsonUtils.toJson(event));

    }

    @Override
    @javax.ejb.Asynchronous
    public void sendQEventLinkChangeMessage(final QEventLinkChangeMessage event) {
        // Send a vertx message broadcasting an link Change
        log.info("!!LINK CHANGE EVENT ->" + event);

        BaseEntity originalParent = null;
        BaseEntity targetParent = null;
        try {

            // update cache for source and target
            if (event.getOldLink() != null) {
                if (event.getOldLink().getSourceCode() != null) {
                    String originalParentCode = event.getOldLink().getSourceCode();
                    originalParent = this.findBaseEntityByCode(originalParentCode);
                    updateDDT(originalParent.getCode(), JsonUtils.toJson(originalParent));
                    QEventAttributeValueChangeMessage parentEvent = new QEventAttributeValueChangeMessage(
                            originalParent.getCode(), originalParent.getCode(), originalParent, event.getToken());
                    this.sendQEventAttributeValueChangeMessage(parentEvent);
                }
            }

            if (event.getLink() != null) {
                if (event.getLink().getSourceCode() != null) {
                    String targetParentCode = event.getLink().getSourceCode();
                    targetParent = this.findBaseEntityByCode(targetParentCode);
                    updateDDT(targetParent.getCode(), JsonUtils.toJson(targetParent));
                    QEventAttributeValueChangeMessage targetEvent = new QEventAttributeValueChangeMessage(
                            targetParent.getCode(), targetParent.getCode(), targetParent, event.getToken());
                    this.sendQEventAttributeValueChangeMessage(targetEvent);

                }
            }

//			String json = JsonUtils.toJson(event);
//			QwandaUtils.apiPostEntity(bridgeApi, json, event.getToken());
            VertxUtils.publish(getUser(), "events", JsonUtils.toJson(event));
        } catch (Exception e) {
            log.error("Error in posting link Change to JMS:" + e.getLocalizedMessage());
        }

    }

    @Override
    public void sendQEventSystemMessage(final String systemCode) {
        Properties properties = new Properties();
        try {
            properties.load(Thread.currentThread().getContextClassLoader().getResource("git.properties").openStream());
        } catch (IOException e) {

        }

        sendQEventSystemMessage(systemCode, properties, securityService.getToken());
    }

    @Override
    public void sendQEventSystemMessage(final String systemCode, final String token) {
        Properties properties = new Properties();
        try {
            properties.load(Thread.currentThread().getContextClassLoader().getResource("git.properties").openStream());
        } catch (IOException e) {

        }
        sendQEventSystemMessage(systemCode, properties, token);
    }

    @Override
    @javax.ejb.Asynchronous
    public void sendQEventSystemMessage(final String systemCode, final Properties properties, final String token) {
        // Send a vertx message broadcasting an link Change
        log.info("!!System EVENT ->" + systemCode + " for realm " + this.getCurrentRealm());

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
        String token = securityService.getToken();
        return token;
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
        }

        return null; // TODO throw Exception
    }

    @Override

    public EntityEntity addLink(final String sourceCode, final String targetCode, final String linkCode,
                                final Object value, final Double weight) {
        try {
            return super.addLink(sourceCode, targetCode, linkCode, value, weight);
        } catch (IllegalArgumentException | BadDataException e) {
            e.printStackTrace();
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
    public Long update(final BaseEntity baseEntity) {
        return super.update(baseEntity);
    }

    @Override
    public BaseEntity upsert(final BaseEntity baseEntity) {
        return super.upsert(baseEntity);
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
            String PRI_ASKS = be.getValue("PRI_ASKS", "");
            if (!StringUtils.isBlank(PRI_ASKS)) {
                VertxUtils.writeCachedJson(be.getRealm(), be.getCode().substring("RUL_".length()) + "_ASKS", PRI_ASKS, getToken());
            }
            String PRI_MSG = be.getValue("PRI_MSG", "");
            if (!StringUtils.isBlank(PRI_MSG)) {
                VertxUtils.writeCachedJson(be.getRealm(), be.getCode().substring("RUL_".length()) + "_MSG", PRI_MSG, getToken());
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

        if (GennySettings.devMode) { // UGLY!!!
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
            log.info("secret:" + secret);

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
    public Long insert(Answer[] answers) throws IllegalArgumentException {
        if (securityService.isAuthorised()) {
            String realm = getRealm();
            for (Answer answer : answers) {
                answer.setRealm(realm); // always override
            }
            return super.insert(answers);
        }
        return -1L;
    }

    public List<Validation> queryValidation(String realm) throws NoResultException {
        return super.queryValidation(realm);
    }

    public List<Attribute> queryAttributes(String realm) {
        return super.queryAttributes(realm);
    }

    public List<BaseEntity> queryBaseEntitys(String realm) {
        return super.queryBaseEntitys(realm);
    }

    public List<EntityAttribute> queryEntityAttribute(String realm) {
        return super.queryEntityAttribute(realm);
    }

    public List<EntityEntity> queryEntityEntity(String realm) {
        return super.queryEntityEntity(realm);
    }

    public List<Question> queryQuestion(String realm) {
        return super.queryQuestion(realm);
    }

    public List<QuestionQuestion> queryQuestionQuestion(String realm) {
        return super.queryQuestionQuestion(realm);
    }

    public List<Ask> queryAsk(String realm) {
        return super.queryAsk(realm);
    }

    public List<QBaseMSGMessageTemplate> queryNotification(String realm) {
        return super.queryNotification(realm);
    }

    public List<QBaseMSGMessageTemplate> queryMessage(String realm) {
        return super.queryMessage(realm);
    }

    public void insertValidations(ArrayList<Validation> validationList) {
        super.insertValidations(validationList);
    }

    public void insertAttributes(ArrayList<Attribute> attributeList) {
        super.insertAttributes(attributeList);
    }

    public void insertEntityAttribute(ArrayList<EntityAttribute> entityAttributeList) {
        super.insertEntityAttribute(entityAttributeList);
    }


    public void insertBaseEntitys(ArrayList<BaseEntity> baseEntityList) {
        super.insertBaseEntitys(baseEntityList);
    }

    public void insertEntityEntitys(ArrayList<EntityEntity> entityEntityList) {
        super.insertEntityEntitys(entityEntityList);
    }

    public void insertAttributeLinks(ArrayList<AttributeLink> attributeLinkList) {
        super.insertAttributeLinks(attributeLinkList);
    }

    public void insertQuestions(ArrayList<Question> questionList) {
        super.insertQuestions(questionList);
    }

    public void insertQuestionQuestions(ArrayList<QuestionQuestion> questionQuestionList) {
        super.insertQuestionQuestions(questionQuestionList);
    }

    public void insertAsks(ArrayList<Ask> askList) {
        super.insertAsks(askList);
    }

    public void inserTemplate(ArrayList<QBaseMSGMessageTemplate> messageList) {
        super.inserTemplate(messageList);
    }
}
