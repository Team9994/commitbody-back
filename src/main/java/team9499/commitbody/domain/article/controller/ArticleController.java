package team9499.commitbody.domain.article.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import team9499.commitbody.domain.article.domain.ArticleCategory;
import team9499.commitbody.domain.article.domain.ArticleType;
import team9499.commitbody.domain.article.dto.ArticleDto;
import team9499.commitbody.domain.article.dto.request.ArticleRequest;
import team9499.commitbody.domain.article.dto.response.AllArticleResponse;
import team9499.commitbody.domain.article.dto.response.ProfileArticleResponse;
import team9499.commitbody.domain.article.event.ElsArticleEvent;
import team9499.commitbody.domain.article.service.ArticleService;
import team9499.commitbody.domain.article.service.ElsArticleService;
import team9499.commitbody.global.authorization.domain.PrincipalDetails;
import team9499.commitbody.global.payload.ErrorResponse;
import team9499.commitbody.global.payload.SuccessResponse;
import team9499.commitbody.global.redis.RedisService;

import java.util.List;
import java.util.Map;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class ArticleController {

    private final ArticleService articleService;
    private final ElsArticleService elsArticleService;
    private final RedisService redisService;
    private final ApplicationEventPublisher eventPublisher;

    @Operation(summary = "게시글 조회", description = "작성된 게시글의 카테고리별로 게시글의 무한 스크롤 방식으로 조회합니다. 기본값: [type : EXERCISE, category : ALL , size : 12]",tags = "게시글")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200_1", description = "OK - 운동 인증 게시글 조회시", content = @Content(schema = @Schema(implementation = SuccessResponse.class),
                    examples = @ExampleObject(value = "{\"success\": true, \"message\": \"조회 성공\", \"data\": {\"hasNext\": false, \"articles\": [{\"articleId\": 1, \"imageUrl\": \"https://d12ryzjapybmlj.cloudfront.net/images/test.png\"}]}}"))),
            @ApiResponse(responseCode = "200_2", description = "OK - 정보 질문 게시글 조회시", content = @Content(schema = @Schema(implementation = SuccessResponse.class),
                    examples = @ExampleObject(value = "{\"success\": true, \"message\": \"조회 성공\", \"data\": {\"hasNext\": false, \"articles\": [{\"articleId\": 2, \"postOwner\": true, \"title\": \"게시글\", \"content\": \"내용\", \"articleCategory\": \"INFORMATION\", \"time\": \"3일 전\", \"likeCount\": 10, \"commentCount\": 0, \"imageUrl\": \"등록된 이미지가 없습니다.\", \"member\": {\"memberId\": 1, \"nickname\": \"닉네임1\", \"profile\": \"https://d12ryzjapybmlj.cloudfront.net/default.PNG\"}}]}}"))),
            @ApiResponse(responseCode = "400_1", description = "BADREQUEST - 사용 불가 토큰",content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"success\" : false,\"message\":\"사용할 수 없는 토큰입니다.\"}"))),
            @ApiResponse(responseCode = "400_2", description = "BADREQUEST - 차단된 사용자 접근시",content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"success\" : false,\"message\":\"사용자를 차단한 상태입니다.\"}"))),
            @ApiResponse(responseCode = "401", description = "UNAUTHORIZED", content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"success\" : false,\"message\":\"토큰이 존재하지 않습니다.\"}")))})
    @GetMapping("/article")
    public ResponseEntity<?> getAllArticle(@RequestParam(value = "type",required = false,defaultValue = "EXERCISE") ArticleType type,
                                           @RequestParam(value = "category",required = false,defaultValue = "ALL") ArticleCategory category,
                                           @RequestParam(value = "lastId",required = false) Long lastId,
                                           @AuthenticationPrincipal PrincipalDetails principalDetails,
                                           @Parameter(example = "{\"size\":12}") @PageableDefault(size = 12) Pageable pageable
                                           ){
        Long memberId = getMemberId(principalDetails);
        AllArticleResponse allArticles = articleService.getAllArticles(memberId, type, category, lastId, pageable);
        return ResponseEntity.ok(new SuccessResponse<>(true, "조회 성공",allArticles));
    }

    @Tag(name = "게시글",description = "게시글 관련 API")
    @Operation(summary = "게시글 등록", description = "게시글을 작성하는 API 입니다. 질문&정보 게시글 등록시에는 'articleCategory' 필드를 사용하지 않아도 됩니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OK - 게시글 작성 성공시 작성된 게시글 ID를 반환합니다.", content = @Content(schema = @Schema(implementation = SuccessResponse.class),
                    examples = @ExampleObject(value = "{\"success\":true,\"message\":\"등록 성공\",\"data\":1}"))),
            @ApiResponse(responseCode = "400_1", description = "BADREQUEST - 사용 불가 토큰",content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"success\" : false,\"message\":\"사용할 수 없는 토큰입니다.\"}"))),
            @ApiResponse(responseCode = "400_2", description = "BADREQUEST - 값 입렵 검증", content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"success\" : false,\"message\":{\"필드명\": \"오류 내용\"}}"))),
            @ApiResponse(responseCode = "400_3", description = "BADREQUEST - 파일 용량 초과(5MB 이하만 저장가능)",content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"success\" : false,\"message\":\"저장 가능한 용량을 초과 했습니다.\"}"))),
            @ApiResponse(responseCode = "400_4", description = "BADREQUEST - 불가능한 이미지 파일 저장시(jpeg, jpg, png, gif 저장가능)",content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"success\" : false,\"message\":\"올바른 이미지 형식이 아닙니다.\"}"))),
            @ApiResponse(responseCode = "400_5", description = "BADREQUEST - 존재하지 않는 시용자 요청시",content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"success\" : false,\"message\":\"사용자를 찾을수 없습니다.\"}"))),
            @ApiResponse(responseCode = "400_6", description = "BADREQUEST - 몸평 카테고리외 동영상 저장시",content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"success\" : false,\"message\":\"이미지 파일만 등록 가능합니다.\"}"))),
            @ApiResponse(responseCode = "401", description = "UNAUTHORIZED", content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"success\" : false,\"message\":\"토큰이 존재하지 않습니다.\"}")))})
    @PostMapping(value = "/article",consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> saveArticle(@Valid @RequestPart("articleSaveRequest") ArticleRequest request, BindingResult result,
                                         @RequestPart(value = "file",required = false)MultipartFile file,
                                         @AuthenticationPrincipal PrincipalDetails principalDetails){
        Long memberId = getMemberId(principalDetails);
        ArticleDto articleDto = articleService.saveArticle(memberId, request.getTitle(), request.getContent(), request.getArticleType(), request.getArticleCategory(), request.getVisibility(), file);

        // 정보&질문 게시글일때만 이벤트 발생
        articleEvent(request, articleDto,"등록");

        return ResponseEntity.ok(new SuccessResponse<>(true,"등록 성공",articleDto.getArticleId()));
    }

    @Operation(summary = "게시글 상세 조회 - 기본 게시글 정보", description = "댓글을 제외한 게시글의 정보를 조회합니다. 차단된 사용자가 접근시 예외 발생",tags = "게시글")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = SuccessResponse.class),
                    examples = @ExampleObject(value = "{\"success\": true, \"message\": \"조회 성공\", \"data\": {\"articleId\": 1, \"postOwner\": false, \"followStatus\": \"CANCEL\", \"title\": \"운동 게시글\", \"content\": \"운동 게시글의 내용입니다.\", \"time\": \"3일 전\", \"likeCount\": 0, \"commentCount\": 0, \"imageUrl\": \"https://d12ryzjapybmlj.cloudfront.net/images/10ee7e78-fd02-450e-a515-604d97d97c74.png\", \"member\": {\"memberId\": 2, \"nickname\": \"두번쨰닉네임\", \"profile\": \"https://d12ryzjapybmlj.cloudfront.net/default.PNG\", \"blockStatus\": false}}}"))),
            @ApiResponse(responseCode = "400_1", description = "BADREQUEST - 사용 불가 토큰",content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"success\" : false,\"message\":\"사용할 수 없는 토큰입니다.\"}"))),
            @ApiResponse(responseCode = "400_2", description = "BADREQUEST - 차단된 사용자 접근시",content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"success\" : false,\"message\":\"사용자를 차단한 상태입니다.\"}"))),
            @ApiResponse(responseCode = "401", description = "UNAUTHORIZED", content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"success\" : false,\"message\":\"토큰이 존재하지 않습니다.\"}")))})
    @GetMapping("/article/{articleId}")
    public ResponseEntity<?> getDetailsArticle(@PathVariable("articleId") Long articleId,
                                               @AuthenticationPrincipal PrincipalDetails principalDetails){
        Long memberId = getMemberId(principalDetails);
        ArticleDto detailArticle = articleService.getDetailArticle(memberId, articleId);
        return ResponseEntity.ok(new SuccessResponse<>(true,"조회 성공",detailArticle));
    }

    @Operation(summary = "프로필 페이지 - 작성한 게시글 조회", description = "프로필 사용자가 작성한 게시글을 조회합니다. Default Size = 12 ",tags = "프로필")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200_1", description = "운동 인증 게시글 조회시", content = @Content(schema = @Schema(implementation = SuccessResponse.class),
                    examples = @ExampleObject(value = "{\"success\": true, \"message\": \"조회 성공\", \"data\": {\"hasNext\": false, \"articles\": [{\"articleId\": 1, \"imageUrl\": \"https://d12ryzjapybmlj.cloudfront.net/images/7280c2c8-8b6b-4eeb-a2e7-55d15162fb06.png\"}]}}"))),
            @ApiResponse(responseCode = "200_2", description = "정보&질문 게시글 조회시", content = @Content(schema = @Schema(implementation = SuccessResponse.class),
                    examples = @ExampleObject(value = "{\"success\": true, \"message\": \"조회 성공\", \"data\": {\"hasNext\": false, \"articles\": [{\"articleId\": 21, \"title\": \"제목\", \"articleCategory\": \"INFORMATION\", \"time\": \"2시간 전\", \"likeCount\": 0, \"commentCount\": 0, \"imageUrl\": \"등록된 이미지가 없습니다.\"}, {\"articleId\": 14, \"title\": \"운동 게시글\", \"articleCategory\": \"BODY_REVIEW\", \"time\": \"2일 전\", \"likeCount\": 0, \"commentCount\": 0, \"imageUrl\": \"https://d12ryzjapybmlj.cloudfront.net/images/925bf666-2787-4f31-89ab-ef24bd26d1d5.png\"}]}}"))),
            @ApiResponse(responseCode = "400", description = "BADREQUEST - 사용 불가 토큰",content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"success\" : false,\"message\":\"사용할 수 없는 토큰입니다.\"}"))),
            @ApiResponse(responseCode = "400", description = "BADREQUEST - 차단된 사용자 접근시",content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"success\" : false,\"message\":\"사용자를 차단한 상태입니다.\"}"))),
            @ApiResponse(responseCode = "401", description = "UNAUTHORIZED", content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"success\" : false,\"message\":\"토큰이 존재하지 않습니다.\"}")))})
    @GetMapping("/my-page/articles/{id}")
    public ResponseEntity<?> getAllProfileArticle(@Parameter(description = "조회할 사용자 ID")@PathVariable("id") Long findMemberId,
                                                  @RequestParam("type")ArticleType articleType,
                                                  @RequestParam(value = "lastId",required = false) Long lastId,
                                                  @AuthenticationPrincipal PrincipalDetails principalDetails,
                                                  @Parameter(example = "{\"size\":12}") @PageableDefault(size = 12) Pageable pageable){
        Long memberId = getMemberId(principalDetails);
        ProfileArticleResponse articles = articleService.getAllProfileArticle(memberId, findMemberId, articleType,lastId, pageable);
        return ResponseEntity.ok(new SuccessResponse<>(true,"조회 성공",articles));
    }
    
    @Operation(summary = "게시글 수정", description = "게시글을 수정하는 API 입니다. 질문&정보 게시글 등록시에는 'articleCategory' 필드를 사용하지 않아도 됩니다.",tags = "게시글")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = SuccessResponse.class),
                    examples = @ExampleObject(value = "{\"success\":true,\"message\":\"수정 성공\"}"))),
            @ApiResponse(responseCode = "400_1", description = "BADREQUEST - 사용 불가 토큰",content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"success\" : false,\"message\":\"사용할 수 없는 토큰입니다.\"}"))),
            @ApiResponse(responseCode = "400_2", description = "BADREQUEST - 값 입렵 검증", content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"success\" : false,\"message\":{\"필드명\": \"오류 내용\"}}"))),
            @ApiResponse(responseCode = "400_3", description = "BADREQUEST - 파일 용량 초과(5MB 이하만 저장가능)",content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"success\" : false,\"message\":\"저장 가능한 용량을 초과 했습니다.\"}"))),
            @ApiResponse(responseCode = "400_4", description = "BADREQUEST - 불가능한 이미지 파일 저장시(jpeg, jpg, png, gif 저장가능)",content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"success\" : false,\"message\":\"올바른 이미지 형식이 아닙니다.\"}"))),
            @ApiResponse(responseCode = "400_5", description = "BADREQUEST - 존재하지 않는 시용자 요청시",content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"success\" : false,\"message\":\"사용자를 찾을수 없습니다.\"}"))),
            @ApiResponse(responseCode = "400_6", description = "BADREQUEST - 몸평 카테고리외 동영상 저장시",content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"success\" : false,\"message\":\"이미지 파일만 등록 가능합니다.\"}"))),
            @ApiResponse(responseCode = "401", description = "UNAUTHORIZED", content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"success\" : false,\"message\":\"토큰이 존재하지 않습니다.\"}"))),
            @ApiResponse(responseCode = "403", description = "타 사용자가 삭제시", content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"success\" : false,\"message\":\"작성자만 이용할 수 있습니다.\"}")))})
    @PutMapping(value = "/article/{articleId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> updateArticle(@PathVariable("articleId") Long articleId,
                                           @RequestPart("updateArticleRequest") ArticleRequest request,
                                           @RequestPart(required = false) MultipartFile file,
                                           @AuthenticationPrincipal PrincipalDetails principalDetails){
        Long memberId = getMemberId(principalDetails);
        ArticleDto articleDto = articleService.updateArticle(memberId, articleId, request.getContent(), request.getTitle(), request.getArticleType(), request.getArticleCategory(), request.getVisibility(), file);

        articleEvent(request, articleDto,"수정");
        return ResponseEntity.ok(new SuccessResponse<>(true,"수정 성공"));
    }

    @Operation(summary = "게시글 삭제", description = "게시글을 삭제하는 API 입니다.",tags = "게시글")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = SuccessResponse.class),
                    examples = @ExampleObject(value = "{\"success\":true,\"message\":\"삭제 성공\"}"))),
            @ApiResponse(responseCode = "400_1", description = "BADREQUEST - 사용 불가 토큰",content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"success\" : false,\"message\":\"사용할 수 없는 토큰입니다.\"}"))),
            @ApiResponse(responseCode = "400_2", description = "BADREQUEST - 존재하지 않는 시용자 요청시",content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"success\" : false,\"message\":\"사용자를 찾을수 없습니다.\"}"))),
            @ApiResponse(responseCode = "401", description = "UNAUTHORIZED", content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"success\" : false,\"message\":\"토큰이 존재하지 않습니다.\"}"))),
            @ApiResponse(responseCode = "403", description = "타 사용자가 삭제시", content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"success\" : false,\"message\":\"작성자만 이용할 수 있습니다.\"}")))})
    @DeleteMapping("/article/{articleId}")
    public ResponseEntity<?> deleteArticle(@PathVariable("articleId") Long articleId,
                                           @AuthenticationPrincipal PrincipalDetails  principalDetails){
        Long memberId = getMemberId(principalDetails);
        ArticleDto articleDto = articleService.deleteArticle(memberId, articleId);

        eventPublisher.publishEvent(new ElsArticleEvent(articleDto,"삭제"));
        return ResponseEntity.ok(new SuccessResponse<>(true,"삭제 성공"));
    }

    @Operation(summary = "게시글 검색", description = "게시글 제목을 통한 게시글 검색을 합니다. category는 [INFORMATION,FEEDBACK,BODY_REVIEW]만 사용가능 ",tags = "게시글")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = SuccessResponse.class),
                    examples = @ExampleObject(value = "{\"success\": true, \"message\": \"검색 성공\", \"data\": {\"totalCount\": 5, \"hasNext\": true, \"articles\": [{\"articleId\": 1, \"title\": \"운동 게시글\", \"articleCategory\": \"FEEDBACK\", \"time\": \"25분 전\", \"likeCount\": 0, \"commentCount\": 0, \"imageUrl\": \"등록된 이미지가 없습니다.\", \"member\": {\"memberId\": 2, \"nickname\": \"닉네임\"}}]}}"))),
            @ApiResponse(responseCode = "400_1", description = "BADREQUEST - 사용 불가 토큰",content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"success\" : false,\"message\":\"사용할 수 없는 토큰입니다.\"}"))),
            @ApiResponse(responseCode = "400_2", description = "BADREQUEST - 차단된 사용자 접근시",content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"success\" : false,\"message\":\"사용자를 차단한 상태입니다.\"}"))),
            @ApiResponse(responseCode = "401", description = "UNAUTHORIZED", content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"success\" : false,\"message\":\"토큰이 존재하지 않습니다.\"}")))})
    @GetMapping("/article/search")
    public ResponseEntity<?> searchArticle(@RequestParam(value = "title",required = false) String title,
                                           @RequestParam(value = "category",required = false) ArticleCategory category,
                                           @RequestParam(value = "lastId",required = false) Long lastId,
                                           @RequestParam(value = "size",required = false,defaultValue = "10") Integer size,
                                           @AuthenticationPrincipal PrincipalDetails principalDetails){
        Long memberId = getMemberId(principalDetails);

        AllArticleResponse allArticleResponse = elsArticleService.searchArticleByTitle(memberId, title, category,size, lastId);
        return ResponseEntity.ok(new SuccessResponse<>(true,"검색 성공",allArticleResponse));
    }

    @Operation(summary = "최근 검색어 - 조회", description = "최근 검색어 기록 10개를 조회합니다.",tags = "게시글")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = SuccessResponse.class),
                    examples = @ExampleObject(value = "{\"success\": true, \"message\": \"조회 성공\", \"data\": [\"9\", \"8\", \"7\", \"6\", \"5\", \"4\", \"3\", \"2\", \"1\"]}"))),
            @ApiResponse(responseCode = "400_1", description = "BADREQUEST - 사용 불가 토큰",content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"success\" : false,\"message\":\"사용할 수 없는 토큰입니다.\"}"))),
            @ApiResponse(responseCode = "400_2", description = "BADREQUEST - 차단된 사용자 접근시",content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"success\" : false,\"message\":\"사용자를 차단한 상태입니다.\"}"))),
            @ApiResponse(responseCode = "401", description = "UNAUTHORIZED", content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"success\" : false,\"message\":\"토큰이 존재하지 않습니다.\"}")))})
    @GetMapping("/article/search-record")
    public ResponseEntity<?> getSearchRecord(@AuthenticationPrincipal PrincipalDetails principalDetails){
        Long memberId = getMemberId(principalDetails);
        List<Object> recentSearchLogs = redisService.getRecentSearchLogs(String.valueOf(memberId));
        return ResponseEntity.ok(new SuccessResponse<>(true,"조회 성공",recentSearchLogs));
    }

    @Operation(summary = "최근 검색어 - 등록", description = "최근 검색어 기록을 등록 합니다. 최대 30개를 저장 가능하며 그이상 데이터는 마지막 데이터를 삭제하여 새롭게 저장합니다.",tags = "게시글")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = SuccessResponse.class),
                    examples = @ExampleObject(value = "{\"success\": true, \"message\": \"등록 성공\"}"))),
            @ApiResponse(responseCode = "400_1", description = "BADREQUEST - 사용 불가 토큰",content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"success\" : false,\"message\":\"사용할 수 없는 토큰입니다.\"}"))),
            @ApiResponse(responseCode = "400_2", description = "BADREQUEST - 차단된 사용자 접근시",content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"success\" : false,\"message\":\"사용자를 차단한 상태입니다.\"}"))),
            @ApiResponse(responseCode = "401", description = "UNAUTHORIZED", content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"success\" : false,\"message\":\"토큰이 존재하지 않습니다.\"}")))})
    @PostMapping("/article/search-record")
    public ResponseEntity<?> saveSearchRecord(@Parameter(schema = @Schema(example = "{\"title\":\"최근 검색어\"}"))@RequestBody Map<String,String> searchRecordRequest,
                                              @AuthenticationPrincipal PrincipalDetails principalDetails){
        Long memberId = getMemberId(principalDetails);
        redisService.setRecentSearchLog(String.valueOf(memberId),searchRecordRequest.get("title"));
        return ResponseEntity.ok(new SuccessResponse<>(true,"등록 성공"));
    }

    @Operation(summary = "최근 검색어 - 삭제", description = "최근 검색어 기록을 삭제 합니다. 선택 삭제시에는 title 사용, 전체 삭제시에는 type의 all을 사용합니다.",tags = "게시글")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = SuccessResponse.class),
                    examples = @ExampleObject(value = "{\"success\": true, \"message\": \"삭제 성공\"}"))),
            @ApiResponse(responseCode = "400_1", description = "BADREQUEST - 사용 불가 토큰",content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"success\" : false,\"message\":\"사용할 수 없는 토큰입니다.\"}"))),
            @ApiResponse(responseCode = "400_2", description = "BADREQUEST - 차단된 사용자 접근시",content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"success\" : false,\"message\":\"사용자를 차단한 상태입니다.\"}"))),
            @ApiResponse(responseCode = "401", description = "UNAUTHORIZED", content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"success\" : false,\"message\":\"토큰이 존재하지 않습니다.\"}")))})
    @DeleteMapping("/article/search-record")
    public ResponseEntity<?> deleteSearchRecord(@RequestParam(value = "title",required = false) String title,
                                                @RequestParam(value = "type",required = false) String type,
                                                @AuthenticationPrincipal PrincipalDetails principalDetails){
        Long memberId = getMemberId(principalDetails);
        redisService.deleteRecentSearchLog(String.valueOf(memberId),title,type);
        return ResponseEntity.ok(new SuccessResponse<>(true,"삭제 성공"));
    }

    private static Long getMemberId(PrincipalDetails principalDetails) {
        Long memberId = principalDetails.getMember().getId();
        return memberId;
    }

    private void articleEvent(ArticleRequest request, ArticleDto articleDto,String type) {
        if (request.getArticleType().equals(ArticleType.INFO_QUESTION))
            eventPublisher.publishEvent(new ElsArticleEvent(articleDto,type));
    }
}
