import javax.servlet.*;
import javax.servlet.http.*;
import java.io.*;
import java.sql.*;

@SuppressWarnings("serial")
public class ComprarServlet extends HttpServlet {
    Connection connection;

    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        connection = ConnectionUtils.getConnection(config);
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        PrintWriter out = response.getWriter();
        HttpSession session = request.getSession(false);

        // Verificar sesión
        if (session == null || session.getAttribute("username") == null) {
            out.write("{\"success\": false, \"message\": \"Usuario no autenticado\"}");
            return;
        }

        String username = (String) session.getAttribute("username");

        try {
            // Leer datos JSON del request (simplificado)
            BufferedReader reader = request.getReader();
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            String json = sb.toString();

            String accion = json.split("\"accion\":\"")[1].split("\"")[0];
            int cantidad = Integer.parseInt(json.split("\"cantidad\":")[1].split(",")[0]);
            double precio = Double.parseDouble(json.split("\"precio\":")[1].split("}")[0]);

            // 1. Obtener ID del usuario
            int userId = getUserId(username);
            if (userId == -1) {
                out.write("{\"success\": false, \"message\": \"Usuario no válido\"}");
                return;
            }

            // 2. Verificar saldo
            if (!tieneSaldoSuficiente(userId, cantidad * precio)) {
                out.write("{\"success\": false, \"message\": \"Saldo insuficiente\"}");
                return;
            }

            // 3. Registrar transacción y actualizar saldo
            registrarTransaccion(userId, accion, cantidad, precio, true);
            actualizarPortfolio(userId, accion, cantidad, precio);
            actualizarSaldo(userId, -(cantidad * precio));

            out.write("{\"success\": true, \"message\": \"Compra realizada con éxito\"}");

        } catch (Exception e) {
            out.write("{\"success\": false, \"message\": \"Error: " + e.getMessage() + "\"}");
            e.printStackTrace();
        }
    }

    // --- Métodos de base de datos (integrados directamente) ---
    private int getUserId(String username) throws SQLException {
        String sql = "SELECT IDUser FROM Users WHERE Username = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            return rs.next() ? rs.getInt("IDUser") : -1;
        }
    }

    private boolean tieneSaldoSuficiente(int userId, double cantidad) throws SQLException {
        String sql = "SELECT Balance FROM Users WHERE IDUser = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            return rs.next() && rs.getDouble("Balance") >= cantidad;
        }
    }

    private void registrarTransaccion(int userId, String accion, int cantidad, double precio, boolean esCompra) throws SQLException {
        // Formato de fecha literal para Access antiguo (#MM/dd/yyyy#)
        String fechaHoy = new java.text.SimpleDateFormat("#MM/dd/yyyy#").format(new java.util.Date());


        // Usar nombres de columnas sin caracteres especiales
        String sql = "INSERT INTO Transactions (IDUser, StockName, Num, Price, [Date], BoughtSell) VALUES (?,?,?,?," + fechaHoy + ",?)";     //1, 'ACS', 3, 51, #04/16/2025#, 1

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setString(2, accion);
            stmt.setInt(3, cantidad);
            stmt.setDouble(4, precio);
            //stmt.setString(5, fechaHoy);
            stmt.setBoolean(5, esCompra);

            stmt.executeUpdate();
        }
    }

    private void actualizarPortfolio(int userId, String accion, int cantidad, double precio) throws SQLException {
        // Lógica para actualizar el portfolio (similar a ConnectionUtils)
        String checkSql = "SELECT Num FROM Portfolio WHERE IDUser = ? AND StockName = ?";

        try (PreparedStatement stmt = connection.prepareStatement(checkSql)) {
            stmt.setInt(1, userId);
            stmt.setString(2, accion);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                String fechaHoy3 = new java.text.SimpleDateFormat("#MM/dd/yyyy#").format(new java.util.Date());
                int nuevaCantidad = rs.getInt("Num") + cantidad;
                String updateSql = "UPDATE Portfolio SET Num = ?, Price = ?, [Date] = " + fechaHoy3 + " WHERE IDUser = ? AND StockName = ?";
                try (PreparedStatement updateStmt = connection.prepareStatement(updateSql)) {
                    updateStmt.setInt(1, nuevaCantidad);
                    updateStmt.setDouble(2, precio);
                    updateStmt.setInt(3, userId);
                    updateStmt.setString(4, accion);

                    updateStmt.executeUpdate();
                }
            } else {
                String fechaHoy2 = new java.text.SimpleDateFormat("#MM/dd/yyyy#").format(new java.util.Date());
                System.out.println("fechaHoy2 generada: " + fechaHoy2);
                String insertSql = "INSERT INTO Portfolio (IDUser, StockName, Num, Price, [Date]) VALUES (?, ?, ?, ?, " + fechaHoy2 + ")";

                try (PreparedStatement insertStmt = connection.prepareStatement(insertSql)) {
                    insertStmt.setInt(1, userId);
                    insertStmt.setString(2, accion);
                    insertStmt.setInt(3, cantidad);
                    insertStmt.setDouble(4, precio);

                    insertStmt.executeUpdate();
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