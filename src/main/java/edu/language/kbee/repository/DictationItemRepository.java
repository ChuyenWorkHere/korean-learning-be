package edu.language.kbee.repository;

import edu.language.kbee.model.DictationItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DictationItemRepository extends JpaRepository<DictationItem, String> {
    List<DictationItem> findByBlock_BlockIdOrderByOrderIndexAsc(String blockId);
}
