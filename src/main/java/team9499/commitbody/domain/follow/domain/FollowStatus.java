package team9499.commitbody.domain.follow.domain;

public enum FollowStatus {
    FOLLOWING, // 팔로잉
    UNFOLLOW,   // 언팔로우
    REQUEST,  // 팔로우 요청 상태
    MUTUAL_FOLLOW, //맞 팔로잉 
    CANCEL,         // 팔로우 취소
    BLOCK   // 차단된 상태
}
