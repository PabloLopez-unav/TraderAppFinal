import java.io.*;
import java.sql.*;
import javax.servlet.*;
import javax.servlet.http.*;

@SuppressWarnings("serial")
public class Historico extends HttpServlet {
    Connection connection;

    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        connection = ConnectionUtils.getConnection(config);
    }

    public void doGet(HttpServletRequest req, HttpServletResponse res) 
            throws ServletException, IOException {
        
        HttpSession session = req.getSession(false); 
        
        String username = null;
        if (session != null) {
            username = (String) session.getAttribute("username");
        }
        if (username == null) {
            res.sendRedirect("Login");
            return;
        }

        res.setContentType("text/html");
        PrintWriter toClient = res.getWriter();
        toClient.println("<html><head><title>Historico</title>");
        toClient.println("<style>table { border-collapse: collapse; width: 80%; margin: 20px auto; }");
        toClient.println("th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }");
        toClient.println("th { background-color: #f2f2f2; }");
        toClient.println("tr:nth-child(even) { background-color: #f9f9f9; }</style></head>");
        toClient.println("<body><h1 style='text-align:center'>Historico</h1>");
        toClient.println("<div style='text-align:center'>");

        try {
            int filterId;
            String userSql = "SELECT IDUser FROM Users WHERE Username = ?";
            PreparedStatement userStatement = connection.prepareStatement(userSql);
            userStatement.setString(1, username);
            ResultSet userResult = userStatement.executeQuery();
            
            if (!userResult.next()) {
                toClient.println("<h2>Error: User not found</h2>");
                toClient.println("</body></html>");
                userStatement.close();
                toClient.close();
                return;
            }
            
            filterId = userResult.getInt("IDUser");
            userStatement.close();

            // Get transaction history
            String sql = "SELECT * FROM Transactions WHERE IDUser = ? ORDER BY Date DESC";
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setInt(1, filterId);
            ResultSet resultSet = statement.executeQuery();
            
            toClient.println("<table>");
            toClient.println("<tr><th>Indice</th><th>Nombre</th>");
            toClient.println("<th>Acciones</th><th>Precio</th><th>Fecha</th><th>Tipo</th></tr>");
            
            int rowNumber = 1;
            while(resultSet.next()) {
                toClient.println("<tr>");
                toClient.println("<td>" + rowNumber++ + "</td>");
                toClient.println("<td>" + resultSet.getString("StockName") + "</td>");
                toClient.println("<td>" + resultSet.getInt("Num") + "</td>");
                toClient.println("<td>" + String.format("%.2f$", resultSet.getDouble("Price")) + "</td>");
                toClient.println("<td>" + resultSet.getString("Date") + "</td>");
                
                boolean isBought = resultSet.getBoolean("Bought/Sell");
                toClient.println("<td>" + (isBought ? "Compra" : "Venta") + "</td>");
                
                toClient.println("</tr>");
            }
            
            toClient.println("</table>");
            statement.close();
        } catch(SQLException e) {
            toClient.println("<h2>Database error:</h2>");
            toClient.println("<pre>" + e.toString() + "</pre>");
        }
        
        toClient.println("</body></html>");
        toClient.close();
    }

    public void destroy() {
        try {
            if(connection != null) connection.close();
        } catch(SQLException e) {
            System.err.println("Error closing connection: " + e);
        }
    }
}