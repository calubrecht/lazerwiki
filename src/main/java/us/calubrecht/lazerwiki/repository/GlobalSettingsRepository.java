package us.calubrecht.lazerwiki.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import us.calubrecht.lazerwiki.model.GlobalSettings;

@Repository
public interface GlobalSettingsRepository extends CrudRepository<GlobalSettings, Integer>  {
    static final Integer SINGLE_ID = 1;

    public default GlobalSettings getSettings() {
        return findById(SINGLE_ID).orElse(new GlobalSettings());
    }
}
