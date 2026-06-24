package us.calubrecht.lazerwiki.repository;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class TagRepository {

  @Autowired JdbcTemplate jdbcTemplate;

  public List<String> getAllActiveTags(String site) {
    return jdbcTemplate.queryForList("SELECT tag FROM activeTags WHERE site=?", String.class, site);
  }
}
