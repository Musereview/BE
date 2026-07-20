import com.mr.global.entity.BaseTimeDeletedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification extends BaseTimeDeletedEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_id")
    private Long id;

    // 유저 아이디
    @Column(name = "user_id", nullable = false)
    private Long userId;

    // 제목
    @Column(name = "title", nullable = false, length = 100)
    private String title;

    // 내용
    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    // 읽음 여부
    @Column(name = "is_read", nullable = false)
    private boolean isRead = false;

    @Builder
    public Notification(Long userId, String title, String content){
        this.userId = userId;
        this.title = title;
        this.content = content;
    }

    public void readTrue(){
        this.isRead = true;
    }
}