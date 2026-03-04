package edu.language.kbee.repository;

import edu.language.kbee.enums.CourseStatus;
import edu.language.kbee.model.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CourseRepository extends JpaRepository<Course, String> {

    List<Course> findAllByStatus(CourseStatus status);

}
