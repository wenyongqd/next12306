package com.next.service;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.next.dao.TrainStationMapper;
import com.next.model.TrainStation;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class TrainStationService {

    private static Cache<Integer, TrainStation> stationCache = CacheBuilder.newBuilder()
            .expireAfterWrite(5, TimeUnit.MINUTES).build();

    @Resource
    private TrainStationMapper trainStationMapper;

    public List<TrainStation> getAll() {
        return trainStationMapper.getAll();
    }

    public String getStationNameById(int stationId) {
        TrainStation trainStation = stationCache.getIfPresent(stationId);
        if (trainStation != null) {
            return trainStation.getName();
        }
        trainStation = trainStationMapper.selectByPrimaryKey(stationId);
        if (trainStation != null) {
            stationCache.put(stationId, trainStation);
            return trainStation.getName();
        }
        return "";
    }
}
