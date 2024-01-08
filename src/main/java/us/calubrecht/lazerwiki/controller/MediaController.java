package us.calubrecht.lazerwiki.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import us.calubrecht.lazerwiki.responses.MediaListResponse;
import us.calubrecht.lazerwiki.service.MediaService;
import us.calubrecht.lazerwiki.service.exception.MediaReadException;
import us.calubrecht.lazerwiki.service.exception.MediaWriteException;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.security.Principal;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("_media/")
public class MediaController {

    @Autowired
    MediaService mediaService;

    @GetMapping("{fileName}")
    public ResponseEntity<byte[]> getFile(@PathVariable String fileName, @RequestParam Map<String,String> requestParams, Principal principal, HttpServletRequest request) {
        try {
            URL url = new URL(request.getRequestURL().toString());
            String size = requestParams.keySet().stream().findAny().orElse(null);
            String userName = principal == null ? null : principal.getName();
            String mimeType = URLConnection.guessContentTypeFromName(fileName);
            MediaType mediaType = mimeType != null ? MediaType.parseMediaType(mimeType) : MediaType.APPLICATION_OCTET_STREAM;
            return ResponseEntity.ok().contentType(mediaType)
                    .cacheControl(CacheControl.noCache().mustRevalidate())
                    .lastModified(mediaService.getFileLastModified(url.getHost(), fileName))
                    .body(mediaService.getBinaryFile(url.getHost(), userName, fileName, size));
        } catch (IOException e) {
            return ResponseEntity.notFound().build();
        } catch (MediaReadException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @PostMapping("upload")
    public ResponseEntity<String> saveFile(@RequestParam("file") MultipartFile file, @RequestParam("namespace") String namespace, Principal principal, HttpServletRequest request) throws IOException {
        try {
            URL url = new URL(request.getRequestURL().toString());
            String userName = principal.getName();
            mediaService.saveFile(url.getHost(), userName, file, namespace);
            return ResponseEntity.ok("Upload successful");
        } catch (MediaWriteException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @GetMapping("list")
    MediaListResponse listFiles(Principal principal, HttpServletRequest request) throws MalformedURLException {
        URL url = new URL(request.getRequestURL().toString());
        String userName = principal == null ? null : principal.getName();
        return mediaService.getAllFiles(url.getHost(), userName);
    }

    @DeleteMapping("{fileName}")
    public ResponseEntity<Void> deleteFile(@PathVariable String fileName, Principal principal, HttpServletRequest request) throws IOException {
        URL url = new URL(request.getRequestURL().toString());
        String userName = principal.getName();
        try {
            mediaService.deleteFile(url.getHost(), fileName, userName);
            return ResponseEntity.ok().build();
        } catch (MediaWriteException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }
}
