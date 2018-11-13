package life.genny.qwanda.util;

import org.hibernate.dialect.function.SQLFunctionTemplate;
import org.hibernate.type.IntegerType;

public class MySQLDialect2 extends org.hibernate.dialect.MySQL5Dialect {

  public MySQLDialect2() {
    super();
    registerFunction("bitwise_and", new SQLFunctionTemplate(IntegerType.INSTANCE, "(?1 & ?2)"));

  }

}