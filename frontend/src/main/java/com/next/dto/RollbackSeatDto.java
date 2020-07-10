package com.next.dto;

import com.next.model.TrainSeat;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Getter
@Setter
@ToString
public class RollbackSeatDto {

    private TrainSeat trainSeat;

    private List<Integer> fromStationIdList;
}
