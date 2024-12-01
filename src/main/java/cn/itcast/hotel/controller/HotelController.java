package cn.itcast.hotel.controller;

import cn.itcast.hotel.service.IHotelService;
import cn.itcast.hotel.vo.PageResult;
import cn.itcast.hotel.vo.RequestParams;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping("/hotel")
public class HotelController {

    @Autowired
    private IHotelService hotelService;

    @RequestMapping("/list")
    public PageResult searchPageInfo(@RequestBody RequestParams params){
        try {
            return hotelService.searchPageInfo(params);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
