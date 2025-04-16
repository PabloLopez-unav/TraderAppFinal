import java.sql.*;
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

    public int getUserId(String username) {
        String sql = "SELECT IDUser FROM Users WHERE username = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("IDUser");
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al obtener ID de usuario: " + e.getMessage());
        }
        return -1;
    }

    public boolean tieneSaldoSuficiente(int userId, double cantidad) throws SQLException {
        String sql = "SELECT Balance FROM Users WHERE IDUser = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getDouble("Balance") >= cantidad;
            }
            return false;
        }
    }

    public void registrarTransaccion(int userId, String accion, int cantidad, double precio, boolean esCompra) throws SQLException {
        java.sql.Date fechaHoy = new java.sql.Date(System.currentTimeMillis());
        String sql = "INSERT INTO Transactions (IDUser, StockName, Num, Price, Date, `Bought/Sell`) VALUES (?, ?, ?, ?, CAST(? AS DATE), ?)";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setString(2, accion);
            stmt.setInt(3, cantidad);
            stmt.setDouble(4, precio);
            stmt.setDate(5, fechaHoy);
            stmt.setBoolean(6, esCompra);
            stmt.executeUpdate();
        }
    }

    public void actualizarPortfolio(int userId, String accion, int cantidad, double precio) throws SQLException {
        String checkSql = "SELECT Num FROM Portfolio WHERE IDUser = ? AND StockName = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(checkSql)) {
            stmt.setInt(1, userId);
            stmt.setString(2, accion);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                java.sql.Date fechaHoy = new java.sql.Date(System.currentTimeMillis());
                int nuevaCantidad = rs.getInt("Num") + cantidad;
                String updateSql = "UPDATE Portfolio SET Num = ?, Price = ?, Date = CAST(? AS DATE) WHERE IDUser = ? AND StockName = ?";
                try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                    updateStmt.setInt(1, nuevaCantidad);
                    updateStmt.setDouble(2, precio);
                    updateStmt.setInt(3, userId);
                    updateStmt.setString(4, accion);
                    updateStmt.setDate(5, fechaHoy);
                    updateStmt.executeUpdate();
                }
            } else {
                java.sql.Date fechaHoy = new java.sql.Date(System.currentTimeMillis());
                String insertSql = "INSERT INTO Portfolio (IDUser, StockName, Num, Price, Date) VALUES (?, ?, ?, ?, CAST(? AS DATE))";
                try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                    insertStmt.setInt(1, userId);
                    insertStmt.setString(2, accion);
                    insertStmt.setInt(3, cantidad);
                    insertStmt.setDouble(4, precio);
                    insertStmt.setDate(5, fechaHoy);
                    insertStmt.executeUpdate();
                }
            }
        }
    }

    public void actualizarSaldo(int userId, double cambio) throws SQLException {
        String sql = "UPDATE Users SET Balance = Balance + ? WHERE IDUser = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDouble(1, cambio);
            stmt.setInt(2, userId);
            stmt.executeUpdate();
        }
    }
}