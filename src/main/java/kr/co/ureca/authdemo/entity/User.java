package kr.co.ureca.authdemo.entity;

import jakarta.persistence.*;
import kr.co.ureca.authdemo.oauth.SocialType;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "users",
        uniqueConstraints = @UniqueConstraint(columnNames = {"social_type", "social_type_id"})
)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "social_type", nullable = false, length = 20)
    private SocialType socialType;

    @Column(name = "social_type_id", nullable = false)
    private String socialTypeId;   // 소셜 타입 안에서의 고유 ID (항상 String)

    private String name;           // 닉네임/이름. 동의 안 하면 null

    private String email;          // 동의 안 하면 null (가짜 값 합성 금지)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    @Builder
    private User(SocialType socialType, String socialTypeId, String name, String email, UserRole role) {
        this.socialType = socialType;
        this.socialTypeId = socialTypeId;
        this.name = name;
        this.email = email;
        this.role = (role != null) ? role : UserRole.ROLE_USER;
    }

    public User update(String name, String email) {
        this.name = name;
        this.email = email;

        return this;
    }
}
