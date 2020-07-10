package com.next.service;

import com.google.common.collect.Lists;
import com.next.dao.TrainTravellerMapper;
import com.next.dao.TrainUserTravellerMapper;
import com.next.model.TrainTraveller;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
public class TrainTravellerService {

    @Resource
    private TrainTravellerMapper trainTravellerMapper;
    @Resource
    private TrainUserTravellerMapper trainUserTravellerMapper;

    public List<TrainTraveller> getByUserId(long userId) {
        List<Long> travellerIdList = trainUserTravellerMapper.getByUserId(userId);
        if (CollectionUtils.isEmpty(travellerIdList)) {
            return Lists.newArrayList();
        }
        return trainTravellerMapper.getByIdList(travellerIdList);
    }
}
