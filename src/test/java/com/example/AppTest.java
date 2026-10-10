package com.example;

import com.example.model.Auteur;
import com.example.service.DataInitService;
import com.example.service.PerformanceTestService;
import org.hibernate.stat.Statistics;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import static org.junit.Assert.*;

public class AppTest {

    private static EntityManagerFactory emf;

    @BeforeClass
    public static void setUp() {
        emf = Persistence.createEntityManagerFactory("hibernate-cache-pu");
        EntityManager em = emf.createEntityManager();
        DataInitService initService = new DataInitService();
        initService.em = em;
        em.getTransaction().begin();
        initService.initData();
        em.getTransaction().commit();
        em.close();
    }

    @AfterClass
    public static void tearDown() {
        if (emf != null && emf.isOpen()) {
            emf.close();
        }
    }

    @Test
    public void testSansCache() {
        EntityManager em = emf.createEntityManager();
        PerformanceTestService service = new PerformanceTestService();
        service.em = em;
        service.testPerformanceSansCache();
        em.close();
    }

    @Test
    public void testJoinFetch() {
        EntityManager em = emf.createEntityManager();
        PerformanceTestService service = new PerformanceTestService();
        service.em = em;
        service.testPerformanceAvecJoinFetch();
        em.close();
    }

    @Test
    public void testEntityGraph() {
        EntityManager em = emf.createEntityManager();
        PerformanceTestService service = new PerformanceTestService();
        service.em = em;
        service.testPerformanceAvecEntityGraph();
        em.close();
    }

    @Test
    public void testPerformanceAvecCache() {
        EntityManager em = emf.createEntityManager();
        PerformanceTestService service = new PerformanceTestService();
        service.em = em;
        service.testPerformanceAvecCache();
        em.close();
    }

    @Test
    public void testSecondLevelCacheHit() {
        emf.getCache().evictAll();
        Statistics stats = emf.unwrap(org.hibernate.SessionFactory.class).getStatistics();
        stats.setStatisticsEnabled(true);
        stats.clear();

        // Première session : charger l'auteur 2 depuis la base
        EntityManager em1 = emf.createEntityManager();
        Auteur a1 = em1.find(Auteur.class, 2L);
        assertNotNull(a1);
        em1.close();

        long putCount = stats.getSecondLevelCachePutCount();
        assertTrue("Le cache de second niveau doit contenir l'entité mise en cache", putCount > 0);

        // Deuxième session : recharger l'auteur 2 depuis le cache L2
        EntityManager em2 = emf.createEntityManager();
        Auteur a2 = em2.find(Auteur.class, 2L);
        assertNotNull(a2);
        em2.close();

        long hitCount = stats.getSecondLevelCacheHitCount();
        assertTrue("Un cache hit doit être enregistré pour l'entité en cache L2", hitCount > 0);
    }
}
