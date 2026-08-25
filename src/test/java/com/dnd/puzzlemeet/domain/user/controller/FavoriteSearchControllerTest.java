package com.dnd.puzzlemeet.domain.user.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dnd.puzzlemeet.TestcontainersConfiguration;
import com.dnd.puzzlemeet.domain.user.entity.FavoriteSearch;
import com.dnd.puzzlemeet.domain.user.entity.User;
import com.dnd.puzzlemeet.domain.user.repository.FavoriteSearchRepository;
import com.dnd.puzzlemeet.domain.user.repository.UserRepository;
import com.dnd.puzzlemeet.global.security.client.KakaoUnlinkClient;
import com.dnd.puzzlemeet.global.security.service.JwtProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class FavoriteSearchControllerTest {

  private static final String ENDPOINT = "/api/v1/users/me/favorite-searches";

  @Autowired private MockMvc mockMvc;
  @Autowired private UserRepository userRepository;
  @Autowired private FavoriteSearchRepository favoriteSearchRepository;
  @Autowired private JwtProvider jwtProvider;
  @Autowired private JdbcTemplate jdbcTemplate;
  @MockitoBean private KakaoUnlinkClient kakaoUnlinkClient;

  @Test
  @DisplayName("access token 없이 장소 즐겨찾기를 요청하면 401로 거절된다")
  void rejectsRequestWithoutAccessToken() throws Exception {
    mockMvc.perform(get(ENDPOINT)).andExpect(status().isUnauthorized());
  }

  @Test
  @DisplayName("공백 장소명으로 즐겨찾기를 등록하면 입력값 검증에 실패한다")
  void rejectsBlankKeyword() throws Exception {
    User user = saveUser(100L, "효창");

    mockMvc
        .perform(
            post(ENDPOINT)
                .header("Authorization", bearerToken(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"keyword\":\"   \"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"));
  }

  @Test
  @DisplayName("100자를 초과한 장소명으로 즐겨찾기를 등록하면 입력값 검증에 실패한다")
  void rejectsKeywordOverOneHundredCharacters() throws Exception {
    User user = saveUser(100L, "효창");

    mockMvc
        .perform(
            post(ENDPOINT)
                .header("Authorization", bearerToken(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"keyword\":\"" + "a".repeat(101) + "\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"));
  }

  @Test
  @DisplayName("유니코드 공백만 있는 장소명은 서비스 검증에서 400으로 거절된다")
  void rejectsUnicodeWhitespaceOnlyKeyword() throws Exception {
    User user = saveUser(100L, "효창");

    mockMvc
        .perform(
            post(ENDPOINT)
                .header("Authorization", bearerToken(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"keyword\":\"\u00A0\u2007\u202F\u3000\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"));
  }

  @Test
  @DisplayName("인증 사용자가 장소명을 등록하면 정규화된 장소명과 201 응답을 받는다")
  void createsNormalizedFavoriteSearch() throws Exception {
    User user = saveUser(100L, "효창");

    mockMvc
        .perform(
            post(ENDPOINT)
                .header("Authorization", bearerToken(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"keyword\":\"  강남\\t역  \"}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.code").value("API_CREATED"))
        .andExpect(jsonPath("$.data.id").isNumber())
        .andExpect(jsonPath("$.data.keyword").value("강남 역"));

    FavoriteSearch saved = favoriteSearchRepository.findAll().getFirst();
    assertThat(saved.getKeyword()).isEqualTo("강남 역");
    assertThat(saved.getNormalizedKeyword()).isEqualTo("강남 역");
    assertThat(saved.getCreatedAt()).isNotNull();
  }

  @Test
  @DisplayName("도로명 주소를 함께 등록하면 등록 응답과 목록 조회에서 모두 돌려준다")
  void savesAndReturnsRoadAddressName() throws Exception {
    User user = saveUser(100L, "효창");

    mockMvc
        .perform(
            post(ENDPOINT)
                .header("Authorization", bearerToken(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"keyword\":\"강남역\",\"roadAddressName\":\"  서울 강남구   강남대로 396  \"}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.data.keyword").value("강남역"))
        .andExpect(jsonPath("$.data.roadAddressName").value("서울 강남구 강남대로 396"));

    mockMvc
        .perform(get(ENDPOINT).header("Authorization", bearerToken(user)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[0].roadAddressName").value("서울 강남구 강남대로 396"));
  }

  @Test
  @DisplayName("도로명 주소 없이 등록하면 응답의 도로명 주소는 null로 내려간다")
  void returnsNullRoadAddressNameWhenOmitted() throws Exception {
    User user = saveUser(100L, "효창");

    mockMvc
        .perform(
            post(ENDPOINT)
                .header("Authorization", bearerToken(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"keyword\":\"강남역\",\"roadAddressName\":\"   \"}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.data.roadAddressName").value(org.hamcrest.Matchers.nullValue()));

    mockMvc
        .perform(get(ENDPOINT).header("Authorization", bearerToken(user)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[0].keyword").value("강남역"))
        .andExpect(jsonPath("$.data[0].roadAddressName").value(org.hamcrest.Matchers.nullValue()));
  }

  @Test
  @DisplayName("200자를 초과한 도로명 주소로 즐겨찾기를 등록하면 입력값 검증에 실패한다")
  void rejectsRoadAddressNameOverTwoHundredCharacters() throws Exception {
    User user = saveUser(100L, "효창");

    mockMvc
        .perform(
            post(ENDPOINT)
                .header("Authorization", bearerToken(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"keyword\":\"강남역\",\"roadAddressName\":\"" + "a".repeat(201) + "\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"));
  }

  @Test
  @DisplayName("장소 즐겨찾기는 사용자별로 분리되어 최근 등록한 순서로 조회된다")
  void listsFavoriteSearchesByUserInLatestOrder() throws Exception {
    User firstUser = saveUser(100L, "효창");
    User secondUser = saveUser(200L, "다른 사용자");

    createFavoriteSearch(firstUser, "첫 번째 장소");
    createFavoriteSearch(firstUser, "두 번째 장소");
    createFavoriteSearch(secondUser, "다른 사용자의 장소");

    mockMvc
        .perform(get(ENDPOINT).header("Authorization", bearerToken(firstUser)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.length()").value(2))
        .andExpect(jsonPath("$.data[0].keyword").value("두 번째 장소"))
        .andExpect(jsonPath("$.data[1].keyword").value("첫 번째 장소"));

    mockMvc
        .perform(get(ENDPOINT).header("Authorization", bearerToken(secondUser)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.length()").value(1))
        .andExpect(jsonPath("$.data[0].keyword").value("다른 사용자의 장소"));
  }

  @Test
  @DisplayName("장소 즐겨찾기가 없으면 빈 목록을 반환한다")
  void returnsEmptyFavoriteSearchList() throws Exception {
    User user = saveUser(100L, "효창");

    mockMvc
        .perform(get(ENDPOINT).header("Authorization", bearerToken(user)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data").isArray())
        .andExpect(jsonPath("$.data").isEmpty());
  }

  @Test
  @DisplayName("공백 형태와 대소문자만 다른 장소명을 다시 등록하면 중복으로 거절된다")
  void rejectsNormalizedDuplicateKeyword() throws Exception {
    User user = saveUser(100L, "효창");
    createFavoriteSearch(user, "  SEOUL\\tStation  ");

    mockMvc
        .perform(
            post(ENDPOINT)
                .header("Authorization", bearerToken(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"keyword\":\"seoul   station\"}"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("FAVORITE_SEARCH_ALREADY_EXISTS"));
  }

  @Test
  @DisplayName("즐겨찾기가 5개이면 중복 오류를 우선하고 새로운 장소는 한도 초과로 거절한다")
  void rejectsDuplicateBeforeLimitAndSixthFavoriteSearch() throws Exception {
    User user = saveUser(100L, "효창");
    for (int index = 1; index <= 5; index++) {
      createFavoriteSearch(user, "장소 " + index);
    }

    mockMvc
        .perform(
            post(ENDPOINT)
                .header("Authorization", bearerToken(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"keyword\":\"  장소   1  \"}"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("FAVORITE_SEARCH_ALREADY_EXISTS"));

    mockMvc
        .perform(
            post(ENDPOINT)
                .header("Authorization", bearerToken(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"keyword\":\"여섯 번째 장소\"}"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("FAVORITE_SEARCH_LIMIT_EXCEEDED"));
  }

  @Test
  @DisplayName("다른 사용자의 즐겨찾기는 숨긴 채 본인 항목을 삭제하고 같은 장소를 다시 등록할 수 있다")
  void protectsOwnershipAndAllowsRecreationAfterDeletion() throws Exception {
    User owner = saveUser(100L, "효창");
    User otherUser = saveUser(200L, "다른 사용자");
    createFavoriteSearch(owner, "강남역");
    Long favoriteSearchId =
        favoriteSearchRepository
            .findAllByUserIdOrderByCreatedAtDescIdDesc(owner.getId())
            .getFirst()
            .getId();

    mockMvc
        .perform(
            delete(ENDPOINT + "/" + favoriteSearchId)
                .header("Authorization", bearerToken(otherUser)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("FAVORITE_SEARCH_NOT_FOUND"));
    assertThat(favoriteSearchRepository.countByUserId(owner.getId())).isEqualTo(1L);

    mockMvc
        .perform(
            delete(ENDPOINT + "/" + favoriteSearchId).header("Authorization", bearerToken(owner)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data").doesNotExist());
    assertThat(favoriteSearchRepository.countByUserId(owner.getId())).isZero();

    createFavoriteSearch(owner, "강남역");
    assertThat(favoriteSearchRepository.countByUserId(owner.getId())).isEqualTo(1L);
  }

  @Test
  @DisplayName("복합 UNIQUE 제약이 같은 사용자의 동일한 정규화 장소명을 DB에서 거절한다")
  void enforcesCompositeUniqueConstraint() {
    assertThat(columnNullable("user_id")).isEqualTo("NO");
    assertThat(columnNullable("normalized_keyword")).isEqualTo("NO");
    assertThat(columnCollation("normalized_keyword")).isEqualTo("utf8mb4_bin");
    assertThat(
            jdbcTemplate.queryForList(
                """
                select group_concat(column_name order by seq_in_index)
                from information_schema.statistics
                where table_schema = database()
                  and table_name = 'favorite_searches'
                  and non_unique = 0
                  and index_name <> 'PRIMARY'
                group by index_name
                """,
                String.class))
        .contains("user_id,normalized_keyword");

    User user = saveUser(100L, "효창");
    favoriteSearchRepository.saveAndFlush(new FavoriteSearch(user, "SEOUL", "seoul", null));

    assertThrows(
        DataIntegrityViolationException.class,
        () ->
            favoriteSearchRepository.saveAndFlush(
                new FavoriteSearch(user, "seoul", "seoul", null)));
  }

  @Test
  @DisplayName("정규화 문자열은 악센트를 정확히 구분하고 동일 문자열의 중복 범위는 사용자 단위다")
  void comparesNormalizedKeywordExactlyWithinEachUser() throws Exception {
    User firstUser = saveUser(100L, "효창");
    User secondUser = saveUser(200L, "다른 사용자");

    createFavoriteSearch(firstUser, "cafe");
    createFavoriteSearch(firstUser, "café");
    createFavoriteSearch(secondUser, "cafe");

    assertThat(favoriteSearchRepository.countByUserId(firstUser.getId())).isEqualTo(2L);
    assertThat(favoriteSearchRepository.countByUserId(secondUser.getId())).isEqualTo(1L);
  }

  @Test
  @DisplayName("회원 탈퇴하면 저장된 장소 즐겨찾기를 DB flush 전에 모두 삭제한다")
  void deletesFavoriteSearchesOnWithdrawal() throws Exception {
    User user = saveUser(100L, "효창");
    createFavoriteSearch(user, "강남역");
    given(kakaoUnlinkClient.unlink(100L)).willReturn(100L);

    mockMvc
        .perform(delete("/api/v1/users/me").header("Authorization", bearerToken(user)))
        .andExpect(status().isOk());

    assertThat(favoriteSearchRepository.countByUserId(user.getId())).isZero();
  }

  private User saveUser(Long kakaoId, String nickname) {
    return userRepository.save(
        new User(kakaoId, nickname, "https://img.kakao.com/" + kakaoId + ".png"));
  }

  private String columnNullable(String columnName) {
    return jdbcTemplate.queryForObject(
        """
        select is_nullable
        from information_schema.columns
        where table_schema = database()
          and table_name = 'favorite_searches'
          and column_name = ?
        """,
        String.class,
        columnName);
  }

  private String columnCollation(String columnName) {
    return jdbcTemplate.queryForObject(
        """
        select collation_name
        from information_schema.columns
        where table_schema = database()
          and table_name = 'favorite_searches'
          and column_name = ?
        """,
        String.class,
        columnName);
  }

  private String bearerToken(User user) {
    return "Bearer " + jwtProvider.createAccessToken(user.getId());
  }

  private void createFavoriteSearch(User user, String keyword) throws Exception {
    mockMvc
        .perform(
            post(ENDPOINT)
                .header("Authorization", bearerToken(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"keyword\":\"" + keyword + "\"}"))
        .andExpect(status().isCreated());
  }
}
