package team9499.commitbody.domain.follow.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.BDDMockito;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.aop.AopAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import team9499.commitbody.config.SecurityTestConfig;
import team9499.commitbody.domain.follow.domain.FollowType;
import team9499.commitbody.domain.follow.dto.FollowDto;
import team9499.commitbody.domain.follow.dto.request.FollowRequest;
import team9499.commitbody.domain.follow.dto.response.FollowResponse;
import team9499.commitbody.domain.follow.service.FollowService;
import team9499.commitbody.global.Exception.ExceptionStatus;
import team9499.commitbody.global.Exception.ExceptionType;
import team9499.commitbody.global.Exception.InvalidUsageException;
import team9499.commitbody.global.aop.AspectAdvice;
import team9499.commitbody.global.aop.Pointcuts;
import team9499.commitbody.global.notification.event.FollowingEvent;
import team9499.commitbody.mock.MockUser;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.BDDMockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@Slf4j
@ExtendWith(MockitoExtension.class)
@Import({SecurityTestConfig.class, Pointcuts.class, AspectAdvice.class})
@WebMvcTest(FollowController.class)
@ImportAutoConfiguration(AopAutoConfiguration.class)
class FollowControllerTest {

    
    @Autowired private MockMvc mockMvc;
    @MockBean private FollowService followService;
    @MockBean private ApplicationEventPublisher eventPublisher;

    private final ObjectMapper ob = new ObjectMapper();
    private final String REQUEST_FOLLOW = "팔로우 요청";


    @DisplayName("팔로워 API 테스트")
    @MockUser
    @Test
    void followRequest() throws Exception{
        FollowRequest request = new FollowRequest();
        request.setFollowId(2L);
        request.setType(FollowType.FOLLOW);

        given(followService.follow(1L,request.getFollowId(),request.getType())).willReturn(REQUEST_FOLLOW);
        eventPublisher.publishEvent(new FollowingEvent(1L,2L));

        mockMvc.perform(post("/api/v1/follow")
                .with(csrf())
                .content(ob.writeValueAsString(request).getBytes(StandardCharsets.UTF_8))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(REQUEST_FOLLOW));

        verify(eventPublisher,times(1)).publishEvent(any(FollowingEvent.class));
    }

    @DisplayName("팔로워 - 반복 요청시 예외")
    @MockUser
    @Test
    void followMutualFollow() throws Exception{
        FollowRequest request = new FollowRequest();
        request.setFollowId(2L);
        request.setType(FollowType.FOLLOW);

        given(followService.follow(1L,request.getFollowId(),request.getType())).willThrow(new InvalidUsageException(ExceptionStatus.BAD_REQUEST, ExceptionType.ALREADY_REQUESTED));

        mockMvc.perform(post("/api/v1/follow")
                        .with(csrf())
                        .content(ob.writeValueAsString(request).getBytes(StandardCharsets.UTF_8))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("이미 처리된 요청입니다."));

        verify(eventPublisher,times(0)).publishEvent(any(FollowingEvent.class));
    }

    @DisplayName("무한 스크롤 조회")
    @MockUser
    @Test
    void getSliceFollows() throws Exception{
        List<FollowDto> followDtoList= new ArrayList<>();
        for (int i =0; i<3;i++){
            followDtoList.add(new FollowDto());        }

        FollowResponse followResponse = new FollowResponse();
        followResponse.setHasNext(false);
        followResponse.setFollows(followDtoList);

        given(followService.getFollowers(anyLong(),anyLong(),isNull(),isNull(),any(Pageable.class))).willReturn(followResponse);

        mockMvc.perform(get("/api/v1/followers/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.follows").isArray())
                .andExpect(jsonPath("$.data.follows.size()").value(3));
    }

}