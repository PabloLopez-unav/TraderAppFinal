import java.io.*;
import javax.servlet.*;
import javax.servlet.http.*;
import java.sql.*;

@SuppressWarnings("serial")
public class Login extends HttpServlet {
    Connection connection;

    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        connection = ConnectionUtils.getConnection(config);
        System.out.println("Login");
    }

    protected void doGet(HttpServletRequest req, HttpServletResponse res) 
            throws ServletException, IOException {
        String login = req.getParameter("login");
        String password = req.getParameter("password");
        
        if (login == null || password == null) {
            System.out.println("LoginHTML");
            res.sendRedirect("Login.html");
            return;
        }
        
        String loggedUser = check(connection, login, password);
        if (loggedUser != null) {
            HttpSession session = req.getSession(true);
            session.setAttribute("username", loggedUser);
            res.sendRedirect("Menu");
        } else {
            System.out.println("LoginFailedHTML");
            res.sendRedirect("LoginFailed.html");
        }
    }

    String check(Connection conn, String username, String password) {
        try {
            String sql = "SELECT Username FROM Users WHERE Username=? AND Password=?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, username);
            stmt.setString(2, password);
            
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getString("Username");
            }
            rs.close();
            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
}