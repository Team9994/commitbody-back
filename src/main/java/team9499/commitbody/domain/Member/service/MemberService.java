package team9499.commitbody.domain.Member.service;

import org.springframework.web.multipart.MultipartFile;
import team9499.commitbody.domain.Member.domain.Gender;
import team9499.commitbody.domain.Member.dto.response.MemberMyPageResponse;

import java.time.LocalDate;

public interface MemberService{

    MemberMyPageResponse getMyPage(Long memberId,String nickname);

    void updateProfile(Long memberId, String nickname, Gender gender, LocalDate birthDay, Float height, Float weight, Float BoneMineralDensity,
                       Float BodyFatPercentage, boolean deleteProfile,MultipartFile file);
}
