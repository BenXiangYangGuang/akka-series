package cn.neucloud.example.window.producer;

import cn.neucloud.example.utils.KafkaProducerUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
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
public class KafkaCSVWidthToNarrowWindowProducer implements CommandLineRunner {

    private static String topic = "csv_window_width_to_narrow";
    private static long time = 3000;
    int v = 1 ;
    public void sendCsvData(){
        for (int i = 1; i < 4; i++){
            String value = System.currentTimeMillis() + "," + getRandomValue() + "," + getRandomValue() + "," + getRandomValue();
            KafkaProducerUtil.sendToKafka(topic,value);
            log.info("send {} to topic :{}" ,value,topic);
        }
    }

    /*public void sendCsvData(){
        for (int i = 1; i < 4; i++){
            String value = System.currentTimeMillis() + "," + getRandomValue() + "," + getRandomValue() + "," + getRandomValue();
            KafkaProducerUtil.sendToKafka(topic,value);
            log.info("send {} to topic :{}" ,value,topic);
        }

        while (true){
            try {
                Thread.sleep(1000);
                String value = v + "";
                KafkaProducerUtil.sendToKafka(topic, value);
                v ++;
                log.info("send {} to topic :{}" ,value,topic);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }*/

    /**
     * 项目启动自动 执行的方法
     *
     * @param strings
     * @throws Exception
     */
    @Override
    public void run(String... strings) throws Exception {

        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                sendCsvData();
            }
        }, 0, time);

    }
    private static int getRandomValue(){
        return (int)(Math.random() * 1000);
    }
    
}
