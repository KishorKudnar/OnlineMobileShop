package controller;

import model.Mobile;
import model.Order;
import model.OrderItem;
import model.User;
import util.HibernateUtil;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@WebServlet("/cart/*")
public class CartServlet extends HttpServlet {

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

        out.println("<html><head><title>Cart</title>");
        out.println("<style>");
        out.println("body { font-family: Arial, sans-serif; margin: 0; background: #f8f9fa; }");
        out.println("header { background-color: #343a40; padding: 15px 0; }");
        out.println("nav ul { list-style: none; margin: 0; padding: 0; display: flex; justify-content: center; }");
        out.println("nav li { margin: 0 15px; }");
        out.println("nav a { color: white; text-decoration: none; padding: 10px 15px; background-color: #495057; border-radius: 5px; }");
        out.println("nav a:hover { background-color: #2980b9; }");
        out.println(".container { max-width: 900px; margin: 30px auto; padding: 20px; background: #ffffff; border-radius: 10px; box-shadow: 0 4px 10px rgba(0,0,0,0.1); }");
        out.println("h1 { text-align: center; color: #343a40; }");
        out.println(".cart-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(250px, 1fr)); gap: 20px; margin-top: 20px; }");
        out.println(".cart-item { padding: 20px; background-color: #e9ecef; border-radius: 8px; box-shadow: 0 2px 6px rgba(0,0,0,0.05); }");
        out.println(".cart-item h3 { margin-top: 0; color: #212529; }");
        out.println(".cart-item p { margin: 8px 0; color: #495057; }");
        out.println("form button { margin-top: 20px; padding: 12px 24px; font-size: 16px; background-color: #007bff; color: white; border: none; border-radius: 6px; cursor: pointer; }");
        out.println("form button:hover { background-color: #0056b3; }");
        out.println("a { color: #007bff; }");
        out.println("a:hover { text-decoration: underline; }");
        out.println("</style></head><body>");

        out.println("<header><nav>" + getNavBar(user) + "</nav></header>");
        out.println("<div class='container'>");
        out.println("<h1>Your Cart</h1>");

        List<OrderItem> cart = (List<OrderItem>) session.getAttribute("cart");

        if (cart == null || cart.isEmpty()) {
            out.println("<p>Your cart is empty. <a href='/mobiles'>Add Mobiles</a>.</p>");
        } else {
            out.println("<div class='cart-grid'>");
            BigDecimal cartTotal = BigDecimal.ZERO;
            for (OrderItem item : cart) {
                BigDecimal itemTotal = item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
                cartTotal = cartTotal.add(itemTotal);
                out.println("<div class='cart-item'>");
                out.println("<h3>" + item.getMobile().getName() + "</h3>");
                out.println("<p>Price: ₹" + item.getUnitPrice() + "</p>");
                out.println("<p>Quantity: " + item.getQuantity() + "</p>");
                out.println("<p>Total: ₹" + itemTotal + "</p>");
                out.println("</div>");
            }
            out.println("</div>");
            out.println("<p style='text-align:center; font-size:18px; margin-top:20px;'><strong>Cart Total: ₹" + cartTotal + "</strong></p>");
            out.println("<form method='post' action='/cart/checkout' style='text-align:center;'>");
            out.println("<button type='submit'>Checkout</button></form>");
        }

        out.println("</div></body></html>");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String pathInfo = req.getPathInfo();
        HttpSession session = req.getSession();
        User user = (User) session.getAttribute("user");

        if (user == null) {
            resp.sendRedirect("/auth/login");
            return;
        }

        try (Session dbSession = HibernateUtil.getSessionFactory().openSession()) {
            if (pathInfo == null) {
                resp.sendRedirect("/cart");
                return;
            }

            if ("/add".equals(pathInfo)) {
                handleAddToCart(req, resp, session, dbSession);
            } else if ("/checkout".equals(pathInfo)) {
                handleCheckout(req, resp, session, dbSession, user);
            } else {
                resp.sendRedirect("/cart");
            }
        }
    }

    private void handleAddToCart(HttpServletRequest req, HttpServletResponse resp, HttpSession session, Session dbSession) throws IOException {
        Integer mobileId = Integer.parseInt(req.getParameter("mobileId"));
        Mobile mobile = dbSession.get(Mobile.class, mobileId);

        if (mobile == null || mobile.getStock() <= 0) {
            resp.sendRedirect("/mobiles");
            return;
        }

        List<OrderItem> cart = (List<OrderItem>) session.getAttribute("cart");
        if (cart == null) {
            cart = new ArrayList<>();
            session.setAttribute("cart", cart);
        }

        OrderItem item = cart.stream()
                .filter(ci -> ci.getMobile().getMobileId().equals(mobileId))
                .findFirst()
                .orElse(null);

        if (item == null) {
            OrderItem newItem = new OrderItem();
            newItem.setMobile(mobile);
            newItem.setQuantity(1);
            newItem.setUnitPrice(mobile.getPrice());
            cart.add(newItem);
        } else if (item.getQuantity() < mobile.getStock()) {
            item.setQuantity(item.getQuantity() + 1);
        }

        resp.sendRedirect("/cart");
    }

    private void handleCheckout(HttpServletRequest req, HttpServletResponse resp, HttpSession session, Session dbSession, User user) throws IOException {
        List<OrderItem> cart = (List<OrderItem>) session.getAttribute("cart");

        if (cart == null || cart.isEmpty()) {
            resp.sendRedirect("/cart");
            return;
        }

        Transaction tx = dbSession.beginTransaction();
        try {
            Order order = new Order();
            order.setUser(user);
            order.setOrderItems(cart);
            order.setTotalAmount(cart.stream()
                    .map(ci -> ci.getUnitPrice().multiply(BigDecimal.valueOf(ci.getQuantity())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add));

            dbSession.persist(order);

            for (OrderItem item : cart) {
                item.setOrder(order);
                dbSession.persist(item);

                Mobile mobile = item.getMobile();
                mobile.setStock(mobile.getStock() - item.getQuantity());
                dbSession.merge(mobile);
            }

            tx.commit();
            session.removeAttribute("cart");
            resp.sendRedirect("/orders");

        } catch (Exception e) {
            if (tx != null) tx.rollback();
            e.printStackTrace();
            resp.sendRedirect("/cart?error=CheckoutFailed");
        }
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
