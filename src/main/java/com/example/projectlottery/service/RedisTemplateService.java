package com.example.projectlottery.service;

import com.example.projectlottery.dto.request.DhLoginRequest;
import com.example.projectlottery.dto.response.LottoResponse;
import com.example.projectlottery.dto.response.ShopResponse;
import com.example.projectlottery.dto.response.querydsl.QShopSummary;
import com.example.projectlottery.util.EncryptionUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@RequiredArgsConstructor
@Service
public class RedisTemplateService {
    private static final String CACHE_LATEST_DRAW_NO_KEY = "L645_LATEST_DRAW_NO";
    private static final String CACHE_WIN_DETAIL_KEY = "L645_WIN_DETAIL";
    private static final String CACHE_SHOP_DETAIL_KEY = "L645_SHOP_DETAIL";
    private static final String CACHE_SHOP_RANKING_KEY = "L645_SHOP_RANKING";
    private static final String CACHE_SHOP_RECENT_RANKING_KEY = "L645_SHOP_RECENT_RANKING";

    private static final String REDIS_KEY_DH_LOGIN_INFO = "DH_LOGIN_INFO";

    private static final String REDIS_KEY_SCRAP_RUNNING_URL = "SCRAP_RUNNING_URL";
    private static final String REDIS_KEY_SCRAP_RUNNING_PARAM1 = "SCRAP_RUNNING_PARAM1";
    private static final String REDIS_KEY_SCRAP_RUNNING_PARAM2 = "SCRAP_RUNNING_PARAM2";

    private final EncryptionUtils encryptionUtils;

    private final RedisTemplate<String, Object> redisTemplate;

    private final ObjectMapper objectMapper;

    private ValueOperations<String, Object> valueOperations;
    private HashOperations<String, String, String> hashOperations;
    private ZSetOperations<String, Object> zSetOperations;

    @PostConstruct
    public void init() {
        valueOperations = redisTemplate.opsForValue();
        hashOperations = redisTemplate.opsForHash();
        zSetOperations = redisTemplate.opsForZSet();

        //동행복권 로그인정보는 30분 뒤에 자동으로 파기된다.
        redisTemplate.expire(REDIS_KEY_DH_LOGIN_INFO, 30, TimeUnit.MINUTES);
    }

    /**
     * redis cache save - scrap 동작 여부
     * @param url scrap url
     * @param param1 scrap query param1
     */
    public void saveScrapRunningInfo(String url, Object param1) {
        //scrap 동작 중일 때, 다른 scrap 동작 수행을 막기 위해 redis 에 저장해두고 판단한다.
        if (Objects.isNull(url)) {
            log.error("Required values must not be null");
            return;
        }

        valueOperations.set(REDIS_KEY_SCRAP_RUNNING_URL, url);
        valueOperations.set(REDIS_KEY_SCRAP_RUNNING_PARAM1, param1);
        log.info("[RedisTemplateService saveScrapRunningInfo() success] running url: {}, param1: {}", url, param1);
    }


    /**
     * redis cache save - scrap 동작 여부
     * @param url scrap url
     * @param param1 scrap query param1
     * @param param2 scrap query param1
     */
    public void saveScrapRunningInfo(String url, Object param1, Object param2) {
        //scrap 동작 중일 때, 다른 scrap 동작 수행을 막기 위해 redis 에 저장해두고 판단한다.
        if (Objects.isNull(url)) {
            log.error("Required values must not be null");
            return;
        }

        valueOperations.set(REDIS_KEY_SCRAP_RUNNING_URL, url);
        valueOperations.set(REDIS_KEY_SCRAP_RUNNING_PARAM1, param1);
        valueOperations.set(REDIS_KEY_SCRAP_RUNNING_PARAM2, param2);
        log.info("[RedisTemplateService saveScrapRunningInfo() success] running url: {}, param1: {}, param2: {}", url, param1, param2);
    }

