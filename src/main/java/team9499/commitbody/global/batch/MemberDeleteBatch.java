package team9499.commitbody.global.batch;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
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
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import team9499.commitbody.domain.Member.domain.Member;

import javax.sql.DataSource;
import java.util.Map;

@Slf4j
@Configuration
public class MemberDeleteBatch {

    private final PlatformTransactionManager dataTransactionManager;
    private final JobRepository jobRepository;
    private final DataSource dataDBSource;

    public MemberDeleteBatch(@Qualifier("dataTransactionManager") PlatformTransactionManager dataTransactionManager,
                             JobRepository jobRepository,
                             @Qualifier("dataDBSource") DataSource dataDBSource) {
        this.dataTransactionManager = dataTransactionManager;
        this.jobRepository = jobRepository;
        this.dataDBSource = dataDBSource;
    }

    @Bean
    public Job deleteMemberJob() {
        log.info("탈퇴한 사용자 데이터 삭제 배치");
        return new JobBuilder("deleteMemberJob", jobRepository)
                .start(firstStep())
                .build();
    }

    @Bean
    public Step firstStep() {
        return new StepBuilder("firstStep", jobRepository)
                .<Member, Member> chunk(10, dataTransactionManager)
                .reader(beforeReader())
                .processor(itemProcessor())
                .writer(deleteWriters())
                .build();
    }

    @Bean
    public JdbcPagingItemReader<Member> beforeReader(){
        return new JdbcPagingItemReaderBuilder<Member>()
                .name("beforeReader")
                .dataSource(dataDBSource)
                .selectClause("select member_id")
                .fromClause("from member")
                .whereClause("where is_withdrawn = true and withdrawn_at <= date(now()) and withdrawn_at is not null")
                .sortKeys(Map.of("member_id", Order.ASCENDING))
                .rowMapper(new CustomMemberRowMapper())
                .pageSize(10)
                .build();

    }

    @Bean
    public ItemProcessor<Member,Member> itemProcessor (){
        return member -> {
            log.info("탈퇴 사용자 Id ={}",member.getId());
            return member;
        };
    }
    @Bean
    public JdbcBatchItemWriter<Member> deleteWriters() {
        String sql = "CALL delete_member_data(:id)";

        return new JdbcBatchItemWriterBuilder<Member>()
                .dataSource(dataDBSource)
                .sql(sql)
                .itemSqlParameterSourceProvider(new BeanPropertyItemSqlParameterSourceProvider<>())
                .assertUpdates(false)
                .build();
    }
}
