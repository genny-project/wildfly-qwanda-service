package life.genny.qwanda.service;

import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;
import javax.persistence.NoResultException;
import javax.persistence.OptimisticLockException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import life.genny.qwanda.attribute.Attribute;
import life.genny.qwanda.attribute.AttributeLink;
import life.genny.qwanda.attribute.EntityAttribute;
import life.genny.qwanda.datatype.DataType;
import life.genny.qwanda.entity.BaseEntity;
import life.genny.qwanda.entity.EntityEntity;
import life.genny.qwanda.exception.BadDataException;
import life.genny.qwanda.validation.Validation;
import life.genny.qwanda.validation.ValidationList;
import life.genny.qwandautils.GennySheets;

/**
 * This Service bean demonstrate various JPA manipulations of {@link BaseEntity}
 *
 * @author Adam Crow
 */
@Singleton
@Startup
public class StartupService {

  private final String g = System.getenv("GOOGLE_CLIENT_SECRET");
  private final String go = System.getenv("GOOGLE_SHEETID");
  File credentialPath = new File(System.getProperty("user.home"),
      ".credentials/sheets.googleapis.com-java-quickstart");

  @Inject
  private BaseEntityService service;

  @PostConstruct
  public void init() {

    final GennySheets gennySheets = new GennySheets(g, go, credentialPath);


    System.out.println(
        "**************************** IMPORTING VALIDATIONS *************************************");

    // Validations
    final Map<String, Validation> validationMap = new HashMap<String, Validation>();

    List<Validation> validations = null;
    try {
      validations = gennySheets.getBeans(Validation.class);
    } catch (final IOException e2) {
      // TODO Auto-generated catch block
      e2.printStackTrace();
    }
    validations.stream().forEach(object -> {

      service.insert(object);
      validationMap.put(object.getCode(), object);
    });

    System.out.println(
        "**************************** IMPORTING DATATYPES *************************************");

    // DataTypes
    final Map<String, DataType> dataTypeMap = new HashMap<String, DataType>();
    List<Map> obj = null;
    try {
      obj = gennySheets.row2DoubleTuples(DataType.class.getSimpleName());
    } catch (final IOException e1) {
      // TODO Auto-generated catch block
      e1.printStackTrace();
    }
    obj.stream().forEach(object -> {
      if (object.get("code") != null) {
        final String code = (String) object.get("code");
        final String name = (String) object.get("name");
        final String validationss = (String) object.get("validations");
        final ValidationList validationList = new ValidationList();
        validationList.setValidationList(new ArrayList<Validation>());
        if (validationss != null) {
          final String[] validationListStr = validationss.split(",");
          for (final String validationCode : validationListStr) {
            validationList.getValidationList().add(validationMap.get(validationCode));
          }
        }
        if (!dataTypeMap.containsKey(code)) {
          final DataType dataType = new DataType(name, validationList);
          dataTypeMap.put(code, dataType);
        }
      }
    });

    System.out.println(
        "**************************** IMPORTING ATTRIBUTES *************************************");

    List<Map> attrs = null;
    try {
      attrs = gennySheets.row2DoubleTuples(Attribute.class.getSimpleName());
    } catch (final IOException e2) {
      // TODO Auto-generated catch block
      e2.printStackTrace();
    }
    attrs.stream().forEach(data -> {
      Attribute attribute = null;
      if (data.get("code") != null) {
        final String datatype = (String) data.get("datatype");
        try {
          System.out.println("ATTRIBUTE:::Code:" + data.get("code") + ":name" + data.get("name")
              + ":datatype:" + data.get("datatype"));
          attribute = service.findAttributeByCode((String) data.get("code"));

        } catch (final NoResultException e) {
          attribute = new Attribute((String) data.get("code"), (String) data.get("name"),
              dataTypeMap.get(datatype));
          // System.out.println("ATTRIBUTE:::Code:" + data.get("code") + ":name" + data.get("name")
          // + ":datatype:" + data.get("datatype") + "##################" + attribute);
          service.insert(attribute);
        } catch (final OptimisticLockException ee) {
          attribute = new Attribute((String) data.get("code"), (String) data.get("name"),
              dataTypeMap.get(datatype));
          // System.out.println("ATTRIBUTE:::Code:" + data.get("code") + ":name" + data.get("name")
          // + ":datatype:" + data.get("datatype") + "##################" + attribute);
          // service.insert(attribute);
        }
      }
    });
    System.out.println(
        "**************************** IMPORTING BASEENTITYS *************************************");

    List<BaseEntity> bes = null;
    try {
      bes = gennySheets.getBeans(BaseEntity.class);
      bes.stream().forEach(object -> {
        service.insert(object);
      });
    } catch (final IOException e2) {
      // TODO Auto-generated catch block
      e2.printStackTrace();
    }


    if (true) {

      // Now link Attributes to Baseentitys
      List<Map> obj2 = null;
      try {
        obj2 = gennySheets.row2DoubleTuples(EntityAttribute.class.getSimpleName());
      } catch (final IOException e1) {
        // TODO Auto-generated catch block
        e1.printStackTrace();
      }
      obj2.stream().forEach(object -> {
        final String beCode = (String) object.get("baseEntityCode");
        final String attributeCode = (String) object.get("attributeCode");
        final String weightStr = (String) object.get("weight");
        final String valueString = (String) object.get("valueString");
        System.out.println("BECode:" + beCode + ":attCode" + attributeCode + ":weight:" + weightStr
            + ": valueString:" + valueString);
        Attribute attribute = null;
        BaseEntity be = null;
        try {
          attribute = service.findAttributeByCode(attributeCode);
          be = service.findBaseEntityByCode(beCode);
          final Double weight = Double.valueOf(weightStr);
          try {
            be.addAttribute(attribute, weight, valueString);
          } catch (final BadDataException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
          }
          service.update(be);
        } catch (final NoResultException e) {

        }
      });

      // Now link be to be
      final AttributeLink linkAttribute2 = new AttributeLink("LNK_CORE", "Parent");
      service.insert(linkAttribute2);

      List<Map> obj3 = null;
      try {
        obj3 = gennySheets.row2DoubleTuples(EntityEntity.class.getSimpleName());
      } catch (final IOException e1) {
        // TODO Auto-generated catch block
        e1.printStackTrace();
      }
      obj3.stream().forEach(object -> {
        final String parentCode = (String) object.get("parentCode");
        final String targetCode = (String) object.get("targetCode");
        object.get("linkCode");
        final String weightStr = (String) object.get("weight");
        object.get("valueString");
        BaseEntity sbe = null;
        BaseEntity tbe = null;
        try {
          sbe = service.findBaseEntityByCode(parentCode);
          tbe = service.findBaseEntityByCode(targetCode);
          final Double weight = Double.valueOf(weightStr);
          sbe.addTarget(tbe, linkAttribute2, weight);
          service.update(tbe);
        } catch (final NoResultException e) {

        } catch (final BadDataException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
      });
    }
  }
}
