package life.genny.qwanda.service;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.InvocationTargetException;
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
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.core.MultivaluedMap;

import life.genny.qwanda.*;
import life.genny.qwanda.attribute.AttributeLink;
import life.genny.qwanda.attribute.EntityAttribute;
import life.genny.qwanda.message.*;
import life.genny.qwanda.validation.Validation;
import life.genny.services.BeanNotNullFields;
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
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.jpa.QueryHints;

@RequestScoped

public class Service extends BaseEntityService2 implements QwandaRepository {
    private static final int BATCHSIZE = 500;

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
      //  VertxUtils.publish(getUser(), "events", JsonUtils.toJson(event));
        VertxUtils.writeMsg("events", JsonUtils.toJson(event));
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
            VertxUtils.writeMsg("events", JsonUtils.toJson(event));
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
    public <T> List<T> queryTableByRealm(String tableName, String realm) {
        List<T> result = Collections.emptyList();
        try {
            Query query = getEntityManager().createQuery(String.format("SELECT temp FROM %s temp where temp.realm=:realmStr", tableName));
            query.setParameter("realmStr", realm);
            result = query.getResultList();
        } catch (Exception e) {
            log.error(String.format("Query table %s Error:%s".format(realm, e.getMessage())));
        }
        return result;
    }

    @Override
    public void bulkUpdate(ArrayList<CodedEntity> objectList, HashMap<String, CodedEntity> mapping) {
        if (objectList.isEmpty()) return;

        BeanNotNullFields copyFields = new BeanNotNullFields();
        for (CodedEntity t : objectList) {
            if (t instanceof QBaseMSGMessageTemplate) {
                QBaseMSGMessageTemplate msg = (QBaseMSGMessageTemplate) mapping.get(t.getCode());
                msg.setName(t.getName());
                msg.setDescription(((QBaseMSGMessageTemplate) t).getDescription());
                msg.setEmail_templateId(((QBaseMSGMessageTemplate) t).getEmail_templateId());
                msg.setSms_template(((QBaseMSGMessageTemplate) t).getSms_template());
                msg.setSubject(((QBaseMSGMessageTemplate) t).getSubject());
                msg.setToast_template(((QBaseMSGMessageTemplate) t).getToast_template());
                getEntityManager().merge(msg);
            } else {
                CodedEntity val = mapping.get(t.getCode());
                if (val == null) {
                    // Should never raise this exception
                    throw new NoResultException(String.format("Can't find %s from database.", t.getCode()));
                }
                try {
                    copyFields.copyProperties(val, t);
                } catch (IllegalAccessException | InvocationTargetException ex) {
                    log.error(String.format("Failed to copy Properties for %s", val.getCode()));
                }

                val.setRealm(getRealm());
                getEntityManager().merge(val);
            }
        }
    }

    private void saveToDDT(BaseEntity baseEntity) {
        String realm = getRealm();
        assert (realm.equals(baseEntity.getRealm()));
        String code = baseEntity.getCode();
        baseEntity.setRealm(realm);
        try {
            String json = JsonUtils.toJson(baseEntity);
            writeToDDT(baseEntity.getCode(), json);
        } catch (javax.validation.ConstraintViolationException e) {
            log.error("Cannot save BaseEntity with code " + code + "," + e.getLocalizedMessage());
        } catch (final ConstraintViolationException e) {
            log.error("Entity Already exists - cannot insert" + code);
        }
    }

    @Override
    public void bulkInsert(ArrayList<CodedEntity> objectList) {
        if (objectList.isEmpty()) return;

        EntityManager em = getEntityManager();
        int index = 1;

        for (CodedEntity t : objectList) {
            em.persist(t);
            if (index % BATCHSIZE == 0) {
                //flush a batch of inserts and release memory:
                log.debug("BaseEntity Batch is full, flush to database.");
                em.flush();
            }
            index += 1;
        }
        em.flush();
    }

    @Override
    public void bulkInsertAsk(ArrayList<Ask> objectList) {

    }

    @Override
    public void bulkUpdateAsk(ArrayList<Ask> objectList, HashMap<String, Ask> mapping) {

    }

    @Override
    public void bulkInsertQuestionQuestion(ArrayList<QuestionQuestion> objectList) {
        if (objectList.isEmpty()) return;

        EntityManager entityManager = getEntityManager();
        int index = 1;

        for (QuestionQuestion t : objectList) {
            entityManager.persist(t);
            if (index % BATCHSIZE == 0) {
                //flush a batch of inserts and release memory:
                log.debug("BaseEntity Batch is full, flush to database.");
                entityManager.flush();
            }
            index += 1;
        }
        entityManager.flush();
    }

    @Override
    public void bulkUpdateQuestionQuestion(ArrayList<QuestionQuestion> objectList, HashMap<String, QuestionQuestion> mapping) {
        for (QuestionQuestion qq : objectList) {
            String uniqCode = qq.getSourceCode() + "-" + qq.getTarketCode();
            QuestionQuestion existing = mapping.get(uniqCode.toUpperCase());
            existing.setMandatory(qq.getMandatory());
            existing.setWeight(qq.getWeight());
            existing.setReadonly(qq.getReadonly());
            existing.setDependency(qq.getDependency());
            getEntityManager().merge(existing);
        }
    }

    @Override
    public void cleanAsk(String realm) {
        String qlString= String.format("delete from ask where realm = '%s'", realm);
        Query query = getEntityManager().createQuery(qlString);
        query.executeUpdate();
    }

    @Override
    public void cleanFrameFromBaseentityAttribute(String realm) {
        String qlString= "delete from baseentity_attribute " +
                "where baseEntityCode like \'RUL_FRM%_GRP\' " +
                "and attributeCode = \'PRI_ASKS\' " +
                "and realm = \'" + realm + "\'";
        Query query = getEntityManager().createQuery(qlString);
        query.executeUpdate();
    }
}
