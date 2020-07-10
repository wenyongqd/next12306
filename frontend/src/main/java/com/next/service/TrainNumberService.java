package com.next.service;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.next.dao.TrainNumberMapper;
import com.next.model.TrainNumber;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

@Service
public class TrainNumberService {

    private static Cache<String, TrainNumber> trainNumberCache = CacheBuilder.newBuilder()
            .expireAfterWrite(5, TimeUnit.MINUTES).build();

    @Resource
    private TrainNumberMapper trainNumberMapper;

    public TrainNumber findByNameFromCache(String name) {
        TrainNumber trainNumber = trainNumberCache.getIfPresent(name);
        if (trainNumber != null) {
            return trainNumber;
        }
        trainNumber = trainNumberMapper.findByName(name);
        if (trainNumber != null) {
            trainNumberCache.put(name, trainNumber);
        }
        return trainNumber;
    }
}
