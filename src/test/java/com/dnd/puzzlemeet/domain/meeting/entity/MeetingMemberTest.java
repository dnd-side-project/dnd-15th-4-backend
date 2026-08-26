package com.dnd.puzzlemeet.domain.meeting.entity;

import static org.assertj.core.api.Assertions.assertThat;

import com.dnd.puzzlemeet.domain.user.entity.User;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MeetingMemberTest {

  @Test
  @DisplayName("새 약속 참여자는 사용자 기본 알림 설정을 복사한다")
  void copiesUserNotificationSettings() {
    User user = new User(100L, "효창", "https://img.kakao.com/a.jpg");
    user.updateNotificationSettings(false, true, false);

    MeetingMember member =
        new MeetingMember(
            meeting(user), user, MeetingMemberRole.HOST, "효창", "https://img.kakao.com/host.png");

    assertThat(member.isLocationNotificationEnabled()).isFalse();
    assertThat(member.isFriendArrivalNotificationEnabled()).isTrue();
    assertThat(member.isChatBubbleNotificationEnabled()).isFalse();
  }

  @Test
  @DisplayName("사용자 기본 알림 설정을 바꿔도 기존 약속 참여자의 설정은 유지된다")
  void keepsCopiedNotificationSettingsWhenUserDefaultsChange() {
    User user = new User(100L, "효창", "https://img.kakao.com/a.jpg");
    user.updateNotificationSettings(false, true, false);
    MeetingMember member =
        new MeetingMember(
            meeting(user), user, MeetingMemberRole.HOST, "효창", "https://img.kakao.com/host.png");

    user.updateNotificationSettings(true, false, true);

    assertThat(member.isLocationNotificationEnabled()).isFalse();
    assertThat(member.isFriendArrivalNotificationEnabled()).isTrue();
    assertThat(member.isChatBubbleNotificationEnabled()).isFalse();
  }

  @Test
  @DisplayName("출발 준비 알림 시도 시각을 기록하고 초기화한다")
  void marksAndClearsDepartureReminderAttempt() {
    User user = new User(100L, "효창", "https://img.kakao.com/a.jpg");
    MeetingMember member =
        new MeetingMember(
            meeting(user), user, MeetingMemberRole.HOST, "효창", "https://img.kakao.com/host.png");
    LocalDateTime attemptedAt = LocalDateTime.of(2026, 8, 25, 12, 30);

    member.markDepartureReminderAttempted(attemptedAt);
    assertThat(member.getDepartureReminderAttemptedAt()).isEqualTo(attemptedAt);

    member.clearDepartureReminderAttempted();
    assertThat(member.getDepartureReminderAttemptedAt()).isNull();
  }

  private Meeting meeting(User host) {
    return new Meeting(
        host,
        "한강 피크닉",
        LocalDateTime.of(2026, 8, 25, 14, 0),
        "서울 여의도 한강공원",
        null,
        BigDecimal.valueOf(37.5283),
        BigDecimal.valueOf(126.9320),
        50,
        100,
        "ABCD1234",
        null);
  }
}
