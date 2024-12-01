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
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
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
}
