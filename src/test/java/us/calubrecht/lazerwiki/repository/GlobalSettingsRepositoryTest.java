package us.calubrecht.lazerwiki.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import us.calubrecht.lazerwiki.LazerWikiApplication;
import us.calubrecht.lazerwiki.model.GlobalSettings;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(classes = {LazerWikiApplication.class})
@ActiveProfiles("test")
public class GlobalSettingsRepositoryTest {

    @Autowired
    GlobalSettingsRepository globalSettingsRepository;

    @Test
    public void testGetSettings() {
        GlobalSettings settings = globalSettingsRepository.getSettings();

        assertEquals(Map.of("Setting1", "Value1"), settings.settings);
    }
}
