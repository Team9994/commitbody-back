package team9499.commitbody.domain.routin.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import team9499.commitbody.domain.routin.domain.RoutineDetails;

import java.util.List;

@Repository
public interface RoutineDetailsRepository extends JpaRepository<RoutineDetails, Long> {

    @Transactional
    @Modifying
    @Query("DELETE FROM RoutineDetails rd where rd.id in :ids")
    void deleteAllByInQuery(@Param("ids") List<Long> ids);

}
