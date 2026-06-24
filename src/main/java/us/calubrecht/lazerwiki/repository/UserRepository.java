package us.calubrecht.lazerwiki.repository;

import java.util.Optional;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import us.calubrecht.lazerwiki.model.User;

@Repository
public interface UserRepository extends CrudRepository<User, Integer> {

  Optional<User> findByUserName(String userName);

  void deleteByUserName(String usrName);
}
