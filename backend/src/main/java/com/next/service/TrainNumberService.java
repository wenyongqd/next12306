package com.next.service;

import com.next.common.TrainType;
import com.next.dao.TrainNumberMapper;
import com.next.exception.BusinessException;
import com.next.model.TrainNumber;
import com.next.param.TrainNumberParam;
import com.next.util.BeanValidator;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
public class TrainNumberService {

    @Resource
    private TrainNumberMapper trainNumberMapper;

    public List<TrainNumber> getAll() {
        return trainNumberMapper.getAll();
    }

    public void save(TrainNumberParam param) {
        BeanValidator.check(param);
        TrainNumber origin = trainNumberMapper.findByName(param.getName());
        if (origin != null) {
            throw new BusinessException("该车次已存在");
        }
        TrainNumber trainNumber = TrainNumber.builder()
                .name(param.getName())
                .trainType(param.getTrainType())
                .type(param.getType().shortValue())
                .seatNum(TrainType.valueOf(param.getTrainType()).getCount())
                .build();
        trainNumberMapper.insertSelective(trainNumber);
    }

    public void update(TrainNumberParam param) {
        BeanValidator.check(param);
        TrainNumber origin = trainNumberMapper.findByName(param.getName());
        if (origin != null && origin.getId().intValue() != param.getId().intValue()) {
            throw new BusinessException("该车次已存在");
        }
        // 可以考虑根据seat判断是否有分配过，不推荐去修改

        TrainNumber trainNumber = TrainNumber.builder()
                .id(param.getId())
                .name(param.getName())
                .trainType(param.getTrainType())
                .type(param.getType().shortValue())
                .seatNum(TrainType.valueOf(param.getTrainType()).getCount())
                .build();
        trainNumberMapper.updateByPrimaryKeySelective(trainNumber);
    }
}
