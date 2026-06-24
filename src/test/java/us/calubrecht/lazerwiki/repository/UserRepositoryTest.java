package us.calubrecht.lazerwiki.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Arrays;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import us.calubrecht.lazerwiki.LazerWikiApplication;
import us.calubrecht.lazerwiki.model.User;
import us.calubrecht.lazerwiki.model.UserRole;

@SpringBootTest(classes = {LazerWikiApplication.class})
@ActiveProfiles("test")
public class UserRepositoryTest {
  @Autowired UserRepository userRepository;

  @Test
  @Transactional
  public void test_queryUser() {
    User bob = userRepository.findByUserName("Bob").get();

    assertNotNull(bob);
    assertEquals(2, bob.roles.size());
  }

  @Test
  @Transactional
  public void test_save() {
    User newUser = new User("Loser", "hashed");
    newUser.roles = Arrays.asList(new UserRole(newUser, "Role1"), new UserRole(newUser, "Role2"));
    newUser.setSettings(Map.of());
    userRepository.save(newUser);

    User loser = userRepository.findByUserName("Loser").get();

    assertNotNull(loser);
    assertEquals(2, loser.roles.size());
  }
}
