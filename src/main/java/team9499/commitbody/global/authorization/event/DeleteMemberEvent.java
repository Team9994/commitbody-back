package team9499.commitbody.global.authorization.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DeleteMemberEvent {

    private Long memberId;

    private String status;

    private LocalDateTime date;
}
