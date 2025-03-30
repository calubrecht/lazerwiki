package us.calubrecht.lazerwiki.repository;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import us.calubrecht.lazerwiki.model.VerificationToken;

@Repository
public interface VerificationTokenRepository  extends CrudRepository<VerificationToken, Integer> {

    VerificationToken findByUserAndTokenAndPurpose(String userName, String token, VerificationToken.Purpose purpose);

    @Modifying
    @Query(value="delete from verificationToken  where expiry < CURRENT_TIMESTAMP", nativeQuery=true)
    void deleteExpired();
}
