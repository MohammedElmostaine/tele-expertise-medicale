package com.teleexpertise.servlet;

import com.teleexpertise.model.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

@WebServlet("/specialiste/dashboard")
public class SpecialisteDashboardServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            resp.sendRedirect(req.getContextPath() + "/auth/login");
            return;
        }

        User user = (User) session.getAttribute("user");
        req.setAttribute("user", user);
        req.setAttribute("pageTitle", "Dashboard Spécialiste");

        req.getRequestDispatcher("/WEB-INF/jsp/specialiste/dashboard.jsp").forward(req, resp);
    }
}
