package com.next.dto;

import com.next.model.TrainOrder;
import com.next.model.TrainOrderDetail;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Getter
@Setter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrainOrderDto {

    private TrainOrder trainOrder;

    private List<TrainOrderDetail> trainOrderDetailList;
}
