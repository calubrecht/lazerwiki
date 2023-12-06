package us.calubrecht.lazerwiki.service;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import us.calubrecht.lazerwiki.macro.CustomMacro;
import us.calubrecht.lazerwiki.macro.Macro;
import us.calubrecht.lazerwiki.model.PageDesc;
import us.calubrecht.lazerwiki.plugin.EditToolbarDef;
import us.calubrecht.lazerwiki.plugin.Plugin;
import us.calubrecht.lazerwiki.plugin.WikiPlugin;
import us.calubrecht.lazerwiki.responses.PageData;
import us.calubrecht.lazerwiki.responses.PageData.PageFlags;
import us.calubrecht.lazerwiki.service.renderhelpers.RenderContext;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = {PluginService.class},
        properties = { "lazerwiki.plugin.scan.packages=us.calubrecht.lazerwiki.service" })
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PluginServiceTest {

    @Autowired
    PluginService underTest;

    @Test
    void registerPlugins() {
      assertTrue(underTest.plugins.size() > 0);
      assertTrue(underTest.toolbarDefs.size() > 0);
    }

    @Test
    void testGetEditToolbarDefs() {
        underTest.toolbarDefs = List.of (new EditToolbarDef("name", "icon.png", "()=> {doSomething()}"));
        assertEquals("var LAZERWIKI_PLUGINS=[{name:\"name\", icon:\"icon.png\", script:()=> {doSomething()}}];", underTest.getEditToolbarDefs());
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
        public GoodPlugin() {

        }

        @Override
        public String getName() {
            return "Good Plugin";
        }

        @Override
        public List<EditToolbarDef> getEditToolbarDefinitions() {
            return List.of (new EditToolbarDef("name", "icon.png", "()=> {doSomething()}"));
        }
    }
}