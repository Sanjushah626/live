package com.myshareapp.controller;

import com.myshareapp.model.*;
import com.myshareapp.repository.*;
import com.myshareapp.service.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Controller
public class WebPageController {
    private static final Pattern EMAIL_RX = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private final UserRepository userRepo;
    private final PortfolioRepository portfolioRepo;
    private final StockRepository stockRepo;
    private final NewsRepository newsRepo;
    private final ContactRepository contactRepo;
    private final PortfolioService portfolioService;
    private final StockService stockService;
    private final YonepseService yonepseService;
    private final PasswordEncoder passwordEncoder;

    public WebPageController(UserRepository userRepo, PortfolioRepository portfolioRepo, StockRepository stockRepo,
                             NewsRepository newsRepo, ContactRepository contactRepo,
                             PortfolioService portfolioService, StockService stockService,
                             YonepseService yonepseService, PasswordEncoder passwordEncoder) {
        this.userRepo = userRepo;
        this.portfolioRepo = portfolioRepo;
        this.stockRepo = stockRepo;
        this.newsRepo = newsRepo;
        this.contactRepo = contactRepo;
        this.portfolioService = portfolioService;
        this.stockService = stockService;
        this.yonepseService = yonepseService;
        this.passwordEncoder = passwordEncoder;
    }

    private User currentUser(Authentication auth) {
        return userRepo.findByUsernameIgnoreCase(auth.getName()).orElseThrow();
    }

    @GetMapping({"/", "/hello.html"})
    public String home(Model model) {
        model.addAttribute("activeNav", "home");
        return "hello";
    }

    @GetMapping("/market.html")
    public String market(Model model) {
        model.addAttribute("activeNav", "market");
        return "market";
    }

    @GetMapping({"/news.php", "/news"})
    public String news(Model model) {
        model.addAttribute("activeNav", "news");
        model.addAttribute("news", newsRepo.findAllByOrderByCreatedAtDesc());
        model.addAttribute("yonepseNews", yonepseService.getExchangeNews(40));
        model.addAttribute("ipos", yonepseService.getUpcomingIpos());
        model.addAttribute("recentIpos", yonepseService.getRecentIpos(20));
        model.addAttribute("dividends", yonepseService.getRecentDividends(80));
        return "news";
    }

    @GetMapping({"/contact.php", "/contact"})
    public String contactForm(Authentication auth, Model model) {
        User u = auth != null && auth.isAuthenticated() ? userRepo.findByUsernameIgnoreCase(auth.getName()).orElse(null) : null;
        model.addAttribute("activeNav", "contact");
        model.addAttribute("success", false);
        model.addAttribute("errors", List.of());
        model.addAttribute("formName", u != null ? u.getUsername() : "");
        model.addAttribute("formEmail", u != null ? u.getEmail() : "");
        model.addAttribute("formSubject", "");
        model.addAttribute("formMessage", "");
        return "contact";
    }

    @PostMapping({"/contact.php", "/contact"})
    public String submitContact(@RequestParam(defaultValue = "") String name,
                                @RequestParam(defaultValue = "") String email,
                                @RequestParam(defaultValue = "") String subject,
                                @RequestParam(defaultValue = "") String message,
                                Authentication auth, Model model) {
        List<String> errors = new ArrayList<>();
        if (name.trim().isEmpty()) errors.add("Please enter your name.");
        if (!EMAIL_RX.matcher(email.trim()).matches()) errors.add("Please enter a valid email address.");
        if (message.trim().isEmpty()) errors.add("Please enter a message.");

        if (errors.isEmpty()) {
            Contact c = new Contact();
            c.setName(name.trim());
            c.setEmail(email.trim());
            c.setSubject(subject.trim());
            c.setMessage(message.trim());
            contactRepo.save(c);
            model.addAttribute("success", true);
            model.addAttribute("formName", auth != null ? name.trim() : "");
            model.addAttribute("formEmail", auth != null ? email.trim() : "");
            model.addAttribute("formSubject", "");
            model.addAttribute("formMessage", "");
        } else {
            model.addAttribute("success", false);
            model.addAttribute("formName", name.trim());
            model.addAttribute("formEmail", email.trim());
            model.addAttribute("formSubject", subject.trim());
            model.addAttribute("formMessage", message.trim());
        }
        model.addAttribute("activeNav", "contact");
        model.addAttribute("errors", errors);
        return "contact";
    }

