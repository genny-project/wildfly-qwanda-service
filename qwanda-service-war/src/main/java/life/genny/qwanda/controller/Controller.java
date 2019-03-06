package life.genny.qwanda.controller;

import java.lang.invoke.MethodHandles;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TimeZone;
import java.util.List;
import java.util.Map.Entry;
import javax.persistence.NoResultException;
import javax.transaction.Transactional;

import org.apache.logging.log4j.Logger;
import life.genny.qwandautils.JsonUtils;

import life.genny.qwanda.Answer;
import life.genny.qwanda.Question;
import life.genny.qwanda.attribute.Attribute;
import life.genny.qwanda.attribute.AttributeLink;
import life.genny.qwanda.attribute.AttributeText;
import life.genny.qwanda.entity.BaseEntity;
import life.genny.qwanda.message.QBaseMSGMessageTemplate;
import life.genny.qwanda.service.Service;
import life.genny.qwanda.validation.Validation;
import life.genny.services.BatchLoading;
import life.genny.qwanda.service.Service;

import life.genny.qwanda.exception.BadDataException;

public class Controller {
	  protected static final Logger log = org.apache.logging.log4j.LogManager
		      .getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());

  public static final String REALM_HIDDEN = "hidden";

  public void updateBaseEntity(
      final Service service, final Map<String, Map<String, Object>> merge) {
    merge
    .get("baseEntitys")
    .entrySet()
    .stream()
    .map(Entry::getValue)
    .forEach(
        record -> {
          final Map<String, Object> item = (HashMap<String, Object>) record;
          final String code = (String) item.get("code");
          BaseEntity be = null;

          try {
            be = service.findBaseEntityByCode(code);
            be.setCode(code);
            be.setRealm(REALM_HIDDEN);
            service.updateRealm(be);
          } catch (final NoResultException e) {
            e.printStackTrace();
          }
          log.info("This has been updated: " + be);
        });
  }

  public void updateAttributes(
      final Service service, final Map<String, Map<String, Object>> merge) {
    merge
    .get("attributes")
    .entrySet()
    .stream()
    .map(Entry::getValue)
    .forEach(
        record -> {
          final Map<String, Object> item = (HashMap<String, Object>) record;
          final String code = (String) item.get("code");
          item.get("name");
          item.get("dataType");
          Attribute attr = null;
          try {
            attr = service.findAttributeByCode(code);
            attr.setCode(code);
            attr.setRealm(REALM_HIDDEN);
            service.updateRealm(attr);
            log.info("This has been updated: " + attr);
          } catch (final NoResultException e) {
            e.printStackTrace();
          }
        });
  }

  public void updateAttributeLink(
      final Service service, final Map<String, Map<String, Object>> merge) {
    merge
    .get("attributeLink")
    .entrySet()
    .stream()
    .map(Entry::getValue)
    .forEach(
        record -> {
          final Map<String, Object> item = (HashMap<String, Object>) record;
          final String code = (String) item.get("code");
          AttributeLink attrLink = null;
          try {
            attrLink = service.findAttributeLinkByCode(code);
            attrLink.setCode(code);
            attrLink.setRealm(REALM_HIDDEN);
            service.updateRealm(attrLink);
            log.info("This has been updated: " + attrLink);
          } catch (final NoResultException e) {
            e.printStackTrace();
          }
        });
  }

  public void updateEntityAttribute(
      final Service service, final Map<String, Map<String, Object>> merge) {
    merge
    .get("attibutesEntity")
    .entrySet()
    .stream()
    .map(Entry::getValue)
    .forEach(
        record -> {
          final Map<String, Object> item = (HashMap<String, Object>) record;
          final String attributeCode = (String) item.get("attributeCode");
          final String baseEntityCode = (String) item.get("baseEntityCode");
          try {
            service.removeEntityAttribute(baseEntityCode, attributeCode);
            log.info("Successfully removed entity attributes");
          } catch (final NoResultException e) {
            e.printStackTrace();
          }
        });
  }

  public void updateEntityEntity(
      final Service service, final Map<String, Map<String, Object>> merge) {
    merge
    .get("basebase")
    .entrySet()
    .stream()
    .map(Entry::getValue)
    .forEach(
        record -> {
          final Map<String, Object> item = (HashMap<String, Object>) record;
          final String linkCode = (String) item.get("linkCode");
          final String parentCode = (String) item.get("parentCode");
          final String targetCode = (String) item.get("targetCode");
          try {
            service.removeLink(parentCode, targetCode, linkCode);
            log.info("Successfully removed entity entities");
          } catch (final Exception e) {
            e.printStackTrace();
          }
        });
  }

  public void updateValidation(
      final Service service, final Map<String, Map<String, Object>> merge) {
    merge
    .get("validations")
    .entrySet()
    .stream()
    .map(Entry::getValue)
    .forEach(
        record -> {
          final Map<String, Object> item = (HashMap<String, Object>) record;
          final String code = (String) item.get("code");

          Validation val = null;

          try {
            val = service.findValidationByCode(code);
            val.setCode(code);
            val.setRealm(REALM_HIDDEN);
            service.updateRealm(val);
          } catch (final NoResultException e) {
            e.printStackTrace();
          }
        });
  }

  public void updateQuestion(final Service service, final Map<String, Map<String, Object>> merge) {
    merge
    .get("questions")
    .entrySet()
    .stream()
    .map(Entry::getValue)
    .forEach(
        record -> {
          final Map<String, Object> item = (HashMap<String, Object>) record;
          final String code = (String) item.get("code");
          Question que = null;

          try {
            que = service.findQuestionByCode(code);
            que.setCode(code);
            que.setRealm(REALM_HIDDEN);

            service.updateRealm(que);
          } catch (final NoResultException e) {
            e.printStackTrace();
          }
        });
  }

  public void updateQuestionQuestion(
      final Service service, final Map<String, Map<String, Object>> merge) {
    merge
    .get("questionQuestions")
    .entrySet()
    .stream()
    .map(Entry::getValue)
    .forEach(
        record -> {
          final Map<String, Object> item = (HashMap<String, Object>) record;
          final String parentCode = (String) item.get("parentCode");
          final String targetCode = (String) item.get("targetCode");

          try {
            service.removeQuestionQuestion(parentCode, targetCode);
            log.info("Successfully removed question questions");
          } catch (final NoResultException e) {
            e.printStackTrace();
          }
        });
  }

  public void updateMessage(final Service service, final Map<String, Map<String, Object>> merge) {
    merge
    .get("messages")
    .entrySet()
    .stream()
    .map(Entry::getValue)
    .forEach(
        record -> {
          final Map<String, Object> template = (HashMap<String, Object>) record;
          final String code = (String) template.get("code");
          QBaseMSGMessageTemplate templateObj;
          try {
            templateObj = service.findTemplateByCode(code);
            templateObj.setCode(code);
            templateObj.setRealm(REALM_HIDDEN);
            service.updateRealm(templateObj);
          } catch (final NoResultException e) {
            e.printStackTrace();
          }
        });
  }

  public Map<String, Map<String, Object>> retrieveDeletionRecords(final Service bes, final String table, final String projectKey ) {
    final BatchLoading bl = new BatchLoading(bes);
    final Map<String, Object> saveProjectData = new HashMap(bl.savedProjectData);
    Map<String, Object> sheetMap;
    Map<String, Object> merge = new HashMap<>();
    final Map<String, Map<String, Object>> superMerge = new HashMap<>();
    
    sheetMap = bl.persistProject(true, table.toLowerCase(), true); // contains records from sheets
    final Iterator iter =
        ((HashMap<String, HashMap>) saveProjectData.get(projectKey)).entrySet().iterator();

    while (iter.hasNext()) {
      final Entry entry = (Entry) iter.next();
      final String key = (String) entry.getKey();
      final HashMap<String, HashMap> newMap = (HashMap<String, HashMap>) sheetMap.get(projectKey);
      if (newMap != null && !newMap.containsKey(key)) {
        log.info("key == " + entry.getKey() + " value == " + entry.getValue());
        merge.put(entry.getKey().toString(), entry.getValue());
      }
    }
    superMerge.put(projectKey, merge);
    log.info("Things to delete: " + merge);
    saveProjectData.put(projectKey, sheetMap.get(projectKey));
    return superMerge;
  }

  @Transactional
  public void synchronizeSheetsToDataBase(final Service bes, String table) {
    final BatchLoading bl = new BatchLoading(bes);
    Map<String, Map<String, Object>> superMerge;
    table = table.toLowerCase();
    switch(table) {
      case "baseentity":
        superMerge = retrieveDeletionRecords(bes, table, "baseEntitys");
        if(!superMerge.isEmpty()) {
          updateBaseEntity(bes, superMerge);
        }
        break;
      
      case "attributelink":
        superMerge = retrieveDeletionRecords(bes, table, "attributeLink");
        if(!superMerge.isEmpty()) {
          updateAttributeLink(bes, superMerge);
        }
        break;
      
      case "entityattribute":
        superMerge = retrieveDeletionRecords(bes, table, "attibutesEntity");
        if(!superMerge.isEmpty()) {
          updateEntityAttribute(bes, superMerge);
        }
        break;
        
      case "attribute":
        superMerge = retrieveDeletionRecords(bes, table, "attributes");
        if(!superMerge.isEmpty()) {
          updateAttributes(bes, superMerge);
        }
        break;
      
      case "question":
        superMerge = retrieveDeletionRecords(bes, table, "questions");
        if(!superMerge.isEmpty()) {
          updateQuestion(bes, superMerge);
        }
        break;
        
      case "questionquestion":
        superMerge = retrieveDeletionRecords(bes, table, "questionQuestions");
        if(!superMerge.isEmpty()) {
          updateQuestionQuestion(bes, superMerge);
        }
        break;
        
      case "message":
        superMerge = retrieveDeletionRecords(bes, table, "messages");
        if(!superMerge.isEmpty()) {
          updateMessage(bes, superMerge);
        }
        break;
        
      case "validation":
        superMerge = retrieveDeletionRecords(bes, table, "validations");
        if(!superMerge.isEmpty()) {
          updateValidation(bes, superMerge);
        }
        break;
        
      case "entityentity":
        superMerge = retrieveDeletionRecords(bes, table, "basebase");
        if(!superMerge.isEmpty()) {
          updateEntityEntity(bes, superMerge);
        }
        break;
        
      default:
        log.info("Incorrect table name");
      }
    bl.persistProject(true, table.toLowerCase(), false);
    }
  
 
}
