package com.next.service;

import com.next.dao.TrainNumberDetailMapper;
import com.next.dao.TrainNumberMapper;
import com.next.exception.BusinessException;
import com.next.model.TrainNumber;
import com.next.model.TrainNumberDetail;
import com.next.param.TrainNumberDetailParam;
import com.next.util.BeanValidator;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
public class TrainNumberDetailService {

    @Resource
    private TrainNumberDetailMapper trainNumberDetailMapper;
    @Resource
    private TrainNumberMapper trainNumberMapper;
    @Resource
    private TrainStationService trainStationService;

    public List<TrainNumberDetail> getAll() {
        return trainNumberDetailMapper.getAll();
    }

    public void save(TrainNumberDetailParam param) {
        BeanValidator.check(param);
        TrainNumber trainNumber = trainNumberMapper.selectByPrimaryKey(param.getTrainNumberId());
        if (trainNumber == null) {
            throw new BusinessException("相关的车次不存在");
        }
        List<TrainNumberDetail> detailList = trainNumberDetailMapper.getByTrainNumberId(param.getTrainNumberId());

        TrainNumberDetail trainNumberDetail = TrainNumberDetail.builder()
                .trainNumberId(param.getTrainNumberId())
                .fromStationId(param.getFromStationId())
                .toStationId(param.getToStationId())
                .stationIndex(detailList.size())
                .relativeMinute(param.getRelativeMinute())
                .waitMinute(param.getWaitMinute())
                .money(param.getMoney())
                .fromCityId(trainStationService.getCityIdByStationId(param.getFromStationId()))
                .toCityId(trainStationService.getCityIdByStationId(param.getToStationId()))
                .build();
        trainNumberDetailMapper.insertSelective(trainNumberDetail);

        if (param.getEnd() == 1) {
            detailList.add(trainNumberDetail);
            trainNumber.setFromStationId(detailList.get(0).getFromStationId());
            trainNumber.setFromCityId(detailList.get(0).getFromCityId());
            trainNumber.setToStationId(detailList.get(detailList.size() - 1).getToStationId());
            trainNumber.setToCityId(detailList.get(detailList.size() - 1).getToCityId());
            trainNumberMapper.updateByPrimaryKeySelective(trainNumber);

            // TODO: 考虑方便前台用户查询两个车站涉及的所有车次
        }
    }

    public void delete(int id) {
        trainNumberDetailMapper.deleteByPrimaryKey(id);
    }
}
