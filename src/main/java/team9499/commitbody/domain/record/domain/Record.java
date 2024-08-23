package team9499.commitbody.domain.record.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import team9499.commitbody.domain.Member.domain.Member;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Record {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "record_id")
    private Long id;

    private String recordName;          // 운동명

    private Integer recordVolume;       // 총 볼륨

    private Integer recordSets;         // 총 횟수

    private Integer recordCalorie;      // 총 칼로리

    private LocalDateTime startTime;    // 운동 시작 시간

    private LocalDateTime endTime;      // 운동 끝 시간

    private Integer duration;           // 진행시간

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @OneToMany(mappedBy = "record", cascade = CascadeType.ALL)
    private List<RecordDetails> detailsList = new ArrayList<>();

    public static Record create(String recordName, LocalDateTime startTime, LocalDateTime endTime,Integer duration, Member member){
        return Record.builder().recordName(recordName).startTime(startTime).endTime(endTime).duration(duration).member(member).build();
    }

}
