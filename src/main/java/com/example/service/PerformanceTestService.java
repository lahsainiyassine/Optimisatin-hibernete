package com.example.service;

import com.example.model.Auteur;

import javax.persistence.EntityGraph;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import java.util.List;

public class PerformanceTestService {

    @PersistenceContext
    public EntityManager em;

    public PerformanceTestService() {
    }

    public PerformanceTestService(EntityManager em) {
        this.em = em;
    }

    public EntityManager getEm() {
        return em;
    }

    public void setEm(EntityManager em) {
        this.em = em;
    }

    /**
     * Étape 7 : Mesure des performances sans cache / sans optimisation
     * Déclenche le problème N+1 requêtes (1 pour la liste d'auteurs + N requêtes pour chaque collection de livres).
     */
    public void testPerformanceSansCache() {
        long start = System.currentTimeMillis();

        TypedQuery<Auteur> query = em.createQuery("SELECT a FROM Auteur a", Auteur.class);
        List<Auteur> auteurs = query.getResultList();

        // Accès aux livres pour provoquer le chargement (Problème N+1)
        for (Auteur auteur : auteurs) {
            auteur.getLivres().size();
        }

        long end = System.currentTimeMillis();
        System.out.println("Durée sans cache : " + (end - start) + " ms");
    }

    /**
     * Étape 8 : Suppression du problème N+1 avec JOIN FETCH
     * Une seule requête SQL récupère à la fois les auteurs et leurs livres.
     */
    public void testPerformanceAvecJoinFetch() {
        long start = System.currentTimeMillis();

        TypedQuery<Auteur> query = em.createQuery("SELECT a FROM Auteur a JOIN FETCH a.livres", Auteur.class);
        query.setHint("org.hibernate.cacheable", true);
        List<Auteur> auteurs = query.getResultList();

        // L'accès aux livres ne déclenche plus de requêtes supplémentaires
        for (Auteur auteur : auteurs) {
            auteur.getLivres().size();
        }

        long end = System.currentTimeMillis();
        System.out.println("Durée avec JOIN FETCH : " + (end - start) + " ms");
    }

    /**
     * Étape 9 : Utilisation des Entity Graphs pour optimiser les requêtes
     * Déclaration déclarative du graphe de chargement sans modifier la requête JPQL.
     */
    public void testPerformanceAvecEntityGraph() {
        long start = System.currentTimeMillis();

        EntityGraph<?> graph = em.getEntityGraph("Auteur.livres");

        TypedQuery<Auteur> query = em.createQuery("SELECT a FROM Auteur a", Auteur.class);
        query.setHint("javax.persistence.fetchgraph", graph);
        List<Auteur> auteurs = query.getResultList();

        for (Auteur auteur : auteurs) {
            auteur.getLivres().size();
        }

        long end = System.currentTimeMillis();
        System.out.println("Durée avec Entity Graph : " + (end - start) + " ms");
    }

    /**
     * Étape 10 : Activation et test du cache de second niveau
     * Effectue plusieurs lectures successives pour observer l'impact du cache.
     */
    public void testPerformanceAvecCache() {
        // Première lecture - chargement depuis la base
        System.out.println("-> Première lecture - chargement depuis la base :");
        testPerformanceAvecJoinFetch();

        // Vidage du cache L1 (Session) pour forcer l'usage du cache L2 lors de la deuxième lecture
        em.clear();

        // Deuxième lecture - chargement depuis le cache
        long start = System.currentTimeMillis();
        TypedQuery<Auteur> query = em.createQuery("SELECT a FROM Auteur a JOIN FETCH a.livres", Auteur.class);
        query.setHint("org.hibernate.cacheable", true);
        List<Auteur> auteurs = query.getResultList();
        for (Auteur auteur : auteurs) {
            auteur.getLivres().size();
        }
        long end = System.currentTimeMillis();
        System.out.println("Durée avec cache (lecture 2) : " + (end - start) + " ms");
    }
}
