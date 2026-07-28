package com.mr.global.event;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class NotificationEvent {
    private Long userId;
    private String title;
    private String content;

    // 연주 분석 알림 팩토리 (곡 이름만 받아서 제목 생성)
    public static NotificationEvent forAnalysis(Long userId, String practiceName) {
        // 곡 이름이 너무 길면 잘라내기 (말줄임표 처리)
        if (practiceName.length() > 30) {
            practiceName = practiceName.substring(0, 30) + "...";
        }
        String generatedTitle = practiceName + " 연주 분석이 완료되었습니다.";
        return new NotificationEvent(userId, generatedTitle, "지금 바로 분석 결과를 확인해보세요!");
    }

    // 연습 시간 달성 알림 팩토리 (유저 이름과 시간만 받아서 제목 생성)
    public static NotificationEvent forPractice(Long userId, String userName, int hours) {
        String generatedTitle = userName + " 님, 이번 주 연습 " + hours + "시간을 달성했습니다!";
        return new NotificationEvent(userId, generatedTitle, "꾸준한 연습이 멋진 연주를 만듭니다!");
    }

    // 학습 완료 알림 팩토리 (학습 주제만 받아서 제목 생성)
    public static NotificationEvent forLearning(Long userId, String topicName) {
        String generatedTitle = topicName + " 학습을 모두 완료했습니다.";
        return new NotificationEvent(userId, generatedTitle, "다음 학습으로 넘어가 볼까요?");
    }
}