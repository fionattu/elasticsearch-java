import org.apache.http.HttpHost;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.DocWriteResponse;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.support.replication.ReplicationResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
// todo: write docker run to shell command
// run es docker:
// docker run -p 9200:9200 -p 9300:9300 -e "discovery.type=single-node" docker.elastic.co/elasticsearch/elasticsearch:7.4.2
// curl -X GET "localhost:9200/test/_doc/1?pretty"
// todo: redesign this class, including the listener
// todo: using log
public class ElasticsearchService {
    public RestHighLevelClient client = null;
    public ElasticsearchService() {
        client = new RestHighLevelClient(
                RestClient.builder(
                        new HttpHost("localhost", 9200, "http"),
                        new HttpHost("localhost", 9201, "http"))
        );
    }
    public void createIndex() throws IOException {
        CreateIndexRequest request = new CreateIndexRequest("test");
        try {
            XContentBuilder builder = XContentFactory.jsonBuilder();
            builder.startObject();
            {
                builder.startObject("properties");
                {
                    builder.startObject("message");
                    builder.endObject();
                }
                builder.endObject();
            }
            builder.endObject();
            request.mapping(String.valueOf(builder));
        } catch (IOException e) {
            e.printStackTrace();
        }
        ActionListener<CreateIndexResponse> listener = new ActionListener<CreateIndexResponse>() {
            @Override
            public void onResponse(CreateIndexResponse createIndexResponse) {
                boolean acknowledged = createIndexResponse.isAcknowledged();
                boolean shardsAcknowledged = createIndexResponse.isShardsAcknowledged();
                    System.out.println(acknowledged);
                    System.out.println(shardsAcknowledged);
                    System.out.println("success");
            }
            @Override
            public void onFailure(Exception e) {
                        System.out.println("failed");
                    }
        };
        client.indices().createAsync(request, RequestOptions.DEFAULT, listener);
        client.close();
    }
    public void insertDocuments() throws IOException {
        Map<String, Object> jsonMap = new HashMap<>();
        jsonMap.put("user", "kimchy");
        jsonMap.put("postDate", new Date());
        jsonMap.put("message", "trying out Elasticsearch");
        IndexRequest indexRequest = new IndexRequest("test")
                .id("1").source(jsonMap);
        ActionListener<IndexResponse> listener = new ActionListener<IndexResponse>() {
            @Override
            public void onResponse(IndexResponse indexResponse) {
                System.out.println("success");
                String index = indexResponse.getIndex();
                String id = indexResponse.getId();
                if (indexResponse.getResult() == DocWriteResponse.Result.CREATED) {
                    System.out.println("create");
                } else if (indexResponse.getResult() == DocWriteResponse.Result.UPDATED) {
                    System.out.println("updated");
                }
                ReplicationResponse.ShardInfo shardInfo = indexResponse.getShardInfo();
                if (shardInfo.getTotal() != shardInfo.getSuccessful()) {
                    System.out.println("shardInfo not completed");
                }
                if (shardInfo.getFailed() > 0) {
                    for (ReplicationResponse.ShardInfo.Failure failure :
                            shardInfo.getFailures()) {
                        String reason = failure.reason();
                        System.out.println("reason: " + reason);
                    }
                }
            }
            @Override
            public void onFailure(Exception e) {
                System.out.println("failed");
            }
        };
        client.indexAsync(indexRequest, RequestOptions.DEFAULT, listener);
//        client.close();
    }
    public static void main(String[] args) throws IOException {
        ElasticsearchService es = new ElasticsearchService();
//        es.createIndex();
        es.insertDocuments();

    }
}



