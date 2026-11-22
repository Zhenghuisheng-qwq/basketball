package com.zhs.service.v4;

import com.zhs.exception.ServiceException;
import com.zhs.response.v4.V4MatchSubmissionDetailResp;
import com.zhs.response.v4.V4MatchSubmissionEligiblePlayerResp;
import com.zhs.support.v4.StatsScreenshotRecognizer;
import com.zhs.support.v4.StatsScreenshotRecognizer.PlayerRef;
import com.zhs.support.v4.StatsScreenshotRecognizer.SideRoster;
import com.zhs.support.v4.StatsScreenshotResult;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Service
public class V4StatsScreenshotService {

    @Resource
    private IBasketV4PlayerMatchSubmissionService matchSubmissionService;
    @Resource
    private StatsScreenshotRecognizer recognizer;

    public StatsScreenshotResult recognize(Long matchId, Long submitEventTeamId, MultipartFile file) {
        if (matchId == null) {
            throw new ServiceException("请选择比赛");
        }
        if (file == null || file.isEmpty()) {
            throw new ServiceException("请上传比赛结算截图");
        }
        V4MatchSubmissionDetailResp detail = matchSubmissionService.getDetail(matchId, submitEventTeamId);
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (Exception ex) {
            throw new ServiceException("读取截图失败");
        }
        return recognizer.recognize(
                bytes,
                file.getContentType(),
                new SideRoster(detail.getHomeTeamName(), toRefs(detail.getHomeEligiblePlayers())),
                new SideRoster(detail.getAwayTeamName(), toRefs(detail.getAwayEligiblePlayers())));
    }

    private static List<PlayerRef> toRefs(List<V4MatchSubmissionEligiblePlayerResp> players) {
        List<PlayerRef> refs = new ArrayList<>();
        if (players == null) {
            return refs;
        }
        for (V4MatchSubmissionEligiblePlayerResp player : players) {
            if (player == null || player.getEventPlayerId() == null) {
                continue;
            }
            refs.add(new PlayerRef(player.getEventPlayerId(), player.getPlayerName(), player.getGameId(), player.getJerseyNo()));
        }
        return refs;
    }
}
