package edu.language.kbee.repository;

import edu.language.kbee.model.Unit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UnitRepository extends JpaRepository<Unit, String> {

    List<Unit> findByCourse_CourseIdOrderByOrderIndexAsc(String courseId);


    int countByCourse_CourseId(String courseId);

}
