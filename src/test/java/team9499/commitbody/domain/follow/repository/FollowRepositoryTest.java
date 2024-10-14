package team9499.commitbody.domain.follow.repository;

import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import team9499.commitbody.annotations.SkipInit;
import team9499.commitbody.domain.Member.domain.Member;
import team9499.commitbody.domain.Member.repository.MemberRepository;
import team9499.commitbody.domain.follow.domain.Follow;
import team9499.commitbody.domain.follow.domain.FollowStatus;
import team9499.commitbody.domain.follow.domain.FollowType;
import team9499.commitbody.global.config.QueryDslConfig;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static team9499.commitbody.domain.Member.domain.LoginType.*;

@Slf4j
@DataJpaTest
@Import(QueryDslConfig.class)
@ActiveProfiles("test")
class FollowRepositoryTest {

    @Autowired private MemberRepository memberRepository;
    @Autowired private FollowRepository followRepository;
    @Autowired private EntityManager em;

    private Member follower;
    private Member following;

    @BeforeEach
    void init(TestInfo testInfo){
        follower = memberRepository.save(Member.builder().nickname("사용자1").socialId("test-id").isWithdrawn(false).loginType(KAKAO).build());
        following = memberRepository.save(Member.builder().nickname("사용자2").socialId("test-id").isWithdrawn(false).loginType(KAKAO).build());

        Method method = testInfo.getTestMethod().orElse(null);
        if (method !=null && method.isAnnotationPresent(SkipInit.class)){
            return;
        }

        Member following1 = memberRepository.save(Member.builder().nickname("1").socialId("test1").loginType(KAKAO).build());
        Member following2 = memberRepository.save(Member.builder().nickname("2").socialId("test2").loginType(KAKAO).build());
        Member following3 = memberRepository.save(Member.builder().nickname("3").socialId("test3").loginType(KAKAO).build());

        followRepository.save(Follow.create(follower,following,FollowType.FOLLOWING));
        followRepository.save(Follow.create(follower,following1,FollowType.FOLLOWING));
        followRepository.save(Follow.create(follower,following2,FollowType.FOLLOWING));
        followRepository.save(Follow.create(follower,following3,FollowType.FOLLOWING));
        followRepository.save(Follow.create(following3,follower,FollowType.FOLLOWING));
    }


    @DisplayName("팔로워,팔로잉 ID로 팔로워 조회")
    @SkipInit
    @Test
    void findByFollowerAndFollowing(){
        Follow followEntity = Follow.create(follower, following, FollowType.FOLLOW);
        followRepository.save(followEntity);

        Optional<Follow> follow = followRepository.findByFollowerIdAndFollowingId(follower.getId(), following.getId());
        
        assertThat(follow).isNotEmpty();
        assertThat(follow.get().getFollower()).isEqualTo(follower);
        assertThat(follow.get().getFollowing()).isEqualTo(following);
        assertThat(follow.get().getStatus()).isEqualTo(FollowStatus.REQUEST);
    }
    
    @DisplayName("팔로잉수 조회")
    @Test
    void getCountFollowing(){
        long countFollowing = followRepository.getCountFollowing(follower.getId());
        assertThat(countFollowing).isEqualTo(4);
    }

    @DisplayName("팔로워수 조회")
    @Test
    void getCountFollowers(){
        long countFollowing = followRepository.getCountFollower(follower.getId());
        assertThat(countFollowing).isEqualTo(1);
    }


    @DisplayName("팔로우 상태 검사")
    @SkipInit
    @Test
    void followStatus(){
        // 팔로우를 하지않았을 경우
        FollowType neither = followRepository.followStatus(follower.getId(), following.getId());

        // 팔로잉 중이거나 맞팔로우 상태일때
        followRepository.save(Follow.create(follower,following,FollowType.FOLLOWING));
        FollowType follow = followRepository.followStatus(follower.getId(), following.getId());
        followRepository.deleteAll();

        // 상대방이 팔로우 요청을 보낸 상태일때 상대방은 팔워 요청 상태
        followRepository.save(Follow.builder().follower(follower).following(following).status(FollowStatus.REQUEST).build());
        FollowType request = followRepository.followStatus(follower.getId(), following.getId());
        followRepository.deleteAll();
        
        assertThat(neither).isEqualTo(FollowType.NEITHER);
        assertThat(follow).isEqualTo(FollowType.FOLLOW);
        assertThat(request).isEqualTo(FollowType.FOLLOW_ONLY);
    }
    
    @DisplayName("팔로우 취소시 FollowStatus 상태 업데이트")
    @SkipInit
    @Test
    void cancelFollow(){
        followRepository.save(Follow.builder().follower(follower).following(following).status(FollowStatus.MUTUAL_FOLLOW).build());
        followRepository.save(Follow.builder().follower(following).following(follower).status(FollowStatus.MUTUAL_FOLLOW).build());

        followRepository.cancelFollow(follower.getId(),following.getId());

        em.flush();
        em.clear();

        List<Follow> all = followRepository.findAll();

        assertThat(all.get(0).getStatus()).isEqualTo(FollowStatus.CANCEL);
        assertThat(all.get(1).getStatus()).isEqualTo(FollowStatus.CANCEL);
    }
    
    @DisplayName("팔로잉 ID 리스트 조회")
    @Test
    void getFollowingIds(){
        List<Long> followings = followRepository.followings(follower.getId());

        assertThat(followings.size()).isEqualTo(4);
    }
}