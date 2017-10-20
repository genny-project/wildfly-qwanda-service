package life.genny.qwanda.service;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;
import life.genny.qwanda.Ask;
import life.genny.qwanda.Question;
import life.genny.qwanda.attribute.Attribute;
import life.genny.qwanda.attribute.AttributeLink;
import life.genny.qwanda.datatype.DataType;
import life.genny.qwanda.entity.BaseEntity;
import life.genny.qwanda.validation.Validation;
import life.genny.qwandautils.GennySheets;

/**
 * This Service bean demonstrate various JPA manipulations of {@link BaseEntity}
 *
 * @author Adam Crow
 */
@Singleton
@Startup
public class StartupService {

  private final static String secret = System.getenv("GOOGLE_CLIENT_SECRET");
  private final static String genny = System.getenv("GOOGLE_SHEETID");
  private final static String channel40 =  "1jNe-MOXx8DFxA2kDeCHjdZ7U-4Fqk1cqyhTgsZvUZ4o";
  static File gennyPath = new File(System.getProperty("user.home"),
      ".credentials/genny");
  static File channelPath = new File(System.getProperty("user.home"),
      ".credentials/channel");
  @Inject
  private BaseEntityService service;

  @PostConstruct
  public void init() {
    Map<String, Runnable> projects= new HashMap<String, Runnable>();
    projects.put("genny", () ->genny(genny, gennyPath));
    projects.put("channel40", () ->genny(channel40, channelPath));
    projects.get("channel40").run();
    
    System.out.println("\n\n**************************************************************************\n\n");
    projects.get("genny").run();
  }

  public  Map<String, Object> genny(String projectType, File path) {
    
    GennySheets sheets = new GennySheets(secret, projectType, path);
    
    Map<String, Validation> gValidations = sheets.validationData();
    gValidations.entrySet().stream().map(tuple -> tuple.getValue()).forEach(validation -> {
      service.insert(validation);
    });
    Map<String, DataType> gDataTypes = sheets.dataTypesData(gValidations);
    Map<String, Attribute> gAttrs = sheets.attributesData(gDataTypes);
    gAttrs.entrySet().stream().map(tuple -> tuple.getValue()).forEach(attr -> {
      service.insert(attr);
    });
    Map<String, BaseEntity> gBes = sheets.baseEntityData();
    gBes.entrySet().stream().map(tuple -> tuple.getValue()).forEach(be -> {
      service.insert(be);
    });
    Map<String, BaseEntity> gAttr2Bes = sheets.attr2BaseEntitys(gAttrs, gBes);
    gAttr2Bes.entrySet().stream().map(tuple -> tuple.getValue()).forEach(be -> {
      System.out.println(be+"***********************");
      service.update(be);
    });
    Map<String, AttributeLink> gAttrLink = sheets.attrLink();
    gAttrLink.entrySet().stream().map(tuple -> tuple.getValue()).forEach(link -> {
      service.insert(link);
    });
    Map<String, BaseEntity> gBes2Bes = sheets.be2BeTarget(gAttrLink, gAttr2Bes);
    gBes2Bes.entrySet().stream().map(tuple -> tuple.getValue()).forEach(tbe -> {
      service.update(tbe);
    });
    Map<String, Question> gQuestions = sheets.questionsData(gAttrs);
    gQuestions.entrySet().stream().map(tuple -> tuple.getValue()).forEach(q -> {
      service.insert(q);
    });
    Map<String, Ask> gAsks = sheets.asksData(gQuestions, gBes);
    gAsks.entrySet().stream().map(tuple -> tuple.getValue()).forEach(ask -> {
      service.insert(ask);
    });
    Map<String, Object> genny = new HashMap<String, Object>();
    genny.put("validations", gValidations);
    genny.put("dataType", gDataTypes);
    genny.put("attributes", gAttrs);
    genny.put("baseEntitys", gBes);
    genny.put("attibutesEntity", gAttr2Bes);
    genny.put("attributeLink", gAttrLink);
    genny.put("basebase", gBes2Bes);
    genny.put("questions", gQuestions);
    genny.put("ask", gAsks);
    
    return genny;
  }
  
}
