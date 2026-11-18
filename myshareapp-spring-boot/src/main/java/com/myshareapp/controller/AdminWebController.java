package com.myshareapp.controller;

import com.myshareapp.model.Contact;
import com.myshareapp.model.News;
import com.myshareapp.model.User;
import com.myshareapp.repository.*;
import com.myshareapp.service.NewsService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@Controller
public class AdminWebController {
    private final UserRepository userRepo;
    private final PortfolioRepository portfolioRepo;
    private final StockRepository stockRepo;
    private final SoldStockRepository soldStockRepo;
    private final NewsRepository newsRepo;
    private final ContactRepository contactRepo;
    private final NewsService newsService;

    public AdminWebController(UserRepository userRepo, PortfolioRepository portfolioRepo, StockRepository stockRepo,
                              SoldStockRepository soldStockRepo, NewsRepository newsRepo,
                              ContactRepository contactRepo, NewsService newsService) {
        this.userRepo = userRepo;
        this.portfolioRepo = portfolioRepo;
        this.stockRepo = stockRepo;
        this.soldStockRepo = soldStockRepo;
        this.newsRepo = newsRepo;
        this.contactRepo = contactRepo;
        this.newsService = newsService;
    }

    private User adminUser(Authentication auth) {
        return userRepo.findByUsernameIgnoreCase(auth.getName()).orElseThrow();
    }

    private List<Map<String, Object>> usersWithCounts() {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (User u : userRepo.findAllByOrderByCreatedAtDesc()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", u.getId());
            m.put("username", u.getUsername());
            m.put("email", u.getEmail());
            m.put("role", u.getRole());
            m.put("portfolioCount", portfolioRepo.countByUserId(u.getId()));
            m.put("createdAt", u.getCreatedAt());
            rows.add(m);
        }
        return rows;
    }

    @GetMapping("/dashboard.php")
    public String dashboard(Authentication auth, Model model) {
        User admin = adminUser(auth);
        List<Map<String, Object>> allUsers = usersWithCounts();
        List<Contact> contacts = contactRepo.findAllByOrderByCreatedAtDesc();
        List<News> news = newsRepo.findAllByOrderByCreatedAtDesc();

        model.addAttribute("pageTitle", "Dashboard");
        model.addAttribute("currentPage", "dashboard.php");
        model.addAttribute("adminUsername", admin.getUsername());
        model.addAttribute("totalUsers", userRepo.count());
        model.addAttribute("totalPortfolios", portfolioRepo.count());
        model.addAttribute("totalNews", newsRepo.count());
        model.addAttribute("totalContacts", contactRepo.count());
        model.addAttribute("recentUsers", allUsers.subList(0, Math.min(5, allUsers.size())));
        model.addAttribute("latestContacts", contacts.subList(0, Math.min(5, contacts.size())));
        model.addAttribute("latestNews", news.subList(0, Math.min(5, news.size())));
        return "admin/dashboard";
    }

    @Transactional
    @GetMapping("/users.php")
    public String users(@RequestParam(required = false) Integer delete, Authentication auth, Model model) {
        User admin = adminUser(auth);
        String msg = "";
        String type = "success";
        if (delete != null) {
            if (Objects.equals(admin.getId(), delete)) {
                msg = "You cannot delete your own account.";
                type = "danger";
            } else if (userRepo.existsById(delete)) {
                portfolioRepo.findByUserId(delete).forEach(p -> {
                    soldStockRepo.deleteByPortfolioId(p.getId());
                    stockRepo.deleteByPortfolioId(p.getId());
                });
                portfolioRepo.deleteByUserId(delete);
                userRepo.deleteById(delete);
                msg = "User deleted successfully.";
            } else {
                msg = "User not found.";
                type = "danger";
            }
        }
        model.addAttribute("pageTitle", "Users");
        model.addAttribute("currentPage", "users.php");
        model.addAttribute("adminUsername", admin.getUsername());
        model.addAttribute("adminUserId", admin.getId());
        model.addAttribute("users", usersWithCounts());
        model.addAttribute("deleteMessage", msg);
        model.addAttribute("deleteMessageType", type);
        return "admin/users";
    }

    @GetMapping("/newss.php")
    public String newsList(@RequestParam(defaultValue = "") String status, Authentication auth, Model model) {
        model.addAttribute("pageTitle", "News Management");
        model.addAttribute("currentPage", "newss.php");
        model.addAttribute("adminUsername", adminUser(auth).getUsername());
        model.addAttribute("newsList", newsRepo.findAllByOrderByCreatedAtDesc());
        model.addAttribute("status", status);
        return "admin/newss";
    }

