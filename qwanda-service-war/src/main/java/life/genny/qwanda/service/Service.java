package life.genny.qwanda.service;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import javax.annotation.PostConstruct;
import javax.ejb.Asynchronous;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;

import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.NoResultException;
import javax.persistence.Persistence;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaDelete;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Root;
import javax.transaction.Transactional;

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

import org.drools.core.impl.EnvironmentFactory;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.RevisionSyntaxException;
import org.jboss.resteasy.specimpl.MultivaluedMapImpl;

import io.vertx.core.json.JsonObject;
import life.genny.qwanda.attribute.Attribute;
import life.genny.qwanda.attribute.EntityAttribute;
import life.genny.qwanda.entity.BaseEntity;
import life.genny.qwanda.entity.EntityEntity;
import life.genny.qwanda.exception.BadDataException;
import life.genny.qwanda.util.PersistenceHelper;
import life.genny.qwandautils.GennySettings;
import life.genny.qwandautils.JsonUtils;
import life.genny.security.SecureResources;
import life.genny.services.BaseEntityService2;
import life.genny.utils.RulesUtils;
import life.genny.utils.VertxUtils;
import life.genny.bootxport.bootx.QwandaRepository;


import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;

import org.json.JSONException;
import org.json.JSONObject;
import org.kie.api.runtime.EnvironmentName;

import life.genny.bootxport.bootx.QwandaRepository;
import life.genny.models.GennyToken;

import javax.persistence.Query;




