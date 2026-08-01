package com.zhs.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "member.init")
public class MemberInitProperties {

    /** 是否启用批量初始化接口 */
    private boolean enabled = true;

    /** 每次调用创建的账号数量 */
    private int batchSize = 29;

    /** 统一登录密码 */
    private String password = "111111aa";

    /** 邮箱域名 */
    private String emailDomain = "qq.com";

    /** 首个邮箱数字前缀（仅在数据文件不存在时使用） */
    private long emailStart = 111111L;

    /** 测试账号数据文件（相对路径基于工作目录向上查找） */
    private String dataFilePath = "测试/测试数据.txt";

    /** 球员认证截图（classpath 资源） */
    private String certScreenshotResource = "classpath:player/5.png";
}
