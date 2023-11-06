package us.calubrecht.lazerwiki.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import us.calubrecht.lazerwiki.model.User;

@Repository
public interface UserRepository  extends CrudRepository<User, String> {


}
