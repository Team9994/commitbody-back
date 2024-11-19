package team9499.commitbody.domain.article.event;

import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import team9499.commitbody.domain.article.dto.ArticleDto;
import team9499.commitbody.domain.article.service.ElsArticleService;

import static team9499.commitbody.global.constants.ElasticFiled.*;

@Component
@RequiredArgsConstructor
public class ArticleEventHandler {

    private final ElsArticleService elsArticleService;

    @EventListener
    public void saveElsArticle(ArticleDto articleDto){
        elsArticleService.saveArticleAsync(articleDto);
    }
    
    @EventListener
    public void ElsArticle(ElsArticleEvent elsArticleEvent){
        String type = elsArticleEvent.getType();
        ArticleDto articleDto = elsArticleEvent.getArticleDto();
        switch (type) {
            case ADD -> elsArticleService.saveArticleAsync(articleDto);
            case UPDATE -> elsArticleService.updateArticleAsync(articleDto);
            case DELETE -> elsArticleService.deleteArticleAsync(articleDto.getArticleId());
        }
    }

    @EventListener
    public void updateElsArticleCount(ElsArticleCountEvent elsArticleCountEvent){
        elsArticleService.updateArticleCountAsync(elsArticleCountEvent.getArticleId(),elsArticleCountEvent.getCount(),elsArticleCountEvent.getType());
    }
}

