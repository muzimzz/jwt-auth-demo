package kr.co.ureca.authdemo.repository;

import kr.co.ureca.authdemo.entity.User;
import kr.co.ureca.authdemo.oauth.SocialType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findBySocialTypeAndSocialTypeId(SocialType socialType, String socialTypeId);
}
