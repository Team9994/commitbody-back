package team9499.commitbody.domain.record.controller;

import static org.mockito.BDDMockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.aop.AopAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import team9499.commitbody.config.SecurityTestConfig;
import team9499.commitbody.domain.record.dto.RecordDto;
import team9499.commitbody.domain.record.dto.RecordSetsDto;
import team9499.commitbody.domain.record.dto.request.RecordRequest;
import team9499.commitbody.domain.record.dto.request.UpdateRecordRequest;
import team9499.commitbody.domain.record.dto.response.RecordMonthResponse;
import team9499.commitbody.domain.record.dto.response.RecordMonthResponse.RecordData;
import team9499.commitbody.domain.record.dto.response.RecordMonthResponse.RecordDay;
import team9499.commitbody.domain.record.dto.response.RecordResponse;
import team9499.commitbody.domain.record.service.RecordService;
import team9499.commitbody.global.Exception.ExceptionStatus;
import team9499.commitbody.global.Exception.ExceptionType;
import team9499.commitbody.global.Exception.InvalidUsageException;
import team9499.commitbody.global.aop.AspectAdvice;
import team9499.commitbody.global.aop.Pointcuts;
import team9499.commitbody.mock.MockUser;

@ExtendWith(MockitoExtension.class)
@Import({SecurityTestConfig.class, Pointcuts.class, AspectAdvice.class})
@WebMvcTest(RecordController.class)
@ImportAutoConfiguration(AopAutoConfiguration.class)
class RecordControllerTest {


    @Autowired private MockMvc mockMvc;
    @MockBean private RecordService recordService;

    private final ObjectMapper ob = new ObjectMapper();


    @DisplayName("기록 저장")
    @MockUser
    @Test
    void saveRecord() throws Exception{
        List<RecordDto> recordDtos = new ArrayList<>();
        recordDtos.add(RecordDto.builder().exerciseId(1L).source("default").sets(List.of(RecordSetsDto.builder().weight(10).reps(10).build())).build());
        recordDtos.add(RecordDto.builder().exerciseId(2L).source("default").sets(List.of(RecordSetsDto.builder().times(10).build())).build());
        recordDtos.add(RecordDto.builder().exerciseId(3L).source("default").sets(List.of(RecordSetsDto.builder().reps(10).build())).build());

        RecordRequest request = new RecordRequest();
        request.setRecordName("기록1");

        request.setExercises(recordDtos);

        given(recordService.saveRecord(anyLong(),anyString(),any(),any(),anyList())).willReturn(1L);

        mockMvc.perform(post("/api/v1/record")
                .with(csrf())
                .content(ob.writeValueAsString(request).getBytes(StandardCharsets.UTF_8))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(1));
    }


    @DisplayName("기록 상세 조회")
    @MockUser
    @Test
    void detailRecord() throws Exception{
        RecordResponse recordResponse = new RecordResponse(1L,"기록1","2024:09:1","1시간",14,200,14,2004,new ArrayList<>());

        given(recordService.getRecord(anyLong(),anyLong())).willReturn(recordResponse);

        mockMvc.perform(get("/api/v1/record/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.recordName").value("기록1"))
                .andExpect(jsonPath("$.data.details").isArray());
    }
    
    @DisplayName("기록 수정")
    @MockUser
    @Test
    void updateRecord() throws Exception{
        List<RecordDto> recordDtos = new ArrayList<>();
        recordDtos.add(RecordDto.builder().exerciseId(1L).source("default").sets(List.of(RecordSetsDto.builder().weight(10).reps(10).build())).build());
        recordDtos.add(RecordDto.builder().exerciseId(2L).source("default").sets(List.of(RecordSetsDto.builder().times(10).build())).build());
        recordDtos.add(RecordDto.builder().exerciseId(3L).source("default").sets(List.of(RecordSetsDto.builder().reps(10).build())).build());

        UpdateRecordRequest request = new UpdateRecordRequest();
        request.setRecordName("변경된 기록");
        request.setRecordDtoList(recordDtos);
        

        doNothing().when(recordService).updateRecord(anyLong(),anyLong(),anyString(),anyList());

        mockMvc.perform(put("/api/v1/record/1")
                        .with(csrf())
                        .content(ob.writeValueAsString(request).getBytes(StandardCharsets.UTF_8))
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("기록 수정 완료"));
    }
    
    @DisplayName("기록 삭제")
    @MockUser
    @Test
    void deleteRecord() throws Exception{

        doNothing().when(recordService).deleteRecord(anyLong(),anyLong());

        mockMvc.perform(delete("/api/v1/record/1")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("삭제 성공"));
    }
    
    @DisplayName("작성저가 아닐 사용자 이용시 예외 테스트")
    @MockUser
    @Test
    void onlyWriterUseException() throws Exception{

        doThrow(new InvalidUsageException(ExceptionStatus.FORBIDDEN, ExceptionType.AUTHOR_ONLY)).when(recordService).deleteRecord(anyLong(),anyLong());
        mockMvc.perform(delete("/api/v1/record/1")
                .with(csrf()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("작성자만 이용할 수 있습니다."));

    }

    @DisplayName("기록 조회")
    @MockUser
    @Test
    void getRecord() throws Exception{

        List<RecordDay> oneDays = new ArrayList<>();
        List<RecordDay> twoDays = new ArrayList<>();
        oneDays.add(new RecordDay(1L,"루틴1","2024.09.02.(월) · 4:34~6:34"));
        twoDays.add(new RecordDay(2L,"루틴2","2024.09.03.(화) · 4:34~6:34"));
        RecordData onerecordData = new RecordData( "2024.09.02.(월)",oneDays);
        RecordData tworecordData = new RecordData( "2024.09.03.(화)",twoDays);
        Map<String,RecordData>  dataMap = Map.of("1",onerecordData,"2",tworecordData);
        RecordMonthResponse recordMonthResponse = new RecordMonthResponse(dataMap, oneDays);

        given(recordService.getRecordForMember(anyLong(),anyInt(),anyInt())).willReturn(recordMonthResponse);

        mockMvc.perform(get("/api/v1/record")
                        .param("year","2024")
                        .param("month","9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.dayRecordCount.size()").value(2))
                .andExpect(jsonPath("$.data.records.size()").value(1));
    }
}