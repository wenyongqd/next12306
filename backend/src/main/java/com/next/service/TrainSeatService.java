package com.next.service;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Table;
import com.next.beans.PageQuery;
import com.next.common.TrainSeatLevel;
import com.next.common.TrainType;
import com.next.common.TrainTypeSeatConstant;
import com.next.dao.TrainNumberDetailMapper;
import com.next.dao.TrainNumberMapper;
import com.next.exception.BusinessException;
import com.next.model.TrainNumber;
import com.next.model.TrainNumberDetail;
import com.next.model.TrainSeat;
import com.next.param.GenerateTicketParam;
import com.next.param.PublishTicketParam;
import com.next.param.TrainSeatSearchParam;
import com.next.seatDao.TrainSeatMapper;
import com.next.util.BeanValidator;
import com.next.util.StringUtil;
import javafx.util.Pair;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Service
public class TrainSeatService {

    @Resource
    private TrainNumberMapper trainNumberMapper;
    @Resource
    private TrainNumberDetailMapper trainNumberDetailMapper;
    @Resource
    private TrainSeatMapper trainSeatMapper;
    @Resource
    private TransactionService transactionService;

    public List<TrainSeat> searchList(TrainSeatSearchParam param, PageQuery pageQuery) {
        BeanValidator.check(param);
        BeanValidator.check(pageQuery);
        TrainNumber trainNumber = trainNumberMapper.findByName(param.getTrainNumber());
        if (trainNumber == null) {
            throw new BusinessException("待查询的车次不存在");
        }
        return trainSeatMapper.searchList(trainNumber.getId(), param.getTicket(),param.getStatus(),
                param.getCarriageNum(), param.getRowNum(), param.getSeatNum(),
                pageQuery.getOffset(), pageQuery.getPageSize());
    }

    public int countList(TrainSeatSearchParam param) {
        BeanValidator.check(param);
        TrainNumber trainNumber = trainNumberMapper.findByName(param.getTrainNumber());
        if (trainNumber == null) {
            throw new BusinessException("待查询的车次不存在");
        }
        return trainSeatMapper.countList(trainNumber.getId(), param.getTicket(),param.getStatus(),
                param.getCarriageNum(), param.getRowNum(), param.getSeatNum());
    }

    public void generate(GenerateTicketParam param) {
        BeanValidator.check(param);
        // 车次
        TrainNumber trainNumber = trainNumberMapper.selectByPrimaryKey(param.getTrainNumberId());
        if(trainNumber == null) {
            throw new BusinessException("该车次不存在");
        }

        // 车次详情
        List<TrainNumberDetail> detailList = trainNumberDetailMapper.getByTrainNumberId(param.getTrainNumberId());
        if (CollectionUtils.isEmpty(detailList)) {
            throw new BusinessException("该车次无详情，需要先添加详情");
        }
        //Collections.sort(detailList, Comparator.comparingInt(TrainNumberDetail::getStationIndex));

        // 座位类型
        TrainType trainType = TrainType.valueOf(trainNumber.getTrainType());

        // 座位配置
        Table<Integer, Integer, Pair<Integer, Integer>> seatTable = TrainTypeSeatConstant.getTable(trainType);

        // 时间
        ZoneId zoneId = ZoneId.systemDefault();
        LocalDateTime fromLocalDateTime = LocalDateTime.parse(param.getFromTime(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));

        List<TrainSeat> list = Lists.newArrayList();
        String ticket = fromLocalDateTime.format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        for(TrainNumberDetail trainNumberDetail : detailList) { // 遍历车次的每一段
            // 每一段的发车时间
            Date fromDate = Date.from(fromLocalDateTime.atZone(zoneId).toInstant());
            // 每一段的到站时间
            Date toDate = Date.from(fromLocalDateTime.plusMinutes(trainNumberDetail.getRelativeMinute()).atZone(zoneId).toInstant());

            Map<Integer, Integer> seatMoneyMap = splitSeatMoney(trainNumberDetail.getMoney());

            for(Table.Cell<Integer, Integer, Pair<Integer, Integer>> cell : seatTable.cellSet()) { // 遍历 每一节车厢 每一排
                Integer carriage = cell.getRowKey(); //当前车厢，获取座位数及座位等级的
                Integer row = cell.getColumnKey(); // 当前车厢的排，锁定座位数
                TrainSeatLevel seatLevel = TrainTypeSeatConstant.getSeatLevel(trainType, carriage); // 座位的等级
                Integer money = seatMoneyMap.get(seatLevel.getLevel()); // 座位的价钱
                Pair<Integer, Integer> rowSeatRange = seatTable.get(carriage, row); // 获取指定车厢指定排的座位号的范围

                for(int index = rowSeatRange.getKey(); index <= rowSeatRange.getValue(); index++) { // 遍历每一排的每个座位号
                    // 生成座位
                    TrainSeat trainSeat = TrainSeat.builder()
                            .carriageNumber(carriage)
                            .rowNumber(row)
                            .seatNumber(index)
                            .money(money)
                            .ticket(ticket)
                            .seatLevel(seatLevel.getLevel())
                            .trainStart(fromDate)
                            .trainEnd(toDate)
                            .trainNumberId(trainNumber.getId())
                            .showNumber(carriage + "车" + row + "排" + index)
                            .status(0)
                            .fromStationId(trainNumberDetail.getFromStationId())
                            .toStationId(trainNumberDetail.getToStationId())
                            .build();
                    list.add(trainSeat);
                }
            }
            fromLocalDateTime = fromLocalDateTime.plusMinutes(trainNumberDetail.getRelativeMinute() + trainNumberDetail.getWaitMinute());
        }

        transactionService.batchInsertSeat(list);
    }

    // 这种写法是错误的，请一定重视
    @Transactional(rollbackFor = Exception.class)
    public void batchInsertSeat(List<TrainSeat> list) {
        List<List<TrainSeat>> trainTicketPartitionList = Lists.partition(list, 1000);
        trainTicketPartitionList.parallelStream().forEach(partitionList -> {
            // 批量插入
            trainSeatMapper.batchInsert(partitionList);
        });
    }

    private Map<Integer, Integer> splitSeatMoney(String money) {
        // money:   0:100,1:80,2:60
        try {
            List<String> list = Splitter.on(",").trimResults().omitEmptyStrings().splitToList(money);
            Map<Integer, Integer> map = Maps.newHashMap();
            list.stream().forEach(str -> {
                String[] array = str.split(":");
                map.put(Integer.parseInt(array[0]), Integer.parseInt(array[1]));
            });
            return map;
        } catch (Exception e) {
            throw new BusinessException("价钱解析出错，请检查车次详情的配置");
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void publish(PublishTicketParam param) {
        BeanValidator.check(param);
        TrainNumber trainNumber = trainNumberMapper.findByName(param.getTrainNumber());
        if (trainNumber == null) {
            throw new BusinessException("车次不存在");
        }
        List<Long> trainSeatIdList = StringUtil.splitToListLong(param.getTrainSeatIds());
        List<List<Long>> idPartitionList = Lists.partition(trainSeatIdList, 1000);
        for(List<Long> partitionList : idPartitionList) {
            int count = trainSeatMapper.batchPublish(trainNumber.getId(), partitionList);
            if (count != partitionList.size()) {
                throw new BusinessException("部分座位不满足条件，请重新查询[初始]状态的座位进行放票");
            }
        }
    }
}
