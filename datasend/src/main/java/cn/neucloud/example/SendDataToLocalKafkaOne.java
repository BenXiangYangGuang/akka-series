package cn.neucloud.example;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.util.Properties;

/**
 * Author: fei2
 * Date:  18-8-10 下午3:44
 * Description:向本地kafka server 发送数据
 * Refer To:
 */
@Slf4j
public class SendDataToLocalKafkaOne {

    private static Properties props;
    private static KafkaProducer<Integer, String> producer ;
    //mf0gybu.neuseer.com:6667
    private static String broker = "localhost:9092";
    static {
        props = new Properties();
        props.put("bootstrap.servers", broker);
        props.put("acks", "all");
        props.put("retries", 0);
        props.put("batch.size", 16384);
        props.put("linger.ms", 1);
        props.put("buffer.memory", 33554432);
        props.put("key.serializer", "org.apache.kafka.common.serialization.IntegerSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");

        String runTimePath = System.getProperty("user.dir");
        log.info("程序运行时的路径 = {}", runTimePath);

        producer = new KafkaProducer<>(props);
    }
    public static void main(String[] args) {
        String topic = "topic1";
        String value = "";
        int v = 1;
        while (true){
            try {
                Thread.sleep(500);
                value = v + "";
                producer.send(new ProducerRecord<>(topic, value));
                v ++;
                log.info("send {} to topic :{}" ,value,topic);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }


    }
}
