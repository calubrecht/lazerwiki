package us.calubrecht.lazerwiki.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import us.calubrecht.lazerwiki.service.ResourceService;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.security.Principal;

@RestController
@RequestMapping("_resources/")
public class ResourceController {

    @Autowired
    ResourceService resourceService;

    @RequestMapping("{fileName}")
    public ResponseEntity<byte[]> getFile(@PathVariable String fileName, HttpServletRequest request) {
        try {
            URL url = new URL(request.getRequestURL().toString());
            String mimeType = URLConnection.guessContentTypeFromName(fileName);
            MediaType mediaType = mimeType != null ? MediaType.parseMediaType(mimeType) : MediaType.APPLICATION_OCTET_STREAM;
            return ResponseEntity.ok().contentType(mediaType).body(resourceService.getBinaryFile(url.getHost(), fileName));
        } catch (IOException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
