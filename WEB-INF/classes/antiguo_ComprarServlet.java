import javax.servlet.*;
import javax.servlet.http.*;
import java.io.*;
import java.sql.*;

public class ComprarServlet extends HttpServlet {

    private final ConnectionUtils dbManager = new ConnectionUtils();

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        PrintWriter out = response.getWriter();

        try {
            // Leer datos del request
            BufferedReader reader = request.getReader();
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            String json = sb.toString();

            // Parsear JSON manualmente (simplificado)
            String accion = json.split("\"accion\":\"")[1].split("\"")[0];
            int cantidad = Integer.parseInt(json.split("\"cantidad\":")[1].split(",")[0]);
            double precio = Math.round(Double.parseDouble(json.split("\"precio\":")[1].split("}")[0]) * 100.0) / 100.0;

            // Obtener usuario
            HttpSession session = request.getSession();
            String username = (String) session.getAttribute("username");
            int userId = dbManager.getUserId(username);

            // Verificar saldo
            double costeTotal = cantidad * precio;
            if (!dbManager.tieneSaldoSuficiente(userId, costeTotal)) {
                out.write("{\"success\": false, \"message\": \"Saldo insuficiente\"}");
                return;
            }

            // Registrar transacción
            dbManager.registrarTransaccion(userId, accion, cantidad, precio, true);
            dbManager.actualizarPortfolio(userId, accion, cantidad, precio);
            dbManager.actualizarSaldo(userId, -costeTotal);

            out.write("{\"success\": true, \"message\": \"Compra realizada con éxito\"}");

        } catch (Exception e) {
            out.write("{\"success\": false, \"message\": \"Error: " + e.getMessage() + "\"}");
            e.printStackTrace();
        }
    }
}