package team9499.commitbody.domain.article.event;

import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import team9499.commitbody.domain.article.dto.ArticleDto;
import team9499.commitbody.domain.article.service.ElsArticleService;

@Component
@RequiredArgsConstructor
public class ArticleEventHandler {

    private final ElsArticleService elsArticleService;

    @EventListener
    public void saveElsArticle(ArticleDto articleDto){
        elsArticleService.saveArticle(articleDto);
    }
}

