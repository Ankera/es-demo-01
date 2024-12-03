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
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.functionscore.FunctionScoreQueryBuilder;
import org.elasticsearch.index.query.functionscore.ScoreFunctionBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.elasticsearch.search.suggest.Suggest;
import org.elasticsearch.search.suggest.SuggestBuilder;
import org.elasticsearch.search.suggest.SuggestBuilders;
import org.elasticsearch.search.suggest.SuggestionBuilder;
import org.elasticsearch.search.suggest.completion.CompletionSuggestion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.*;

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

    @Override
    public Map<String, List<String>> getFilters(RequestParams params) {
        SearchRequest request = new SearchRequest("hotel");
        request.source().size(0);
        HashMap<String, List<String>> map = new HashMap<>();
        try {
            this.buildBasicRequest(request, params);

            this.buildAggs(request);

            SearchResponse response = restHighLevelClient.search(request, RequestOptions.DEFAULT);

            List<String> city = this.getBuckNames(response, "cityAggs");
            List<String> brand = this.getBuckNames(response, "brandAggs");
            List<String> starName = this.getBuckNames(response, "starNameAggs");



            map.put("city", city);
            map.put("brand", brand);
            map.put("starName", starName);

            return map;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void buildAggs(SearchRequest request) {
        request.source().aggregation(AggregationBuilders.terms("cityAggs").field("city").size(20));
        request.source().aggregation(AggregationBuilders.terms("brandAggs").field("brand").size(20));
        request.source().aggregation(AggregationBuilders.terms("starNameAggs").field("starName").size(20));
    }

    public List<String> getBuckNames(SearchResponse response, String key) {
        List<String> infos = new ArrayList<>();
        Aggregations aggregations = response.getAggregations();
        Terms brandAggs = aggregations.get(key);
        if (brandAggs == null || CollectionUtils.isEmpty(brandAggs.getBuckets())) {
            return infos;
        }
        List<? extends Terms.Bucket> buckets = brandAggs.getBuckets();

        for (Terms.Bucket bucket : buckets) {
            infos.add(bucket.getKeyAsString());
        }
        return infos;
    }

    @Override
    public List<String> getSuggestion(String key) {
        SearchRequest request = new SearchRequest("hotel");
        request.source().suggest(new SuggestBuilder().addSuggestion(
                "hotelSuggestion",
                SuggestBuilders.completionSuggestion("suggestion")
                        .prefix(key)
                        .skipDuplicates(true)
                        .size(15)
        ));

        try {
            SearchResponse response = restHighLevelClient.search(request, RequestOptions.DEFAULT);
            Suggest suggest = response.getSuggest();
            CompletionSuggestion hotelSuggestion = suggest.getSuggestion("hotelSuggestion");
            if (hotelSuggestion == null && CollectionUtils.isEmpty(hotelSuggestion.getOptions())) {
                return new ArrayList<>();
            }
            List<String> suggestions = new ArrayList<>();
            List<CompletionSuggestion.Entry.Option> options = hotelSuggestion.getOptions();
            for (CompletionSuggestion.Entry.Option option : options) {
                String string = option.getText().string();
                suggestions.add(string);
            }
            return suggestions;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
