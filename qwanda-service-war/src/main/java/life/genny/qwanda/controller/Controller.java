package life.genny.qwanda.controller;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.persistence.NoResultException;
import life.genny.qwanda.Question;
import life.genny.qwanda.QuestionQuestion;
import life.genny.qwanda.attribute.Attribute;
import life.genny.qwanda.attribute.AttributeLink;
import life.genny.qwanda.converter.StringListConverter;
import life.genny.qwanda.datatype.DataType;
import life.genny.qwanda.entity.BaseEntity;
import life.genny.qwanda.exception.BadDataException;
import life.genny.qwanda.message.QBaseMSGMessageTemplate;
import life.genny.qwanda.service.Service;
import life.genny.qwanda.validation.Validation;
import life.genny.services.BatchLoading;

public class Controller {


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

  public void updateBaseEntity(final Service service,
      final Map<String, Map<String, Object>> merge) {
    merge.get("baseentity").entrySet().stream().map(entry -> entry.getValue()).forEach(record -> {
      Map<String, Object> item = (HashMap<String, Object>) record;
      String code = (String) item.get("code");
      String name = (String) item.get("name");
      System.out.println("The code is " + code);
      BaseEntity be = null;

      try {
        be = service.findBaseEntityByCode(code);
        be.setCode(code);
        be.setName(name);
        service.update(be);
      } catch (NoResultException e) {
        System.out.println("no such Thing as: " + code);
        be = new BaseEntity(code, name);
        Long l = service.insert(be);
        System.out.println("This is long: " + l);
      }
      System.out.println("This has been updated" + be);
    });
  }

  public void updateAttributes(final Service service, final Map<String, Map<String, Object>> merge,
      final Map<String, DataType> data) {
    System.out.println("one time or ...");
    merge.get("attributes").entrySet().stream().map(entry -> entry.getValue()).forEach(record -> {
      Map<String, Object> item = (HashMap<String, Object>) record;
      String code = (String) item.get("code");
      String name = (String) item.get("name");
      String dataType = (String) item.get("dataType");
      DataType dataTypeRecord = data.get(dataType);
      Attribute attr = service.findAttributeByCode(code);
      if (attr == null) {
        attr = new Attribute(code, name, dataTypeRecord);
        Long l = service.insert(attr);
        System.out.println("This is long: " + l);
      } else {
        attr.setCode(code);
        attr.setName(name);
        attr.setDataType(dataTypeRecord);
        Long updatedAttr = service.update(attr);
        System.out.println("attr updated id: " + updatedAttr);
      }
    });
  }

  public void updateAttributeLink(final Service service,
      final Map<String, Map<String, Object>> merge) {
    merge.get("attributeLink").entrySet().stream().map(entry -> entry.getValue())
        .forEach(record -> {
          Map<String, Object> item = (HashMap<String, Object>) record;
          String code = (String) item.get("code");
          String name = (String) item.get("name");
          AttributeLink attrLink = null;
          try {
            attrLink = service.findAttributeLinkByCode(code);
            attrLink.setCode(code);
            attrLink.setName(name);
            Long attrLinkId = service.update(attrLink);
            System.out.println("attr updated id: " + attrLinkId);
          } catch (NoResultException e) {
            attrLink = new AttributeLink(code, name);
            Long l = service.insert(attrLink);
            System.out.println("This is long: " + l);
          }
        });
  }

