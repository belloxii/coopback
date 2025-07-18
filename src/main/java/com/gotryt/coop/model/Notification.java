package com.gotryt.coop.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Who the notification is for
    @ManyToOne
    @JoinColumn(name = "recipient_id")
    private User recipient;

    // Optional: who triggered the notification (admin, user, guarantor, etc.)
    @ManyToOne
    @JoinColumn(name = "sender_id")
    private User sender;

    // Type of notification (to distinguish purpose)
    @Enumerated(EnumType.STRING)
    private NotificationType type;

    // Optional reference to a Loan, Share, or Guarantor entity (via ID or object)
    private Long referenceId;

    // Descriptive message to show in frontend
    private String message;

    // Track if user has read it
    @Builder.Default
    private boolean read = false;

    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
}
