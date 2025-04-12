package controller;

import model.User;
import util.HibernateUtil;
import util.PasswordUtil;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import org.hibernate.Session;
import java.io.IOException;
import java.io.PrintWriter;

@WebServlet("/auth/*")
public class AuthServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("text/html");
        PrintWriter out = resp.getWriter();
        HttpSession session = req.getSession();
        String pathInfo = req.getPathInfo() == null ? "" : req.getPathInfo();
        User user = (User) session.getAttribute("user");

        out.println("<html><head><title>Online Mobile Shop - Auth</title>");
        out.println("<link rel='stylesheet' href='style.css'></head><body>");
        out.println("<header><nav>" + getNavBar(user) + "</nav></header>");
        out.println("<div class='container'>");

        switch (pathInfo) {
            case "/register":
                out.println("<h1>Register</h1>");
                out.println("<form method='post' action='/auth/register'>");
                out.println("<input type='text' name='username' placeholder='Username' required>");
                out.println("<input type='email' name='email' placeholder='Email' required>");
                out.println("<input type='password' name='password' placeholder='Password' required>");
                out.println("<button type='submit'>Register</button>");
                out.println("</form>");
                break;
            case "/login":
                out.println("<h1>Login</h1>");
                out.println("<form method='post' action='/auth/login'>");
                out.println("<input type='text' name='username' placeholder='Username' required>");
                out.println("<input type='password' name='password' placeholder='Password' required>");
                out.println("<button type='submit'>Login</button>");
                out.println("</form>");
                break;
            case "/logout":
                session.invalidate();
                resp.sendRedirect("/auth/login");
                return;
            default:
                if (user != null) {
                    out.println("<h1>Welcome, " + user.getUsername() + "</h1>");
                    out.println("<p>Explore mobiles or manage your cart and orders.</p>");
                } else {
                    out.println("<h1>Welcome to Online Mobile Shop</h1>");
                    out.println("<p>Please <a href='/auth/login'>login</a> or <a href='/auth/register'>register</a>.</p>");
                }
        }
        out.println("</div></body></html>");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("text/html");
        PrintWriter out = resp.getWriter();
        HttpSession session = req.getSession();
        String pathInfo = req.getPathInfo();

        try (Session dbSession = HibernateUtil.getSessionFactory().openSession()) {
            if ("/register".equals(pathInfo)) {
                String username = req.getParameter("username");
                String email = req.getParameter("email");
                String password = req.getParameter("password");

                User existingUser = dbSession.createQuery("FROM User WHERE username = :username", User.class)
                        .setParameter("username", username)
                        .uniqueResult();

                if (existingUser != null) {
                    out.println("<h1>Error</h1><p>Username already exists. <a href='/auth/register'>Try again</a>.</p>");
                } else {
                    User user = new User(username, email, PasswordUtil.hashPassword(password));
                    dbSession.beginTransaction();
                    dbSession.persist(user);
                    dbSession.getTransaction().commit();
                    out.println("<h1>Success</h1><p>Registered successfully! <a href='/auth/login'>Login</a>.</p>");
                }
            } else if ("/login".equals(pathInfo)) {
                String username = req.getParameter("username");
                String password = req.getParameter("password");

                User user = dbSession.createQuery("FROM User WHERE username = :username", User.class)
                        .setParameter("username", username)
                        .uniqueResult();

                if (user != null && PasswordUtil.checkPassword(password, user.getPasswordHash())) {
                    session.setAttribute("user", user);
                    resp.sendRedirect("/auth");
                    return;
                } else {
                    out.println("<h1>Error</h1><p>Invalid credentials. <a href='/auth/login'>Try again</a>.</p>");
                }
            }
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
