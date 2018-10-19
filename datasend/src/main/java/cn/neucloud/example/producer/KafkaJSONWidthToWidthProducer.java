package cn.neucloud.example.producer;

import cn.neucloud.example.utils.KafkaProducerUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Timer;
import java.util.TimerTask;

/**
 * @Author: fei2
 * @Date:2018/6/12 17:23
 * @Description:
 * @Refer To:
 */
@Slf4j
@Component
public class KafkaJSONWidthToWidthProducer implements CommandLineRunner{
    private static String topic = "json_width_to_width";
    private static long time = 3000;
    int v = 1 ;
    public void sendCsvData(){
        long date = System.currentTimeMillis();
        String value = "{\"t1\": "+date+",\"col1\": "+getRandomValue()+",\"col2\": "+getRandomValue()+",\"col3\": "+getRandomValue()+"}";
        KafkaProducerUtil.sendToKafka(topic,value);
        log.info("send {} to topic :{}" ,value,topic);

    }
    private int getRandomValue(){
        return (int)(Math.random() * 1000);
    }

    @Override
    public void run(String... strings){
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                sendCsvData();
            }
        }, 0, time);
    }
}
