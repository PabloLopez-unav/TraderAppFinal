import java.io.*;
import javax.servlet.*;
import javax.servlet.http.*;
import java.sql.*;

public class Menu extends HttpServlet {
    Connection connection;
    
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        connection = ConnectionUtils.getConnection(config);
        System.out.println("Menu");
    }
    
    public void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException { 
        HttpSession session = request.getSession(false); 
        
        if (session == null){
            System.out.println("NoSession");
            response.sendRedirect("Login");
            return;
        }
        
        System.out.println("TheresSession");
        String username = (String) session.getAttribute("username");
        if (username == null) {
            System.out.println("FailedUsername");
            response.sendRedirect("Login");
            return;
        }
        
        String sessionId = session.getId();
        try {
            int userId = -1;
            String userSql = "SELECT IDUser FROM Users WHERE Username = ?";
            try (PreparedStatement psUser = connection.prepareStatement(userSql)) {
                psUser.setString(1, username);
                try (ResultSet rs = psUser.executeQuery()) {
                    if (rs.next()) {
                        userId = rs.getInt("IDUser");
                    }
                }
            }

            if (userId != -1) {
                long now = System.currentTimeMillis();
                long truncated = (now / 1000) * 1000;
                Timestamp ts = new Timestamp(truncated);
                
                String insertSql = "INSERT INTO Logins (IDUser, Fecha, SesionID) VALUES (?, ?, ?)";
                try (PreparedStatement psIns = connection.prepareStatement(insertSql)) {
                    psIns.setInt(1, userId);
                    psIns.setTimestamp(2, ts);
                    psIns.setString(3, sessionId);
                    psIns.executeUpdate();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        response.sendRedirect("Menu.html");
    }
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException { 
        doGet( request, response);
    }
    
    public void destroy() {
        // No hay recursos globales que cerrar
    }
}