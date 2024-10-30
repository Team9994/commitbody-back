package team9499.commitbody.domain.exercise.service.Impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import team9499.commitbody.domain.Member.domain.Member;
import team9499.commitbody.domain.Member.repository.MemberRepository;
import team9499.commitbody.domain.exercise.domain.CustomExercise;
import team9499.commitbody.domain.exercise.domain.enums.ExerciseEquipment;
import team9499.commitbody.domain.exercise.domain.enums.ExerciseTarget;
import team9499.commitbody.domain.exercise.dto.CustomExerciseDto;
import team9499.commitbody.domain.exercise.dto.response.ExerciseResponse;
import team9499.commitbody.domain.exercise.repository.CustomExerciseRepository;
import team9499.commitbody.domain.exercise.repository.ExerciseInterestRepository;
import team9499.commitbody.domain.exercise.repository.ExerciseRepository;
import team9499.commitbody.domain.exercise.service.ExerciseInterestService;
import team9499.commitbody.domain.exercise.service.ExerciseService;
import team9499.commitbody.domain.like.repository.LikeRepository;
import team9499.commitbody.domain.record.repository.RecordRepository;
import team9499.commitbody.global.Exception.ExceptionStatus;
import team9499.commitbody.global.Exception.ExceptionType;
import team9499.commitbody.global.Exception.NoSuchException;
import team9499.commitbody.global.aws.s3.S3Service;
import team9499.commitbody.global.redis.RedisService;

import java.util.*;

@Slf4j
@Service
@Transactional(transactionManager = "dataTransactionManager")
@RequiredArgsConstructor
public class ExerciseServiceImpl implements ExerciseService {

    private final CustomExerciseRepository customExerciseRepository;
    private final LikeRepository commentLikeRepository;
    private final RecordRepository recordRepository;
    private final ExerciseInterestRepository exerciseInterestRepository;
    private final ExerciseInterestService exerciseInterestService;
    private final ExerciseRepository exerciseRepository;
    private final RedisService redisService;
    private final MemberRepository memberRepository;
    private final S3Service s3Service;

    @Value("${cloud.aws.cdn.url}")
    private String CDN_RUL;

    /**
     * 커스텀 운동등록 메서드
     * 이미지는 s3에 업로드
     */
    @Override
    public CustomExerciseDto saveCustomExercise(String exerciseName, ExerciseTarget exerciseTarget, ExerciseEquipment exerciseEquipment,Long memberId, MultipartFile file) {
        String storedFileName = s3Service.uploadFile(file);
        Optional<Member> redisMember = getRedisMember(memberId);

        CustomExercise customExercise = new CustomExercise().save(exerciseName, storedFileName, exerciseTarget, exerciseEquipment,redisMember.get());
        CustomExercise exercise = customExerciseRepository.save(customExercise);
        return CustomExerciseDto.fromDto(exercise, getImgUrl(storedFileName));
    }

    /**
     * 커스텀 운동 업데이트 메서드
     */
    @Override
    public CustomExerciseDto updateCustomExercise(String exerciseName, ExerciseTarget exerciseTarget, ExerciseEquipment exerciseEquipment, Long memberId, Long customExerciseId, MultipartFile file) {
        CustomExercise customExercise = getCustomExercise(customExerciseId,memberId);
        String updateImage = s3Service.updateFile(file, customExercise.getCustomGifUrl());
        customExercise.update(exerciseName,exerciseTarget,exerciseEquipment,updateImage);
        return CustomExerciseDto.fromDto(customExercise,getImgUrl(updateImage));
    }

    /**
     * DB 커스텀 운동 삭제 메서드
     */
    @Override
    public void deleteCustomExercise(Long customExerciseId, Long memberId) {
        CustomExercise customExercise = getCustomExercise(customExerciseId,memberId);
        recordRepository.deleteCustomExercise(customExercise.getId());                      // 운동 기록 삭제
        exerciseInterestRepository.deleteAllByCustomExerciseId(customExercise.getId());     // 관심 운동 삭제
        commentLikeRepository.deleteByCustomExerciseId(customExercise.getId());     // 댓글 좋아요 삭제
        customExerciseRepository.delete(customExercise);                    // 커스텀 운동 삭제
    }

    /**
     * 상세 운동 조회하는 메서드
     * @param exerciseId 운동 ID
     * @param memberId 로그인한 사용자 ID
     * @param source 운동 정보 타입 [custom,default]
     */
    @Transactional(readOnly = true)
    @Override
    public ExerciseResponse detailsExercise(Long exerciseId, Long memberId, String source) {
        ExerciseResponse exerciseDetailReport = exerciseRepository.getExerciseDetailReport(memberId, exerciseId, source);
        boolean interestStatus = exerciseInterestService.checkInterestStatus(exerciseId, memberId);
        exerciseDetailReport.setInterestStatus(interestStatus);

        return exerciseDetailReport;
    }


    private Optional<Member> getRedisMember(Long memberId) {
        Optional<Member> optionalMember = redisService.getMemberDto(String.valueOf(memberId));

        if (optionalMember.isEmpty()){
            Member member = memberRepository.findById(memberId).orElseThrow(() -> new NoSuchException(ExceptionStatus.BAD_REQUEST, ExceptionType.No_SUCH_MEMBER));
            optionalMember = Optional.of(member);
        }
        return optionalMember;
    }

    private CustomExercise getCustomExercise(Long customExerciseId, Long memberId) {
        return customExerciseRepository.findByIdAndAndMemberId(customExerciseId,memberId).orElseThrow(() -> new NoSuchException(ExceptionStatus.BAD_REQUEST, ExceptionType.NO_SUCH_DATA));
    }


    private String getImgUrl(String storedFileName) {
        return storedFileName != null ? CDN_RUL + storedFileName : null;
    }
}