  public void updateAttibutesEntity(final Service service,
      final Map<String, Map<String, Object>> merge) {
    merge.get("attibutesEntity").entrySet().stream().map(entry -> entry.getValue())
        .forEach(record -> {
          Map<String, Object> item = (HashMap<String, Object>) record;
          String attributeCode = ((String) item.get("attributeCode"));
          String valueString = ((String) item.get("valueString"));
          if (valueString != null) {
            valueString = valueString.replaceAll("^\"|\"$", "");;
          }
          String baseEntityCode = ((String) item.get("baseEntityCode"));
          String weight = (String) item.get("weight");
          Attribute attribute = null;
          BaseEntity be = null;
          try {
            attribute = service.findAttributeByCode(attributeCode);
            be = service.findBaseEntityByCode(baseEntityCode);
            Double weightField = null;
            try {
              weightField = Double.valueOf(weight);
            } catch (java.lang.NumberFormatException ee) {
              weightField = 0.0;
            }
            try {
              be.addAttribute(attribute, weightField, valueString);
            } catch (final BadDataException e) {
              e.printStackTrace();
            }
            service.upsert(be);
          } catch (final NoResultException e) {
          }
        });
  }

  public void updateBaseBase(final Service service, final Map<String, Map<String, Object>> merge) {
    merge.get("basebase").entrySet().stream().map(entry -> entry.getValue()).forEach(record -> {
      Map<String, Object> item = (HashMap<String, Object>) record;
      System.out.println("This is going to be updated" + item);
      String linkCode = ((String) item.get("linkCode"));
      String parentCode = ((String) item.get("parentCode"));
      String targetCode = ((String) item.get("targetCode"));
      String weightStr = ((String) item.get("weight"));
      String valueString = ((String) item.get("valueString"));
      final Double weight = Double.valueOf(weightStr);
      BaseEntity sbe = null;
      BaseEntity tbe = null;
      Attribute linkAttribute = service.findAttributeByCode(linkCode);
      try {
        sbe = service.findBaseEntityByCode(parentCode);
        tbe = service.findBaseEntityByCode(targetCode);
        sbe.addTarget(tbe, linkAttribute, weight, valueString);
        service.updateWithAttributes(sbe);
      } catch (final NoResultException e) {
        System.out.println("CODE NOT PRESENT IN LINKING: "+parentCode+":"+targetCode+":"+linkAttribute);
    } catch (final BadDataException e) {
      e.printStackTrace();
    }
    });
  }
  
  public void updateValidations(final Service service,
      final Map<String, Map<String, Object>> merge) {
    merge.get("validations").entrySet().stream().map(entry -> entry.getValue()).forEach(record -> {
      Map<String, Object> item = (HashMap<String, Object>) record;
      String regex = ((String) item.get("regex"));
      String code = ((String) item.get("code"));
      String name = ((String) item.get("name"));
      String recursiveStr = ((String) item.get("recursive"));
      String multiAllowedStr = ((String) item.get("multi_allowed"));
      Boolean recursive = getBooleanFromString(recursiveStr);
      Boolean multiAllowed = getBooleanFromString(multiAllowedStr);
      String selectionBaseEntityGroupString = ((String) item.get("group_codes"));
      List<String> selectionBaseEntityGroupList = new ArrayList<String>();
      if (selectionBaseEntityGroupString != null) {
        selectionBaseEntityGroupList = new ArrayList<>(Arrays.asList(selectionBaseEntityGroupString.split(",")));
      }

      Validation val = null;
      try {
        val = service.findValidationByCode(code);
        if (val != null) {
          val.setCode(code);
          val.setName(name);
          val.setRegex(regex);
          val.setRecursiveGroup(recursive);
          val.setMultiAllowed(multiAllowed);
          val.setSelectionBaseEntityGroupList(selectionBaseEntityGroupList);
        }
      } catch(final NoResultException e) {
        val = new Validation(code, name, selectionBaseEntityGroupList, recursive, multiAllowed);
        val.setRegex(regex);
      }
      service.upsert(val);
    });
  }
  
