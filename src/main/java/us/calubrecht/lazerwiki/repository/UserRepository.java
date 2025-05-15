package us.calubrecht.lazerwiki.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import us.calubrecht.lazerwiki.model.User;

import java.util.Optional;

@Repository
public interface UserRepository  extends CrudRepository<User, Integer> {

    Optional<User> findByUserName(String userName);

    void deleteByUserName(String usrName);
}

