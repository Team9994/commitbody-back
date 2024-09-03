package team9499.commitbody.domain.routin.repository.impl;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import team9499.commitbody.domain.routin.repository.CustomRoutineRepository;

import static team9499.commitbody.domain.routin.domain.QRoutine.*;
import static team9499.commitbody.domain.routin.domain.QRoutineDetails.*;

@Repository
@RequiredArgsConstructor
public class CustomRoutineRepositoryImpl implements CustomRoutineRepository {

    private final JPAQueryFactory jpaQueryFactory;

    @Override
    public void deleteRoutine(Long routineId) {

        // 루틴 상세 설명 삭제
        jpaQueryFactory.delete(routineDetails)
                .where(routineDetails.routine.id.eq(routineId))
                .execute();

        // 루틴 삭제
        jpaQueryFactory.delete(routine)
                .where(routine.id.eq(routineId))
                .execute();
    }
}
