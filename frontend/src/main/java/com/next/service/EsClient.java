package com.next.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHost;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.message.BasicHeader;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.get.MultiGetRequest;
import org.elasticsearch.action.get.MultiGetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EsClient implements ApplicationListener<ContextRefreshedEvent> {

    private final static int CONNECT_TIMEOUT = 100;
    private final static int SOCKET_TIMEOUT = 60 * 1000;
    private final static int REQUEST_TIMEOUT = SOCKET_TIMEOUT;
    private RestHighLevelClient restHighLevelClient;  //JDK8
    private BasicHeader[] basicHeaders;

    @Override
    public void onApplicationEvent(ContextRefreshedEvent contextRefreshedEvent) {
        try {
            initClient();
        } catch (Exception e) {
            log.error("es client init exception", e);
            try {
                Thread.sleep(1000);
            } catch (Exception e1) {

            }
            initClient();
        }
    }

    private void initClient() {
        log.info("es client init start");
        basicHeaders = new BasicHeader[]{
                new BasicHeader("Accept", "application/json; charset=UTF-8")
        };
        RestClientBuilder builder = RestClient.builder(new HttpHost("127.0.0.1", 9200, "http"));
        builder.setDefaultHeaders(basicHeaders)
                .setRequestConfigCallback((RequestConfig.Builder configBuilder) -> {
                    configBuilder.setConnectTimeout(CONNECT_TIMEOUT);
                    configBuilder.setSocketTimeout(SOCKET_TIMEOUT);
                    configBuilder.setConnectionRequestTimeout(REQUEST_TIMEOUT);
                    return configBuilder;
                });
        restHighLevelClient = new RestHighLevelClient(builder);
        log.info("es client init end");
    }

    public IndexResponse index(IndexRequest indexRequest) throws Exception {
        try {
            return restHighLevelClient.index(indexRequest, basicHeaders);
        } catch (Exception e) {
            log.error("es.index exception, indexRequest:{}", indexRequest, e);
            throw e;
        }
    }

    public UpdateResponse update(UpdateRequest updateRequest) throws Exception {
        try {
            return restHighLevelClient.update(updateRequest, basicHeaders);
        } catch (Exception e) {
            log.error("es.update exception, updateRequest:{}", updateRequest, e);
            throw e;
        }
    }

    public GetResponse get(GetRequest getRequest) throws Exception {
        try {
            return restHighLevelClient.get(getRequest, basicHeaders);
        } catch (Exception e) {
            log.error("es.get exception, getRequest:{}", getRequest, e);
            throw e;
        }
    }

    public MultiGetResponse multiGet(MultiGetRequest multiGetRequest) throws Exception {
        try {
            return restHighLevelClient.multiGet(multiGetRequest, basicHeaders);
        } catch (Exception e) {
            log.error("es.multiGet exception, multiGetRequest:{}", multiGetRequest, e);
            throw e;
        }
    }

    public BulkResponse bulk(BulkRequest bulkRequest) throws Exception {
        try {
            return restHighLevelClient.bulk(bulkRequest, basicHeaders);
        } catch (Exception e) {
            log.error("es.bulk exception, bulkRequest:{}", bulkRequest, e);
            throw e;
        }
    }

}
