package team9499.commitbody.domain.block.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.aop.AopAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import team9499.commitbody.config.SecurityTestConfig;
import team9499.commitbody.domain.block.event.CancelBlockMemberEvent;
import team9499.commitbody.domain.block.event.ElsBlockMemberEvent;
import team9499.commitbody.domain.block.servcice.BlockMemberService;
import team9499.commitbody.global.aop.AspectAdvice;
import team9499.commitbody.global.aop.Pointcuts;
import team9499.commitbody.mock.MockUser;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.mockito.BDDMockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@Import({SecurityTestConfig.class, Pointcuts.class, AspectAdvice.class})
@WebMvcTest(BlockMemberController.class)
@ImportAutoConfiguration(AopAutoConfiguration.class)
class BlockMemberControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private BlockMemberService blockMemberService;
    @MockBean private ApplicationEventPublisher eventPublisher;

    private final ObjectMapper ob = new ObjectMapper();


    private final Long blockerId = 1L;
    private final Long blockedId = 2L;
    private final String success = "차단 성공";
    private final String fail = "차단 실패";

    @DisplayName("사용자 차단 API - 차단 성공시")
    @MockUser
    @Test
    void blockMemberAPI() throws Exception {

        Map<String,String> request = Map.of("blockedId","2");

        given(blockMemberService.blockMember(eq(blockerId),eq(blockedId))).willReturn(success);

        eventPublisher.publishEvent(new ElsBlockMemberEvent(blockerId,blockedId,success));
        eventPublisher.publishEvent(new CancelBlockMemberEvent(blockerId, blockedId, success));

        mockMvc.perform(post("/api/v1/block/member")
                        .with(csrf())
                        .content(ob.writeValueAsString(request).getBytes(StandardCharsets.UTF_8))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(success));

        verify(eventPublisher).publishEvent(any(ElsBlockMemberEvent.class));
        verify(eventPublisher).publishEvent(any(CancelBlockMemberEvent.class));
    }

    @DisplayName("사용자 차단 API - 차단 해제시")
    @MockUser
    @Test
    void unblockMemberAPI() throws Exception {

        Map<String,String> request = Map.of("blockedId","2");

        given(blockMemberService.blockMember(eq(blockerId),eq(blockedId))).willReturn(fail);

        eventPublisher.publishEvent(new ElsBlockMemberEvent(blockerId,blockedId,fail));
        eventPublisher.publishEvent(new CancelBlockMemberEvent(blockerId, blockedId, fail));

        mockMvc.perform(post("/api/v1/block/member")
                        .with(csrf())
                        .content(ob.writeValueAsString(request).getBytes(StandardCharsets.UTF_8))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(fail));

        verify(eventPublisher).publishEvent(any(ElsBlockMemberEvent.class));
        verify(eventPublisher).publishEvent(any(CancelBlockMemberEvent.class));
    }

}