  public void updateQuestion(final Service service,
      final Map<String, Map<String, Object>> merge) {
    merge.get("questions").entrySet().stream().map(entry -> entry.getValue())
        .forEach(record -> {
          Map<String, Object> item = (HashMap<String, Object>) record;
          String code = (String) item.get("code");
          String name = (String) item.get("name");
          String attrCode = (String) item.get("attribute_code");
          String html = (String) item.get("html");
          

          Attribute attr;
          attr = service.findAttributeByCode(attrCode);
          Question q = new Question(code, name, attr);
          q.setHtml(html);
          Question existing = service.findQuestionByCode(code);
          if (existing == null) {
                service.insert(q);
          } else {
              existing.setName(name);
              existing.setHtml(html);
              existing.setCode(attrCode);
              service.upsert(existing); 
          }
        });
  }
  
  
  public void updateQuestionQuestions(final Service service,
      final Map<String, Map<String, Object>> merge) {
    merge.get("questionQuestions").entrySet().stream().map(entry -> entry.getValue())
        .forEach(record -> {
          Map<String, Object> item = (HashMap<String, Object>) record;
          String parentCode = ((String) item.get("parentCode"));
          String targetCode = ((String) item.get("targetCode"));
          String weightStr = ((String) item.get("weight"));
          String mandatoryStr = ((String) item.get("mandatory"));
          final Double weight = Double.valueOf(weightStr);
          Boolean mandatory = "TRUE".equalsIgnoreCase(mandatoryStr);
          Question sbe = null;
          Question tbe = null;

          try {
            sbe = service.findQuestionByCode(parentCode);
            tbe = service.findQuestionByCode(targetCode);
            QuestionQuestion qq = sbe.addChildQuestion(tbe.getCode(), weight, mandatory);

            qq = service.upsert(qq);

          } catch (final NoResultException e) {
            System.out.println("No Result! in QuestionQuestions Loading");
          } catch (final BadDataException e) {
            e.printStackTrace();
          }
        });
  }

  public void updateMessages(final Service service, final Map<String, Map<String, Object>> merge) {
    merge.get("messages").entrySet().stream().map(entry -> entry.getValue()).forEach(record -> {
      Map<String, Object> template = (HashMap<String, Object>) record;
      String code = (String) template.get("code");
      String name = (String) template.get("name");
      String description = (String) template.get("description");
      String subject = (String) template.get("subject");
      String emailTemplateDocId = (String) template.get("email");
      String smsTemplate = (String) template.get("sms");
      String toastTemplate = (String) template.get("toast");
      QBaseMSGMessageTemplate templateObj;
      try {
        templateObj = service.findTemplateByCode(code);
        if(templateObj != null) {
          templateObj.setCode(code);
          templateObj.setName(name);
          templateObj.setCreated(LocalDateTime.now());
          templateObj.setDescription(description);
          templateObj.setEmail_templateId(emailTemplateDocId);
          templateObj.setSms_template(smsTemplate);
          templateObj.setSubject(subject);
          templateObj.setToast_template(toastTemplate);
          service.update(templateObj);
        } 
      }catch(final NoResultException e) {
        templateObj = new QBaseMSGMessageTemplate();
        templateObj.setCode(code);
        templateObj.setName(name);
        templateObj.setCreated(LocalDateTime.now());
        templateObj.setDescription(description);
        templateObj.setEmail_templateId(emailTemplateDocId);
        templateObj.setSms_template(smsTemplate);
        templateObj.setSubject(subject);
        templateObj.setToast_template(toastTemplate);
        service.insert(templateObj);
      }
    });
  }


  static Map<String, Object> merge = new HashMap<String, Object>();

