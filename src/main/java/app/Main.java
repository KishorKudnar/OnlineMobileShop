package app;

import org.apache.catalina.Context;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.servlets.DefaultServlet;
import controller.*;
import util.HibernateUtil;
import java.io.File;

public class Main {
    public static void main(String[] args) throws Exception {
        System.out.println("Initializing Hibernate...");
        HibernateUtil.getSessionFactory(); // Auto-create/update tables
        System.out.println("Hibernate initialized");

        Tomcat tomcat = new Tomcat();
        tomcat.setPort(8088);

        String projectRoot = new File(".").getCanonicalPath();
        String docBase = new File(projectRoot, "src/main/webapp").getAbsolutePath();

        Context context = tomcat.addContext("", docBase);
        context.addWelcomeFile("/auth");

        Tomcat.addServlet(context, "default", new DefaultServlet());
        context.addServletMappingDecoded("/*", "default");

        context.setResources(new org.apache.catalina.webresources.StandardRoot(context));

        tomcat.addServlet("", "AuthServlet", "controller.AuthServlet");
        tomcat.addServlet("", "MobileServlet", "controller.MobileServlet");
        tomcat.addServlet("", "CartServlet", "controller.CartServlet");
        tomcat.addServlet("", "OrderServlet", "controller.OrderServlet");

        context.addServletMappingDecoded("/auth/*", "AuthServlet");
        context.addServletMappingDecoded("/mobiles/*", "MobileServlet");
        context.addServletMappingDecoded("/cart/*", "CartServlet");
        context.addServletMappingDecoded("/orders/*", "OrderServlet");

        tomcat.getConnector();

        System.out.println("Starting Tomcat on port 8088...");
        tomcat.start();
        tomcat.getServer().await();
    }
}
