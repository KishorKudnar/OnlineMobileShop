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

        out.println("<html><head><title>Cart</title><link rel='stylesheet' href='/style.css'></head><body>");
        out.println("<header><nav>" + getNavBar(user) + "</nav></header><div class='container'>");
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
            out.println("<p><strong>Cart Total: ₹" + cartTotal + "</strong></p>");
            out.println("<form method='post' action='/cart/checkout'><button type='submit'>Checkout</button></form>");
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
