package cn.itcast.hotel;

import cn.itcast.hotel.pojo.Hotel;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexResponse;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.common.xcontent.XContentType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;

import static cn.itcast.hotel.constants.HotelIndexConstants.MAPPING_TEMPLATE;

@SpringBootTest
class HotelDemoApplicationTests {

    @Autowired
    private RestHighLevelClient client;

    @Test
    void test01() throws IOException {
        CreateIndexRequest request = new CreateIndexRequest("hotel");

        request.source(MAPPING_TEMPLATE, XContentType.JSON);

        CreateIndexResponse response = client.indices().create(request, RequestOptions.DEFAULT);

        boolean acknowledged = response.isAcknowledged();
        if (acknowledged) {
            System.out.println("success");
        } else {
            System.out.println("fail");
        }
    }

    @Autowired
    private Hotel hotel;

    @Test
    void test02() {
        String address = hotel.getAddress();
        System.out.println(address);
    }

    @Test
    void test03() throws IOException {
        GetIndexRequest request = new GetIndexRequest("hotel9992");
        boolean exists = client.indices().exists(request, RequestOptions.DEFAULT);
        if (exists) {
            System.out.println("exists");
        } else
            System.out.println("not exists");
    }



}
