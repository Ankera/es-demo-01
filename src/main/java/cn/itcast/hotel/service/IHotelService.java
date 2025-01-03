package cn.itcast.hotel.service;

import cn.itcast.hotel.pojo.Hotel;
import cn.itcast.hotel.vo.PageResult;
import cn.itcast.hotel.vo.RequestParams;
import com.baomidou.mybatisplus.extension.service.IService;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface IHotelService extends IService<Hotel> {
    PageResult searchPageInfo(RequestParams params) throws IOException;

    Map<String, List<String>> getFilters(RequestParams params);

    List<String> getSuggestion(String key);
}
