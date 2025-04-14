import java.sql.Connection;
import java.sql.DriverManager;
import java.io.*;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletContext;


@SuppressWarnings("serial")
public class ConnectionUtils {
    public static Connection getConnection(ServletConfig config) throws ServletException {
        Connection connection = null;;
        try {
            Class.forName("sun.jdbc.odbc.JdbcOdbcDriver");
            //String url="jdbc:odbc:Base_de_Datos";
            ServletContext context = config.getServletContext();
            
            String dbPath = context.getRealPath("BaseDeDatos.mdb");
            System.out.println("Database path: " + dbPath); // Check Tomcat logs
            
			String url=new String("jdbc:odbc:Driver={Microsoft Access Driver (*.mdb)};Dbq=" + dbPath);
            connection=DriverManager.getConnection(url);
        } catch(Exception e) {
            throw new ServletException("Database connection did fail", e);
        }
        return connection;
    }
    public static Connection getConnection() {
        Connection connection = null;;
        try {
            Class.forName("sun.jdbc.odbc.JdbcOdbcDriver");
            String url="jdbc:odbc:BaseDeDatos";
            connection=DriverManager.getConnection(url); 
        } catch(Exception e) {
            e.printStackTrace();
        }
        return connection;
    }
    public static Connection close(Connection connection) {
        try {
            connection.close(); 
        } catch(Exception e) {
            e.printStackTrace();
        }
        return connection;
    }
}