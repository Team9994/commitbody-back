package team9499.commitbody.global.constants;

public class ElasticFiled {

    // COMMON
    public static final String MEMBER_ID = "memberId";  // 멤버 필드
    public static final String STATUS = "status";   // 상태
    public static final String _SCORE = "_score";
    public static final String _SOURCE = "_source";
    public static final String CUSTOM_ = "custom_";
    public static final String CUSTOM = "custom";
    public static final String DEFAULT_ = "default_";
    public static final String DEFAULT = "default";
    public static final String ID = "id";
    public static final String _ID = "_id";

    public static final String ADD = "등록";
    public static final String CANCEL = "해제";
    public static final String UPDATE = "수정";
    public static final String DELETE = "삭제";


    public static final String MEMBER_INDEX = "member_index";
    public static final String INTEREST_INDEX = "exercise_interest_index";


    // EXERCISE DOC
    public static final String EXERCISE_INDEX = "exercise_index";
    public static final String EXERCISE_ID = "exerciseId";
    public static final String EXERCISE_NAME = "exerciseName";
    public static final String EXERCISE_GIF = "gifUrl";
    public static final String EXERCISE_TARGET = "exerciseTarget";
    public static final String EXERCISE_TYPE = "exerciseType";
    public static final String EXERCISE_EQUIPMENT = "exerciseEquipment";
    public static final String EXERCISE_INTEREST = "interest";
    public static final String SOURCE = "source";

    public static final String NO_IMAGE = "등록된 이미지 파일이 없습니다.";
    public static final String WEIGHT_AND_LEP = "무게와 횟수";

    // ARTICLE DOC
    public static final String ARTICLE_INDEX  = "article_index";

    public static final String BLOCK_MEMBER_INDEX = "block_member_index";
    public static final String BLOCKER_ID = "blockerId";
    public static final String BLOCKED_ = "blocked_";


    // Script
    public static final String CTX_WITH_DRAW = "ctx._source.withDraw = params.writDraw";
    public static final String INCREMENT_LIKE_COUNT = "ctx._source.like_count += 1";
    public static final String DECREMENT_LIKE_COUNT = "ctx._source.like_count -= 1";
    public static final String INCREMENT_COMMENT_COUNT = "ctx._source.comment_count += params.count";
    public static final String DECREMENT_COMMENT_COUNT = "ctx._source.comment_count -= params.count";
    public static final String BLOCKER_REMOVE_IF = "ctx._source.blockerId.removeIf(id -> id == params.id)";

    public static final String PAINLESS = "painless";
    public static final String WITH_DRAW = "withDraw";
    public static final String WRIT_DRAW_KR = "탈퇴";


    // MAPPING DOC
    public static final String CATEGORY = "category";
    public static final String TITLE = "title";
    public static final String IMG_URL = "img_url";
    public static final String VISIBILITY = "visibility";
    public static final String CONTENT = "content";
    public static final String LIKE_COUNT = "like_count";
    public static final String COMMENT_COUNT = "comment_count";
    public static final String TIME = "time";
    public static final String WRITER = "writer";
}
