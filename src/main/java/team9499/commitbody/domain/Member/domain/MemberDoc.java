package team9499.commitbody.domain.Member.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.WriteTypeHint;

@Data
@Builder
@Document(indexName = "member_index", writeTypeHint = WriteTypeHint.FALSE)
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@NoArgsConstructor
public class MemberDoc {

    @Id
    private String id;

    @Field(type = FieldType.Text, name = "nickname")
    private String nickname;

    @Field(type = FieldType.Text, name = "profile")
    private String profile;

    public static MemberDoc create(String id,String nickname, String profile){
        return new MemberDoc(id,nickname,profile);
    }
}
