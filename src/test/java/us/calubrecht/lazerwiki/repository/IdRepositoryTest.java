package us.calubrecht.lazerwiki.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import us.calubrecht.lazerwiki.LazerWikiApplication;

@SpringBootTest(classes = {LazerWikiApplication.class})
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class IdRepositoryTest {

  @Autowired IdRepository idRepository;

  @Test
  @Order(1)
  void test_getNewId() {
    assertEquals(8L, idRepository.getNewId());
    assertEquals(9L, idRepository.getNewId());
  }

  @Test
  @Order(2) // Run last, it ruins the IdResository object
  void test_getNewId_failure() {
    JdbcTemplate mockTemplate = Mockito.mock(JdbcTemplate.class);

    idRepository.jdbcTemplate = mockTemplate;
    // Assert that if update does not provide an Id, throws IllegalStateException rather than NPE
    assertThrows(IllegalStateException.class, () -> idRepository.getNewId());
  }
}
