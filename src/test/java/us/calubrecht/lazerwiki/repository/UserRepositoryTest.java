package us.calubrecht.lazerwiki.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import us.calubrecht.lazerwiki.LazerWikiApplication;
import us.calubrecht.lazerwiki.model.User;
import us.calubrecht.lazerwiki.model.UserRole;

import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(classes = {LazerWikiApplication.class})
@ActiveProfiles("test")
public class UserRepositoryTest
{
    @Autowired
    UserRepository userRepository;
    @Test
    @Transactional
    public void testQueryUser() {
        User bob = userRepository.findByUserName("Bob").get();

        assertNotNull(bob);
        assertEquals(2, bob.roles.size());
    }

    @Test
    @Transactional
    public void testSave() {
        User newUser = new User("Loser", "hashed");
        newUser.roles = Arrays.asList(new UserRole(newUser, "Role1"), new UserRole(newUser, "Role2"));
        newUser.setSettings(Map.of());
        userRepository.save(newUser);

        User loser = userRepository.findByUserName("Loser").get();

        assertNotNull(loser);
        assertEquals(2, loser.roles.size());
    }
}
