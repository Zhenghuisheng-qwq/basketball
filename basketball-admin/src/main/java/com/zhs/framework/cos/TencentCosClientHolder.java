package com.zhs.framework.cos;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.http.HttpProtocol;
import com.qcloud.cos.region.Region;
import com.zhs.common.exception.ServiceException;
import com.zhs.framework.config.properties.TencentCosProperties;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 懒加载 COS 客户端；密钥未配置时不创建，避免启动失败。
 */
@Component
public class TencentCosClientHolder {

    @Autowired
    private TencentCosProperties cosProperties;

    private volatile COSClient cosClient;

    public COSClient getRequiredClient() {
        if (!cosProperties.isConfigured()) {
            throw new ServiceException("腾讯云 COS 未配置密钥，请先填写 tencent.cos.secret-id / secret-key");
        }
        COSClient client = cosClient;
        if (client == null) {
            synchronized (this) {
                client = cosClient;
                if (client == null) {
                    COSCredentials credentials = new BasicCOSCredentials(
                            cosProperties.getSecretId().trim(),
                            cosProperties.getSecretKey().trim());
                    ClientConfig clientConfig = new ClientConfig(new Region(cosProperties.getRegion().trim()));
                    clientConfig.setHttpProtocol(cosProperties.isHttps() ? HttpProtocol.https : HttpProtocol.http);
                    cosClient = client = new COSClient(credentials, clientConfig);
                }
            }
        }
        return client;
    }

    @PreDestroy
    public void destroy() {
        COSClient client = cosClient;
        if (client != null) {
            client.shutdown();
            cosClient = null;
        }
    }
}
