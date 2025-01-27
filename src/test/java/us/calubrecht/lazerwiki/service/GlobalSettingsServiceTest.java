package us.calubrecht.lazerwiki.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import us.calubrecht.lazerwiki.model.GlobalSettings;
import us.calubrecht.lazerwiki.repository.GlobalSettingsRepository;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = GlobalSettingsService.class)
@ActiveProfiles("test")
class GlobalSettingsServiceTest {

    @Autowired
    GlobalSettingsService underTest;

    @MockBean
    GlobalSettingsRepository repo;

    @Test
    void getSettings() {
        Map<String, Object> settingsMap = Map.of("Setting1", "Value");
        GlobalSettings settings = new GlobalSettings();
        settings.settings = settingsMap;
        when(repo.getSettings()).thenReturn(settings);

        assertEquals("Value", underTest.getSettings().settings.get("Setting1"));
    }


    @Test
    void setSettings() {
        Map<String, Object> settingsMap = Map.of("Setting1", "Value", "Setting2", "Value2");
        GlobalSettings settings = new GlobalSettings();
        settings.settings = settingsMap;
        settings.id = 1;
        when(repo.getSettings()).thenReturn(settings);

        GlobalSettings newSettings = new GlobalSettings();
        newSettings.settings = Map.of("Setting2", "Value3", "Settings3", 4);
        underTest.setSettings(newSettings);

        Map<String, Object> expectedSettingsMap = Map.of("Setting1", "Value", "Setting2", "Value3", "Settings3", 4);
        GlobalSettings expectedSettings = new GlobalSettings();
        expectedSettings.id = settings.id;
        expectedSettings.settings = expectedSettingsMap;
        verify(repo).save(expectedSettings);
    }
}