package team9499.commitbody.global.notification.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.stereotype.Repository;
import team9499.commitbody.global.notification.dto.NotificationDto;

import java.util.List;
import java.util.stream.Collectors;

import static team9499.commitbody.global.notification.domain.QNotification.*;

@Repository
@RequiredArgsConstructor
public class CustomNotificationRepositoryImpl implements CustomNotificationRepository {

    private final JPAQueryFactory jpaQueryFactory;

    @Override
    public Slice<NotificationDto> getAllNotification(Long memberId, Long lastId, Pageable pageable) {
        BooleanBuilder lastIdBuilder = lastIdBuilder(lastId);
        List<Tuple> notificationQuery = notificationQuery(memberId, pageable, lastIdBuilder);
        List<NotificationDto> notificationDtos = getNotificationDtos(notificationQuery);
        return new SliceImpl<>(notificationDtos, pageable, isHasNext(pageable, notificationDtos));
    }

    private static BooleanBuilder lastIdBuilder(Long lastId) {
        BooleanBuilder lastIdBuilder = new BooleanBuilder();
        if (lastId != null) {
            lastIdBuilder.and(notification.id.lt(lastId));
        }
        return lastIdBuilder;
    }

    private List<Tuple> notificationQuery(Long memberId, Pageable pageable, BooleanBuilder lastIdBuilder) {
        return jpaQueryFactory.select(notification, notification.receiver, notification.sender)
                .from(notification)
                .where(lastIdBuilder, notification.receiver.id.eq(memberId))
                .limit(pageable.getPageSize() + 1)
                .orderBy(notification.createdAt.desc())
                .fetch();
    }

    private static List<NotificationDto> getNotificationDtos(List<Tuple> notificationQuery) {
        return notificationQuery.stream().map(t -> NotificationDto.of(t.get(notification))).collect(Collectors.toList());
    }

    private static boolean isHasNext(Pageable pageable, List<NotificationDto> notificationDtos) {
        boolean hasNext = false;

        if (notificationDtos.size() > pageable.getPageSize()) {
            notificationDtos.remove(pageable.getPageSize());
            hasNext = true;
        }
        return hasNext;
    }

}
