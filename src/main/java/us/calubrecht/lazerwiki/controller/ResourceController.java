package us.calubrecht.lazerwiki.controller;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.URLConnection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import us.calubrecht.lazerwiki.service.MacroCssService;
import us.calubrecht.lazerwiki.service.ResourceService;

@RestController
@RequestMapping("_resources/")
public class ResourceController extends LazerWikiController {

  @Autowired ResourceService resourceService;

  @Autowired MacroCssService macroCssService;

  @RequestMapping("{fileName}")
  public ResponseEntity<byte[]> getFile(@PathVariable String fileName, HttpServletRequest request) {
    try {
      String site = getSite(request);
      String mimeType = URLConnection.guessContentTypeFromName(fileName);
      MediaType mediaType =
          mimeType != null
              ? MediaType.parseMediaType(mimeType)
              : MediaType.APPLICATION_OCTET_STREAM;
      return ResponseEntity.ok()
          .contentType(mediaType)
          .cacheControl(CacheControl.noCache().mustRevalidate())
          .lastModified(resourceService.getFileLastModified(site, fileName))
          .body(resourceService.getBinaryFile(site, fileName));
    } catch (IOException e) {
      return ResponseEntity.notFound().build();
    }
  }

  @RequestMapping("internal/{fileName}")
  public ResponseEntity<byte[]> getFileInternal(
      @PathVariable String fileName, HttpServletRequest request) {
    String mimeType = URLConnection.guessContentTypeFromName(fileName);
    MediaType mediaType =
        mimeType != null ? MediaType.parseMediaType(mimeType) : MediaType.APPLICATION_OCTET_STREAM;
    if (fileName.equals("plugin.css")) {
      return ResponseEntity.ok()
          .cacheControl(CacheControl.noCache())
          .lastModified(resourceService.getBootTime())
          .contentType(mediaType)
          .body(macroCssService.getCss().getBytes());
    }
    return ResponseEntity.notFound().build();
  }
}