import org.apache.commons.lang3.StringUtils;
import org.hibernate.exception.ConstraintViolationException;

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

		}
		sendQEventSystemMessage(systemCode, properties, token);
	}

	@Override
	@javax.ejb.Asynchronous
	public void sendQEventSystemMessage(final String systemCode, final Properties properties, final String token) {
		// Send a vertx message broadcasting an link Change
		log.info("!!System EVENT ->" + systemCode+" for realm "+this.getCurrentRealm());

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
		
 		user =  super.findBaseEntityByCode(securityService.getUserCode(), true);

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
			String PRI_ASKS = be.getValue("PRI_ASKS","");
			if (!StringUtils.isBlank(PRI_ASKS)) {
				VertxUtils.writeCachedJson(be.getRealm(),be.getCode().substring("RUL_".length()) + "_ASKS", PRI_ASKS, getToken());
			}
			String PRI_MSG = be.getValue("PRI_MSG","");
			if (!StringUtils.isBlank(PRI_MSG)) {
				VertxUtils.writeCachedJson(be.getRealm(),be.getCode().substring("RUL_".length()) + "_MSG", PRI_MSG, getToken());
			}
			String PRI_POJO = be.getValue("PRI_POJO","");
			if (!StringUtils.isBlank(PRI_POJO)) {
				VertxUtils.writeCachedJson(be.getRealm(),be.getCode().substring("RUL_".length()), PRI_POJO, getToken());
			}

		} else 	if (be.getCode().startsWith("RUL_THM_")) {
			// write themes and frames and ASKS ands MSG
			String PRI_POJO = be.getValue("PRI_POJO","");
			if (!StringUtils.isBlank(PRI_POJO)) {
				VertxUtils.writeCachedJson(be.getRealm(),be.getCode().substring("RUL_".length()), PRI_POJO, getToken());
			}

		}
	}
	
	public Integer pushBEsToCache(final String realm) {
		CriteriaBuilder builder = this.helper.getEntityManager().getCriteriaBuilder();

		CriteriaQuery<BaseEntity> query = builder.createQuery(BaseEntity.class);
		Root<BaseEntity> be = query.from(BaseEntity.class);
		Join<BaseEntity, EntityAttribute> ea = (Join) be.fetch("baseEntityAttributes");
		query.select(be);
		query.distinct(true);
		query.where(builder.equal(ea.get("realm"), realm));

		List<BaseEntity> results = this.helper.getEntityManager().createQuery(query).getResultList();

		log.info("Pushing " + realm + " : " + results.size() + " Basentitys to Cache");
		writeToDDT(results);
		log.info("Pushed " + realm + " : " + results.size() + " Basentitys to Cache");
		return results.size();
	}
	
	public Integer pushRulesToCache(final String realm) {
		CriteriaBuilder cb = this.helper.getEntityManager().getCriteriaBuilder();

		CriteriaQuery<BaseEntity> query = cb.createQuery(BaseEntity.class);
		Root<BaseEntity> be = query.from(BaseEntity.class);
		Join<BaseEntity, EntityAttribute> ea = (Join) be.fetch("baseEntityAttributes");
		query.select(be);
		query.distinct(true);
		query.where(cb.equal(ea.get("realm"), realm),cb.like(be.get("code"), "RUL_%"));

		List<BaseEntity> results = this.helper.getEntityManager().createQuery(query).getResultList();

		log.info("Pushing " + realm + " : " + results.size() + " Baseentity Rules to Cache");
		writeToDDT(results);
		log.info("Pushed " + realm + " : " + results.size() + " Baseentity Rules to Cache");
		return results.size();
	}
	
	
	public Integer pushQuestionsToCache(final String realm) {
		CriteriaBuilder cb = this.helper.getEntityManager().getCriteriaBuilder();

		final List<Question> results = this.helper.getEntityManager()
				.createQuery("SELECT a FROM Question a where a.realm=:realmStr").setParameter("realmStr", realm)
				.getResultList();


		log.info("Pushing " + realm + " : " + results.size() + " Questions to Cache");
		writeQuestionsToDDT(results);
		log.info("Pushed " + realm + " : " + results.size() + " Questions to Cache");
		return results.size();
	}
	
	
	public void clearCache() {
		VertxUtils.clearCache(getRealm(),getToken());
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
	
	//@Asynchronous
	//@Transactional
	public void loadRulesFromGit(final String realm, List<String> gitProjectUrlList, final String gitUsername, final String gitPassword, final String gitBranch,GennyToken userToken)
	{
		boolean recursive = true;
		Map<String,BaseEntity> ruleBes = new HashMap<>();
		
		log.info(String.format("Loading Rules from GitHub, Realm:%s, GitHub UserName:%s", realm, gitUsername));
		log.info(String.format("Loading Rules from Git Locations : %s, Branch: %s", gitProjectUrlList, gitBranch));

		if (StringUtils.isBlank(gitPassword)) {
			log.error("No GIT ACCOUNT SET!");
			return;
		}
		
		try {
			for (String gitProjectUrl : gitProjectUrlList) {
				ruleBes.putAll(RulesUtils.getRulesFromGit(gitProjectUrl, gitBranch, realm, gitUsername, gitPassword, recursive,userToken));
			}
		} catch (RevisionSyntaxException | BadDataException | GitAPIException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
//		EntityManagerFactory emf = null;
//		
//		try {
//			emf = Persistence.createEntityManagerFactory("qwanda-service-war");
//		} catch (Exception e) {
//			log.warn("No persistence enabled, are you running wildfly-qwanda-service?");
//		}

		EntityManager em = helper.getEntityManager(); //emf.createEntityManager();
		
		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaDelete<EntityAttribute> criteriaDelete = cb.createCriteriaDelete(EntityAttribute.class);

		    Root<EntityAttribute> root = criteriaDelete.from(EntityAttribute.class);
		    criteriaDelete.where(cb.like(root.get("baseEntityCode"), "RUL_%"),
					cb.equal(root.get("realm"), realm));
		    em.createQuery(criteriaDelete).executeUpdate();
		    
			CriteriaDelete<BaseEntity> criteriaDeleteBE = cb.createCriteriaDelete(BaseEntity.class);

		    Root<BaseEntity> rootBE = criteriaDeleteBE.from(BaseEntity.class);
		    criteriaDeleteBE.where(cb.like(rootBE.get("code"), "RUL_%"),
					cb.equal(rootBE.get("realm"), realm));
		    em.createQuery(criteriaDeleteBE).executeUpdate();
		    log.info("All rules deleted");

		    
		    CriteriaQuery<Attribute> query = cb.createQuery(Attribute.class);
		Root<Attribute> rootAttribute = query.from(Attribute.class);

		query = query.select(rootAttribute).where(cb.equal(rootAttribute.get("realm"), realm));

		List<Attribute> attributes = new ArrayList<>();
		try {
			attributes = em.createQuery(query).getResultList();
		} catch (NoResultException nre) {

		}

		// Ugly
		Map<String,Attribute> attributeMap = new ConcurrentHashMap<>();
		for (Attribute attribute : attributes) {
			attributeMap.put(attribute.getCode(),attribute);
		}
		
//		EntityTransaction tx = em.getTransaction();
//		tx.begin();
		// Now write the rules to the database
		for (String ruleBe : ruleBes.keySet()) {
//			if (existingBeMap.keySet().contains(ruleBe)) {
//				// Update the be
//				BaseEntity existingBe = existingBeMap.get(ruleBe);	
//				BaseEntity newBe = ruleBes.get(ruleBe);
//				orphanedBes.remove(existingBe);
//				Integer hashCode = existingBe.getValue("PRI_HASHCODE",0);
//				String newRuleContent = newBe.getValue("PRI_KIE_TEXT", "");
//				Integer newHashCode = newRuleContent.hashCode();
//				String existingContent = existingBe.getValueAsString("PRI_KIE_TEXT");
//				if (!newRuleContent.equals(existingContent)) {  //(newHashCode != hashCode) {
//					existingBe.merge(newBe);
//					try {
//						existingBe.setValue(RulesUtils.getAttribute("PRI_HASHCODE", userToken.getToken()), newRuleContent.hashCode());
////						existingBe.setValue(RulesUtils.getAttribute("PRI_MSG", userToken.getToken()), "");
////						existingBe.setValue(RulesUtils.getAttribute("PRI_ASKS", userToken.getToken()), "");
////						existingBe.setValue(RulesUtils.getAttribute("PRI_FRM", userToken.getToken()), "");
//
//					} catch (BadDataException e) {
//						// TODO Auto-generated catch block
//						e.printStackTrace();
//					}
//					
//					em.merge(existingBe);
//				}
//				
//			//TODO: clear the MSG and ASKS attribute
//				
//				
//			} else {
				BaseEntity rbe = ruleBes.get(ruleBe);
				em.persist(rbe);
				for (EntityAttribute ea : rbe.getBaseEntityAttributes()) {
					ea.pk.setBaseEntity(rbe);
					ea.setRealm(realm);
					Attribute eaAttribute =attributeMap.get(ea.getAttributeCode());
					if (eaAttribute!=null) {
						ea.pk.setAttribute(eaAttribute);
						em.persist(ea);
					} else {
						log.error("Attribute does not exist! "+ea.getAttributeCode());
						Attribute newAttribute = ea.pk.getAttribute();
						em.persist(newAttribute);
						ea.pk.setAttribute(newAttribute);
						em.persist(ea);
						attributeMap.put(ea.getAttributeCode(), newAttribute);
					}
				}
//			}
		}
		
		// Noe delete the old ones
//		for (BaseEntity orphanAnnie : orphanedBes)
//		{
//			em.remove(orphanAnnie);
//		}
//		
	//	tx.commit();
		//em.close();
		log.info("All rules saved");
		pushRulesToCache(realm) ;
		
		return;
	}

	/*
		return True if rules exist in database, else False
	 */
	public boolean doRulesExistInDatabase(final String realm) {
			CriteriaBuilder cb = this.helper.getEntityManager().getCriteriaBuilder();
			CriteriaQuery<BaseEntity> query = cb.createQuery(BaseEntity.class);
			Root<BaseEntity> be = query.from(BaseEntity.class);
			Join<BaseEntity, EntityAttribute> ea = (Join) be.fetch("baseEntityAttributes");
			query.select(be);
			query.distinct(true);
			query.where(cb.equal(ea.get("realm"), realm),cb.like(be.get("code"), "RUL_%"));

			List<BaseEntity> results = this.helper.getEntityManager().createQuery(query).getResultList();

			return !results.isEmpty();
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
            getEntityManager().merge(existing);
        }
    }
}
