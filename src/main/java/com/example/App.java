package com.example;

import com.example.service.DataInitService;
import com.example.service.PerformanceTestService;
import org.hibernate.stat.Statistics;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

public class App {

    public static void main(String[] args) {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("hibernate-cache-pu");
        
        // Activation des statistiques Hibernate pour observer les hits et misses du cache L2
        Statistics stats = emf.unwrap(org.hibernate.SessionFactory.class).getStatistics();
        stats.setStatisticsEnabled(true);

        EntityManager em = emf.createEntityManager();

        DataInitService dataInitService = new DataInitService();
        PerformanceTestService performanceTestService = new PerformanceTestService();

        dataInitService.em = em;
        performanceTestService.em = em;

        em.getTransaction().begin();
        dataInitService.initData();
        em.getTransaction().commit();

        // Vider le cache de premier niveau (L1) pour observer les requêtes et les caches L2
        em.clear();

        System.out.println("\n--- Test sans cache ---");
        performanceTestService.testPerformanceSansCache();

        em.clear();

        System.out.println("\n--- Test avec JOIN FETCH ---");
        performanceTestService.testPerformanceAvecJoinFetch();

        em.clear();

        System.out.println("\n--- Test avec Entity Graph ---");
        performanceTestService.testPerformanceAvecEntityGraph();

        em.clear();

        System.out.println("\n--- Test avec cache de second niveau ---");
        performanceTestService.testPerformanceAvecCache();

        // Affichage des statistiques Hibernate du Cache de second niveau
        System.out.println("\n--- Statistiques Hibernate du Cache L2 ---");
        System.out.println("Second Level Cache Hits   : " + stats.getSecondLevelCacheHitCount());
        System.out.println("Second Level Cache Misses : " + stats.getSecondLevelCacheMissCount());
        System.out.println("Second Level Cache Puts   : " + stats.getSecondLevelCachePutCount());
        System.out.println("Query Cache Hits          : " + stats.getQueryCacheHitCount());
        System.out.println("Query Cache Misses        : " + stats.getQueryCacheMissCount());
        System.out.println("Query Cache Puts          : " + stats.getQueryCachePutCount());

        em.close();
        emf.close();
    }
}
