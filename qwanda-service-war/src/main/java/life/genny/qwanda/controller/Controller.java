package life.genny.qwanda.controller;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.persistence.NoResultException;
import javax.transaction.Transactional;
import javax.ws.rs.core.MultivaluedMap;
import org.jboss.resteasy.specimpl.MultivaluedMapImpl;
import life.genny.qwanda.Question;
import life.genny.qwanda.QuestionQuestion;
import life.genny.qwanda.attribute.Attribute;
import life.genny.qwanda.attribute.AttributeLink;
import life.genny.qwanda.datatype.DataType;
import life.genny.qwanda.entity.BaseEntity;
import life.genny.qwanda.entity.EntityEntity;
import life.genny.qwanda.exception.BadDataException;
import life.genny.qwanda.message.QBaseMSGMessageTemplate;
import life.genny.qwanda.service.Service;
import life.genny.qwanda.validation.Validation;
import life.genny.services.BatchLoading;

public class Controller {

  public static final String REALM_HIDDEN = "hidden";

  private Boolean getBooleanFromString(final String booleanString) {
    if (booleanString == null) {
      return false;
    }

    if (("TRUE".equalsIgnoreCase(booleanString.toUpperCase()))
        || ("YES".equalsIgnoreCase(booleanString.toUpperCase()))
        || ("T".equalsIgnoreCase(booleanString.toUpperCase()))
        || ("Y".equalsIgnoreCase(booleanString.toUpperCase()))
        || ("1".equalsIgnoreCase(booleanString))) {
      return true;
    }
    return false;
  }

  public void updateBaseEntity(
      final Service service, final Map<String, Map<String, Object>> merge) {
    merge
        .get("baseentity")
        .entrySet()
        .stream()
        .map(entry -> entry.getValue())
        .forEach(
            record -> {
              Map<String, Object> item = (HashMap<String, Object>) record;
              String code = (String) item.get("code");
              BaseEntity be = null;

              try {
                be = service.findBaseEntityByCode(code);
                be.setCode(code);
                be.setRealm(REALM_HIDDEN);
                service.updateRealm(be);
              } catch (NoResultException e) {
                e.printStackTrace();
              }
              System.out.println("This has been updated: " + be);
            });
  }

