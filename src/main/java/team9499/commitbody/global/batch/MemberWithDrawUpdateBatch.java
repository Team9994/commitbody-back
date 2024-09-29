package team9499.commitbody.global.batch;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.database.BeanPropertyItemSqlParameterSourceProvider;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.JdbcPagingItemReader;
import org.springframework.batch.item.database.Order;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.database.builder.JdbcPagingItemReaderBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import team9499.commitbody.domain.article.domain.Article;
import team9499.commitbody.domain.comment.article.domain.ArticleComment;
import team9499.commitbody.domain.like.domain.ContentLike;
import team9499.commitbody.global.batch.mapper.CustomCommentRowMapper;
import team9499.commitbody.global.batch.mapper.CustomLikeRowMapper;

import javax.sql.DataSource;
import java.util.Map;

@Slf4j
@Configuration
public class MemberWithDrawUpdateBatch {

    private final PlatformTransactionManager dataTransactionManager;
    private final JobRepository jobRepository;
    private final DataSource dataDBSource;

    public MemberWithDrawUpdateBatch(@Qualifier("dataTransactionManager")PlatformTransactionManager dataTransactionManager,
                                     JobRepository jobRepository,
                                     @Qualifier("dataDBSource") DataSource dataDBSource) {
        this.dataTransactionManager = dataTransactionManager;
        this.jobRepository = jobRepository;
        this.dataDBSource = dataDBSource;
    }

    @Bean
    public Job updateBatchJob(){
        return new JobBuilder("updateBatchJob",jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(commentStep())
                .next(likeStep())
                .build();
    }

    @Bean
    public Step commentStep(){
        return new StepBuilder("commentStep",jobRepository)
                .<ArticleComment, Article> chunk(10,dataTransactionManager)
                .reader(commentReader(null))
                .processor(commentProcessor())
                .writer(commentWriter(null))
                .build();
    }

    @Bean
    @StepScope
    public JdbcPagingItemReader<ArticleComment> commentReader(@Value("#{jobParameters[memberId]}") Long memberId) {
        return new JdbcPagingItemReaderBuilder<ArticleComment>()
                .name("commentReader")
                .dataSource(dataDBSource)
                .selectClause("select *")
                .fromClause("from article_comment")
                .whereClause("where member_id = :memberId and parent_id is null")
                .sortKeys(Map.of("article_id", Order.ASCENDING))
                .rowMapper(new CustomCommentRowMapper())
                .parameterValues(Map.of("memberId", memberId))  // JobParameter로 전달된 memberId 사용
                .pageSize(10)
                .build();
    }

    @Bean
    public ItemProcessor<ArticleComment,Article> commentProcessor(){
        return ArticleComment::getArticle;
    }

    @Bean
    @StepScope
    public JdbcBatchItemWriter<Article> commentWriter(@Value("#{jobParameters[action]}") String action) {
        String mathSymbols = getMathSymbols(action);
        String s = "UPDATE article SET comment_count = comment_count "+mathSymbols+" WHERE article_id = :id";
        return new JdbcBatchItemWriterBuilder<Article>()
                .dataSource(dataDBSource)
                .sql(s)
                .itemSqlParameterSourceProvider(new BeanPropertyItemSqlParameterSourceProvider<>())
                .assertUpdates(false)
                .build();
    }

    @Bean
    public Step likeStep(){
        return new StepBuilder("likeStep",jobRepository)
                .<ContentLike, Article> chunk(10,dataTransactionManager)
                .reader(likeReader(null))
                .processor(likeProcessor())
                .writer(likeWriter(null))
                .build();
    }

    @Bean
    @StepScope
    public JdbcPagingItemReader<ContentLike> likeReader(@Value("#{jobParameters[memberId]}") Long memberId) {
        return new JdbcPagingItemReaderBuilder<ContentLike>()
                .name("likeReader")
                .dataSource(dataDBSource)
                .selectClause("select *")
                .fromClause("from content_like")
                .whereClause("where member_id = :memberId and article_id is not null and like_status = true")
                .sortKeys(Map.of("article_id", Order.ASCENDING))
                .rowMapper(new CustomLikeRowMapper())
                .parameterValues(Map.of("memberId", memberId))  // JobParameter로 전달된 memberId 사용
                .pageSize(10)
                .build();
    }

    @Bean
    public ItemProcessor<ContentLike,Article> likeProcessor(){
        return ContentLike::getArticle;
    }

    @Bean
    @StepScope
    public JdbcBatchItemWriter<Article> likeWriter(@Value("#{jobParameters[action]}") String action) {
        String mathSymbols = getMathSymbols(action);
        String s = "UPDATE article SET like_count = like_count "+mathSymbols+" WHERE article_id = :id";
        return new JdbcBatchItemWriterBuilder<Article>()
                .dataSource(dataDBSource)
                .sql(s)
                .itemSqlParameterSourceProvider(new BeanPropertyItemSqlParameterSourceProvider<>())
                .assertUpdates(false)
                .build();
    }

    private static String getMathSymbols(String action) {
        return action.equals("탈퇴") ? "-1" : "+1";
    }
}
