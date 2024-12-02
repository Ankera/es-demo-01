package cn.itcast.hotel;

import cn.itcast.hotel.mapper.HotelMapper;
import cn.itcast.hotel.pojo.Hotel;
import cn.itcast.hotel.pojo.HotelDoc;
import com.alibaba.fastjson.JSON;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.SearchContextAggregations;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@SpringBootTest
public class HotelDocumentTest {

    @Autowired
    private RestHighLevelClient client;

    @Autowired
    private HotelMapper hotelMapper;

    @Test
    public void test01() throws IOException {
        Hotel hotel = hotelMapper.selectById(39106L);
        HotelDoc hotelDoc = new HotelDoc(hotel);
        Object json = JSON.toJSON(hotelDoc);
        System.out.println(json);

        IndexRequest request = new IndexRequest("hotel").id(hotelDoc.getId() + "");
        request.source(json, XContentType.JSON);

        IndexResponse index = client.index(request, RequestOptions.DEFAULT);
        System.out.println("===index" + index);
    }

    @Test
    public void test02() throws IOException {
        GetRequest getRequest = new GetRequest("hotel", "39106");
        GetResponse result = client.get(getRequest, RequestOptions.DEFAULT);
        String json = result.getSourceAsString();
        HotelDoc hotelDoc = JSON.parseObject(json, HotelDoc.class);
        System.out.println(hotelDoc);
    }

    @Test
    public void test03() throws IOException {
        List<Hotel> hotels = hotelMapper.selectList(null);

        BulkRequest request = new BulkRequest();

        hotels.forEach(hotel -> {
            HotelDoc hotelDoc = new HotelDoc(hotel);
            IndexRequest hotelReq = new IndexRequest("hotel")
                    .id(hotel.getId() + "")
                    .source(JSON.toJSONString(hotelDoc), XContentType.JSON);
            request.add(hotelReq);
        });

        client.bulk(request, RequestOptions.DEFAULT);
    }

    @Test
    public void test04() throws IOException {
        SearchRequest request = new SearchRequest("hotel");
        request.source().size(0);
        request.source().aggregation(AggregationBuilders.terms("brandAggs").field("brand").size(20));

        SearchResponse search = client.search(request, RequestOptions.DEFAULT);
        Aggregations aggregations = search.getAggregations();
        Terms brandAggs = aggregations.get("brandAggs");
        brandAggs.getBuckets().forEach(b -> {
            System.out.print(b.getKeyAsString() + "==" + b.getDocCount());
            System.out.println();
        });
    }
}
