package com.zhs.response.v4;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class V4RegistrationDetailResp extends V4MyRegistrationResp {
    private List<V4RegistrationPlayerItemResp> players;
}
