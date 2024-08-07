package team9499.commitbody.domain.exercise.service.Impl;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team9499.commitbody.domain.exercise.domain.CustomExercise;
import team9499.commitbody.domain.exercise.domain.ExerciseDoc;
import team9499.commitbody.domain.exercise.repository.CustomExerciseRepository;
import team9499.commitbody.domain.exercise.repository.ExerciseElsRepository;
import team9499.commitbody.domain.exercise.service.ElasticExerciseService;


@Service
@Transactional
@RequiredArgsConstructor
public class ElasticExerciseServiceImpl implements ElasticExerciseService {

    private final CustomExerciseRepository customExerciseRepository;
    private final ExerciseElsRepository exerciseElsRepository;

    @Value("${cloud.aws.cdn.url}")
    private String cdnUrl;

    /**
     * 커스텀 운동 저장
     */
    @Override
    public void saveExercise(Long customExerciseId) {
        CustomExercise customExercise = customExerciseRepository.findById(customExerciseId).orElse(null);
        ExerciseDoc exerciseDoc = new ExerciseDoc().customExercise(customExercise,getCustomGifUrl(customExercise));
        exerciseElsRepository.save(exerciseDoc);
    }

    private String getCustomGifUrl(CustomExercise customExercise) {
        return customExercise.getCustomGifUrl() ==null ? "등록된 이미지 파일이 없습니다." : cdnUrl+customExercise.getCustomGifUrl();
    }
}