    /**
     * redis cache clear - scrap 동작 여부
     */
    public void deleteScrapRunningInfo() {
        try {
            redisTemplate.delete(REDIS_KEY_SCRAP_RUNNING_URL);
            redisTemplate.delete(REDIS_KEY_SCRAP_RUNNING_PARAM1);
            redisTemplate.delete(REDIS_KEY_SCRAP_RUNNING_PARAM2);
            log.info("[RedisTemplateService deleteScrapRunningInfo() success]");
        } catch (Exception e) {
            log.error("[RedisTemplateService deleteScrapRunningInfo() failed]: {}", e.getMessage());
        }
    }

    /**
     * redis cache hit - scrap 동작 여부
     */
    public Map<String, Object> getScrapRunningInfo() {
        try {
            Map<String, Object> result = new HashMap<>();
            result.put("url", valueOperations.get(REDIS_KEY_SCRAP_RUNNING_URL));
            result.put("param1", valueOperations.get(REDIS_KEY_SCRAP_RUNNING_PARAM1));
            result.put("param2", valueOperations.get(REDIS_KEY_SCRAP_RUNNING_PARAM2));
            return result;

        } catch (Exception e) {
            log.error("[RedisTemplateService getScrapRunningInfo() failed]: {}", e.getMessage());
            return null;
        }
    }

    /**
     * redis cache save - 최신 회차 번호
     * @param latestDrawNo 최신 회차 번호
     */
    public void saveLatestDrawNo(Long latestDrawNo) {
        if (Objects.isNull(latestDrawNo)) {
            log.error("Required values must not be null");
            return;
        }

        valueOperations.set(CACHE_LATEST_DRAW_NO_KEY,  String.valueOf(latestDrawNo));
        log.info("[RedisTemplateService saveLatestDrawNo() success] drawNo: {}", latestDrawNo);
    }

    /**
     * redis cache clear - 최신 회차 번호
     */
    public void deleteLatestDrawNo() {
        try {
            redisTemplate.delete(CACHE_LATEST_DRAW_NO_KEY);
            log.info("[RedisTemplateService deleteLatestDrawNo() success]");
        } catch (Exception e) {
            log.error("[RedisTemplateService deleteLatestDrawNo() failed]: {}", e.getMessage());
        }
    }

    /**
     * redis cache hit - 최신 회차 번호
     */
    public Long getLatestDrawNo() {
        try {
            return Long.valueOf(String.valueOf(valueOperations.get(CACHE_LATEST_DRAW_NO_KEY)));
        } catch (Exception e) {
            log.error("[RedisTemplateService getLatestDrawNo() failed]: {}", e.getMessage());
            return null;
        }
    }

    /**
     * redis cache save - 로또추첨결과 view response dto
     * @param dto 로또추첨결과 view response dto
     */
    public void saveWinDetail(LottoResponse dto) {
        if (Objects.isNull(dto) || Objects.isNull(dto.drawNo())) {
            log.error("Required values must not be null");
            return;
        }

        try {
            //drawNo 를 hashKey 로 사용
            hashOperations.put(CACHE_WIN_DETAIL_KEY, String.valueOf(dto.drawNo()), serializeResponseDto(dto));
            log.info("[RedisTemplateService saveWinDetail() success] drawNo: {}", dto.drawNo());
        } catch (Exception e) {
            log.error("[RedisTemplateService saveWinDetail() failed]: {}", e.getMessage());
        }
    }

    /**
     * redis cache clear - 로또추첨결과 view response dto
     */
    public void deleteAllWinDetail() {
        try {
            redisTemplate.delete(CACHE_WIN_DETAIL_KEY);
            log.info("[RedisTemplateService deleteAllWinDetail() success]");
        } catch (Exception e) {
            log.error("[RedisTemplateService deleteAllWinDetail() failed]: {}", e.getMessage());
        }
    }

