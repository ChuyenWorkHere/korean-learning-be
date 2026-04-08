package edu.language.kbee.repository;

import edu.language.kbee.model.UserActivity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserActivityRepository extends JpaRepository<UserActivity, String> {

    Optional<UserActivity> findByUser_UserIdAndActivityDate(String userId, LocalDate activityDate);

    List<UserActivity> findAllByUser_UserIdAndActivityDateBetweenOrderByActivityDateAsc(String userId, LocalDate startDate, LocalDate endDate);
}
