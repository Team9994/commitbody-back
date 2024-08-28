//package team9499.commitbody.testel;
//
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Service;
//import team9499.commitbody.domain.exercise.domain.Exercise;
//import team9499.commitbody.domain.exercise.domain.enums.ExerciseEquipment;
//import team9499.commitbody.domain.exercise.domain.ExerciseMethod;
//import team9499.commitbody.domain.exercise.domain.enums.ExerciseTarget;
//import team9499.commitbody.domain.exercise.domain.enums.ExerciseType;
//import team9499.commitbody.domain.exercise.repository.ExerciseCommentRepository;
//import team9499.commitbody.domain.exercise.repository.ExerciseMethodRepository;
//
//import java.io.*;
//import java.util.ArrayList;
//import java.util.List;
//
//import static team9499.commitbody.domain.exercise.domain.enums.ExerciseTarget.*;
//
//@Slf4j
//@Service
//@RequiredArgsConstructor
//public class CSV {
//
//    private final ExerciseCommentRepository exerciseRepository;
//    private final ExerciseMethodRepository exerciseMethodRepository;
//
//    public void readCsvFile() {
//        String csvFile ="C:/data/exercise-utf8.csv";
//        String line;
//        String csvSplitBy = ",";
//        List<Exercise> exerciseList = new ArrayList<>();
//        List<ExerciseMethod> exerciseMethods = new ArrayList<>();
//        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(csvFile)));) {
//
//            int count =0;
//
//            while ((line = br.readLine()) != null) {
//                if (count ==0){
//                    count++;
//                    continue;
//                }
//
//                String[] split = line.split(csvSplitBy);
//
//                Long id = Long.parseLong(split[3]);      //id
//                String ex_name = split[4];
//                ExerciseTarget exerciseTarget = switchs(split[6]);
//                ExerciseEquipment exerciseEquipment = switchsEq(split[1]);
//                ExerciseType exerciseType = switchsEqty(split[split.length-1]);
//                Exercise exercise = new Exercise(id, ex_name, null, exerciseTarget,exerciseType,exerciseEquipment);
//                exerciseList.add(exercise);
//                for(int i = 7; i < split.length - 2; i++){
//                    log.info("대체 넌 뭐냐? ={}", split[i]);
//                    if (split[i] != null && !split[i].isEmpty()){
//                        exerciseMethods.add(ExerciseMethod.builder().exercise_content(split[i]).exercise(exercise).build());
//                    }
//                }
//
//            }
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }finally {
//            exerciseRepository.saveAll(exerciseList);
//            exerciseMethodRepository.saveAll(exerciseMethods);
//        }
//    }
//
//    private ExerciseType switchsEqty(String s) {
//        switch (s){
//            case "REPS_ONLY" -> {
//                return ExerciseType.REPS_ONLY;
//            }
//            case "TIME_ONLY" -> {
//                return ExerciseType.TIME_ONLY;
//            }
//            default -> {
//                return ExerciseType.WEIGHT_AND_REPS;
//            }
//        }
//    }
//
//    private ExerciseEquipment switchsEq(String s) {
//        switch (s){
//            case "맨몸" -> {
//                return ExerciseEquipment.BODYWEIGHT;
//            }
//            case "케이블" -> {
//                return ExerciseEquipment.CABLE;
//            }
//            case "머신" -> {
//                return ExerciseEquipment.MACHINE;
//            }
//            case "중량" -> {
//                return ExerciseEquipment.WEIGHT;
//            }
//            case "바벨" -> {
//                return ExerciseEquipment.BARBELL;
//            }
//            case "스트레칭" -> {
//                return ExerciseEquipment.STRETCHING;
//            }
//            case "덤벨" -> {
//                return ExerciseEquipment.DUMBBELL;
//            }
//            case "스미스 머신" -> {
//                return ExerciseEquipment.SMITH_MACHINE;
//            }
//            case "밴드" -> {
//                return ExerciseEquipment.BAND;
//            }
//            case "유산소" -> {
//                return ExerciseEquipment.CARDIO;
//            }default -> {
//                return null;
//            }
//
//        }
//    }
//
//    public static ExerciseTarget switchs(String target){
//        switch (target){
//            case "복근" -> {
//                return 복근;
//            }
//            case "등" -> {
//                return 등;
//            }
//            case "가슴" -> {
//                return 가슴;
//            }
//            case "엉덩이" -> {
//                return 엉덩이;
//            }
//            case "대퇴사두근" -> {
//                return 대퇴사두근;
//            }
//            case "삼두" -> {
//                return 삼두;
//            }
//            case "이두" -> {
//                return 이두;
//            }
//            case "어깨" -> {
//                return 어깨;
//            }
//            case "종아리" -> {
//                return 종아리;
//            }
//            case "전완" -> {
//                return 전완;
//            }
//            case "햄스트링" -> {
//                return 햄스트링;
//            }default -> {
//                return 기타;
//            }
//
//        }
//    }
//}
//