  public void updateAttributes(
      final Service service, final Map<String, Map<String, Object>> merge) {
    merge
        .get("attributes")
        .entrySet()
        .stream()
        .map(entry -> entry.getValue())
        .forEach(
            record -> {
              Map<String, Object> item = (HashMap<String, Object>) record;
              String code = (String) item.get("code");
              String name = (String) item.get("name");
              String dataType = (String) item.get("dataType");
              Attribute attr = null;
              try {
                attr = service.findAttributeByCode(code);
                attr.setCode(code);
                attr.setRealm(REALM_HIDDEN);
                service.updateRealm(attr);
                System.out.println("This has been updated: " + attr);
              } catch (NoResultException e) {
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
        .map(entry -> entry.getValue())
        .forEach(
            record -> {
              Map<String, Object> item = (HashMap<String, Object>) record;
              String code = (String) item.get("code");
              AttributeLink attrLink = null;
              try {
                attrLink = service.findAttributeLinkByCode(code);
                attrLink.setCode(code);
                attrLink.setRealm(REALM_HIDDEN);
                service.updateRealm(attrLink);
                System.out.println("This has been updated: " + attrLink);
              } catch (NoResultException e) {
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
        .map(entry -> entry.getValue())
        .forEach(
            record -> {
              Map<String, Object> item = (HashMap<String, Object>) record;
              String attributeCode = ((String) item.get("attributeCode"));
              String baseEntityCode = ((String) item.get("baseEntityCode"));
              try {
                service.removeEntityAttribute(baseEntityCode, attributeCode);
                System.out.println("Successfully removed entity attributes");
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
        .map(entry -> entry.getValue())
        .forEach(
            record -> {
              Map<String, Object> item = (HashMap<String, Object>) record;
              String linkCode = ((String) item.get("linkCode"));
              String parentCode = ((String) item.get("parentCode"));
              String targetCode = ((String) item.get("targetCode"));
              try {
                service.removeLink(parentCode, targetCode, linkCode);
                System.out.println("Successfully removed entity entities");
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
        .map(entry -> entry.getValue())
        .forEach(
            record -> {
              Map<String, Object> item = (HashMap<String, Object>) record;
              String code = ((String) item.get("code"));

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
        .map(entry -> entry.getValue())
        .forEach(
            record -> {
              Map<String, Object> item = (HashMap<String, Object>) record;
              String code = ((String) item.get("code"));
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
        .map(entry -> entry.getValue())
        .forEach(
            record -> {
              Map<String, Object> item = (HashMap<String, Object>) record;
              String parentCode = ((String) item.get("parentCode"));
              String targetCode = ((String) item.get("targetCode"));

              try {
                service.removeQuestionQuestion(parentCode, targetCode);
                System.out.println("Successfully removed question questions");
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
        .map(entry -> entry.getValue())
        .forEach(
            record -> {
              Map<String, Object> template = (HashMap<String, Object>) record;
              String code = (String) template.get("code");
              QBaseMSGMessageTemplate templateObj;
              try {
                templateObj = service.findTemplateByCode(code);
                templateObj.setCode(code);
                templateObj.setRealm(REALM_HIDDEN);
                service.updateRealm(templateObj);
              } catch (NoResultException e) {
                e.printStackTrace();
              }
            });
  }

  Map<String, Object> merge = new HashMap<String, Object>();

  @Transactional
  public void synchronizeSheetsToDataBase(final Service bes, final String table) {
    BatchLoading bl = new BatchLoading(bes);
    Map<String, Object> saveProjectData = new HashMap(bl.savedProjectData);
    String tablesLC = table.toLowerCase();
    Map<String, Object> sheetMap;
    Map<String, DataType> data = new HashMap<String, DataType>();
    Map<String, Map<String, Object>> superMerge = new HashMap<String, Map<String, Object>>();

    if (tablesLC.equalsIgnoreCase("baseentity")) {
      sheetMap = bl.persistProject(true, tablesLC); // contains records from sheets
      Iterator iter =
          ((HashMap<String, HashMap>) saveProjectData.get("baseEntitys")).entrySet().iterator();

      while (iter.hasNext()) {
        Map.Entry entry = (Entry) iter.next();
        String key = (String) entry.getKey();
        HashMap<String, HashMap> newMap = (HashMap<String, HashMap>) sheetMap.get("baseEntitys");
        if (newMap != null && !newMap.containsKey(key)) {
          System.out.println("key == " + entry.getKey() + " value == " + entry.getValue());
          merge.put(entry.getKey().toString(), entry.getValue());
        }
      }
      superMerge.put("baseentity", merge);
      System.out.println("Things to delete: " + merge);
      if (!merge.isEmpty()) {
        updateBaseEntity(bes, superMerge);
      }
      merge = new HashMap<String, Object>();
    }
    
    else if (tablesLC.equalsIgnoreCase("attributelink")) {
      sheetMap = bl.persistProject(true, tablesLC); // contains records from sheets

      Iterator iter =
          ((HashMap<String, HashMap>) saveProjectData.get("attributeLink")).entrySet().iterator();
      
      while (iter.hasNext()) {
        Map.Entry entry = (Entry) iter.next();
        String key = (String) entry.getKey();
        HashMap<String, HashMap> newMap = (HashMap<String, HashMap>) sheetMap.get("attributeLink");
        if (newMap != null && !newMap.containsKey(key)) {
          System.out.println("key == " + entry.getKey() + " value == " + entry.getValue());
          merge.put(entry.getKey().toString(), entry.getValue());
        }
      }
      superMerge.put("attributeLink", merge);
      System.out.println("Things to delete: " + merge);
      if (!merge.isEmpty()) updateAttributeLink(bes, superMerge);
      merge = new HashMap<String, Object>();
    }
    
    else if (tablesLC.equalsIgnoreCase("entityattribute")) {
      sheetMap = bl.persistProject(true, tablesLC); // contains records from sheets
      Iterator iter =
          ((HashMap<String, HashMap>) saveProjectData.get("attibutesEntity")).entrySet().iterator();

      while (iter.hasNext()) {
        Map.Entry entry = (Entry) iter.next();
        String key = (String) entry.getKey();
        HashMap<String, HashMap> newMap =
            (HashMap<String, HashMap>) sheetMap.get("attibutesEntity");
        if (newMap != null && !newMap.containsKey(key)) {
          System.out.println("key == " + entry.getKey() + " value == " + entry.getValue());
          merge.put(entry.getKey().toString(), entry.getValue());
        }
      }
      superMerge.put("attibutesEntity", merge);
      System.out.println("Things to delete: " + merge);
      if (!merge.isEmpty()) updateEntityAttribute(bes, superMerge);
      merge = new HashMap<String, Object>();
    }

    else if (tablesLC.equalsIgnoreCase("attribute")) {
      sheetMap = bl.persistProject(true, tablesLC); // contains records from sheets

      Iterator iter =
          ((HashMap<String, HashMap>) saveProjectData.get("attributes")).entrySet().iterator();
      while (iter.hasNext()) {
        Map.Entry entry = (Entry) iter.next();
        String key = (String) entry.getKey();
        HashMap<String, HashMap> newMap = (HashMap<String, HashMap>) sheetMap.get("attributes");
        if (newMap != null && !newMap.containsKey(key)) {
          System.out.println("key == " + entry.getKey() + " value == " + entry.getValue());
          merge.put(entry.getKey().toString(), entry.getValue());
        }
      }
      superMerge.put("attributes", merge);
      System.out.println("Things to delete: " + merge);
      if (!merge.isEmpty()) updateAttributes(bes, superMerge);
      merge = new HashMap<String, Object>();
    }

    else if (tablesLC.equalsIgnoreCase("question")) {
      sheetMap = bl.persistProject(true, tablesLC); // contains records from sheets
      Iterator iter =
          ((HashMap<String, HashMap>) saveProjectData.get("questions")).entrySet().iterator();

      while (iter.hasNext()) {
        Map.Entry entry = (Entry) iter.next();
        String key = (String) entry.getKey();
        HashMap<String, HashMap> newMap = (HashMap<String, HashMap>) sheetMap.get("questions");
        if (newMap != null && !newMap.containsKey(key)) {
          System.out.println("key == " + entry.getKey() + " value == " + entry.getValue());
          merge.put(entry.getKey().toString(), entry.getValue());
        }
      }
      superMerge.put("questions", merge);
      System.out.println("Things to delete: " + merge);
      if (!merge.isEmpty()) updateQuestion(bes, superMerge);
      merge = new HashMap<String, Object>();
    }

    else if (tablesLC.equalsIgnoreCase("questionquestion")) {
      sheetMap = bl.persistProject(true, tablesLC); // contains records from sheets
      Iterator iter =
          ((HashMap<String, HashMap>) saveProjectData.get("questionQuestions"))
              .entrySet()
              .iterator();
      
      Iterator iterator =
          ((HashMap<String, HashMap>) sheetMap.get("questionQuestions")).entrySet().iterator();
      while(iterator.hasNext()) {
        Map.Entry entry = (Entry) iterator.next();
        System.out.println("SHEET MAP key == " + entry.getKey() + " value == " + entry.getValue());
      }

      while (iter.hasNext()) {
        Map.Entry entry = (Entry) iter.next();
        String key = (String) entry.getKey();
        HashMap<String, HashMap> newMap =
            (HashMap<String, HashMap>) sheetMap.get("questionQuestions");
        System.out.println("SAVEPROJECTDATA key == " + entry.getKey() + " value == " + entry.getValue());
        if (newMap != null && !newMap.containsKey(key)) {
          System.out.println("key == " + entry.getKey() + " value == " + entry.getValue());
          merge.put(entry.getKey().toString(), entry.getValue());
        }
      }
      superMerge.put("questionQuestions", merge);
      System.out.println("Things to delete: " + merge);
      if (!merge.isEmpty()) updateQuestionQuestion(bes, superMerge);
      merge = new HashMap<String, Object>();
    }

    else if (tablesLC.equalsIgnoreCase("message")) {
      sheetMap = bl.persistProject(true, tablesLC); // contains records from sheets
      Iterator iter =
          ((HashMap<String, HashMap>) saveProjectData.get("messages")).entrySet().iterator();

      while (iter.hasNext()) {
        Map.Entry entry = (Entry) iter.next();
        String key = (String) entry.getKey();
        HashMap<String, HashMap> newMap = (HashMap<String, HashMap>) sheetMap.get("messages");
        if (newMap != null && !newMap.containsKey(key)) {
          System.out.println("key == " + entry.getKey() + " value == " + entry.getValue());
          merge.put(entry.getKey().toString(), entry.getValue());
        }
      }
      superMerge.put("messages", merge);
      System.out.println("Things to delete: " + merge);
      if (!merge.isEmpty()) updateMessage(bes, superMerge);
      merge = new HashMap<String, Object>();
    }

    else if (tablesLC.equalsIgnoreCase("validation")) {
      sheetMap = bl.persistProject(true, tablesLC); // contains records from sheets
      Iterator iter =
          ((HashMap<String, HashMap>) saveProjectData.get("validations")).entrySet().iterator();

      while (iter.hasNext()) {
        Map.Entry entry = (Entry) iter.next();
        String key = (String) entry.getKey();
        HashMap<String, HashMap> newMap = (HashMap<String, HashMap>) sheetMap.get("validations");
        if (newMap != null && !newMap.containsKey(key)) {
          System.out.println("key == " + entry.getKey() + " value == " + entry.getValue());
          merge.put(entry.getKey().toString(), entry.getValue());
        }
      }
      superMerge.put("validations", merge);
      System.out.println("Things to delete: " + merge);
      if (!merge.isEmpty()) updateValidation(bes, superMerge);
      merge = new HashMap<String, Object>();
    }

    else if (tablesLC.equalsIgnoreCase("entityentity")) {
      sheetMap = bl.persistProject(true, tablesLC); // contains records from sheets
      Iterator iter =
          ((HashMap<String, HashMap>) saveProjectData.get("basebase")).entrySet().iterator();

      while (iter.hasNext()) {
        Map.Entry entry = (Entry) iter.next();
        String key = (String) entry.getKey();
        HashMap<String, HashMap> newMap = (HashMap<String, HashMap>) sheetMap.get("basebase");
        if (newMap != null && !newMap.containsKey(key)) {
          System.out.println("key == " + entry.getKey() + " value == " + entry.getValue());
          merge.put(entry.getKey().toString(), entry.getValue());
        }
      }
      superMerge.put("basebase", merge);
      System.out.println("Things to delete: " + merge);
      if (!merge.isEmpty()) updateEntityEntity(bes, superMerge);
      merge = new HashMap<String, Object>();
    }
  }
}
