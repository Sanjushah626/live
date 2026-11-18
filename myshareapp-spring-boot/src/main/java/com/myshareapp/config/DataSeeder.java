package com.myshareapp.config;

import com.myshareapp.model.*;
import com.myshareapp.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Component
public class DataSeeder implements CommandLineRunner {
    private final UserRepository userRepo;
    private final PortfolioRepository portfolioRepo;
    private final StockRepository stockRepo;
    private final SoldStockRepository soldStockRepo;
    private final NewsRepository newsRepo;
    private final ContactRepository contactRepo;

    public DataSeeder(UserRepository userRepo, PortfolioRepository portfolioRepo, StockRepository stockRepo,
                      SoldStockRepository soldStockRepo, NewsRepository newsRepo, ContactRepository contactRepo) {
        this.userRepo = userRepo;
        this.portfolioRepo = portfolioRepo;
        this.stockRepo = stockRepo;
        this.soldStockRepo = soldStockRepo;
        this.newsRepo = newsRepo;
        this.contactRepo = contactRepo;
    }

    @Override
    public void run(String... args) {
        if (userRepo.count() > 0) return;

        User suraj = new User();
        suraj.setUsername("suraj");
        suraj.setEmail("suraj@gmail.com");
        suraj.setPassword("$2y$10$Ul5lxvW2z7ITKv4qj.vHVOf2YnuY2znk.KCTtZbfGC6BZTmRUGlj6");
        suraj.setRole("admin");
        suraj.setCreatedAt(LocalDateTime.of(2026, 7, 11, 7, 28, 22));
        suraj = userRepo.save(suraj);

        User sanju = new User();
        sanju.setUsername("sanju");
        sanju.setEmail("sanju@gmail.com");
        sanju.setPassword("$2y$10$vFb.mTgNYy5.U3pP6OZ/qe4aurrTDsR/dQKL0CgmeutdnMvNnUWny");
        sanju.setRole("user");
        sanju.setCreatedAt(LocalDateTime.of(2026, 7, 11, 7, 49, 1));
        sanju = userRepo.save(sanju);

        User sarika = new User();
        sarika.setUsername("sarika");
        sarika.setEmail("sarika@gmail.com");
        sarika.setPassword("$2y$10$BZ3CILqoyaPLV7tyOGIXT.yo3nOG2/lEmd2Li14aD8USG1be7rLWq");
        sarika.setRole("user");
        sarika.setCreatedAt(LocalDateTime.of(2026, 7, 17, 14, 53, 59));
        sarika = userRepo.save(sarika);

        User demoUser = new User();
        demoUser.setUsername("user");
        demoUser.setEmail("user@gmail.com");
        demoUser.setPassword("$2y$10$nlbDR6QRL6VuvVSe3.4LHu2hBU6BZrHv9wKEmpmHhtDyZ2sWKF9My");
        demoUser.setRole("user");
        demoUser.setCreatedAt(LocalDateTime.of(2026, 10, 26, 8, 48, 10));
        demoUser = userRepo.save(demoUser);

        Portfolio pLong = savePortfolio(sanju.getId(), "longterm", "personal", "35600.00", "5033.39");
        Portfolio pTest = savePortfolio(sanju.getId(), "test", "personal", "0.00", "0.00");
        Portfolio pSarika = savePortfolio(sarika.getId(), "sarika", "personal", "254000.00", "-467275.45");
        Portfolio pPcil = savePortfolio(sarika.getId(), "pcil", "personal", "0.00", "0.00");
        Portfolio pUser = savePortfolio(demoUser.getId(), "longterm", "personal", "0.00", "0.00");

        saveStock(pLong.getId(), "PHCL", "Secondary", 400, "261.30", LocalDate.of(2026, 1, 8), 0, "0.00");
        Stock sMehl = saveStock(pLong.getId(), "MEHL", "Secondary", 0, "300.00", null, 100, "35600.00");
        saveStock(pLong.getId(), "SOPL", "IPO", 10, "100.00", null, 0, "0.00");
        saveStock(pTest.getId(), "AHPC", "Secondary", 145, "254.00", null, 0, "0.00");
        Stock sEbl = saveStock(pSarika.getId(), "EBL", "Secondary", 0, "198.00", null, 100, "54000.00");
        Stock sAnlb = saveStock(pSarika.getId(), "ANLB", "Secondary", 100, "6980.00", null, 100, "200000.00");
        saveStock(pPcil.getId(), "PCIL", "IPO", 10, "100.00", null, 0, "0.00");
        saveStock(pUser.getId(), "PHCL", "Secondary", 400, "258.00", null, 0, "0.00");

        saveSold(pSarika.getId(), sEbl.getId(), "EBL", 100, "198.00", "540.00", "19800.00", "54000.00", "178.20", "8.10", "25.00", "2549.15", "31439.55", LocalDate.of(2026, 7, 17));
        saveSold(pLong.getId(), sMehl.getId(), "MEHL", 100, "300.00", "356.00", "30000.00", "35600.00", "128.16", "5.34", "25.00", "408.11", "5033.39", LocalDate.of(2026, 7, 18));
        saveSold(pSarika.getId(), sAnlb.getId(), "ANLB", 100, "6980.00", "2000.00", "698000.00", "200000.00", "660.00", "30.00", "25.00", "0.00", "-498715.00", LocalDate.of(2026, 7, 28));

        saveNews(suraj.getId(), "Nepal SBI Bank Announces 8.25% Dividend for FY 2082/83",
                "Nepal SBI Bank has announced an 8.25% dividend for its shareholders, consisting of 3.4% bonus shares and 4.85% cash dividend.",
                "news_6a68dac04ed79.jpg", LocalDateTime.of(2026, 11, 13, 16, 30, 0));
        saveNews(suraj.getId(), "Super Mai Hydropower Announces 18.421% Dividend",
                "Super Mai Hydropower has proposed an 18.421% dividend for FY 2082/83, including 14% bonus shares and 4.421% cash dividend.",
                null, LocalDateTime.of(2026, 11, 13, 16, 31, 17));
        saveNews(suraj.getId(), "Unilever Nepal Announces 1540% Cash Dividend",
                "Unilever Nepal has recommended a 1540% cash dividend for FY 2082/83, equivalent to Rs. 1,540 per equity share.",
                null, LocalDateTime.of(2026, 11, 13, 16, 33, 4));
        saveNews(suraj.getId(), "Api Power Announces 5.2632% Dividend",
                "Api Power Company has proposed a 5.2632% total dividend for FY 2082/83, consisting of 5% bonus shares and 0.2632% cash dividend.",
                null, LocalDateTime.of(2026, 11, 13, 16, 33, 49));
        saveNews(suraj.getId(), "Chhimek Laghubitta Announces 25% Dividend",
                "Chhimek Laghubitta has declared a 25% dividend for FY 2082/83, comprising 11.9631% bonus shares and 13.0369% cash dividend.",
                null, LocalDateTime.of(2026, 11, 13, 16, 34, 12));

        saveContact("sanju", "sanju@gmail.com", "test", "testing if it is working or not", LocalDateTime.of(2026, 7, 11, 7, 49, 35));
        saveContact("sanju", "sanju@gmail.com", "upload some news", "news tab is empty upload some news", LocalDateTime.of(2026, 7, 11, 7, 50, 12));
        saveContact("sarika", "sarika@gmail.com", "Review", "5 star", LocalDateTime.of(2026, 7, 17, 14, 59, 1));
    }

