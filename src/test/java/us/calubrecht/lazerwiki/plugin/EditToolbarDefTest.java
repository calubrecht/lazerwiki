package us.calubrecht.lazerwiki.plugin;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EditToolbarDefTest {

    @Test
    void toJSDefinition() {
        EditToolbarDef defWithIcon = new EditToolbarDef("withIcon", "anIcon.png", "() => {}");
        assertEquals("{name:\"withIcon\", icon:\"anIcon.png\", script:() => {}}", defWithIcon.toJSDefinition());
        EditToolbarDef defNoIcon = new EditToolbarDef("withIcon", null, "() => {}");
        assertEquals("{name:\"withIcon\", icon:\"toolbarBtn.png\", script:() => {}}", defNoIcon.toJSDefinition());
    }
}