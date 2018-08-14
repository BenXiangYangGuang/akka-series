package cn.neucloud.example.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;

import java.util.Properties;

/**
 * @Author: fei2
 * @Date:2018/6/12 17:23
 * @Description:
 * @Refer To:
 */
@Slf4j
public class KafkaConsumerUtil {
    
    private static Properties props;
    private static KafkaConsumer<Integer,String> consumer;
    private static String kaFkaBroker = "mf0gybu.neuseer.com:6667";
    private static String consumerGroup = "group_zYy5r8J0";
    static {
        props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kaFkaBroker);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, consumerGroup);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        props.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, "1000");
        props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, "30000");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
    
        props.put("auto.commit.interval.ms", "1000");
        String runTimePath = System.getProperty("user.dir");
        log.info("程序运行时的路径 = {}", runTimePath);
        props.put("security.protocol", "SASL_PLAINTEXT");
        System.setProperty("java.security.auth.login.config", runTimePath+"/conf/kafka_client_jaas.conf");
        System.setProperty("java.security.krb5.conf", runTimePath+"/conf/krb5.conf");
        consumer = new KafkaConsumer<>(props);
      
    }
    
    
    public static KafkaConsumer<Integer,String> getConsumer() {
        return consumer;
    }
    
}
