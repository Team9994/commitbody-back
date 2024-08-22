package team9499.commitbody.domain.exercise.service;

import org.springframework.web.multipart.MultipartFile;
import team9499.commitbody.domain.exercise.domain.enums.ExerciseEquipment;
import team9499.commitbody.domain.exercise.domain.enums.ExerciseTarget;
import team9499.commitbody.domain.exercise.dto.SearchExerciseResponse;
import team9499.commitbody.domain.exercise.dto.response.ExerciseResponse;

public interface ExerciseService {

    SearchExerciseResponse searchExercise(String name, String target,String equipment, Integer from, Integer size, Boolean like, String memberId,String exerciseType);

    Long saveCustomExercise(String exerciseName, ExerciseTarget exerciseTarget, ExerciseEquipment exerciseEquipment, Long memberId, MultipartFile file);

    Long updateCustomExercise(String exerciseName, ExerciseTarget exerciseTarget, ExerciseEquipment exerciseEquipment, Long memberId, Long customExerciseId,MultipartFile file);

    void deleteCustomExercise(Long customExerciseId,Long memberId);

    ExerciseResponse detailsExercise(Long exerciseId, Long memberId, String source);
}
