package us.calubrecht.lazerwiki.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import us.calubrecht.lazerwiki.plugin.EditToolbarDef;
import us.calubrecht.lazerwiki.plugin.Plugin;
import us.calubrecht.lazerwiki.plugin.WikiPlugin;

@SpringBootTest(
    classes = {PluginService.class},
    properties = {"lazerwiki.plugin.scan.packages=us.calubrecht.lazerwiki.service"})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PluginServiceTest {

  @Autowired PluginService underTest;

  @MockitoBean SiteService siteService;

  @Test
  void test_registerPlugins() {
    assertFalse(underTest.plugins.isEmpty());
  }

  @Test
  void test_getEditToolbarDefs() {
    assertEquals(
        "var LAZERWIKI_PLUGINS=[{name:\"name\", icon:\"icon.png\", script:()=> {doSomething()}}];",
        underTest.getEditToolbarDefs("site1"));
    when(siteService.getSettingForHostname("site2", "pluginBlacklist"))
        .thenReturn(List.of("Good Plugin"));
    assertEquals("var LAZERWIKI_PLUGINS=[];", underTest.getEditToolbarDefs("site2"));
  }

  @WikiPlugin
  static class BadPlugin extends Plugin {
    public BadPlugin() {
      throw new RuntimeException("oop");
    }

    @Override
    public String getName() {
      return null;
    }

    @Override
    public List<EditToolbarDef> getEditToolbarDefinitions() {
      return null;
    }
  }

  @WikiPlugin
  static class GoodPlugin extends Plugin {
    public GoodPlugin() {}

    @Override
    public String getName() {
      return "Good Plugin";
    }

    @Override
    public List<EditToolbarDef> getEditToolbarDefinitions() {
      return List.of(new EditToolbarDef("name", "icon.png", "()=> {doSomething()}"));
    }
  }
}
