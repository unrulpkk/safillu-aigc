package cn.iocoder.yudao.module.aigc.config;
import cn.iocoder.yudao.module.aigc.service.GenerationService;
import jakarta.annotation.PreDestroy;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import java.util.concurrent.*;
@Configuration
@EnableScheduling
public class GenerationWorker {
    private final GenerationService service;
    private final ExecutorService pool=Executors.newFixedThreadPool(2);
    private final Semaphore slots=new Semaphore(2);
    public GenerationWorker(GenerationService service){this.service=service;}
    @Scheduled(fixedDelay=1500,initialDelay=10000)
    public void tick(){for(long id:service.due()){if(!slots.tryAcquire())return;pool.submit(()->{try{service.execute(id);}finally{slots.release();}});}}
    @PreDestroy public void stop(){pool.shutdown();}
}
