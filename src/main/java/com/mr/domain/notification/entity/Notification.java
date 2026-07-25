package com.mr.domain.notification.entity;

import com.mr.domain.user.entity.User;
import com.mr.global.entity.BaseTimeDeletedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "notification",
        indexes = {
                // 1. 특정 유저의 알림 목록을 최신순(생성일순)으로 정렬해 보여줄 때
                @Index(name = "idx_notification_user_created",
                        columnList = "user_id, created_at"),

                // 2. 유저가 읽지 않은 알림 개수를 셀 때
                @Index(name = "idx_notification_user_read",
                        columnList = "user_id, is_read")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification extends BaseTimeDeletedEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_id")
    private Long id;

    // 유저 아이디
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // 제목
    @Column(name = "title", nullable = false, length = 100)
    private String title;

    // 내용
    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    // 읽음 여부
    @Column(name = "is_read", nullable = false)
    private boolean isRead = false;


    @Builder(access = AccessLevel.PRIVATE)
    private Notification(User user, String title, String content) {
        validateUser(user);
        validateTitle(title);
        validateContent(content);

        this.user = user;
        this.title = title;
        this.content = content;
        this.isRead = false;
    }

    public static Notification create(User user, String title, String content) {
        return Notification.builder()
                .user(user)
                .title(title)
                .content(content)
                .build();
    }

    // 알림 읽음 처리
    public void markAsRead() {
        this.isRead = true;
    }

    private void validateUser(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User는 필수입니다.");
        }
    }

    private void validateTitle(String title) {
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Title은 필수입니다.");
        }
    }

    private void validateContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("Content는 필수입니다.");
        }
    }
}