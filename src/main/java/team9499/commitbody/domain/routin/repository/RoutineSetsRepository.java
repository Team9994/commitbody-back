//package team9499.commitbody.domain.routin.repository;
//
//import org.springframework.data.jpa.repository.JpaRepository;
//import org.springframework.data.jpa.repository.Modifying;
//import org.springframework.data.jpa.repository.Query;
//import org.springframework.data.repository.query.Param;
//import org.springframework.stereotype.Repository;
//import org.springframework.transaction.annotation.Transactional;
//import team9499.commitbody.domain.routin.domain.RoutineSets;
//
//import java.util.List;
//
//@Repository
//public interface RoutineSetsRepository extends JpaRepository<RoutineSets, Long> {
//
//    List<RoutineSets> findAllByRoutineDetailsId(Long routineDetailsId);
//    RoutineSets findByIdAndRoutineDetailsId(Long setsId, Long routineDetailsId);
//    void deleteByIdAndRoutineDetailsId(Long setsId, Long routineDetailsId);
//
//    @Transactional
//    @Modifying
//    @Query("delete from RoutineSets r where r.id in :ids")
//    void deleteAllByInInQuery(@Param("ids") List<Long> ids);
//}
