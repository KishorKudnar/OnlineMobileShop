package controller;

import model.Order;
import model.OrderItem;
import model.User;
import util.HibernateUtil;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import org.hibernate.Session;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
@WebServlet("/orders/*")
public class OrderServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("text/html");
        PrintWriter out = resp.getWriter();
        HttpSession session = req.getSession();
        User user = (User) session.getAttribute("user");

        if (user == null) {
            resp.sendRedirect("/auth/login");
            return;
        }

        out.println("<html><head><title>Online Mobile Shop - Orders</title>");
        out.println("<link rel='stylesheet' href='/style.css'>");
        out.println("<style>");
        out.println("body { font-family: Arial, sans-serif; margin: 0; padding: 0; background-color: #f4f4f4; }");
        out.println("header { background-color: #333; padding: 15px 0; text-align: center; color: white; }");
        out.println("nav ul { list-style: none; display: flex; justify-content: center; padding: 0; margin: 0; }");
        out.println("nav li { margin: 0 10px; }");
        out.println("nav a { color: white; text-decoration: none; background-color: #444; padding: 10px 20px; border-radius: 5px; }");
        out.println("nav a:hover { background-color: #2980b9; }");
        out.println(".container { max-width: 1000px; margin: 30px auto; padding: 20px; background: white; border-radius: 8px; box-shadow: 0 4px 8px rgba(0,0,0,0.1); }");
        out.println("h1 { text-align: center; color: #2c3e50; }");
        out.println(".order-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); gap: 20px; }");
        out.println(".order-card { background-color: #ecf0f1; padding: 15px 20px; border-radius: 8px; box-shadow: 0px 2px 5px rgba(0,0,0,0.1); }");
        out.println(".order-card h3 { color: #34495e; margin-top: 0; }");
        out.println(".order-card p { margin: 5px 0; color: #555; }");
        out.println("a { color: #2980b9; text-decoration: none; }");
        out.println("a:hover { text-decoration: underline; }");
        out.println("</style></head><body>");

        out.println("<header><nav>" + getNavBar(user) + "</nav></header>");
        out.println("<div class='container'><h1>Your Orders</h1>");

        try (Session dbSession = HibernateUtil.getSessionFactory().openSession()) {
            List<Order> orders = dbSession.createQuery(
                    "FROM Order o WHERE o.user.userId = :userId", Order.class)
                    .setParameter("userId", user.getUserId())
                    .list();

            if (orders.isEmpty()) {
                out.println("<p>You have no orders yet. <a href='/mobiles'>Shop Now</a>.</p>");
            } else {
                out.println("<div class='order-grid'>");
                for (Order order : orders) {
                    out.println("<div class='order-card'>");
                    out.println("<h3>Order ID: " + order.getOrderId() + "</h3>");
                    out.println("<p>Date: " + order.getOrderDate() + "</p>");

                    List<OrderItem> orderItems = dbSession.createQuery(
                            "FROM OrderItem WHERE order.orderId = :orderId", OrderItem.class)
                            .setParameter("orderId", order.getOrderId())
                            .list();

                    for (OrderItem item : orderItems) {
                        out.println("<p>📱 " + item.getMobile().getName() +
                                " | Qty: " + item.getQuantity() +
                                " | ₹" + item.getUnitPrice() + "</p>");
                    }

                    out.println("<p><strong>Total: ₹" + order.getTotalAmount() + "</strong></p>");
                    out.println("<p>Status: <em>" + order.getStatus() + "</em></p>");
                    out.println("</div>");
                }
                out.println("</div>");
            }

        } catch (Exception e) {
            out.println("<h1>Error</h1><p>Failed to load orders: " + e.getMessage() + "</p>");
            e.printStackTrace();
        }

        out.println("</div></body></html>");
    }

    private String getNavBar(User user) {
        StringBuilder nav = new StringBuilder("<ul class='nav-list'>");

        nav.append("<li><a href='/auth'>Home</a></li>");
        nav.append("<li><a href='/mobiles'>Mobiles</a></li>");
        nav.append("<li><a href='/cart'>Cart</a></li>");

        if (user != null) {
            nav.append("<li><a href='/orders'>Orders</a></li>");
            nav.append("<li><a href='/mobiles/add'>Add Mobile</a></li>");
            nav.append("<li><a href='/auth/logout'>Logout (" + user.getUsername() + ")</a></li>");
        } else {
            nav.append("<li><a href='/auth/login'>Login</a></li>");
            nav.append("<li><a href='/auth/register'>Register</a></li>");
        }

        nav.append("</ul>");
        return nav.toString();
    }
}