    @GetMapping({"/login.php", "/login"})
    public String loginPage(@RequestParam(required = false) String error, Model model) {
        model.addAttribute("activeNav", "login");
        model.addAttribute("error", error != null ? "Invalid username or password." : "");
        return "login";
    }

    @GetMapping({"/signup.php", "/signup"})
    public String signupPage(Model model) {
        model.addAttribute("activeNav", "signup");
        model.addAttribute("errors", List.of());
        model.addAttribute("success", false);
        model.addAttribute("formUsername", "");
        model.addAttribute("formEmail", "");
        return "signup";
    }

    @PostMapping({"/signup.php", "/signup"})
    public String signupSubmit(@RequestParam(defaultValue = "") String username,
                               @RequestParam(defaultValue = "") String email,
                               @RequestParam(defaultValue = "") String password,
                               @RequestParam(name = "confirm_password", defaultValue = "") String confirmPassword,
                               Model model) {
        String cleanUser = username.trim();
        String cleanEmail = email.trim();
        List<String> errors = new ArrayList<>();
        if (cleanUser.isEmpty()) errors.add("Please enter a username.");
        if (!EMAIL_RX.matcher(cleanEmail).matches()) errors.add("Please enter a valid email address.");
        if (password.length() < 4) errors.add("Password must be at least 4 characters.");
        if (!password.equals(confirmPassword)) errors.add("Passwords do not match.");
        if (errors.isEmpty() && userRepo.existsByUsernameIgnoreCaseOrEmailIgnoreCase(cleanUser, cleanEmail)) {
            errors.add("That username or email is already registered.");
        }

        if (errors.isEmpty()) {
            User u = new User();
            u.setUsername(cleanUser);
            u.setEmail(cleanEmail);
            u.setPassword(passwordEncoder.encode(password));
            u.setRole("user");
            userRepo.save(u);
            model.addAttribute("success", true);
            model.addAttribute("formUsername", "");
            model.addAttribute("formEmail", "");
        } else {
            model.addAttribute("success", false);
            model.addAttribute("formUsername", cleanUser);
            model.addAttribute("formEmail", cleanEmail);
        }
        model.addAttribute("activeNav", "signup");
        model.addAttribute("errors", errors);
        return "signup";
    }

    @GetMapping({"/portfolio.php", "/portfolio"})
    public String portfolioPage(Authentication auth,
                                @RequestParam(required = false) String status,
                                @RequestParam(required = false) String error,
                                Model model) {
        User u = currentUser(auth);
        model.addAttribute("activeNav", "portfolio");
        model.addAttribute("username", u.getUsername());
        model.addAttribute("portfolios", portfolioService.getUserPortfoliosWithViews(u.getId()));
        model.addAttribute("todayDate", LocalDate.now().toString());
        model.addAttribute("status", status == null ? "" : status);
        model.addAttribute("error", error == null ? "" : error);
        return "portfolio";
    }

    @PostMapping("/add_portfolio.php")
    public String addPortfolio(Authentication auth,
                               @RequestParam("portfolio_name") String name,
                               @RequestParam(value = "portfolio_type", defaultValue = "personal") String type) {
        try {
            portfolioService.createPortfolio(currentUser(auth).getId(), name, type);
            return "redirect:/portfolio.php";
        } catch (IllegalStateException dup) {
            return "redirect:/portfolio.php?error=duplicate_portfolio";
        }
    }

    @GetMapping("/edit_portfolio.php")
    public String editPortfolioForm(Authentication auth, @RequestParam Integer id, Model model) {
        Portfolio p = portfolioRepo.findByIdAndUserId(id, currentUser(auth).getId()).orElse(null);
        if (p == null) return "redirect:/portfolio.php";
        model.addAttribute("activeNav", "portfolio");
        model.addAttribute("portfolio", p);
        return "edit_portfolio";
    }

    @PostMapping("/edit_portfolio.php")
    public String editPortfolioSubmit(Authentication auth, @RequestParam Integer id,
                                      @RequestParam("portfolio_name") String name,
                                      @RequestParam("portfolio_type") String type) {
        portfolioService.updatePortfolio(currentUser(auth).getId(), id, name, type);
        return "redirect:/portfolio.php";
    }

    @GetMapping("/delete_portfolio.php")
    public String deletePortfolioConfirm(Authentication auth, @RequestParam Integer id, Model model) {
        Portfolio p = portfolioRepo.findByIdAndUserId(id, currentUser(auth).getId()).orElse(null);
        if (p == null) return "redirect:/portfolio.php";
        model.addAttribute("activeNav", "portfolio");
        model.addAttribute("portfolio", p);
        model.addAttribute("stockCount", stockRepo.countByPortfolioId(p.getId()));
        model.addAttribute("deleteError", null);
        return "delete_portfolio";
    }

