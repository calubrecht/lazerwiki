package us.calubrecht.lazerwiki.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import us.calubrecht.lazerwiki.service.MediaService;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.security.Principal;
import java.util.Optional;

@RestController
@RequestMapping("_media/")
public class MediaController {

    @Autowired
    MediaService mediaService;

    @RequestMapping("{fileName}")
    public ResponseEntity<byte[]> getFile(@PathVariable String fileName, Principal principal, HttpServletRequest request) {
        try {
            URL url = new URL(request.getRequestURL().toString());
            String userName = principal == null ? null : principal.getName();
            String mimeType = URLConnection.guessContentTypeFromName(fileName);
            MediaType mediaType = mimeType != null ? MediaType.parseMediaType(mimeType) : MediaType.APPLICATION_OCTET_STREAM;
            return ResponseEntity.ok().contentType(mediaType).body(mediaService.getBinaryFile(url.getHost(), userName, fileName));
        } catch (IOException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