    private Portfolio savePortfolio(Integer userId, String name, String type, String soldVal, String realized) {
        Portfolio p = new Portfolio();
        p.setUserId(userId);
        p.setPortfolioName(name);
        p.setPortfolioType(type);
        p.setTotalSoldValue(new BigDecimal(soldVal));
        p.setTotalRealizedProfit(new BigDecimal(realized));
        return portfolioRepo.save(p);
    }

    private Stock saveStock(Integer pid, String sym, String type, int qty, String price, LocalDate dt, int soldUnits, String soldVal) {
        Stock s = new Stock();
        s.setPortfolioId(pid);
        s.setSymbol(sym);
        s.setPurchaseType(type);
        s.setQuantity(qty);
        s.setBuyPrice(new BigDecimal(price));
        s.setBuyDate(dt);
        s.setSoldUnits(soldUnits);
        s.setSoldValue(new BigDecimal(soldVal));
        return stockRepo.save(s);
    }

    private void saveSold(Integer pid, Integer sid, String sym, int qty, String buyP, String sellP, String inv,
                          String saleVal, String broker, String sebon, String dp, String cgt, String net, LocalDate dt) {
        SoldStock ss = new SoldStock();
        ss.setPortfolioId(pid);
        ss.setStockId(sid);
        ss.setSymbol(sym);
        ss.setQuantitySold(qty);
        ss.setBuyPrice(new BigDecimal(buyP));
        ss.setSellPrice(new BigDecimal(sellP));
        ss.setInvestmentCost(new BigDecimal(inv));
        ss.setSaleValue(new BigDecimal(saleVal));
        ss.setBrokerCharge(new BigDecimal(broker));
        ss.setSebonCharge(new BigDecimal(sebon));
        ss.setDpCharge(new BigDecimal(dp));
        ss.setCapitalGainTax(new BigDecimal(cgt));
        ss.setNetProfit(new BigDecimal(net));
        ss.setSellDate(dt);
        soldStockRepo.save(ss);
    }

    private void saveNews(Integer uid, String title, String desc, String img, LocalDateTime at) {
        News n = new News();
        n.setUserId(uid);
        n.setTitle(title);
        n.setDescription(desc);
        n.setImage(img);
        n.setCreatedAt(at);
        newsRepo.save(n);
    }

    private void saveContact(String name, String email, String subj, String msg, LocalDateTime at) {
        Contact c = new Contact();
        c.setName(name);
        c.setEmail(email);
        c.setSubject(subj);
        c.setMessage(msg);
        c.setCreatedAt(at);
        contactRepo.save(c);
    }
}
