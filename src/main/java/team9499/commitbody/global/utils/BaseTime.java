package team9499.commitbody.global.utils;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public class BaseTime {

    // 저장될 때 시간이 자동 저장
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    // 업데이트 될때 시간이 자동으로 변경
    @LastModifiedDate
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