    /**
     * redis cache hit - 로또추첨결과 view response dto
     */
    public LottoResponse getWinDetail(Long drawNo) {
        try {
            return deserializeResponseDto(hashOperations.get(CACHE_WIN_DETAIL_KEY, String.valueOf(drawNo)), LottoResponse.class);
        } catch (Exception e) {
            log.info("[RedisTemplateService getWinDetail() failed] drawNo: {}", drawNo);
            return null;
        }
    }

    /**
     * redis cache save - 로또판매점상세 view response dto
     * @param dto 로또판매점상세 view response dto
     */
    public void saveShopDetail(ShopResponse dto) {
        if (Objects.isNull(dto) || Objects.isNull(dto.id())) {
            log.error("Required values must not be null");
            return;
        }

        try {
            //shopId 를 hashKey 로 사용
            hashOperations.put(CACHE_SHOP_DETAIL_KEY, String.valueOf(dto.id()), serializeResponseDto(dto));
            log.info("[RedisTemplateService saveShopDetail() success] shopId: {}", dto.id());
        } catch (Exception e) {
            log.error("[RedisTemplateService saveShopDetail() failed]: {}", e.getMessage());
        }
    }

    /**
     * redis cache delete - 로또판매점상세 view response dto
     */
    public void deleteAllShopDetail() {
        try {
            redisTemplate.delete(CACHE_SHOP_DETAIL_KEY);
            log.info("[RedisTemplateService deleteAllShopDetail() success]");
        } catch (Exception e) {
            log.error("[RedisTemplateService deleteAllShopDetail() failed]: {}", e.getMessage());
        }
    }

    /**
     * redis cache hit - 로또판매점상세 view response dto
     */
    public ShopResponse getShopDetail(Long shopId) {
        try {
            return deserializeResponseDto(hashOperations.get(CACHE_SHOP_DETAIL_KEY, String.valueOf(shopId)), ShopResponse.class);
        } catch (Exception e) {
            log.info("[RedisTemplateService saveShopRanking() failed] shopId: {}", shopId);
            return null;
        }
    }

    /**
     * redis cache save - 로또명당 view response dto
     * @param recentYn 신흥명당 (최근 52주) 여부
     * @param shopRankingResponses 로또명당 view response dto
     */
    public void saveShopRanking(boolean recentYn, List<QShopSummary> shopRankingResponses) {
        String key = recentYn ? CACHE_SHOP_RECENT_RANKING_KEY : CACHE_SHOP_RANKING_KEY;

        for (QShopSummary dto : shopRankingResponses) {
            if (Objects.isNull(dto) || Objects.isNull(dto.id())) {
                log.error("Required values must not be null");
                return;
            }

            try {
                //dto serialized 값을 value 로 사용
                //score 는 정렬 로직(1등, 2등 배출 내림차순)에 따라 가중치 계산한 값을 사용 (1등.desc, 2등.desc, id.asc)
                double score = (dto.firstPrizeWinCount() * 100000D) + (dto.secondPrizeWinCount() * 1D) + (1 - dto.id() / 100000000D);
                zSetOperations.add(key, serializeResponseDto(dto), score);
                log.info("[RedisTemplateService saveShopRanking() success] cachedKey: {}, shopId: {}, score: {}", key, dto.id(), score);
            } catch (Exception e) {
                log.error("[RedisTemplateService saveShopRanking() failed] cachedKey: {} - {}", key, e.getMessage());
            }
        }
    }

    /**
     * redis cache clear - 로또명당 view response dto
     */
    public void deleteAllShopRanking() {
        try {
            redisTemplate.delete(CACHE_SHOP_RANKING_KEY);
            log.info("[RedisTemplateService deleteAllShopRanking() success] cachedKey: CACHE_SHOP_RANKING_KEY");
            redisTemplate.delete(CACHE_SHOP_RECENT_RANKING_KEY);
            log.info("[RedisTemplateService deleteAllShopRanking() success] cachedKey: CACHE_SHOP_RECENT_RANKING_KEY");
        } catch (Exception e) {
            log.error("[RedisTemplateService deleteAllShopRanking() failed] : {}", e.getMessage());
        }
    }

