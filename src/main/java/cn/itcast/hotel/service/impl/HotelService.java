package cn.itcast.hotel.service.impl;

import cn.itcast.hotel.mapper.HotelMapper;
import cn.itcast.hotel.pojo.Hotel;
import cn.itcast.hotel.pojo.HotelDoc;
import cn.itcast.hotel.service.IHotelService;
import cn.itcast.hotel.vo.PageResult;
import cn.itcast.hotel.vo.RequestParams;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.geo.GeoPoint;
import org.elasticsearch.common.lucene.search.function.CombineFunction;
import org.elasticsearch.common.unit.DistanceUnit;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.functionscore.FunctionScoreQueryBuilder;
import org.elasticsearch.index.query.functionscore.ScoreFunctionBuilder;
import org.elasticsearch.index.query.functionscore.ScoreFunctionBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.filter.RequestContextFilter;

import java.io.IOException;
import java.util.ArrayList;

@Service
public class HotelService extends ServiceImpl<HotelMapper, Hotel> implements IHotelService {

    @Autowired
    private RestHighLevelClient restHighLevelClient;

    @Override
    public PageResult searchPageInfo(RequestParams params) throws IOException {
        SearchRequest request = new SearchRequest("hotel");

        this.buildBasicRequest(request, params);

        int page = params.getPage() == null ? 1 : params.getPage();
        int size = params.getSize() == null ? 5 : params.getSize();
        request.source().from((page -1) * size).size(size);

        // 排序规则
        if (StringUtils.isNotBlank(params.getLocation())) {
            request.source().sort(SortBuilders.geoDistanceSort("location", new GeoPoint(params.getLocation())).order(SortOrder.ASC).unit(DistanceUnit.KILOMETERS));
        }

        SearchResponse response = restHighLevelClient.search(request, RequestOptions.DEFAULT);
        return this.handleResponse(response);
    }

    private void buildBasicRequest(SearchRequest request, RequestParams params) throws IOException {
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();

        if (StringUtils.isBlank(params.getKey())) {
            boolQuery.must(QueryBuilders.matchAllQuery());
        } else {
            boolQuery.must(QueryBuilders.matchQuery("all", params.getKey()));
        }

        if (StringUtils.isNotBlank(params.getBrand())) {
            boolQuery.must(QueryBuilders.matchQuery("brand", params.getBrand()));
        }

        if (StringUtils.isNotBlank(params.getCity())) {
            boolQuery.must(QueryBuilders.matchQuery("city", params.getCity()));
        }

        if (StringUtils.isNoneBlank(params.getStarName())) {
            boolQuery.must(QueryBuilders.matchQuery("starName", params.getStarName()));
        }

        if (params.getMinPrice() != null && params.getMaxPrice() != null) {
            boolQuery.filter(QueryBuilders.rangeQuery("price").gte(params.getMinPrice()).lte(params.getMaxPrice()));
        }


        FunctionScoreQueryBuilder query =
                QueryBuilders.functionScoreQuery(boolQuery, new FunctionScoreQueryBuilder.FilterFunctionBuilder[]{
                        new FunctionScoreQueryBuilder.FilterFunctionBuilder(QueryBuilders.termQuery("isAD", "true"),
                                ScoreFunctionBuilders.weightFactorFunction(20f))
                });
        query.boostMode(CombineFunction.SUM);
        request.source().query(query);
    }

    private PageResult handleResponse(SearchResponse response) {
        if (response == null) {
            return null;
        }

        SearchHits hits = response.getHits();
        if (hits != null) {
            assert hits.getTotalHits() != null;
            long total = hits.getTotalHits().value;
            SearchHit[] hitsArray = hits.getHits();
            ArrayList<HotelDoc> docs = new ArrayList<>();
            if (ArrayUtils.isNotEmpty(hitsArray)) {
                for (SearchHit hit : hitsArray) {
                    String jsonData = hit.getSourceAsString();
                    HotelDoc doc = JSON.parseObject(jsonData, HotelDoc.class);

                    Object[] sortValues = hit.getSortValues();
                    if (ArrayUtils.isNotEmpty(sortValues)) {
                        doc.setDistance(sortValues[0]);
                    }

                    docs.add(doc);
                }
            }
            return new PageResult(total, docs);
        }
        return null;
    }
}
