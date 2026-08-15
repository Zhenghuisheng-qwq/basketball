package com.zhs.basket.service;

import jakarta.servlet.http.HttpServletResponse;

public interface IBasketPrivateFileService {
    void streamCertMaterialFile(Long fileId, HttpServletResponse response);

    void streamFileAsset(Long fileId, HttpServletResponse response);
}