    /**
     * redis cache hit - 로또명당 view response dto
     * @param recentYn 신흥명당 (최근 52주) 여부
     */
    public List<QShopSummary> getAllShopRanking(boolean recentYn) {
        String key = recentYn ? CACHE_SHOP_RECENT_RANKING_KEY : CACHE_SHOP_RANKING_KEY;

        try {
            return zSetOperations.reverseRange(key, 0, 99).stream().map((o -> {
                try {
                    return deserializeResponseDto(o.toString(), QShopSummary.class);
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
            })).toList();
        } catch (Exception e) {
            log.info("[RedisTemplateService getAllShopRanking() failed] cachedKey: {} - {}", key, e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * redis cache save - 동행복권 로그인 request dto (for selenium 로또 구매)
     * @param dto 동행복권 로그인 request dto
     */
    public void saveDhLoginInfo(DhLoginRequest dto) {
        if (Objects.isNull(dto) || Objects.isNull(dto.id()) || Objects.isNull(dto.password())) {
            log.error("Required values must not be null");
            return;
        }

        try {
            //비밀번호를 암호화해서 저장한다.
            dto = DhLoginRequest.of(dto.id(), encryptionUtils.encrypt(dto.password()));

            //id 를 hashKey 로 사용
            hashOperations.put(REDIS_KEY_DH_LOGIN_INFO, dto.id(), serializeResponseDto(dto));
            log.info("[RedisTemplateService saveDhLoginInfo() success] id: {}", dto.id());
        } catch (Exception e) {
            log.error("[RedisTemplateService saveDhLoginInfo() failed]: {}", e.getMessage());
        }
    }

    /**
     * redis cache clear - 동행복권 로그인 request dto
     * @param id 동행복권 사용자 id
     */
    public void deleteDhLoginInfo(String id) {
        try {
            hashOperations.delete(REDIS_KEY_DH_LOGIN_INFO, id);
            log.info("[RedisTemplateService deleteDhLoginINfo() success] id: {}", id);
        } catch (Exception e) {
            log.error("[RedisTemplateService deleteDhLoginINfo() failed]: {}", e.getMessage());
        }
    }

    /**
     * redis cache hit - 동행복권 로그인 request dto
     * @param id 동행복권 사용자 id
     * @return 동행복권 로그인 request dto
     */
    public DhLoginRequest getDhLoginInfo(String id) {
        try {
            DhLoginRequest encrypted = deserializeResponseDto(hashOperations.get(REDIS_KEY_DH_LOGIN_INFO, id), DhLoginRequest.class);

            //비밀번호를 복호화해서 전달한다.
            return DhLoginRequest.of(encrypted.id(), encryptionUtils.decrypt(encrypted.password()));
        } catch (Exception e) {
            log.info("[RedisTemplateService getDhLoginInfo() failed] id: {}", id);
            return null;
        } finally {
            deleteDhLoginInfo(id); //동행복권 로그인 정보는 일회용으로 구매 시, 즉시 파기한다.
        }
    }

    /**
     * redis all cache clear - scrap 완료 후에 모든 cache 삭제
     */
    public void flushAllCache() {
        this.deleteLatestDrawNo();
        this.deleteAllWinDetail();
        this.deleteAllShopDetail();
        this.deleteAllShopRanking();
    }

    /**
     * serialize dto
     * @param dto target dto
     * @return serialized dto
     * @throws JsonProcessingException
     */
    private String serializeResponseDto(Object dto) throws JsonProcessingException {
        return objectMapper.writeValueAsString(dto);
    }

    /**
     * deserialize dto
     * @param serializedValue serialized dto string
     * @param valueType original dto type
     * @return deserialized dto
     * @param <T> generic
     * @throws JsonProcessingException
     */
    private <T> T deserializeResponseDto(String serializedValue, Class<T> valueType) throws JsonProcessingException {
        return objectMapper.readValue(serializedValue, valueType);
    }
}
