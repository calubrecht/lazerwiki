package us.calubrecht.lazerwiki.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import us.calubrecht.lazerwiki.service.MacroCssService;
import us.calubrecht.lazerwiki.service.ResourceService;
import us.calubrecht.lazerwiki.service.WarResourceService;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Optional;

@RestController
@RequestMapping("")
@ConditionalOnProperty("lazerwiki.standalone.ui.warfile")
public class WarfileController {
    @Autowired
    WarResourceService resourceService;  /// Seperate version that only loads from external ui war

    @RequestMapping("assets/{fileName}")
    public ResponseEntity<byte[]> getFile(@PathVariable String fileName, HttpServletRequest request) {
        return getResponseEntity("assets/" + fileName, request);
    }

    @NotNull
    private ResponseEntity<byte[]> getResponseEntity(String fileName, HttpServletRequest request) {
        try {
            String mimeType = URLConnection.guessContentTypeFromName(fileName);
            MediaType mediaType = mimeType != null ? MediaType.parseMediaType(mimeType) : MediaType.APPLICATION_OCTET_STREAM;

            // IF file in jar then serve...
            return ResponseEntity.ok().contentType(mediaType).
                    cacheControl(CacheControl.noCache().mustRevalidate()).
                    lastModified(resourceService.getFileLastModified(fileName)).
                    body(resourceService.getBinaryFile(fileName));
        } catch (IOException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @RequestMapping("")
    public ResponseEntity<byte[]> getIndexFile(HttpServletRequest request) {
        return getResponseEntity("index.html", request);
    }

    @RequestMapping("page/{filename}")
    public ResponseEntity<byte[]> getPageFile(HttpServletRequest request) {
        return getResponseEntity("index.html", request);
    }

    @RequestMapping("{filename}")
    public ResponseEntity<byte[]> getRootFile(@PathVariable("filename") String filename, HttpServletRequest request) {
        return getResponseEntity(filename, request);
    }
}
