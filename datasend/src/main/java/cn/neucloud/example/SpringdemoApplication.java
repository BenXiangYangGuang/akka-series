package cn.neucloud.example;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 主程序启动后，kafka在neuseer上; 触发自动发送数据和接受数据
 */
@SpringBootApplication
public class SpringdemoApplication {
	public static void main(String[] args) {
		SpringApplication.run(SpringdemoApplication.class, args);
	}
}