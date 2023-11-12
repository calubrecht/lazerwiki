package us.calubrecht.lazerwiki.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;

@Service
public class EntityManagerProxy {
    @PersistenceContext
    private EntityManager em;

    public void flush() {
        em.flush();
    }
}
