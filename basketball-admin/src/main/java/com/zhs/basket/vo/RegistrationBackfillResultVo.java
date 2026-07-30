package com.zhs.basket.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class RegistrationBackfillResultVo {
    private int scanned;
    private int succeeded;
    private int skipped;
    private int failed;
    private List<String> messages = new ArrayList<>();
}
