package us.calubrecht.lazerwiki.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import us.calubrecht.lazerwiki.model.GlobalSettings;
import us.calubrecht.lazerwiki.service.GlobalSettingsService;

@RestController
@RequestMapping("api/settings/")
public class SettingsController {

  @Autowired GlobalSettingsService globalSettingsService;

  @GetMapping("globalSettings")
  public ResponseEntity<GlobalSettings> getGlobalSettings() {
    return ResponseEntity.ok(globalSettingsService.getSettings());
  }
}
