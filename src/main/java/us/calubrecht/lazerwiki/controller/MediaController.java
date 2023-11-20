package us.calubrecht.lazerwiki.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.apache.tomcat.util.http.fileupload.FileUploadException;
import org.apache.tomcat.util.http.fileupload.impl.FileSizeLimitExceededException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartFile;
import us.calubrecht.lazerwiki.model.MediaListResponse;
import us.calubrecht.lazerwiki.model.MediaRecord;
import us.calubrecht.lazerwiki.service.MediaService;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.security.Principal;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("_media/")
public class MediaController {

    @Autowired
    MediaService mediaService;

    @GetMapping("{fileName}")
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

    @PostMapping("upload")
    public String saveFile(@RequestParam("file") MultipartFile file, @RequestParam("namespace") String namespace, Principal principal, HttpServletRequest request) {
        try {
            URL url = new URL(request.getRequestURL().toString());
            String userName = principal.getName();
            mediaService.saveFile(url.getHost(), userName, file, namespace);
            return "Upload successful";
        } catch (IOException e) {
            return "oops";
        }
    }

    @GetMapping("list")
    MediaListResponse listFiles(Principal principal, HttpServletRequest request) throws MalformedURLException {
        URL url = new URL(request.getRequestURL().toString());
        String userName = principal == null ? null : principal.getName();
        return mediaService.getAllFiles(url.getHost(), userName);
    }

    @DeleteMapping("{fileName}")
    public void deleteFile(@PathVariable String fileName, Principal principal, HttpServletRequest request) throws IOException {
        URL url = new URL(request.getRequestURL().toString());
        String userName = principal.getName();
        mediaService.deleteFile(url.getHost(), fileName, userName);
    }
}
