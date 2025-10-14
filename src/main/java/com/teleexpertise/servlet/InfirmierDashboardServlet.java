package com.teleexpertise.servlet;

import com.teleexpertise.dao.PatientDAO;
import com.teleexpertise.model.Patient;
import com.teleexpertise.model.User;
import com.teleexpertise.service.FileAttenteService;
import com.teleexpertise.service.PatientService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@WebServlet("/infirmier/dashboard")
public class InfirmierDashboardServlet extends HttpServlet {

    private PatientService patientService;
    private FileAttenteService fileAttenteService;
    private PatientDAO patientDAO;

    @Override
    public void init() throws ServletException {
        patientService = new PatientService();
        fileAttenteService = new FileAttenteService();
        patientDAO = new PatientDAO();
    }

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

        String action = req.getParameter("action");

        if (action != null) {
            switch (action) {
                case "listPatients":
                    handleListPatients(req, resp);
                    return;
                case "addPatientForm":
                    handleAddPatientForm(req, resp);
                    return;
                case "editPatientForm":
                    handleEditPatientForm(req, resp);
                    return;
                case "deletePatient":
                    handleDeletePatient(req, resp);
                    return;
                case "fileAttente":
                    handleFileAttente(req, resp);
                    return;
                case "addToFileAttente":
                    handleAddToFileAttente(req, resp);
                    return;
                case "removeFromFileAttente":
                    handleRemoveFromFileAttente(req, resp);
                    return;
                case "searchPatients":
                    handleSearchPatients(req, resp);
                    return;
                default:
                    break;
            }
        }

        // Dashboard principal avec toutes les données nécessaires
        List<Patient> allPatients = patientDAO.findAll();
        List<Patient> patientsEnAttente = allPatients
                .stream()
                .filter(Patient::isEnAttente)
                .sorted((p1, p2) -> {
                    if (p1.getHeureArrivee() != null && p2.getHeureArrivee() != null) {
                        return p1.getHeureArrivee().compareTo(p2.getHeureArrivee());
                    } else if (p1.getHeureArrivee() != null) {
                        return -1;
                    } else if (p2.getHeureArrivee() != null) {
                        return 1;
                    }
                    return 0;
                })
                .collect(java.util.stream.Collectors.toList());

        req.setAttribute("allPatients", allPatients);
        req.setAttribute("patientsEnAttente", patientsEnAttente);
        req.setAttribute("pageTitle", "Dashboard Infirmier");

        req.getRequestDispatcher("/WEB-INF/jsp/infirmier/dashboard.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            resp.sendRedirect(req.getContextPath() + "/auth/login");
            return;
        }

        String action = req.getParameter("action");

