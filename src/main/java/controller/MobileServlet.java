package controller;

import model.Mobile;
import model.Company;
import model.Category;
import model.User;
import util.HibernateUtil;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.util.List;

@WebServlet("/mobiles/*")
public class MobileServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("text/html");
        PrintWriter out = resp.getWriter();
        HttpSession session = req.getSession();
        User user = (User) session.getAttribute("user");

        String pathInfo = req.getPathInfo() == null ? "" : req.getPathInfo();

        out.println("<html><head><title>Mobiles</title><link rel='stylesheet' href='/style.css'></head><body>");
        out.println("<header><nav>" + getNavBar(user) + "</nav></header><div class='container'>");

        try (Session dbSession = HibernateUtil.getSessionFactory().openSession()) {
            if ("/add".equals(pathInfo) && user != null) {
                out.println("<h1>Add New Mobile</h1>");
                out.println("<form method='post' action='/mobiles/add'>");
                out.println("<input type='text' name='name' placeholder='Mobile Name' required>");
                out.println("<input type='number' name='price' placeholder='Price' step='0.01' required>");
                out.println("<input type='number' name='stock' placeholder='Stock' required>");
                out.println("<input type='text' name='company' placeholder='Company Name' required>");
                out.println("<input type='text' name='color' placeholder='Company Color' required>");
                out.println("<input type='text' name='category' placeholder='Category Name' required>");
                out.println("<button type='submit'>Add Mobile</button>");
                out.println("</form>");
            } else {
                out.println("<h1>Available Mobiles</h1>");
                List<Mobile> mobiles = dbSession.createQuery("FROM Mobile", Mobile.class).list();

                if (mobiles.isEmpty()) {
                    out.println("<p>No mobiles available.</p>");
                } else {
                    out.println("<div class='mobile-grid'>");
                    for (Mobile mobile : mobiles) {
                        out.println("<div class='mobile-card'>");
                        out.println("<h3>" + mobile.getName() + "</h3>");
                        out.println("<p>Company: " + mobile.getCompany().getBrandName() + "</p>");
                        out.println("<p>Category: " + mobile.getCategory().getName() + "</p>");
                        out.println("<p>Price: ₹" + mobile.getPrice() + "</p>");
                        out.println("<p>Stock: " + mobile.getStock() + "</p>");
                        out.println("</div>");
                    }
                    out.println("</div>");
                }
            }
        }
        out.println("</div></body></html>");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String pathInfo = req.getPathInfo();
        HttpSession session = req.getSession();
        User user = (User) session.getAttribute("user");

        if ("/add".equals(pathInfo) && user != null) {
            String name = req.getParameter("name");
            BigDecimal price = new BigDecimal(req.getParameter("price"));
            int stock = Integer.parseInt(req.getParameter("stock"));
            String companyName = req.getParameter("company");
            String companyColor = req.getParameter("color");
            String categoryName = req.getParameter("category");

            try (Session dbSession = HibernateUtil.getSessionFactory().openSession()) {
                Transaction tx = dbSession.beginTransaction();

                Company company = new Company();
                company.setBrandName(companyName);
                company.setColor(companyColor);
                company.setBio("Popular Brand");
                dbSession.persist(company);

                Category category = dbSession.createQuery(
                   "FROM Category WHERE name = :name", Category.class)
                   .setParameter("name", categoryName)
                   .uniqueResult();

                if (category == null) {
                    category = new Category();
                    category.setName(categoryName);
                    category.setDescription("Popular Category");
                    dbSession.persist(category);
                }

                Mobile mobile = new Mobile();
                mobile.setName(name);
                mobile.setPrice(price);
                mobile.setStock(stock);
                mobile.setCompany(company);
                mobile.setCategory(category);

                dbSession.persist(mobile);
                tx.commit();
            }
        }
        resp.sendRedirect("/mobiles");
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
