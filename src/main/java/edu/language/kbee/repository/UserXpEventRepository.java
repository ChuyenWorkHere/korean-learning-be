package edu.language.kbee.repository;

import edu.language.kbee.enums.XpSourceType;
import edu.language.kbee.model.UserXpEvent;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserXpEventRepository extends JpaRepository<UserXpEvent, String> {

    boolean existsByUser_UserIdAndSourceTypeAndSourceId(String userId, XpSourceType sourceType, String sourceId);

    List<UserXpEvent> findByUser_UserIdOrderByCreatedDateDesc(String userId, Pageable pageable);
}
