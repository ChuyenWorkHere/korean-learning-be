package edu.language.kbee.repository;

import edu.language.kbee.model.DictationItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DictationItemRepository extends JpaRepository<DictationItem, String> {
    List<DictationItem> findByBlock_BlockIdOrderByOrderIndexAsc(String blockId);

    @Query("SELECT d FROM DictationItem d JOIN FETCH d.block b WHERE b.blockId IN :blockIds ORDER BY d.orderIndex ASC")
    List<DictationItem> findByBlock_BlockIdInOrderByOrderIndexAsc(@Param("blockIds") List<String> blockIds);
}
