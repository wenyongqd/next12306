package com.next.seatDao;

import com.next.model.TrainSeat;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface TrainSeatMapper {
    int deleteByPrimaryKey(Long id);

    int insert(TrainSeat record);

    int insertSelective(TrainSeat record);

    TrainSeat selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(TrainSeat record);

    int updateByPrimaryKey(TrainSeat record);

    void batchInsert(@Param("list") List<TrainSeat> list);

    List<TrainSeat> searchList(@Param("trainNumberId") int trainNumberId, @Param("ticket") String ticket,
                               @Param("status") Integer status, @Param("carriageNum") Integer carriageNum,
                               @Param("rowNum") Integer rowNum, @Param("seatNum") Integer seatNum,
                               @Param("offset") int offset, @Param("pageSize") int pageSize);

    int countList(@Param("trainNumberId") int trainNumberId, @Param("ticket") String ticket,
                  @Param("status") Integer status, @Param("carriageNum") Integer carriageNum,
                  @Param("rowNum") Integer rowNum, @Param("seatNum") Integer seatNum);

    int batchPublish(@Param("trainNumberId") int trainNumberId, @Param("trainSeatIdList") List<Long> trainSeatIdList);

    List<TrainSeat> getToPlaceSeatList(@Param("trainNumberId") int trainNumberId,@Param("carriageNum") Integer carriageNum,
                                       @Param("rowNum") Integer rowNum, @Param("seatNum") Integer seatNum,
                                       @Param("fromStationIdList") List<Integer> fromStationIdList);

    int batchPlace(@Param("trainNumberId") int trainNumberId, @Param("idList") List<Long> idList,
                   @Param("travellerId") long travellerId, @Param("userId") long userId);

    int batchRollbackPlace(@Param("trainSeat") TrainSeat trainSeat, @Param("fromStationIdList") List<Integer> fromStationIdList);

    void cancelSeat(@Param("trainNumberId") int trainNumberId, @Param("ticket") String ticket,
                    @Param("carriageNum") Integer carriageNum, @Param("rowNum") Integer rowNum,
                    @Param("seatNum") Integer seatNum,@Param("userId") long userId,
                    @Param("travellerId") long travellerId);
}