import java.io.*;
import java.sql.*;
import javax.servlet.*;
import javax.servlet.http.*;

@SuppressWarnings("serial")
public class Cartera extends HttpServlet {
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
        }
        
        res.setContentType("text/html");
        PrintWriter toClient = res.getWriter();
        toClient.println(Utils.header("Login"));

        int filterId = 2;
        try {
            String idParam = req.getParameter("IDUser");
            if (idParam != null) {
                filterId = Integer.parseInt(idParam);
                if (filterId < 1) filterId = 1;
            }
        } catch (NumberFormatException e) {
            filterId = 1;
        }

        // HTML header
        toClient.println("<html><head><title>Portfolio</title>");
        toClient.println("<style>table { border-collapse: collapse; width: 80%; margin: 20px auto; }");
        toClient.println("th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }");
        toClient.println("th { background-color: #f2f2f2; }");
        toClient.println("tr:nth-child(even) { background-color: #f9f9f9; }</style></head>");
        toClient.println("<body><h1 style='text-align:center'>Cartera Personal</h1>");
        toClient.println("<div style='text-align:center'>");
        
        //toClient.println("<form method='get'>");
        //toClient.println("Filter by User ID: <input type='number' name='IDUser' min='1' value='" + filterId + "'>");
        //toClient.println("<input type='submit' value='Filter'>");
        //toClient.println("</form></div>");

        try {
            // Use PreparedStatement to prevent SQL injection
            String sql = "SELECT * FROM Portfolio WHERE IDUser = ?";
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setInt(1, filterId);
            
            ResultSet resultSet = statement.executeQuery();
            
            // Create table
            toClient.println("<table>");
            toClient.println("<tr><th>Indice</th><th>Nombre</th>");
            toClient.println("<th>Acciones</th><th>Precio</th><th>Fecha</th></tr>");
            
            // Process results with sequential numbering
            int rowNumber = 1;
            while(resultSet.next()) {
                toClient.println("<tr>");
                toClient.println("<td>" + rowNumber++ + "</td>");
                toClient.println("<td>" + resultSet.getString("StockName") + "</td>");
                toClient.println("<td>" + resultSet.getInt("Num") + "</td>");
                toClient.println("<td>" + resultSet.getDouble("Price") + "</td>");
                toClient.println("<td>" + resultSet.getString("Date") + "</td>");
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