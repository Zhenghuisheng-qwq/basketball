package com.zhs.framework.config;

import java.util.TimeZone;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

/**
 * 程序注解配置
 *
 * @author ruoyi
 */
@Configuration
// 表示通过aop框架暴露该代理对象,AopContext能够访问
@EnableAspectJAutoProxy(exposeProxy = true)
// 指定要扫描的Mapper类的包的路径
@MapperScan("com.zhs.**.mapper")
public class ApplicationConfig
{
    /**
     * 时区配置
     */
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jacksonObjectMapperCustomization()
    {
        LongStringDeserializer longDeserializer = new LongStringDeserializer();
        return jacksonObjectMapperBuilder -> jacksonObjectMapperBuilder
                .timeZone(TimeZone.getDefault())
                .simpleDateFormat("yyyy-MM-dd HH:mm:ss")
                // 雪花 ID 超出 JS Number 安全整数范围，统一序列化为字符串
                .serializerByType(Long.class, ToStringSerializer.instance)
                .serializerByType(Long.TYPE, ToStringSerializer.instance)
                .deserializerByType(Long.class, longDeserializer)
                .deserializerByType(Long.TYPE, longDeserializer);
    }
}
