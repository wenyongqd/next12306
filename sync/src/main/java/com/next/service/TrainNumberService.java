package com.next.service;

import com.alibaba.otter.canal.protocol.CanalEntry;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.next.common.TrainEsConstant;
import com.next.dao.TrainNumberDetailMapper;
import com.next.dao.TrainNumberMapper;
import com.next.model.TrainNumber;
import com.next.model.TrainNumberDetail;
import com.next.util.JsonMapper;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.get.MultiGetItemResponse;
import org.elasticsearch.action.get.MultiGetRequest;
import org.elasticsearch.action.get.MultiGetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.update.UpdateRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@Slf4j
public class TrainNumberService {

    @Resource
    private TrainNumberMapper trainNumberMapper;
    @Resource
    private TrainNumberDetailMapper trainNumberDetailMapper;
    @Resource
    private TrainCacheService trainCacheService;
    @Resource
    private EsClient esClient;

    public void handle(List<CanalEntry.Column> columns, CanalEntry.EventType eventType) throws Exception {
        if (eventType != CanalEntry.EventType.UPDATE) {
            log.info("not update, no need care");
            return;
        }
        int trainNumberId = 0;
        for (CanalEntry.Column column : columns) {
            if (column.getName().equals("id")) {
                trainNumberId = Integer.parseInt(column.getValue());
                break;
            }
        }
        TrainNumber trainNumber = trainNumberMapper.selectByPrimaryKey(trainNumberId);
        if (trainNumber == null) {
            log.error("not found trainNumber, trainNumberId:{}", trainNumberId);
            return;
        }
        List<TrainNumberDetail> detailList = trainNumberDetailMapper.getByTrainNumberId(trainNumberId);
        if (CollectionUtils.isEmpty(detailList)) {
            log.warn("no detail, no need care, trainNumber:{}", trainNumber.getName());
            return;
        }
        trainCacheService.set(
                "TN_" + trainNumber.getName(),
                JsonMapper.obj2String(detailList)
        );
        log.info("trainNumber:{} detailList update redis", trainNumber.getName());

        saveES(detailList, trainNumber);
        log.info("trainNumber:{} detailList update es", trainNumber.getName());
    }

    private void saveES(List<TrainNumberDetail> detailList, TrainNumber trainNumber) throws Exception {
        /**
         * A->B fromStationId->toStationId
         *
         * trainNumber: A->B->C  D386:北京->锦州->大连  D387:北京->鞍山->大连
         * 北京-大连？
         * D386: 北京-锦州、锦州-大连、北京-大连
         * D387: 北京-鞍山、鞍山-大连、北京-大连
         *
         * fromStationId->toStationId : trainNumberId1,trainNumberId2，...
         */
        List<String> list = Lists.newArrayListWithExpectedSize(detailList.size() * detailList.size());
        if (detailList.size() == 1) {
            int fromStationId = trainNumber.getFromStationId();
            int toStationId = trainNumber.getToStationId();
            list.add(fromStationId + "_" + toStationId);
        } else { // 多段, 保证detailList是有序的
            for (int i = 0; i < detailList.size(); i++) {
                int tmpFromStationId = detailList.get(i).getFromStationId();
                for(int j = i; j < detailList.size(); j++) {
                    int tmpToStationId = detailList.get(j).getToStationId();
                    list.add(tmpFromStationId + "_" + tmpToStationId);
                }
            }
        }

        // 组装批量请求，获取es已经存储的数据
        MultiGetRequest multiGetRequest = new MultiGetRequest();
        for (String item : list) {
            multiGetRequest.add(new MultiGetRequest.Item(TrainEsConstant.INDEX, TrainEsConstant.TYPE, item));
        }
        MultiGetResponse multiGetResponse = esClient.multiGet(multiGetRequest);

        BulkRequest bulkRequest = new BulkRequest();

        // 处理处理的每一项
        for (MultiGetItemResponse itemResponse : multiGetResponse.getResponses()) {
            if (itemResponse.isFailed()) {
                log.error("multiGet item failed, itemResponse:{}", itemResponse);
                continue;
            }
            GetResponse getResponse = itemResponse.getResponse();
            if (getResponse == null) {
                log.error("multiGet item getResponse is null, itemResponse:{}", itemResponse);
                continue;
            }

            // 存储更新es的数据
            Map<String, Object> dataMap = Maps.newHashMap();

            Map<String, Object> map = getResponse.getSourceAsMap();
            if (!getResponse.isExists() || map == null) {
                // add index
                dataMap.put(TrainEsConstant.COLUMN_TRAIN_NUMBER, trainNumber.getName());
                IndexRequest indexRequest = new IndexRequest(TrainEsConstant.INDEX, TrainEsConstant.TYPE, getResponse.getId()).source(dataMap);
                bulkRequest.add(indexRequest);
                continue;
            }

            String origin = (String) map.get(TrainEsConstant.COLUMN_TRAIN_NUMBER);
            Set<String> set = Sets.newHashSet(Splitter.on(",").trimResults().omitEmptyStrings().split(origin));
            if(!set.contains(trainNumber.getName())) {
                // update index
                dataMap.put(TrainEsConstant.COLUMN_TRAIN_NUMBER, origin + "," + trainNumber.getName());
                UpdateRequest updateRequest = new UpdateRequest(TrainEsConstant.INDEX, TrainEsConstant.TYPE, getResponse.getId()).doc(dataMap);
                bulkRequest.add(updateRequest);
            }
        }

        // 批量更新es里的数据
        BulkResponse bulkResponse = esClient.bulk(bulkRequest);
        log.info("es bulk, response:{}", JsonMapper.obj2String(bulkResponse));
        if (bulkResponse.hasFailures()) {
            throw new RuntimeException("es bulk failure");
        }
    }
}
