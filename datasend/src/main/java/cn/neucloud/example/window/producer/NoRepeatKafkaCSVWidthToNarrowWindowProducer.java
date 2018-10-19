package cn.neucloud.example.window.producer;

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
public class NoRepeatKafkaCSVWidthToNarrowWindowProducer implements CommandLineRunner {

    private static String topic = "window_remove_repeat";
    private static long time = 3000;
    static int v = 1 ;

    public void sendCsvData(){

        while (true){
            try {
                Thread.sleep(50);
                String value = System.currentTimeMillis() + ","+ v + "";
                KafkaProducerUtil.sendToKafka(topic, value);
                v ++;
                log.info("send {} to topic :{}" ,value,topic);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

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
}
