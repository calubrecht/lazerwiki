package us.calubrecht.lazerwiki.service;

import org.springframework.transaction.annotation.Transactional;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import us.calubrecht.lazerwiki.model.GlobalSettings;
import us.calubrecht.lazerwiki.repository.GlobalSettingsRepository;

@Service
public class GlobalSettingsService {

  @Autowired GlobalSettingsRepository repo;

  @Transactional(readOnly = true)
  public GlobalSettings getSettings() {
    return repo.getSettings();
  }

  @Transactional
  public void setSettings(GlobalSettings settings) {
    GlobalSettings existingSettings = repo.getSettings();

    Map<String, Object> newSettings = new HashMap<>(existingSettings.settings);
    newSettings.putAll(settings.settings);
    settings.id = existingSettings.id;
    settings.settings = newSettings;

    repo.save(settings);
  }
}