    @GetMapping("/add_news.php")
    public String addNewsForm(Authentication auth, Model model) {
        model.addAttribute("pageTitle", "Add News");
        model.addAttribute("currentPage", "add_news.php");
        model.addAttribute("adminUsername", adminUser(auth).getUsername());
        model.addAttribute("errors", List.of());
        model.addAttribute("success", false);
        model.addAttribute("title", "");
        model.addAttribute("description", "");
        return "admin/add_news";
    }

    @PostMapping("/add_news.php")
    public String addNewsSubmit(@RequestParam(defaultValue = "") String title,
                                @RequestParam(defaultValue = "") String description,
                                @RequestParam(value = "image", required = false) MultipartFile image,
                                Authentication auth, Model model) {
        User admin = adminUser(auth);
        List<String> errors = new ArrayList<>();
        if (title.trim().isEmpty()) errors.add("Title is required.");
        if (title.trim().length() > 255) errors.add("Title must be 255 characters or fewer.");
        if (description.trim().isEmpty()) errors.add("Description is required.");

        if (errors.isEmpty()) {
            try {
                newsService.createNews(admin.getId(), title, description, image);
                model.addAttribute("success", true);
                model.addAttribute("title", "");
                model.addAttribute("description", "");
            } catch (Exception e) {
                errors.add(e.getMessage());
                model.addAttribute("success", false);
                model.addAttribute("title", title.trim());
                model.addAttribute("description", description.trim());
            }
        } else {
            model.addAttribute("success", false);
            model.addAttribute("title", title.trim());
            model.addAttribute("description", description.trim());
        }
        model.addAttribute("pageTitle", "Add News");
        model.addAttribute("currentPage", "add_news.php");
        model.addAttribute("adminUsername", admin.getUsername());
        model.addAttribute("errors", errors);
        return "admin/add_news";
    }

    @GetMapping("/edit_news.php")
    public String editNewsForm(@RequestParam Integer id, Authentication auth, Model model) {
        News n = newsRepo.findById(id).orElse(null);
        if (n == null) return "redirect:/newss.php";
        model.addAttribute("pageTitle", "Edit News");
        model.addAttribute("currentPage", "edit_news.php");
        model.addAttribute("adminUsername", adminUser(auth).getUsername());
        model.addAttribute("news", n);
        model.addAttribute("errors", List.of());
        model.addAttribute("title", n.getTitle());
        model.addAttribute("description", n.getDescription());
        return "admin/edit_news";
    }

    @PostMapping("/edit_news.php")
    public String editNewsSubmit(@RequestParam Integer id,
                                 @RequestParam(defaultValue = "") String title,
                                 @RequestParam(defaultValue = "") String description,
                                 @RequestParam(value = "image", required = false) MultipartFile image,
                                 Authentication auth, Model model) {
        News n = newsRepo.findById(id).orElse(null);
        if (n == null) return "redirect:/newss.php";
        List<String> errors = new ArrayList<>();
        if (title.trim().isEmpty()) errors.add("Title is required.");
        if (description.trim().isEmpty()) errors.add("Description is required.");
        if (errors.isEmpty()) {
            try {
                newsService.updateNews(id, title, description, image);
                return "redirect:/newss.php?status=updated";
            } catch (Exception e) {
                errors.add(e.getMessage());
            }
        }
        model.addAttribute("pageTitle", "Edit News");
        model.addAttribute("currentPage", "edit_news.php");
        model.addAttribute("adminUsername", adminUser(auth).getUsername());
        model.addAttribute("news", n);
        model.addAttribute("errors", errors);
        model.addAttribute("title", title.trim());
        model.addAttribute("description", description.trim());
        return "admin/edit_news";
    }

    @GetMapping("/delete_news.php")
    public String deleteNews(@RequestParam Integer id) {
        newsService.deleteNews(id);
        return "redirect:/newss.php?status=deleted";
    }

    @GetMapping("/contacts.php")
    public String contacts(@RequestParam(defaultValue = "") String status, Authentication auth, Model model) {
        model.addAttribute("pageTitle", "Contact Messages");
        model.addAttribute("currentPage", "contacts.php");
        model.addAttribute("adminUsername", adminUser(auth).getUsername());
        model.addAttribute("contacts", contactRepo.findAllByOrderByCreatedAtDesc());
        model.addAttribute("status", status);
        return "admin/contacts";
    }

    @GetMapping("/view_contact.php")
    public String viewContact(@RequestParam Integer id, Authentication auth, Model model) {
        Contact c = contactRepo.findById(id).orElse(null);
        if (c == null) return "redirect:/contacts.php";
        model.addAttribute("pageTitle", "View Message");
        model.addAttribute("currentPage", "view_contact.php");
        model.addAttribute("adminUsername", adminUser(auth).getUsername());
        model.addAttribute("contact", c);
        return "admin/view_contact";
    }

    @GetMapping("/delete_contact.php")
    public String deleteContact(@RequestParam Integer id) {
        contactRepo.deleteById(id);
        return "redirect:/contacts.php?status=deleted";
    }
}
