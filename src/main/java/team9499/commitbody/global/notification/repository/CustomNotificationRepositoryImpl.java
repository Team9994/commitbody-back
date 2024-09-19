package team9499.commitbody.global.notification.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.stereotype.Repository;
import team9499.commitbody.global.notification.dto.NotificationDto;
import team9499.commitbody.global.notification.domain.Notification;

import java.util.List;
import java.util.stream.Collectors;

import static team9499.commitbody.global.notification.domain.QNotification.*;

@Slf4j
@Repository
@RequiredArgsConstructor
public class CustomNotificationRepositoryImpl implements CustomNotificationRepository{

    private final JPAQueryFactory jpaQueryFactory;


    @Override
    public Slice<NotificationDto> getAllNotification(Long memberId, Long lastId, Pageable pageable) {
        BooleanBuilder lastIdBuilder = new BooleanBuilder();
        if (lastId != null) {
            lastIdBuilder.and(notification.id.lt(lastId));
        }
        List<Tuple> tupleList = jpaQueryFactory.select(notification, notification.receiver, notification.sender)
                .from(notification)
                .where(lastIdBuilder, notification.receiver.id.eq(memberId))
                .limit(pageable.getPageSize() + 1)
                .orderBy(notification.createdAt.desc())
                .fetch();

        List<NotificationDto> notificationDtoList = tupleList.stream().map(t -> NotificationDto.of(t.get(notification))).collect(Collectors.toList());

        boolean hasNext = false;

        if (notificationDtoList.size() > pageable.getPageSize()){
            notificationDtoList.remove(pageable.getPageSize());
            hasNext = true;
        }

        return new SliceImpl<>(notificationDtoList,pageable,hasNext);
    }
}
