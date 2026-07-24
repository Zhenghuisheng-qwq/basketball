package com.zhs.service.storage;

import com.zhs.model.BasketFileAsset;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * 账户文件物理存储
 */
public interface IAccountFileStorageService {

    /**
     * 写入文件并返回存储元数据
     */
    StoredFileInfo store(MultipartFile file, boolean publicAccess) throws IOException;

    /**
     * 读取文件字节
     */
    byte[] read(BasketFileAsset asset) throws IOException;

    /**
     * 删除物理文件
     */
    void delete(BasketFileAsset asset) throws IOException;
}
