package cn.neucloud.example.consumer;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.Serdes;

import java.util.Collections;
import java.util.Properties;

/**
 * Author: fei2
 * Date:  19-1-10 下午3:58
 * Description:
 * Refer To:
 */
@Slf4j
public class ConsumbData {
    private  final String topicName = "csv_width_to_width_target";
    private KafkaConsumer<String, String> consumer = new KafkaConsumer<>(loadConfig());
    private static String broker = "mf0gybu.neuseer.com:6667";
    public static void main(String[] args) {
        new ConsumbData().singleThreadConsumer();
    }
    /**
     *  加载Producer相关配置
     */
    private Properties loadConfig(){
        Properties props = new Properties();
        props.put("bootstrap.servers", broker);
        props.put("group.id", "demo");
        props.put("auto.offset.reset", "earliest");
        props.put("enable.auto.commit", "true");
        props.put("auto.commit.interval.ms", "1000");
        props.put("key.deserializer", Serdes.String().deserializer().getClass().getName());
        props.put("value.deserializer", Serdes.String().deserializer().getClass().getName());
        String runTimePath = System.getProperty("user.dir");
        props.put("security.protocol", "SASL_PLAINTEXT");
        System.setProperty("java.security.auth.login.config", runTimePath + "/conf/kafka_client_jaas.conf");
        System.setProperty("java.security.krb5.conf", runTimePath + "/conf/krb5.conf");
        return props;
    }
    /**
     * 单线程消费者
     */
    public void singleThreadConsumer(){
        consumer.subscribe(Collections.singletonList(topicName));
        while (true) {
            ConsumerRecords<String, String> records = consumer.poll(10);
            for (ConsumerRecord<String, String> record : records){
                System.out.println("topic:" + topicName + " -- offset: "+ record.offset()+ " --  value:" + record.value());
//                log.info("offset = " + record.offset() + ", key = " + record.key() + ", value = " + record.value());
            }
        }
    }
}