    @PostMapping("/delete_portfolio.php")
    public String deletePortfolioSubmit(Authentication auth, @RequestParam Integer id) {
        portfolioService.deletePortfolio(currentUser(auth).getId(), id);
        return "redirect:/portfolio.php?status=portfolio_deleted";
    }

    @GetMapping({"/export_portfolio.php", "/export_portfolio"})
    public ResponseEntity<byte[]> exportPortfolio(Authentication auth, @RequestParam(required = false) Integer id) {
        User u = currentUser(auth);
        List<Portfolio> selected = id != null
                ? portfolioRepo.findByIdAndUserId(id, u.getId()).map(List::of).orElse(List.of())
                : portfolioRepo.findByUserId(u.getId());
        String csv = portfolioService.buildCsv(selected);
        String filename = (id != null && !selected.isEmpty())
                ? "myshareapp_" + selected.get(0).getPortfolioName().replaceAll("[^a-zA-Z0-9_-]+", "_") + ".csv"
                : "myshareapp_all_portfolios.csv";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=utf-8"))
                .body(csv.getBytes(StandardCharsets.UTF_8));
    }

    @PostMapping("/add_stock.php")
    public String addStock(Authentication auth,
                           @RequestParam("portfolio_id") Integer portfolioId,
                           @RequestParam String symbol,
                           @RequestParam(value = "purchase_type", defaultValue = "Secondary") String purchaseType,
                           @RequestParam int quantity,
                           @RequestParam(value = "buy_price", required = false) BigDecimal buyPrice,
                           @RequestParam(value = "buy_date", required = false) String buyDate) {
        try {
            stockService.addStock(currentUser(auth).getId(), portfolioId, symbol, purchaseType, quantity, buyPrice, buyDate);
            return "redirect:/portfolio.php";
        } catch (Exception e) {
            return "redirect:/portfolio.php?error=" + URLEncoder.encode(e.getMessage(), StandardCharsets.UTF_8);
        }
    }

    @GetMapping("/edit_stock.php")
    public String editStockForm(Authentication auth, @RequestParam Integer id, Model model) {
        try {
            Stock s = stockService.findForUser(id, currentUser(auth).getId());
            model.addAttribute("activeNav", "portfolio");
            model.addAttribute("stock", s);
            return "edit_stock";
        } catch (Exception e) {
            return "redirect:/portfolio.php";
        }
    }

    @PostMapping("/edit_stock.php")
    public String editStockSubmit(Authentication auth, @RequestParam Integer id,
                                  @RequestParam String symbol,
                                  @RequestParam("purchase_type") String purchaseType,
                                  @RequestParam int quantity,
                                  @RequestParam(value = "buy_price", required = false) BigDecimal buyPrice,
                                  @RequestParam(value = "buy_date", required = false) String buyDate) {
        stockService.editStock(currentUser(auth).getId(), id, symbol, purchaseType, quantity, buyPrice, buyDate);
        return "redirect:/portfolio.php";
    }

    @GetMapping("/delete_stock.php")
    public String deleteStock(Authentication auth, @RequestParam Integer id) {
        try {
            stockService.deleteStock(currentUser(auth).getId(), id);
        } catch (Exception ignored) {}
        return "redirect:/portfolio.php";
    }

    @PostMapping("/sell_stock.php")
    public String sellStock(Authentication auth,
                            @RequestParam("stock_id") Integer stockId,
                            @RequestParam("sell_quantity") int sellQty,
                            @RequestParam("sell_price") BigDecimal sellPrice,
                            @RequestParam(value = "sell_date", required = false) String sellDate) {
        try {
            stockService.sellStock(currentUser(auth).getId(), stockId, sellQty, sellPrice, sellDate);
            return "redirect:/portfolio.php";
        } catch (Exception e) {
            return "redirect:/portfolio.php?error=" + URLEncoder.encode(e.getMessage(), StandardCharsets.UTF_8);
        }
    }

    @GetMapping("/access-denied")
    public String accessDenied(Model model) {
        model.addAttribute("status", 403);
        model.addAttribute("title", "Access Denied");
        model.addAttribute("message", "Administrator privileges are required to view this page.");
        return "error";
    }
}
