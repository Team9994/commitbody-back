package team9499.commitbody.domain.follow.domain;

public enum FollowType {
    FOLLOW,
    FOLLOWING,
    CANCEL,
    MUTUAL_FOLLOW,
    UNFOLLOW,
    FOLLOW_ONLY,       // 상대방이 나를 팔로우하고 있지만 나는 그를 팔로우하지 않는 상태
    BOTH,           // 서로 팔로우 중
    NEITHER         // 서로 팔로우 하지 않음

}
