import javax.servlet.*;
import javax.servlet.http.*;
import java.io.*;
import java.sql.*;

@SuppressWarnings("serial")
public class VenderServlet extends HttpServlet {
    Connection connection;

    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        connection = ConnectionUtils.getConnection(config);
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        String username = null;
        if (session != null) {
            username = (String) session.getAttribute("username");
        }
        if (username == null) {
            response.sendRedirect("Login");
            return;
        }

        response.setContentType("application/json");
        PrintWriter out = response.getWriter();

        try {
            int userId = getUserId(username);
            if (userId == -1) {
                out.write("[]");
                return;
            }

            String sql = "SELECT StockName, Num, Price, [Date] FROM Portfolio WHERE IDUser = ?";
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setInt(1, userId);
            ResultSet resultSet = statement.executeQuery();

            // Generar JSON manualmente
            StringBuilder json = new StringBuilder("[");
            boolean first = true;

            while(resultSet.next()) {
                if (!first) {
                    json.append(",");
                }
                json.append("{")
                        .append("\"StockName\":\"").append(resultSet.getString("StockName")).append("\",")
                        .append("\"Num\":").append(resultSet.getInt("Num")).append(",")
                        .append("\"Price\":").append(resultSet.getDouble("Price")).append(",")
                        .append("\"Date\":\"").append(resultSet.getString("Date")).append("\"")
                        .append("}");
                first = false;
            }
            json.append("]");

            out.write(json.toString());
            statement.close();
        } catch(SQLException e) {
            out.write("[]");
            e.printStackTrace();
        }
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        PrintWriter out = response.getWriter();
        HttpSession session = request.getSession(false);

        if (session == null || session.getAttribute("username") == null) {
            out.write("{\"success\": false, \"message\": \"Usuario no autenticado\"}");
            return;
        }

        String username = (String) session.getAttribute("username");

        try {
            // Leer JSON manualmente
            BufferedReader reader = request.getReader();
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            String json = sb.toString();

            // Parsear datos manualmente
            String accion = json.split("\"accion\":\"")[1].split("\"")[0];
            int cantidad = Integer.parseInt(json.split("\"cantidad\":")[1].split(",")[0]);
            double precio = Double.parseDouble(json.split("\"precio\":")[1].split("}")[0]);

            int userId = getUserId(username);
            if (userId == -1) {
                out.write("{\"success\": false, \"message\": \"Usuario no válido\"}");
                return;
            }

            if (!tieneAccionesSuficientes(userId, accion, cantidad)) {
                out.write("{\"success\": false, \"message\": \"No tienes suficientes acciones para vender\"}");
                return;
            }

            registrarTransaccion(userId, accion, cantidad, precio, false);
            actualizarPortfolio(userId, accion, -cantidad, precio);
            actualizarSaldo(userId, cantidad * precio);

            out.write("{\"success\": true, \"message\": \"Venta realizada con éxito\"}");

        } catch (Exception e) {
            out.write("{\"success\": false, \"message\": \"Error: " + e.getMessage().replace("\"", "\\\"") + "\"}");
            e.printStackTrace();
        }
    }

    // --- Métodos auxiliares ---
    private int getUserId(String username) throws SQLException {
        String sql = "SELECT IDUser FROM Users WHERE Username = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            return rs.next() ? rs.getInt("IDUser") : -1;
        }
    }

    private boolean tieneAccionesSuficientes(int userId, String accion, int cantidad) throws SQLException {
        String sql = "SELECT Num FROM Portfolio WHERE IDUser = ? AND StockName = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setString(2, accion);
            ResultSet rs = stmt.executeQuery();
            return rs.next() && rs.getInt("Num") >= cantidad;
        }
    }

    private void registrarTransaccion(int userId, String accion, int cantidad, double precio, boolean esCompra) throws SQLException {
        String fechaHoy = new java.text.SimpleDateFormat("#MM/dd/yyyy#").format(new java.util.Date());
        String sql = "INSERT INTO Transactions (IDUser, StockName, Num, Price, [Date], BoughtSell) VALUES (?,?,?,?," + fechaHoy + ",?)";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setString(2, accion);
            stmt.setInt(3, cantidad);
            stmt.setDouble(4, precio);
            stmt.setBoolean(5, esCompra);
            stmt.executeUpdate();
        }
    }

    private void actualizarPortfolio(int userId, String accion, int cantidad, double precio) throws SQLException {
        String checkSql = "SELECT Num FROM Portfolio WHERE IDUser = ? AND StockName = ?";
        try (PreparedStatement stmt = connection.prepareStatement(checkSql)) {
            stmt.setInt(1, userId);
            stmt.setString(2, accion);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                int nuevaCantidad = rs.getInt("Num") + cantidad;
                if (nuevaCantidad <= 0) {
                    String deleteSql = "DELETE FROM Portfolio WHERE IDUser = ? AND StockName = ?";
                    try (PreparedStatement deleteStmt = connection.prepareStatement(deleteSql)) {
                        deleteStmt.setInt(1, userId);
                        deleteStmt.setString(2, accion);
                        deleteStmt.executeUpdate();
                    }
                } else {
                    String fechaHoy = new java.text.SimpleDateFormat("#MM/dd/yyyy#").format(new java.util.Date());
                    String updateSql = "UPDATE Portfolio SET Num = ?, Price = ?, [Date] = " + fechaHoy + " WHERE IDUser = ? AND StockName = ?";
                    try (PreparedStatement updateStmt = connection.prepareStatement(updateSql)) {
                        updateStmt.setInt(1, nuevaCantidad);
                        updateStmt.setDouble(2, precio);
                        updateStmt.setInt(3, userId);
                        updateStmt.setString(4, accion);
                        updateStmt.executeUpdate();
                    }
                }
            }
        }
    }

    private void actualizarSaldo(int userId, double cambio) throws SQLException {
        String sql = "UPDATE Users SET Balance = Balance + ? WHERE IDUser = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setDouble(1, cambio);
            stmt.setInt(2, userId);
            stmt.executeUpdate();
        }
    }

    public void destroy() {
        try {
            if (connection != null) connection.close();
        } catch (SQLException e) {
            System.err.println("Error al cerrar la conexión: " + e.getMessage());
        }
    }
}