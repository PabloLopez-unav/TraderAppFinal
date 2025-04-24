public class Utils {
    public static String header(String title) {
        StringBuilder str = new StringBuilder();
        str.append("<!DOCTYPE HTML>");
        str.append("<html>");
        str.append("<head><title>" + title + "</title>");
        str.append("<link rel='icon' href='favicon.ico' />");
        str.append("<link rel='stylesheet' href='style.css'>");
        str.append("</head>");
        str.append("<body>");
        str.append("<div class='menu'>");
        str.append("<a href='Menu'>Menu </a>");
        str.append("<a href='Cartera'>Mi Cartera </a>");
        str.append("<a href='Historico'>Historico </a>");
        //str.append("<a href='Comprar'>Comprar Acciones</a>");
        //str.append("<a href='Vender'>Vender Acciones</a>");
        str.append("</div>");
        return str.toString();
    }

    public static String footer() {
        StringBuilder str = new StringBuilder();
        str.append("</body>");
        str.append("<div class='logout'>");
        str.append("<a href='Menu'>&lt;&lt;&lt; Volver al men\u00FA</a>");
        str.append("</div>");
        str.append("</html>");
        return str.toString();
    }
}