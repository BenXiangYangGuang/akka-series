package cn.neucloud.example.consumer;

import cn.neucloud.example.utils.KafkaConsumerUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

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
    private static String topic = "Suct_Data_Redirct";
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
    public void colletData(){
        consumer = KafkaConsumerUtil.getConsumer();
        consumer.subscribe(Collections.singletonList(topic));
        while (true) {
            ConsumerRecords<Integer, String> records = consumer.poll(10);
            for (ConsumerRecord<Integer, String> record : records) {
                log.info("Received message: (" + record.key() + " : " + record.value() + ") at offset " + record.offset());
            }
        }
    }
    
}