        if ("savePatient".equals(action)) {
            handleSavePatient(req, resp);
        } else if ("searchPatients".equals(action)) {
            handleSearchPatients(req, resp);
        }
    }

    private void handleListPatients(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // Récupérer tous les patients
        List<Patient> allPatients = patientDAO.findAll();

        // Récupérer les paramètres de recherche
        String searchTerm = req.getParameter("searchTerm");
        String filterStatus = req.getParameter("filterStatus");

        // Filtrer les patients selon les critères de recherche
        List<Patient> filteredPatients = null;

        if ((searchTerm != null && !searchTerm.trim().isEmpty()) ||
            (filterStatus != null && !filterStatus.trim().isEmpty() && !"tous".equals(filterStatus))) {

            filteredPatients = allPatients.stream()
                .filter(patient -> {
                    boolean matchesSearch = true;
                    boolean matchesStatus = true;

                    // Filtrage par terme de recherche
                    if (searchTerm != null && !searchTerm.trim().isEmpty()) {
                        String searchLower = searchTerm.toLowerCase().trim();
                        matchesSearch =
                            (patient.getNom() != null && patient.getNom().toLowerCase().contains(searchLower)) ||
                            (patient.getPrenom() != null && patient.getPrenom().toLowerCase().contains(searchLower)) ||
                            (patient.getNumeroSecuriteSociale() != null && patient.getNumeroSecuriteSociale().contains(searchTerm.trim())) ||
                            (patient.getTelephone() != null && patient.getTelephone().contains(searchTerm.trim()));
                    }

                    // Filtrage par statut
                    if (filterStatus != null && !filterStatus.trim().isEmpty() && !"tous".equals(filterStatus)) {
                        if ("disponible".equals(filterStatus)) {
                            matchesStatus = !patient.isEnAttente();
                        } else if ("attente".equals(filterStatus)) {
                            matchesStatus = patient.isEnAttente();
                        }
                    }

                    return matchesSearch && matchesStatus;
                })
                .collect(java.util.stream.Collectors.toList());
        }

        // Calculer les patients en attente
        List<Patient> patientsEnAttente = allPatients
                .stream()
                .filter(Patient::isEnAttente)
                .sorted((p1, p2) -> {
                    if (p1.getHeureArrivee() != null && p2.getHeureArrivee() != null) {
                        return p1.getHeureArrivee().compareTo(p2.getHeureArrivee());
                    } else if (p1.getHeureArrivee() != null) {
                        return -1;
                    } else if (p2.getHeureArrivee() != null) {
                        return 1;
                    }
                    return 0;
                })
                .collect(java.util.stream.Collectors.toList());

        // Mettre les données dans les attributs de la requête
        req.setAttribute("allPatients", allPatients);
        req.setAttribute("patientsEnAttente", patientsEnAttente);
        if (filteredPatients != null) {
            req.setAttribute("filteredPatients", filteredPatients);
        }
        req.setAttribute("pageTitle", "Liste des Patients");
        req.getRequestDispatcher("/WEB-INF/jsp/infirmier/dashboard.jsp").forward(req, resp);
    }

    private void handleAddPatientForm(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        List<Patient> allPatients = patientDAO.findAll();
        List<Patient> patientsEnAttente = allPatients
                .stream()
                .filter(Patient::isEnAttente)
                .sorted((p1, p2) -> {
                    if (p1.getHeureArrivee() != null && p2.getHeureArrivee() != null) {
                        return p1.getHeureArrivee().compareTo(p2.getHeureArrivee());
                    } else if (p1.getHeureArrivee() != null) {
                        return -1;
                    } else if (p2.getHeureArrivee() != null) {
                        return 1;
                    }
                    return 0;
                })
                .collect(java.util.stream.Collectors.toList());

        req.setAttribute("allPatients", allPatients);
        req.setAttribute("patientsEnAttente", patientsEnAttente);
        req.setAttribute("formMode", "add");
        req.setAttribute("pageTitle", "Ajouter un Patient");
        req.getRequestDispatcher("/WEB-INF/jsp/infirmier/dashboard.jsp").forward(req, resp);
    }

    private void handleEditPatientForm(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String patientId = req.getParameter("id");
        List<Patient> allPatients = patientDAO.findAll();
        List<Patient> patientsEnAttente = allPatients
                .stream()
                .filter(Patient::isEnAttente)
                .sorted((p1, p2) -> {
                    if (p1.getHeureArrivee() != null && p2.getHeureArrivee() != null) {
                        return p1.getHeureArrivee().compareTo(p2.getHeureArrivee());
                    } else if (p1.getHeureArrivee() != null) {
                        return -1;
                    } else if (p2.getHeureArrivee() != null) {
                        return 1;
                    }
                    return 0;
                })
                .collect(java.util.stream.Collectors.toList());

        req.setAttribute("allPatients", allPatients);
        req.setAttribute("patientsEnAttente", patientsEnAttente);

        if (patientId != null) {
            try {
                Long id = Long.parseLong(patientId);
                Patient patient = patientDAO.findById(id).orElse(null);
                if (patient != null) {
                    req.setAttribute("patient", patient);
                    req.setAttribute("formMode", "edit");
                    req.setAttribute("pageTitle", "Modifier le Patient");
                }
            } catch (NumberFormatException e) {
                req.setAttribute("error", "ID patient invalide");
            }
        }
        req.getRequestDispatcher("/WEB-INF/jsp/infirmier/dashboard.jsp").forward(req, resp);
    }

    private void handleSavePatient(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        try {
            String idStr = req.getParameter("id");
            Patient patient;

            if (idStr != null && !idStr.isEmpty()) {
                // Modification d'un patient existant
                Long id = Long.parseLong(idStr);
                patient = patientDAO.findById(id).orElse(null);
                if (patient == null) {
                    HttpSession session = req.getSession();
                    session.setAttribute("error", "Patient non trouvé");
                    resp.sendRedirect(req.getContextPath() + "/infirmier/dashboard?action=listPatients");
                    return;
                }
            } else {
                // Création d'un nouveau patient
                patient = new Patient();
                // Nouveau patient automatiquement en attente
                patient.setEnAttente(true);
                patient.setStatut("En attente");
                patient.setCreatedAt(LocalDateTime.now());
                patient.setHeureArrivee(LocalDateTime.now());
            }

            // Mise à jour des champs
            patient.setNom(req.getParameter("nom"));
            patient.setPrenom(req.getParameter("prenom"));
            patient.setDateNaissance(LocalDate.parse(req.getParameter("dateNaissance")));
            patient.setNumeroSecuriteSociale(req.getParameter("numeroSecuriteSociale"));
            patient.setTelephone(req.getParameter("telephone"));
            patient.setAdresse(req.getParameter("adresse"));
            patient.setMutuelle(req.getParameter("mutuelle"));
            patient.setAntecedents(req.getParameter("antecedents"));
            patient.setAllergies(req.getParameter("allergies"));
            patient.setTraitementsCours(req.getParameter("traitementsCours"));

            if (idStr != null && !idStr.isEmpty()) {
                patientDAO.update(patient);
                HttpSession session = req.getSession();
                session.setAttribute("success", "Patient modifié avec succès");
            } else {
                patientDAO.create(patient);
                // Ajouter automatiquement le nouveau patient à la file d'attente
                fileAttenteService.add(patient, LocalDate.now());
                HttpSession session = req.getSession();
                session.setAttribute("success", "Patient ajouté avec succès et placé en file d'attente");
            }
        } catch (Exception e) {
            HttpSession session = req.getSession();
            session.setAttribute("error", "Erreur lors de la sauvegarde: " + e.getMessage());
        }

        resp.sendRedirect(req.getContextPath() + "/infirmier/dashboard?action=listPatients");
    }

    private void handleDeletePatient(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String patientId = req.getParameter("id");
        if (patientId != null) {
            try {
                Long id = Long.parseLong(patientId);
                patientDAO.delete(id);
                HttpSession session = req.getSession();
                session.setAttribute("success", "Patient supprimé avec succès");
            } catch (NumberFormatException e) {
                HttpSession session = req.getSession();
                session.setAttribute("error", "ID patient invalide");
            } catch (Exception e) {
                HttpSession session = req.getSession();
                session.setAttribute("error", "Erreur lors de la suppression");
            }
        }
        resp.sendRedirect(req.getContextPath() + "/infirmier/dashboard?action=listPatients");
    }

    private void handleFileAttente(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        // Récupérer les patients en attente directement depuis la base de données
        List<Patient> patientsEnAttente = patientDAO.findAll()
                .stream()
                .filter(Patient::isEnAttente)
                .sorted((p1, p2) -> {
                    if (p1.getHeureArrivee() != null && p2.getHeureArrivee() != null) {
                        return p1.getHeureArrivee().compareTo(p2.getHeureArrivee());
                    } else if (p1.getHeureArrivee() != null) {
                        return -1;
                    } else if (p2.getHeureArrivee() != null) {
                        return 1;
                    }
                    return 0;
                })
                .collect(java.util.stream.Collectors.toList());

        req.setAttribute("patientsEnAttente", patientsEnAttente);
        req.setAttribute("showFileAttente", true);
        req.setAttribute("pageTitle", "File d'Attente");
        req.getRequestDispatcher("/WEB-INF/jsp/infirmier/dashboard.jsp").forward(req, resp);
    }

    private void handleAddToFileAttente(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String patientId = req.getParameter("patientId");
        if (patientId != null) {
            try {
                Long id = Long.parseLong(patientId);
                Patient patient = patientDAO.findById(id).orElse(null);
                if (patient != null) {
                    patient.setEnAttente(true);
                    // Mettre à jour l'heure d'arrivée pour respecter l'ordre chronologique dans la file d'attente
                    patient.setHeureArrivee(LocalDateTime.now());
                    patientDAO.update(patient);
                    fileAttenteService.add(patient, LocalDate.now());
                    HttpSession session = req.getSession();
                    session.setAttribute("success", "Patient ajouté à la file d'attente");
                }
            } catch (NumberFormatException e) {
                HttpSession session = req.getSession();
                session.setAttribute("error", "ID patient invalide");
            }
        }
        resp.sendRedirect(req.getContextPath() + "/infirmier/dashboard?action=fileAttente");
    }

    private void handleRemoveFromFileAttente(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String patientId = req.getParameter("patientId");
        if (patientId != null) {
            try {
                Long id = Long.parseLong(patientId);
                Patient patient = patientDAO.findById(id).orElse(null);
                if (patient != null) {
                    patient.setEnAttente(false);
                    patient.setHeureArrivee(null);
                    patientDAO.update(patient);
                    fileAttenteService.remove(patient, LocalDate.now());
                    HttpSession session = req.getSession();
                    session.setAttribute("success", "Patient retiré de la file d'attente");
                }
            } catch (NumberFormatException e) {
                HttpSession session = req.getSession();
                session.setAttribute("error", "ID patient invalide");
            }
        }
        resp.sendRedirect(req.getContextPath() + "/infirmier/dashboard?action=fileAttente");
    }

    private void handleSearchPatients(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String searchTerm = req.getParameter("searchTerm");
        if (searchTerm != null && !searchTerm.trim().isEmpty()) {
            List<Patient> patientsFound = patientService.rechercherPatients(searchTerm.trim());
            req.setAttribute("patientsFound", patientsFound);
            req.setAttribute("searchTerm", searchTerm);
        }
        req.setAttribute("showSearchResults", true);
        req.setAttribute("pageTitle", "Recherche de Patients");
        req.getRequestDispatcher("/WEB-INF/jsp/infirmier/dashboard.jsp").forward(req, resp);
    }
}
