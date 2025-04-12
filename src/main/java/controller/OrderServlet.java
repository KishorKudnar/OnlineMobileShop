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
        out.println("<link rel='stylesheet' href='/style.css'></head><body>");
        out.println("<header><nav>" + getNavBar(user) + "</nav></header>");
        out.println("<div class='container'><h1>Your Orders</h1>");

        try (Session dbSession = HibernateUtil.getSessionFactory().openSession()) {

            // Fetch orders of logged-in user
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
                        out.println("<p>Mobile: " + item.getMobile().getName() +
                                " | Quantity: " + item.getQuantity() +
                                " | Price: ₹" + item.getUnitPrice() + "</p>");
                    }

                    out.println("<p>Total Amount: ₹" + order.getTotalAmount() + "</p>");
                    out.println("<p>Status: " + order.getStatus() + "</p>");
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
