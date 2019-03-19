package cn.neucloud.example.consumer;

import cn.neucloud.example.utils.KafkaConsumerUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import javax.security.auth.kerberos.KerberosKey;
import java.util.Collections;

/**
 * @Author: fei2
 * @Date:2018/6/12 18:02
 * @Description:
 * @Refer To:
 */
@Slf4j
@Component
public class KafkaCsvConsumer implements CommandLineRunner{
    
    private KafkaConsumer<Integer,String> consumer;
//    private static String topic = "topic_zYy5r8J0";
    private static String topic = "csv_width_to_width";
    /**
     * 项目启动自动执行方法
     * @param strings
     * @throws Exception
     */
    @Override
    public void run(String... strings) throws Exception {

        new Thread(){
            @Override
            public void run() {
                colletData();
            }
        }.start();

    }

    public static void main(String[] args) {
        KafkaCsvConsumer kafkaCsvConsumer = new KafkaCsvConsumer();
        kafkaCsvConsumer.colletData();
    }

    public void colletData(){
        consumer = KafkaConsumerUtil.getConsumer();
        consumer.subscribe(Collections.singletonList(topic));
        while (true) {
            ConsumerRecords<Integer, String> records = consumer.poll(10);
            for (ConsumerRecord<Integer, String> record : records) {
                System.out.println("value:" + record.value());
//                log.info("Received message: (" + record.key() + " : " + record.value() + ") at offset " + record.offset());
            }
        }
    }

}
