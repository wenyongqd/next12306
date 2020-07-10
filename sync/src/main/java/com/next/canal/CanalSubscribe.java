package com.next.canal;

import com.alibaba.otter.canal.client.CanalConnector;
import com.alibaba.otter.canal.client.CanalConnectors;
import com.alibaba.otter.canal.common.utils.AddressUtils;
import com.alibaba.otter.canal.protocol.CanalEntry;
import com.alibaba.otter.canal.protocol.Message;
import com.next.service.TrainNumberService;
import com.next.service.TrainSeatService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.net.InetSocketAddress;
import java.util.List;

@Service
@Slf4j
public class CanalSubscribe implements ApplicationListener{

    @Resource
    private  TrainSeatService trainSeatService;
    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        canalSunscrib();
    }

    private void canalSunscrib() {
        // 创建链接
        CanalConnector connector = CanalConnectors.newSingleConnector(new InetSocketAddress(AddressUtils.getHostIp(),
                11111), "example", "", "");
        int batchSize = 1000;
        new Thread(() -> {
            try {
                log.info("canal subscribe");
                connector.connect();
                connector.subscribe(".*\\..*");
                connector.rollback();
                int totalEmptyCount = 120;
                while (true) {
                    Message message = connector.getWithoutAck(batchSize); // 获取指定数量的数据
                    long batchId = message.getId();
                    int size = message.getEntries().size();
                    if (batchId == -1 || size == 0) {
                        safeSleep(100);
                        continue;
                    }
                    try {
                        log.info("new message, batchId:{}, size:{}", batchId, size);
                        printEntry(message.getEntries());
                        connector.ack(batchId); // 提交确认
                    } catch (Exception e1) {
                        log.error("canal data handle exception, batchId:{}", batchId, e1);
                        connector.rollback(batchId); // 处理失败, 回滚数据
                    }
                }

            } catch (Exception e3){
                log.error("cannal subscribe exception",e3);
                safeSleep(1000);
                canalSunscrib();
            }
        }).start();

    }

    private void printEntry(List<CanalEntry.Entry> entrys) {
        for (CanalEntry.Entry entry : entrys) {
            if (entry.getEntryType() == CanalEntry.EntryType.TRANSACTIONBEGIN || entry.getEntryType() == CanalEntry.EntryType.TRANSACTIONEND) {
                continue;
            }

            CanalEntry.RowChange rowChage = null;
            try {
                rowChage = CanalEntry.RowChange.parseFrom(entry.getStoreValue());
            } catch (Exception e) {
                throw new RuntimeException("RowChange.parse exception , data:" + entry.toString(),e);
            }

            CanalEntry.EventType eventType = rowChage.getEventType();
            String schemaName = entry.getHeader().getSchemaName();
            String tableName = entry.getHeader().getTableName();
            log.info("name:[{},{}],eventType:{}", schemaName, tableName,eventType);
            for (CanalEntry.RowData rowData : rowChage.getRowDatasList()) {
                if (eventType == CanalEntry.EventType.DELETE) {
                    handleColumn(rowData.getBeforeColumnsList(), eventType, schemaName, tableName);
                } else {
                    handleColumn(rowData.getAfterColumnsList(), eventType, schemaName, tableName);
                }
            }
        }
    }

    private void handleColumn(List<CanalEntry.Column> columns,CanalEntry.EventType eventType, String schemaName, String tableName) {
        if (schemaName.contains("train_seat")) {
           trainSeatService.handle(columns, eventType);
        } else if (tableName.equals("train_number_detail")) {

        } else {
            log.info("drop data, no need care");
        }
    }
    private void safeSleep(int mills) {
        try {
            Thread.sleep(mills);
        } catch (Exception e2) {

        }
    }

}