  public static void getProject(final Service bes, final String sheetId, final String tables) {
    BatchLoading bl = new BatchLoading(bes);
    bl.sheets.setSheetId(sheetId);
    Map<String, Object> saveProjectData = bl.savedProjectData;
    String tablesLC = tables.toLowerCase();
    Map<String, Object> table2Update = new HashMap<String, Object>();
    Map<String, DataType> data = new HashMap<String, DataType>();
    Map<String, Map<String, Object>> superMerge = new HashMap<String, Map<String, Object>>();

    if (tablesLC.contains("validation")) {
      table2Update.put("validations", bl.sheets.newGetVal());
      ((HashMap<String, HashMap>) table2Update.get("validations")).entrySet().stream()
          .forEach(map -> {
            try {
              HashMap<String, HashMap> newMap =
                  ((HashMap<String, HashMap>) saveProjectData.get("validations")).get(map.getKey());
              if (!newMap.entrySet().containsAll(map.getValue().entrySet())) {
                merge.put(map.getKey(), map.getValue());
                ((HashMap<String, HashMap>) saveProjectData.get("validations")).put(map.getKey(),
                    map.getValue());
              }
            } catch (NullPointerException e) {
              merge.put(map.getKey(), map.getValue());
              ((HashMap<String, HashMap>) saveProjectData.get("validations")).put(map.getKey(),
                  map.getValue());
            }
          });
      Controller ctl = new Controller();
      superMerge.put("validations", merge);
      if (!merge.isEmpty())
        ctl.updateValidations(bes, superMerge);
      System.out.println(merge);
      merge = new HashMap<String, Object>();
    }

    if (tablesLC.contains("datatype")) {
      table2Update.put("dataType", bl.sheets.newGetDType());
      ((HashMap<String, HashMap>) table2Update.get("dataType")).entrySet().stream().forEach(map -> {
        try {
          HashMap<String, HashMap> newMap =
              ((HashMap<String, HashMap>) saveProjectData.get("dataType")).get(map.getKey());
          if (!newMap.entrySet().containsAll(map.getValue().entrySet())) {
            merge.put(map.getKey(), map.getValue());
            ((HashMap<String, HashMap>) saveProjectData.get("dataType")).put(map.getKey(),
                map.getValue());
          }
        } catch (NullPointerException e) {
          merge.put(map.getKey(), map.getValue());
          ((HashMap<String, HashMap>) saveProjectData.get("dataType")).put(map.getKey(),
              map.getValue());
        }
      });
      Map<String, Object> dataTypeTmp = new HashMap<String, Object>();
      dataTypeTmp.put("dataType", merge);
      merge = new HashMap<String, Object>();
      data = bl.dataType(dataTypeTmp);



      System.out.println("this is data :: " + data);
    }
    if (tablesLC.matches(".*attribute(?!link).*")) {
      table2Update.put("attributes", bl.sheets.newGetAttr());
      ((HashMap<String, HashMap>) table2Update.get("attributes")).entrySet().stream()
          .forEach(map -> {
            try {
              HashMap<String, HashMap> newMap =
                  ((HashMap<String, HashMap>) saveProjectData.get("attributes")).get(map.getKey());
              if (!newMap.entrySet().containsAll(map.getValue().entrySet())) {
                merge.put(map.getKey(), map.getValue());
                ((HashMap<String, HashMap>) saveProjectData.get("attributes")).put(map.getKey(),
                    map.getValue());
              }
            } catch (NullPointerException e) {
              merge.put(map.getKey(), map.getValue());
              ((HashMap<String, HashMap>) saveProjectData.get("attributes")).put(map.getKey(),
                  map.getValue());
            }
          });
      Controller ctl = new Controller();
      superMerge.put("attributes", merge);
      if (!merge.isEmpty()) {
        if (data.isEmpty()) {
          Map<String, DataType> dataTypes = bl.dataType(saveProjectData);
          System.out.println("Save Project Data: " + dataTypes);
          ctl.updateAttributes(bes, superMerge, dataTypes);
        } else {
          System.out.println("Data is being printed: " + data);
          ctl.updateAttributes(bes, superMerge, data);
          data = new HashMap<String, DataType>();
        }
      }
      System.out.println(merge);
      merge = new HashMap<String, Object>();
    }
    if (tablesLC.contains("attributelink")) {
      table2Update.put("attributeLink", bl.sheets.newGetAttrLink());
      ((HashMap<String, HashMap>) table2Update.get("attributeLink")).entrySet().stream()
          .forEach(map -> {
            try {
              HashMap<String, HashMap> newMap =
                  ((HashMap<String, HashMap>) saveProjectData.get("attributeLink"))
                      .get(map.getKey());
              if (!newMap.entrySet().containsAll(map.getValue().entrySet())) {
                merge.put(map.getKey(), map.getValue());
                ((HashMap<String, HashMap>) saveProjectData.get("attributeLink")).put(map.getKey(),
                    map.getValue());
              }
            } catch (NullPointerException e) {
              merge.put(map.getKey(), map.getValue());
              ((HashMap<String, HashMap>) saveProjectData.get("attributeLink")).put(map.getKey(),
                  map.getValue());
            }
          });
      Controller ctl = new Controller();
      superMerge.put("attributeLink", merge);
      if (!merge.isEmpty())
        ctl.updateAttributeLink(bes, superMerge);
      System.out.println(merge);
      merge = new HashMap<String, Object>();
    }
    if (tablesLC.contains("baseentity")) {
      table2Update.put("baseEntitys", bl.sheets.newGetBase());
      ((HashMap<String, HashMap>) table2Update.get("baseEntitys")).entrySet().stream()
          .forEach(map -> {
            HashMap<String, HashMap> newMap;
            try {
              newMap =
                  ((HashMap<String, HashMap>) saveProjectData.get("baseEntitys")).get(map.getKey());
              if (!newMap.entrySet().containsAll(map.getValue().entrySet())) {
                merge.put(map.getKey(), map.getValue());
                ((HashMap<String, HashMap>) saveProjectData.get("baseEntitys")).put(map.getKey(),
                    map.getValue());
              }
            } catch (NullPointerException e) {
              newMap =
                  ((HashMap<String, HashMap>) saveProjectData.get("baseEntitys")).get(map.getKey());
              merge.put(map.getKey(), map.getValue());
              ((HashMap<String, HashMap>) saveProjectData.get("baseEntitys")).put(map.getKey(),
                  map.getValue());
            }
          });
      Controller ctl = new Controller();
      superMerge.put("baseentity", merge);
      if (!merge.isEmpty())
        ctl.updateBaseEntity(bes, superMerge);
      System.out.println(merge);
      merge = new HashMap<String, Object>();
    }
    if (tablesLC.contains("entityattribute")) {
      table2Update.put("attibutesEntity", bl.sheets.newGetEntAttr());
      ((HashMap<String, HashMap>) table2Update.get("attibutesEntity")).entrySet().stream()
          .forEach(map -> {
            try {
              HashMap<String, HashMap> newMap =
                  ((HashMap<String, HashMap>) saveProjectData.get("attibutesEntity"))
                      .get(map.getKey());
              if (!newMap.entrySet().containsAll(map.getValue().entrySet())) {
                merge.put(map.getKey(), map.getValue());
                ((HashMap<String, HashMap>) saveProjectData.get("attibutesEntity"))
                    .put(map.getKey(), map.getValue());
              }
            } catch (NullPointerException e) {
              merge.put(map.getKey(), map.getValue());
              ((HashMap<String, HashMap>) saveProjectData.get("attibutesEntity")).put(map.getKey(),
                  map.getValue());
            }
          });
      Controller ctl = new Controller();
      superMerge.put("attibutesEntity", merge);
      if (!merge.isEmpty())
        ctl.updateAttibutesEntity(bes, superMerge);
      System.out.println(merge);
      merge = new HashMap<String, Object>();
    }
    if (tablesLC.contains("entityentity")) {
      table2Update.put("basebase", bl.sheets.newGetEntEnt());
      ((HashMap<String, HashMap>) table2Update.get("basebase")).entrySet().stream().forEach(map -> {
        try {
          HashMap<String, HashMap> newMap =
              ((HashMap<String, HashMap>) saveProjectData.get("basebase")).get(map.getKey());
          if (!newMap.entrySet().containsAll(map.getValue().entrySet())) {
            merge.put(map.getKey(), map.getValue());
            ((HashMap<String, HashMap>) saveProjectData.get("basebase")).put(map.getKey(),
                map.getValue());
          }
        } catch (NullPointerException e) {
          merge.put(map.getKey(), map.getValue());
          ((HashMap<String, HashMap>) saveProjectData.get("basebase")).put(map.getKey(),
              map.getValue());
        }
      });
      Controller ctl = new Controller();
      superMerge.put("basebase", merge);
      if (!merge.isEmpty())
        ctl.updateBaseBase(bes, superMerge);
      System.out.println(merge);
      merge = new HashMap<String, Object>();
    }

    if (tablesLC.contains("question")) {
      table2Update.put("questions", bl.sheets.newGetQtn());
      ((HashMap<String, HashMap>) table2Update.get("questions")).entrySet().stream()
          .forEach(map -> {
            try {
              HashMap<String, HashMap> newMap =
                  ((HashMap<String, HashMap>) saveProjectData.get("questions"))
                      .get(map.getKey());
              if (!newMap.entrySet().containsAll(map.getValue().entrySet())) {
                merge.put(map.getKey(), map.getValue());
                ((HashMap<String, HashMap>) saveProjectData.get("questions"))
                    .put(map.getKey(), map.getValue());
              }
            } catch (NullPointerException e) {
              merge.put(map.getKey(), map.getValue());
              ((HashMap<String, HashMap>) saveProjectData.get("questions"))
                  .put(map.getKey(), map.getValue());
            }
          });
      Controller ctl = new Controller();
      superMerge.put("questions", merge);
      if (!merge.isEmpty())
        ctl.updateQuestion(bes, superMerge);
      System.out.println(merge);
      merge = new HashMap<String, Object>();
    }
    
    if (tablesLC.contains("questionquestion")) {
      table2Update.put("questionQuestions", bl.sheets.newGetQueQue());
      ((HashMap<String, HashMap>) table2Update.get("questionQuestions")).entrySet().stream()
          .forEach(map -> {
            try {
              HashMap<String, HashMap> newMap =
                  ((HashMap<String, HashMap>) saveProjectData.get("questionQuestions"))
                      .get(map.getKey());
              if (!newMap.entrySet().containsAll(map.getValue().entrySet())) {
                merge.put(map.getKey(), map.getValue());
                ((HashMap<String, HashMap>) saveProjectData.get("questionQuestions"))
                    .put(map.getKey(), map.getValue());
              }
            } catch (NullPointerException e) {
              merge.put(map.getKey(), map.getValue());
              ((HashMap<String, HashMap>) saveProjectData.get("questionQuestions"))
                  .put(map.getKey(), map.getValue());
            }
          });
      Controller ctl = new Controller();
      superMerge.put("questionQuestions", merge);
      if (!merge.isEmpty())
        ctl.updateQuestionQuestions(bes, superMerge);
      System.out.println(merge);
      merge = new HashMap<String, Object>();
    }

    if (tablesLC.contains("messages")) {
      table2Update.put("messages", bl.sheets.getMessageTemplates());
      ((HashMap<String, HashMap>) table2Update.get("messages")).entrySet().stream().forEach(map -> {
        try {
          HashMap<String, HashMap> newMap =
              ((HashMap<String, HashMap>) saveProjectData.get("messages")).get(map.getKey());
          if (!newMap.entrySet().containsAll(map.getValue().entrySet())) {
            merge.put(map.getKey(), map.getValue());
            ((HashMap<String, HashMap>) saveProjectData.get("messages")).put(map.getKey(),
                map.getValue());
          }
        } catch (NullPointerException e) {
          merge.put(map.getKey(), map.getValue());
          ((HashMap<String, HashMap>) saveProjectData.get("messages")).put(map.getKey(),
              map.getValue());
        }
      });
      Controller ctl = new Controller();
      superMerge.put("messages", merge);
      if (!merge.isEmpty())
        ctl.updateMessages(bes, superMerge);
      System.out.println(merge);
      merge = new HashMap<String, Object>();
    }

    superMerge = new HashMap<String, Map<String, Object>>();
  }
}
