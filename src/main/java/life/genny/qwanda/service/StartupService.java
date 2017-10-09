package life.genny.qwanda.service;

import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import life.genny.qwanda.attribute.Attribute;
import life.genny.qwanda.attribute.AttributeLink;
import life.genny.qwanda.attribute.AttributeText;
import life.genny.qwanda.entity.BaseEntity;
import life.genny.qwanda.exception.BadDataException;
import life.genny.qwandautils.GennySheets;

/**
 * This Service bean demonstrate various JPA manipulations of {@link BaseEntity}
 *
 * @author Adam Crow
 */
@Singleton
@Startup
public class StartupService {


  public static final String SPREADSHEET_URL =
      "https://spreadsheets.google.com/feeds/spreadsheets/1VSXJUn8_BHG1aW0DQrFDnvLjx_jxcNiD33QzqO5D-jc";
  // Fill
  // in
  // google
  // spreadsheet
  // URI

  //
  // public static final String RANGE = "!A1:ZZ";
  // // public static final String CLIENT_SECRET = System.getenv("GOOGLE_CLIENT_SECRET");
  // public static final String CLIENT_SECRET =
  // "{\"installed\":{\"client_id\":\"260075856207-9d7a02ekmujr2bh7i53dro28n132iqhe.apps.googleusercontent.com\",\"project_id\":\"genny-sheets-181905\",\"auth_uri\":\"https://accounts.google.com/o/oauth2/auth\",\"token_uri\":\"https://accounts.google.com/o/oauth2/token\",\"auth_provider_x509_cert_url\":\"https://www.googleapis.com/oauth2/v1/certs\",\"client_secret\":\"vgXEFRgQvh3_t_e5Hj-eb6IX\",\"redirect_uri\":[\"http://localhost\"]}}";
  // public static final String APPLICATION_NAME = "Google Sheets API Java Quickstart";
  // // public static final String SHEETID = System.getenv("GOOGLE_SHEETID");
  // public static final String SHEETID = "1VSXJUn8_BHG1aW0DQrFDnvLjx_jxcNiD33QzqO5D-jc";
  // /** Directory to store user credentials for this application. */
  // public static final java.io.File DATA_STORE_DIR = new java.io.File(
  // System.getProperty("user.home"), ".credentials/sheets.googleapis.com-java-quickstart");
  //
  // /** Global instance of the {@link FileDataStoreFactory}. */
  // private static FileDataStoreFactory DATA_STORE_FACTORY;
  //
  // /** Global instance of the JSON factory. */
  // private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
  //
  // /** Global instance of the HTTP transport. */
  // private static HttpTransport HTTP_TRANSPORT;
  //
  // /**
  // * Global instance of the scopes required by this quickstart.
  // *
  // * If modifying these scopes, delete your previously saved credentials at
  // * ~/.credentials/sheets.googleapis.com-java-quickstart
  // */
  // private static final List<String> SCOPES = Arrays.asList(SheetsScopes.SPREADSHEETS);
  //
  // public static Sheets getSheetsService() throws IOException {
  // final Credential credential = authorize();
  // return new Sheets.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
  // .setApplicationName(APPLICATION_NAME).build();
  // }
  //
  // public static Credential authorize() throws IOException {
  // // Load client secrets.
  // System.out.println(System.getProperty("user.home"));
  // final InputStream in = IOUtils.toInputStream(CLIENT_SECRET, "UTF-8");
  //
  // System.getenv("JBOSS_HOME");
  //
  // // final GoogleClientSecrets clientSecrets =
  // // GoogleClientSecrets.load(new JacksonFactory(), new FileReader(fileName));
  //
  // // FileInputStream in = null;
  // // try {
  // // in = new FileInputStream(fileName);
  // // if (in == null) {
  // // throw new IllegalStateException(
  // // "Not able to find the file /google/sheets.googleapis.com-java-quickstart");
  // // }
  // // System.out.println("Got genny_sheet.json");
  // // } catch (final FileNotFoundException e) {
  // // e.printStackTrace();
  // // }
  // final GoogleClientSecrets clientSecrets =
  // GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));
  //
  // // Build flow and trigger user authorization request.
  // final GoogleAuthorizationCodeFlow flow =
  // // new GoogleAuthorizationCodeFlow.Builder(HTTP_TRANSPORT, JSON_FACTORY, clientSecrets,
  // // SCOPES)
  // // .setDataStoreFactory(DATA_STORE_FACTORY).build();
  // new GoogleAuthorizationCodeFlow.Builder(HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
  // .setDataStoreFactory(DATA_STORE_FACTORY).setAccessType("offline").build();
  // final LocalServerReceiver localReceiver =
  // new LocalServerReceiver.Builder().setPort(8998).setHost("localhost").build();
  //
  // final Credential credential =
  // new AuthorizationCodeInstalledApp(flow, localReceiver).authorize("user");
  // System.out.println("Credentials saved to " + DATA_STORE_DIR.getAbsolutePath());
  // return credential;
  // }
  //
  // static {
  // try {
  // HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
  // DATA_STORE_FACTORY = new FileDataStoreFactory(DATA_STORE_DIR);
  // } catch (final Throwable t) {
  // t.printStackTrace();
  // System.exit(1);
  // }
  // }
  // static Gson g = new Gson();
  //
  // public static <T> List<T> transform(final List<List<Object>> values, final Class object) {
  // final List<String> keys = new ArrayList<String>();
  // final List<T> k = new ArrayList<T>();
  // for (final Object key : values.get(0)) {
  // keys.add((String) key);
  // }
  // // values.stream().peek(act-> System.out.println(act+"ok1")).
  // values.remove(0);
  // for (final List row : values) {
  // final Map<String, Object> mapper = new HashMap<String, Object>();
  // for (int counter = 0; counter < row.size(); counter++) {
  // mapper.put(keys.get(counter), row.get(counter));
  // }
  // final T lo = (T) g.fromJson(mapper.toString(), object);
  // k.add(lo);
  // }
  // return k;
  // }
  //
  // public static <T> List<T> getBeans(final Class clazz) throws IOException {
  // final Sheets service = getSheetsService();
  // final String range = clazz.getSimpleName() + RANGE;
  // final com.google.api.services.sheets.v4.model.ValueRange response =
  // service.spreadsheets().values().get(SHEETID, range).execute();
  // final List<List<Object>> values = response.getValues();
  // return transform(values, clazz);
  // }

