package cn.itcast.hotel;

import cn.itcast.hotel.pojo.Hotel;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@MapperScan("cn.itcast.hotel.mapper")
@SpringBootApplication
public class HotelDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(HotelDemoApplication.class, args);
    }

    @Bean
    public RestHighLevelClient restHighLevelClient(){
        System.out.println("=====restHighLevelClient===this is demo==");
        return new RestHighLevelClient(RestClient.builder(
                HttpHost.create("http://182.92.130.109:9200")
        ));
    }

    @Bean
    public Hotel hotel() {
        Hotel hotel = new Hotel();
        hotel.setAddress("=====address===this is demo==");
        return hotel;
    }
}
