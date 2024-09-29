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

@Table(name = "record", indexes = {
        @Index(name = "idx_member_id_end_time",columnList = "member_id, end_time ASC")
})
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

    @Column(name = "record_name")
    private String recordName;          // 운동명

    @Column(name = "record_volume")
    private Integer recordVolume;       // 총 볼륨

    @Column(name = "record_sets")
    private Integer recordSets;         // 총 횟수

    @Column(name = "record_calorie")
    private Integer recordCalorie;      // 총 칼로리

    @Column(name = "start_time")
    private LocalDateTime startTime;    // 운동 시작 시간

    @Column(name = "end_time")
    private LocalDateTime endTime;      // 운동 끝 시간

    private Integer duration;           // 진행시간

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id",foreignKey =  @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private Member member;

    @OneToMany(mappedBy = "record", cascade = CascadeType.ALL)
    private List<RecordDetails> detailsList = new ArrayList<>();

    public static Record create(String recordName, LocalDateTime startTime, LocalDateTime endTime,Integer duration, Member member){
        return Record.builder().recordName(recordName).startTime(startTime).endTime(endTime).duration(duration).member(member).build();
    }

    public void updateRecord(Integer recordVolume,Integer recordCalorie,Integer recordSets){
        this.recordCalorie = recordCalorie;
        this.recordVolume = recordVolume;
        this.recordSets =recordSets;
    }

    public void updateRecordName(String recordName){
        this.recordName = recordName;
    }
}
