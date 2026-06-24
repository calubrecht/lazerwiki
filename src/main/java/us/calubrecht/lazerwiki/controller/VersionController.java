package us.calubrecht.lazerwiki.controller;

import org.springframework.beans.factory.annotation.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/")
public class VersionController {
  @Value("${app.version}")
  private String appVersion;

  @RequestMapping("version")
  public String version() {
    return appVersion;
  }
}
