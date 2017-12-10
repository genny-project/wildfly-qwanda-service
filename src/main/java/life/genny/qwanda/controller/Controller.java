package life.genny.qwanda.controller;

import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
import life.genny.qwanda.service.Service;
import life.genny.services.BaseEntityService2;
import life.genny.services.BatchLoading;

public class Controller {


  @Inject
  private BaseEntityService2 service;

  public static void getProject(final Service bes, final String sheetId, final String tables) {
    BatchLoading bl = new BatchLoading(bes);
    bl.sheets.setSheetId(sheetId);
    Map<String, Object> saveProjectData = bl.savedProjectData;
    String tablesLC = tables.toLowerCase();
    Map<String, Object> table2Update = new HashMap<String, Object>();
    Map<String, Object> merge = new HashMap<String, Object>();
    if (tablesLC.contains("ask")) {
      table2Update.put("ask", bl.sheets.newGetAsk());
      ((HashMap<String, HashMap>) table2Update.get("ask")).entrySet().stream().forEach(map -> {
        try {
          HashMap<String, HashMap> newMap =
              ((HashMap<String, HashMap>) saveProjectData.get("ask")).get(map.getKey());
          if (!newMap.entrySet().containsAll(map.getValue().entrySet())) {
            merge.put(map.getKey(), map);
          }
        } catch (NullPointerException e) {
          merge.put(map.getKey(), map);
        }
      });
      System.out.println(merge);
    }
    if (tablesLC.contains("datatype")) {
      table2Update.put("dataType", bl.sheets.newGetDType());
      ((HashMap<String, HashMap>) table2Update.get("dataType")).entrySet().stream().forEach(map -> {
        try {
          HashMap<String, HashMap> newMap =
              ((HashMap<String, HashMap>) saveProjectData.get("dataType")).get(map.getKey());
          if (!newMap.entrySet().containsAll(map.getValue().entrySet())) {
            merge.put("dataType", map);
          }
        } catch (NullPointerException e) {
          merge.put("dataType", map);
        }
      });
      bl.dataType(table2Update);
      System.out.println(merge);
    }
    if (tablesLC.contains("attribute")) {
      table2Update.put("attributes", bl.sheets.newGetAttr());
      if (table2Update.get("dataType") == null)
        table2Update.put("dataType", bl.sheets.newGetDType());
      ((HashMap<String, HashMap>) table2Update.get("attributes")).entrySet().stream()
          .forEach(map -> {
            try {
              HashMap<String, HashMap> newMap =
                  ((HashMap<String, HashMap>) saveProjectData.get("attributes")).get(map.getKey());
              if (!newMap.entrySet().containsAll(map.getValue().entrySet())) {
                merge.put("attributes", map);
              }
            } catch (NullPointerException e) {
              merge.put("attributes", map);
            }
          });
      System.out.println(merge);
    }
    if (tablesLC.contains("attributelink")) {
      bl.attributeLinks(table2Update);
      ((HashMap<String, HashMap>) table2Update.get("attributelink")).entrySet().stream()
          .forEach(map -> {
            try {
              HashMap<String, HashMap> newMap =
                  ((HashMap<String, HashMap>) saveProjectData.get("attributelink"))
                      .get(map.getKey());
              if (!newMap.entrySet().containsAll(map.getValue().entrySet())) {
                merge.put("attributelink", map);
              }
            } catch (NullPointerException e) {
              merge.put("attributelink", map);
            }
          });
      System.out.println(merge);
    }
    if (tablesLC.contains("baseentity")) {
      table2Update.put("baseEntitys", bl.sheets.newGetBase());
      ((HashMap<String, HashMap>) table2Update.get("baseEntitys")).entrySet().stream()
          .forEach(map -> {
            try {
              HashMap<String, HashMap> newMap =
                  ((HashMap<String, HashMap>) saveProjectData.get("baseEntitys")).get(map.getKey());
              if (!newMap.entrySet().containsAll(map.getValue().entrySet())) {
                merge.put(map.getKey(), map);
              }
            } catch (NullPointerException e) {
              merge.put(map.getKey(), map);
            }
          });
      System.out.println(merge);
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
                merge.put("attibutesEntity", map);
              }
            } catch (NullPointerException e) {
              merge.put("attibutesEntity", map);
            }
          });
      System.out.println(merge);
    }
    if (tablesLC.contains("entityentity")) {
      table2Update.put("basebase", bl.sheets.newGetEntEnt());
      ((HashMap<String, HashMap>) table2Update.get("basebase")).entrySet().stream().forEach(map -> {
        try {
          HashMap<String, HashMap> newMap =
              ((HashMap<String, HashMap>) saveProjectData.get("basebase")).get(map.getKey());
          if (!newMap.entrySet().containsAll(map.getValue().entrySet())) {
            merge.put("basebase", map);
          }
        } catch (NullPointerException e) {
          merge.put("basebase", map);
        }
      });
      System.out.println(merge);
    }
    if (tablesLC.contains("validation")) {
      table2Update.put("validations", bl.sheets.newGetVal());
      ((HashMap<String, HashMap>) table2Update.get("validations")).entrySet().stream()
          .forEach(map -> {
            try {
              HashMap<String, HashMap> newMap =
                  ((HashMap<String, HashMap>) saveProjectData.get("validations")).get(map.getKey());
              if (!newMap.entrySet().containsAll(map.getValue().entrySet())) {
                merge.put("validations", map);
              }
            } catch (NullPointerException e) {
              merge.put("validations", map);
            }
          });
      System.out.println(merge);
    }
    if (tablesLC.contains("question")) {
      table2Update.put("questions", bl.sheets.newGetQtn());
      ((HashMap<String, HashMap>) table2Update.get("questions")).entrySet().stream()
          .forEach(map -> {
            try {
              HashMap<String, HashMap> newMap =
                  ((HashMap<String, HashMap>) saveProjectData.get("questions")).get(map.getKey());
              if (!newMap.entrySet().containsAll(map.getValue().entrySet())) {
                merge.put("questions", map);
              }
            } catch (NullPointerException e) {
              merge.put("questions", map);
            }
          });
      System.out.println(merge);
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
                merge.put("questionQuestions", map);
              }
            } catch (NullPointerException e) {
              merge.put("questionQuestions", map);
            }
          });
      System.out.println(merge);
    }
  }

}
