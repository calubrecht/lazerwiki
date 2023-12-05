package us.calubrecht.lazerwiki.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;

import us.calubrecht.lazerwiki.service.PluginService;

@RestController
@RequestMapping("")
public class PluginController {

    /*@Autowired
    ObjectMapper mapper;*/
    @Autowired
    PluginService pluginService;

    @GetMapping("_resources/js/pluginJS.js")
    public ResponseEntity<byte[]> getPluginJS(HttpServletRequest request) {
        try {
         /*   String script= "(currentText, selectStart, selectEnd) => {"+
                    "window.alert(\"Selected Text is \" + currentText.substring(selectStart, selectEnd)); return [-1, \"PLUGIN\"]}";
            Map<String, String> plugins = Map.of(
                    "icon", "plugin.png",
                    "name", "Do Something",
                    "script", "SCRIPTHERE");
            String sPlugins = mapper.writeValueAsString(plugins).replace("\"SCRIPTHERE\"", script);
            String output = "window.LAZERWIKI_PLUGINS=[%s];".formatted(sPlugins);*/
            URL url = new URL(request.getRequestURL().toString());
            String mimeType = URLConnection.guessContentTypeFromName("pluginJS.js");
            MediaType mediaType = MediaType.parseMediaType(mimeType);
            return ResponseEntity.ok().contentType(mediaType).body(pluginService.getEditToolbarDefs().getBytes());
        } catch (IOException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
