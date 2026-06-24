package us.calubrecht.lazerwiki.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import us.calubrecht.lazerwiki.LazerWikiApplication;

@SpringBootTest(classes = {LazerWikiApplication.class})
@ActiveProfiles("test")
public class IdRepositoryTest {

  @Autowired IdRepository idRepository;

  @Test
  void testGetNewId() {
    assertEquals(8L, idRepository.getNewId());
    assertEquals(9L, idRepository.getNewId());
  }
}
