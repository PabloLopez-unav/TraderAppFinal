import java.io.*;
import javax.servlet.*;
import javax.servlet.http.*;
import java.net.*;
import java.util.*;

public class Menu extends HttpServlet { 

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException { 
        HttpSession session = request.getSession(true); 
        Integer accessCount = (Integer)session.getAttribute("accessCount");
        if (accessCount == null) {
            accessCount = 0;
        } else {
            accessCount++;
        }
        session.setAttribute("accessCount", accessCount);
        
        String username = null;
        if (session != null) {
        username = (String) session.getAttribute("username");
        }
        if (username == null) {
            response.sendRedirect("Login");
        } else {
            RequestDispatcher dispatcher = request.getRequestDispatcher("Menu.html");
            dispatcher.forward(request, response);
        }
    }
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException { 
        doGet( request, response);
    }
}