  @Inject
  private BaseEntityService service;

  @PostConstruct
  public void init() {
    try {
      System.out.println("STARTING !!!!");
      // Set up a dummy Attribute
      final Attribute attributeImageUrl =
          new AttributeText(AttributeText.getDefaultCodePrefix() + "IMAGE_URL", "Image Url");
      service.insert(attributeImageUrl);

      final AttributeLink linkAttribute = new AttributeLink("LNK_CORE", "Parent");
      service.insert(linkAttribute);


      // create a users directory and a contacts directory
      // and link these users to each
      System.out.println(
          "###############################Google Sheets#############################################");


      final List<BaseEntity> bes = GennySheets.getBaseEntitys();

      for (final BaseEntity be : bes) {
        be.setCreated(LocalDateTime.now(Clock.systemUTC()));
        be.addAttribute(attributeImageUrl, 1.0, "dir-ico");
        System.out.println("BaseEntity:" + be);
        service.insert(be);

      }

      // List<List<Object>> cells;
      // try {
      // cells = GennySheets.getStrings(BaseEntity.class.getSimpleName(), "!A1:ZZ");
      // for (final List<Object> objList : cells) {
      // System.out.println("Row");
      // for (final Object obj : objList) {
      // System.out.println("CELL:" + obj);
      // }
      //
      // // final BaseEntity baseEntity = new BaseEntity();
      // }
      // } catch (final IOException e) {
      // // TODO Auto-generated catch block
      // e.printStackTrace();
      // }



      System.out.println(
          "#######################################################################################");



      // create a users directory and a contacts directory
      // and link these users to each

      // create a group attribute (This is because of a json/hibernate lazy issue)



      // final Group root = new Group("ROOT", "Root");
      // root.addAttribute(attributeImageUrl, 1.0, "dir-ico");
      // service.insert(root);
      //
      // final Group dashboard = new Group("DASHBOARD", "Dashboard");
      // dashboard.addAttribute(attributeImageUrl, 1.0, "dir-ico");
      // service.insert(dashboard);
      //
      // final Group driverView = new Group("DRIVER_VIEW", "Driver View");
      // driverView.addAttribute(attributeImageUrl, 1.0, "dir-ico");
      // service.insert(driverView);
      //
      // final Group ownerView = new Group("OWNER_VIEW", "Owner View");
      // ownerView.addAttribute(attributeImageUrl, 1.0, "dir-ico");
      // service.insert(ownerView);
      //
      // final Group loads = new Group("LOADS", "Loads");
      // loads.addAttribute(attributeImageUrl, 1.0, "dir-ico");
      // service.insert(loads);
      //
      //
      // final Group contacts = new Group("CONTACTS", "Contacts");
      // contacts.addAttribute(attributeImageUrl, 1.0, "dir-ico");
      // service.insert(contacts);
      //
      // final Group people = new Group("PEOPLE", "People");
      // people.addAttribute(attributeImageUrl, 1.0, "dir-ico");
      // service.insert(people);
      //
      // final Group companys = new Group("COMPANYS", "Companies");
      // companys.addAttribute(attributeImageUrl, 1.0, "dir-ico");
      // service.insert(companys);
      //
      // final Group users = new Group("USERS", "Users");
      // users.addAttribute(attributeImageUrl, 1.0, "dir-ico");
      // service.insert(users);
      //
      // final Group settings = new Group("SETTINGS", "Settings");
      // settings.addAttribute(attributeImageUrl, 1.0, "dir-ico");
      // service.insert(settings);
      //


      // final AttributeLink linkAttribute = new AttributeLink("LNK_CORE", "Parent");
      // service.insert(linkAttribute);
      //
      // root.addTarget(dashboard, linkAttribute, 1.0);
      // dashboard.addTarget(driverView, linkAttribute, 1.0);
      // dashboard.addTarget(ownerView, linkAttribute, 0.8);
      // root.addTarget(loads, linkAttribute, 0.2);
      // root.addTarget(contacts, linkAttribute, 1.0);
      // contacts.addTarget(people, linkAttribute, 0.8);
      // contacts.addTarget(companys, linkAttribute, 0.8);
      // contacts.addTarget(users, linkAttribute, 0.2);
      // root.addTarget(settings, linkAttribute, 0.2);
      // service.update(dashboard);
      // service.update(root);
      //
      // // Adding Live View Child items
      // final Group available = new Group("AVAILABLE", "Available");
      // available.addAttribute(attributeImageUrl, 1.0, "dir-ico");
      // service.insert(available);
      //
      //
      // // Adding Live View Child items
      // final Group pending = new Group("PENDING", "Pending");
      // pending.addAttribute(attributeImageUrl, 1.0, "dir-ico");
      // service.insert(pending);
      //
      // final Group quote = new Group("QUOTE", "Quote");
      // quote.addAttribute(attributeImageUrl, 1.0, "dir-ico");
      // service.insert(quote);
      //
      //
      // final Group accepted = new Group("ACCEPTED", "Accepted");
      // accepted.addAttribute(attributeImageUrl, 1.0, "dir-ico");
      // service.insert(accepted);
      //
      // final Group dispatched = new Group("DISPATCHED", "Dispatched");
      // dispatched.addAttribute(attributeImageUrl, 1.0, "dir-ico");
      // service.insert(dispatched);
      //
      // final Group intransit = new Group("IN-TRANSIT", "In-Transit");
      // intransit.addAttribute(attributeImageUrl, 1.0, "dir-ico");
      // service.insert(intransit);
      //
      // final Group atdestination = new Group("AT-DESTINATION", "At-Destination");
      // atdestination.addAttribute(attributeImageUrl, 1.0, "dir-ico");
      // service.insert(atdestination);
      //
      // final Group delivered = new Group("DELIVERED", "Delivered");
      // delivered.addAttribute(attributeImageUrl, 1.0, "dir-ico");
      // service.insert(delivered);
      //
      // driverView.addTarget(available, linkAttribute, 1.0);
      // driverView.addTarget(quote, linkAttribute, 1.0);
      // driverView.addTarget(accepted, linkAttribute, 1.0);
      // driverView.addTarget(dispatched, linkAttribute, 1.0);
      // driverView.addTarget(intransit, linkAttribute, 1.0);
      // driverView.addTarget(atdestination, linkAttribute, 1.0);
      // driverView.addTarget(delivered, linkAttribute, 1.0);
      // service.update(driverView);
      //
      // ownerView.addTarget(pending, linkAttribute, 1.0);
      // ownerView.addTarget(quote, linkAttribute, 1.0);
      // ownerView.addTarget(accepted, linkAttribute, 1.0);
      // ownerView.addTarget(dispatched, linkAttribute, 1.0);
      // ownerView.addTarget(intransit, linkAttribute, 1.0);
      // ownerView.addTarget(atdestination, linkAttribute, 1.0);
      // ownerView.addTarget(delivered, linkAttribute, 1.0);
      // service.update(ownerView);
      //
      // // contacts.addTarget(, linkAttribute, weight);
      //
      // // Adding Settings items
      // final Group yourDetils = new Group("YOUR_DETAILS", "Your-Details");
      // yourDetils.addAttribute(attributeImageUrl, 1.0, "dir-ico");
      // service.insert(yourDetils);
      //
      // final Group loadTypes = new Group("LOAD_TYPES", "Load-Types");
      // loadTypes.addAttribute(attributeImageUrl, 1.0, "dir-ico");
      // service.insert(loadTypes);
      //
      // final Group truckSpec = new Group("TRUCK_SPECIFICATION", "Truck-Specification");
      // truckSpec.addAttribute(attributeImageUrl, 1.0, "dir-ico");
      // service.insert(truckSpec);
      //
      // final Group documents = new Group("DOCUMENTS", "Documents");
      // documents.addAttribute(attributeImageUrl, 1.0, "dir-ico");
      // service.insert(documents);
      //
      // final Group updatePwd = new Group("UPDATE_PASSWORD", "Update-Password");
      // updatePwd.addAttribute(attributeImageUrl, 1.0, "dir-ico");
      // service.insert(updatePwd);
      //
      // final Group paymentDetails = new Group("PAYMENT_DETAILS", "Payment-Details");
      // paymentDetails.addAttribute(attributeImageUrl, 1.0, "dir-ico");
      // service.insert(paymentDetails);
      //
      // settings.addTarget(yourDetils, linkAttribute, 1.0);
      // settings.addTarget(loadTypes, linkAttribute, 1.0);
      // settings.addTarget(truckSpec, linkAttribute, 1.0);
      // settings.addTarget(documents, linkAttribute, 1.0);
      // settings.addTarget(updatePwd, linkAttribute, 1.0);
      // settings.addTarget(paymentDetails, linkAttribute, 1.0);
      // service.update(settings);
      //
      //
      // // Adding Loads child item
      // final Group viewLoads = new Group("VIEW_LOADS", "View-Loads");
      // viewLoads.addAttribute(attributeImageUrl, 1.0, "dir-ico");
      // service.insert(viewLoads);
      //
      // final Group postLoads = new Group("POST_LOADS", "Post-Loads");
      // postLoads.addAttribute(attributeImageUrl, 1.0, "dir-ico");
      // service.insert(postLoads);
      //
      // loads.addTarget(viewLoads, linkAttribute, 1.0);
      // loads.addTarget(postLoads, linkAttribute, 1.0);
      // service.update(loads);
      //
      // // Adding Users child item
      // final Group admin = new Group("ADMIN", "Admin");
      // admin.addAttribute(attributeImageUrl, 1.0, "dir-ico");
      // service.insert(admin);
      //
      // final Group driver = new Group("DRIVER", "Pacific-Driver");
      // driver.addAttribute(attributeImageUrl, 1.0, "dir-ico");
      // service.insert(driver);
      //
      // final Group loadOwner = new Group("LOAD_OWNER", "Load-Owner");
      // loadOwner.addAttribute(attributeImageUrl, 1.0, "dir-ico");
      // service.insert(loadOwner);
      //
      // users.addTarget(admin, linkAttribute, 1.0);
      // users.addTarget(driver, linkAttribute, 1.0);
      // users.addTarget(loadOwner, linkAttribute, 1.0);
      // service.update(users);
      //
      //
      // // Adding Transport Company child item
      // final Group aurizon = new Group("AURIZON", "Aurizon");
      // aurizon.addAttribute(attributeImageUrl, 1.0, "dir-ico");
      // service.insert(aurizon);
      //
      // final Group pacificNational = new Group("PACIFIC_NATIONAL", "Pacific-National");
      // pacificNational.addAttribute(attributeImageUrl, 1.0, "dir-ico");
      // service.insert(pacificNational);
      //
      // final Group linFox = new Group("LINFOX", "linfox");
      // linFox.addAttribute(attributeImageUrl, 1.0, "dir-ico");
      // service.insert(linFox);
      //
      // final Group sctLogistics = new Group("SCT_LOGISTICS", "SCT-Logistics");
      // sctLogistics.addAttribute(attributeImageUrl, 1.0, "dir-ico");
      // service.insert(sctLogistics);
      //
      // final Group glenGroup = new Group("GLEN_CAMERON_GROUP", "Glen-Cameron-Group");
      // glenGroup.addAttribute(attributeImageUrl, 1.0, "dir-ico");
      // service.insert(glenGroup);
      //
      // companys.addTarget(aurizon, linkAttribute, 1.0);
      // companys.addTarget(pacificNational, linkAttribute, 1.0);
      // companys.addTarget(linFox, linkAttribute, 1.0);
      // companys.addTarget(sctLogistics, linkAttribute, 1.0);
      // companys.addTarget(glenGroup, linkAttribute, 1.0);
      // service.update(companys);
      //
      // // check if groups exist
      // final List<Group> parentGroupList = new ArrayList<Group>();
      // parentGroupList.add(contacts);
      // parentGroupList.add(users);
      //
      // // try {
      // // service.importKeycloakUsers(parentGroupList, linkAttribute);
      // // } catch (Exception e) {
      // // System.out.println("Unable to load in Keycloak Users");
      // // }
      //
      // final Attribute attributeFirstname =
      // new AttributeText(AttributeText.getDefaultCodePrefix() + "FIRSTNAME", "Firstname");
      // final Attribute attributeLastname =
      // new AttributeText(AttributeText.getDefaultCodePrefix() + "LASTNAME", "Lastname");
      //
      // // Attribute attributeFirstname = service.findAttributeByCode("PRI_FIRSTNAME");
      // // Attribute attributeLastname = service.findAttributeByCode("PRI_LASTNAME");
      // final Attribute attributeBirthdate = new AttributeDateTime(
      // AttributeText.getDefaultCodePrefix() + "BIRTHDATE", "Date of Birth");
      // final Attribute attributeKeycloakId =
      // new AttributeText(AttributeText.getDefaultCodePrefix() + "KEYCLOAK_ID", "Keycloak ID");
      //
      // service.insert(attributeFirstname);
      // service.insert(attributeLastname);
      // service.insert(attributeBirthdate);
      // service.insert(attributeKeycloakId);
      //
      // final Person person = new Person(Person.getDefaultCodePrefix() + "USER1", "James Bond");
      //
      // person.addAttribute(attributeFirstname, 1.0, "Sean");
      // person.addAttribute(attributeLastname, 0.8, "Connery");
      // person.addAttribute(attributeBirthdate, 0.6, LocalDateTime.of(1989, 1, 7, 16, 0));
      // person.addAttribute(attributeKeycloakId, 0.0, "6ea705a3-f523-45a4-aca3-dc22e6c24f4f");
      //
      // service.insert(person);
      //
      // // create test questions
      // final Question questionFirstname = new Question(Question.getDefaultCodePrefix() +
      // "FIRSTNAME",
      // "Firstname", attributeFirstname);
      // final Question questionLastname =
      // new Question(Question.getDefaultCodePrefix() + "LASTNAME", "Lastname", attributeLastname);
      // final Question questionBirthdate = new Question(Question.getDefaultCodePrefix() +
      // "BIRTHDATE",
      // "Birthdate", attributeBirthdate);
      //
      // service.insert(questionFirstname);
      // service.insert(questionLastname);
      // service.insert(questionBirthdate);
      //
      // // Now ask the question!
      //
      // final Ask askFirstname = new Ask(questionFirstname, person, person);
      // final Ask askLastname = new Ask(questionLastname, person, person);
      // final Ask askBirthdate = new Ask(questionBirthdate, person, person);
      //
      // service.insert(askFirstname);
      // service.insert(askLastname);
      // service.insert(askBirthdate);
      //
      // final Answer answerFirstname = new Answer(askFirstname, "Bob");
      // final Answer answerLastname = new Answer(askLastname, "Console");
      // final Answer answerBirthdate = new Answer(askBirthdate, "1989-01-07T16:00:00");
      //
      // person.addAnswer(answerFirstname, 1.0);
      // person.addAnswer(answerLastname, 1.0);
      // person.addAnswer(answerBirthdate, 1.0);
      //
      // askFirstname.add(answerFirstname);
      // askLastname.add(answerLastname);
      // askBirthdate.add(answerBirthdate);
      //
      // service.update(askFirstname);
      // service.update(askLastname);
      // service.update(askBirthdate);
      //
      // service.update(person);
      //
      // final Question question =
      // service.findQuestionByCode(Question.getDefaultCodePrefix() + "FIRSTNAME");
      // System.out.println(question);
      //
      // final List<Ask> asks = service.findAsksByTargetBaseEntityId(person.getId());
      // System.out.println(asks);

    } catch (final BadDataException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

  }
}
