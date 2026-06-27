package us.calubrecht.lazerwiki.service;

import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.exc.StreamReadException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import us.calubrecht.lazerwiki.model.Site;
import us.calubrecht.lazerwiki.model.User;
import us.calubrecht.lazerwiki.repository.SiteRepository;
import us.calubrecht.lazerwiki.service.exception.SiteSettingsException;
import us.calubrecht.lazerwiki.util.DbSupport;

@Service
public class SiteService {

  @Value("${lazerWiki.default.site.title}")
  String defaultTitle;

  @Autowired SiteRepository siteRepository;

  ObjectMapper objectMapper;

  @PostConstruct
  public void init() {
    objectMapper =
        JsonMapper.builder()
            .enable(DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY)
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            .build();
  }

  @Transactional(readOnly = true)
  @Cacheable("sitesForHostname")
  public String getSiteForHostname(String hostname) {
    Site s = siteRepository.findByHostname(hostname.toLowerCase());
    if (s != null) {
      return s.name;
    }
    return siteRepository.findByHostname("*").name;
  }

  @Transactional(readOnly = true)
  public String getSiteNameForHostname(String hostname) {
    Site s = siteRepository.findByHostname(hostname.toLowerCase());
    if (s == null) {
      s = siteRepository.findByHostname("*");
    }
    if (s == null || s.siteName == null) {
      return defaultTitle;
    }
    return s.siteName;
  }

  @Transactional(readOnly = true)
  public Object getSettingForHostname(String hostname, String setting) {
    Site s = siteRepository.findByHostname(hostname.toLowerCase());
    if (s == null || !s.settings.containsKey(setting)) {
      s = siteRepository.findByHostname("*");
    }
    if (s == null) {
      return null;
    }
    return s.settings.get(setting);
  }

  @Transactional(readOnly = true)
  public Object getSettingForSite(String site, String setting) {
    Optional<Site> sOpt = siteRepository.findById(site);
    Site s = sOpt.filter(ss -> ss.settings.containsKey(setting)).orElse(null);
    if (s == null) {
      s = siteRepository.findByHostname("*");
    }
    if (s == null) {
      return null;
    }
    return s.settings.get(setting);
  }

  @Transactional(readOnly = true)
  @Cacheable("hostForSitename")
  public String getHostForSitename(String site) {
    Optional<Site> s = siteRepository.findById(site);
    return s.map(ss -> ss.hostname).orElse("*");
  }

  @Transactional(readOnly = true)
  public List<Site> getAllSites(User u) {
    List<String> roles = u.getRolesString();
    return DbSupport.toStream(siteRepository.findAll())
        .filter(site -> canAdminSite(roles, site.name))
        .toList();
  }

  boolean canAdminSite(List<String> roles, String siteName) {
    return roles.contains("ROLE_ADMIN") || roles.contains("ROLE_ADMIN:" + siteName);
  }

  @Transactional
  @CacheEvict(value = "sitesForHostname", allEntries = true)
  public boolean addSite(String name, String hostName, String siteName) {
    if (siteRepository.findById(name).isPresent()) {
      return false;
    }
    Site s = new Site(name, hostName, siteName);
    siteRepository.save(s);
    return true;
  }

  @Transactional
  @CacheEvict(
      value = {"sitesForHostname", "hostsForSitename"},
      allEntries = true)
  public Site setSiteSettings(String siteName, String hostName, String settings, User u)
      throws SiteSettingsException {
    if (!canAdminSite(u.getRolesString(), siteName)) {
      throw new SiteSettingsException("Cannot admin site " + siteName);
    }
    if (siteRepository.findById(siteName).isEmpty()) {
      throw new SiteSettingsException("Site \"" + siteName + "\" does not exist");
    }
    try {
      @SuppressWarnings("unchecked")
      Map<String, Object> settingsMap = objectMapper.readValue(settings, Map.class);
      Optional<Site> site = siteRepository.findById(siteName);
      site.ifPresent(
          s -> {
            s.hostname = hostName;
            s.settings = settingsMap;
            siteRepository.save(s);
          });
      return site.orElse(null);
    } catch (StreamReadException sre) {
      throw new SiteSettingsException("Settings must be set to valid json");
    }
  }
}
