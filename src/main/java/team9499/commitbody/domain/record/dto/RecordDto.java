package team9499.commitbody.domain.record.dto;

import lombok.Data;

import java.util.List;

@Data
public class RecordDto {

    private Long exerciseId;

    private String source;

    private List<RecordSetsDto> sets;
}
