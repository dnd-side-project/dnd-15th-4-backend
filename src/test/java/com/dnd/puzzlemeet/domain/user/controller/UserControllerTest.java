package com.dnd.puzzlemeet.domain.user.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dnd.puzzlemeet.TestcontainersConfiguration;
import com.dnd.puzzlemeet.domain.user.entity.User;
import com.dnd.puzzlemeet.domain.user.repository.UserRepository;
import com.dnd.puzzlemeet.global.security.service.JwtProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class UserControllerTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private UserRepository userRepository;
  @Autowired private JwtProvider jwtProvider;

  @Test
  @DisplayName("access token으로 인증된 사용자가 본인 정보를 조회한다")
  void getMeReturnsAuthenticatedUser() throws Exception {
    User user = userRepository.save(new User(100L, "효창", "https://img.kakao.com/a.jpg"));
    String accessToken = jwtProvider.createAccessToken(user.getId());

    mockMvc
        .perform(get("/api/v1/users/me").header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.id").value(user.getId()))
        .andExpect(jsonPath("$.data.nickname").value("효창"))
        .andExpect(jsonPath("$.data.profileImageUrl").value("https://img.kakao.com/a.jpg"));
  }

  @Test
  @DisplayName("access token 없이 요청하면 401로 거절된다")
  void getMeWithoutTokenIsRejected() throws Exception {
    mockMvc.perform(get("/api/v1/users/me")).andExpect(status().isUnauthorized());
  }

  @Test
  @DisplayName("access token으로 인증된 사용자가 본인 닉네임을 수정한다")
  void updateMeUpdatesAuthenticatedUserNickname() throws Exception {
    User user = userRepository.save(new User(100L, "효창", "https://img.kakao.com/a.jpg"));
    String accessToken = jwtProvider.createAccessToken(user.getId());

    mockMvc
        .perform(
            patch("/api/v1/users/me")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nickname\":\"새닉네임\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.id").value(user.getId()))
        .andExpect(jsonPath("$.data.nickname").value("새닉네임"))
        .andExpect(jsonPath("$.data.profileImageUrl").value("https://img.kakao.com/a.jpg"));
  }

  @Test
  @DisplayName("공백 닉네임으로 수정하면 입력값 검증에 실패한다")
  void updateMeRejectsBlankNickname() throws Exception {
    User user = userRepository.save(new User(100L, "효창", "https://img.kakao.com/a.jpg"));
    String accessToken = jwtProvider.createAccessToken(user.getId());

    mockMvc
        .perform(
            patch("/api/v1/users/me")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nickname\":\"   \"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"));
  }
}
