package cn.itcast.hotel;

import org.apache.lucene.search.TotalHits;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;

@SpringBootTest
public class HotelSearchTest {

    @Autowired
    private RestHighLevelClient restHighLevelClient;

    @Test
    public void test01() throws IOException {
        SearchRequest request = new SearchRequest("hotel");

        request.source().query(QueryBuilders.matchAllQuery());

        SearchResponse response = restHighLevelClient.search(request, RequestOptions.DEFAULT);
        SearchHits hits = response.getHits();
        for (SearchHit hit : hits) {
            System.out.println(hit.getSourceAsString());
        }
        TotalHits totalHits = hits.getTotalHits();
        assert totalHits != null;
        System.out.println(totalHits.toString());
    }
}
