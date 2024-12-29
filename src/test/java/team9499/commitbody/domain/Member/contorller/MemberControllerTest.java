package team9499.commitbody.domain.Member.contorller;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartFile;
import team9499.commitbody.config.SecurityTestConfig;
import team9499.commitbody.domain.Member.domain.AccountStatus;
import team9499.commitbody.domain.Member.domain.Gender;
import team9499.commitbody.domain.Member.dto.MemberDto;
import team9499.commitbody.domain.Member.dto.request.ProfileUpdateRequest;
import team9499.commitbody.domain.Member.dto.response.MemberInfoResponse;
import team9499.commitbody.domain.Member.dto.response.MemberMyPageResponse;
import team9499.commitbody.domain.Member.service.MemberDocService;
import team9499.commitbody.domain.Member.service.MemberService;
import team9499.commitbody.domain.follow.domain.FollowType;
import team9499.commitbody.global.Exception.ExceptionStatus;
import team9499.commitbody.global.Exception.ExceptionType;
import team9499.commitbody.global.Exception.InvalidUsageException;
import team9499.commitbody.global.aop.AspectAdvice;
import team9499.commitbody.global.aop.Pointcuts;
import team9499.commitbody.mock.MockUser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.mockito.BDDMockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Slf4j
@ExtendWith(MockitoExtension.class)
@Import({SecurityTestConfig.class, Pointcuts.class, AspectAdvice.class})
@WebMvcTest(MemberController.class)
@ImportAutoConfiguration(AopAutoConfiguration.class)
class MemberControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private MemberService memberService;
    @MockBean
    private MemberDocService memberDocService;

    private final ObjectMapper ob = new ObjectMapper();

    @DisplayName("사용자 검색")
    @MockUser
    @Test
    void searchMemberByNickname() throws Exception {
        List<MemberDto> memberDtoList = new ArrayList<>();
        memberDtoList.add(MemberDto.createNickname(1L, "사용자1", "TEST.PNG"));
        memberDtoList.add(MemberDto.createNickname(2L, "사용자2", "TEST.PNG"));

        MemberInfoResponse memberInfoResponse = new MemberInfoResponse(2L, memberDtoList);
        given(memberDocService.findMemberForNickname(any(), eq("사용"), anyInt(), anyInt())).willReturn(memberInfoResponse);

        mockMvc.perform(get("/api/v1/search/member")
                        .param("nickname", "사용"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalCount").value(2))
                .andExpect(jsonPath("$.data.members.size()").value(2))
                .andExpect(jsonPath("$.data.members.[0].nickname").value("사용자1"));
    }

    @DisplayName("마이페이지 조회")
    @MockUser
    @Test
    void viewMyPage() throws Exception {
        MemberDto memberDto = MemberDto.builder().nickname("테스트닉네임").build();
        MemberMyPageResponse memberMyPageResponse = new MemberMyPageResponse(memberDto,"myPage",1,0, FollowType.FOLLOW, false, AccountStatus.PUBLIC);
        given(memberService.getMyPage(anyLong(), anyString())).willReturn(memberMyPageResponse);

        mockMvc.perform(get("/api/v1/my-page/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(memberMyPageResponse));
    }

    @DisplayName("상대방 차단시 예외발생")
    @MockUser
    @Test
    void blockException() throws Exception {
        given(memberService.getMyPage(anyLong(), anyString())).willThrow(new InvalidUsageException(ExceptionStatus.BAD_REQUEST, ExceptionType.BLOCK));

        mockMvc.perform(get("/api/v1/my-page/1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("사용자를 차단한 상태입니다."));
    }

    @DisplayName("프로필 수정")
    @MockUser
    @Test
    void updateProfile() throws Exception {
        ProfileUpdateRequest request = new ProfileUpdateRequest("변경 닉네임", Gender.MALE, null, 178.1f, 76f, 1f, 11f, true);

        MockMultipartFile updateRequest = new MockMultipartFile("profileUpdateRequest", null, "application/json", ob.writeValueAsString(request).getBytes());

        doNothing().when(memberService).updateProfile(anyLong(), anyString(), any(Gender.class), any(), anyFloat(), anyFloat(), anyFloat(), anyFloat(), anyBoolean(), isNull());

        mockMvc.perform(multipart("/api/v1/profile")
                        .file(updateRequest)
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .accept(MediaType.APPLICATION_JSON)
                        .with(csrf())
                        .with(request1 -> {
                            request1.setMethod("PUT");
                            return request1;
                        }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("업데이트 성공"));

        verify(memberService,times(1)).updateProfile(anyLong(),anyString(),any(),any(),anyFloat(),anyFloat(),anyFloat(),anyFloat(),anyBoolean(),isNull());
    }

    @DisplayName("프로필 수정 - 파일 용량이 클때")
    @MockUser
    @Test
    void maxFileSizeException() throws Exception {
        ProfileUpdateRequest request = new ProfileUpdateRequest("변경 닉네임", Gender.MALE, null, 178.1f, 76f, 1f, 11f, true);

        MockMultipartFile updateRequest = new MockMultipartFile("profileUpdateRequest", null, "application/json", ob.writeValueAsString(request).getBytes());
        int fiveMB = 6 * 1024 * 1024;
        byte[] largeFile = new byte[fiveMB];
        Arrays.fill(largeFile, (byte) 'A');
        MockMultipartFile file = new MockMultipartFile("file", "bigSize.PNG", "image/png", largeFile);

        doThrow(new MaxUploadSizeExceededException(5)).when(memberService).updateProfile(anyLong(), anyString(), any(Gender.class), any(), anyFloat(), anyFloat(), anyFloat(), anyFloat(), anyBoolean(), any(MultipartFile.class));

        mockMvc.perform(multipart("/api/v1/profile")
                        .file(updateRequest)
                        .file(file)
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .accept(MediaType.APPLICATION_JSON)
                        .with(csrf())
                        .with(request1 -> {
                            request1.setMethod("PUT");
                            return request1;
                        }))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("저장 가능한 용량을 초과 했습니다."));
    }
    
    @DisplayName("알림 수신 여부 조회")
    @MockUser
    @Test
    void getNotificationStatus() throws Exception{
        given(memberService.getNotification(anyLong())).willReturn(true);

        mockMvc.perform(get("/api/v1/notification/settings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("알림 수신 유뮤"))
                .andExpect(jsonPath("$.data").value(true));
    }
    
    @DisplayName("알림 수신 여부 설정")
    @MockUser
    @Test
    void updateNotificationStatus() throws Exception{
        given(memberService.updateNotification(anyLong())).willReturn("알림 수신");

        mockMvc.perform(post("/api/v1/notification/settings")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("알림 수신"));
    }
    

}