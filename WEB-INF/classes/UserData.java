import java.util.Vector;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UserData {
    int ID;
    String username;
    int password;
    float balance;
    
    UserData (String ID, String username, int password, float balance) {
        this.ID    = ID;
        this.username  = username;
        this.password   = password;
        this.balance = balance;
    }
    
}
