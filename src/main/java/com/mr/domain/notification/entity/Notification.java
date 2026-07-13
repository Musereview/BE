import com.mr.global.entity.BaseTimeDeletedEntity;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "notification")
@Getter
@Setter
public class Notification extends BaseTimeDeletedEntity{

    @Id
    @Column(name = "notification_id")
    private Long id;

    // 유저 아이디
    @JoinColumn(name = "user_id", nullable = false)
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