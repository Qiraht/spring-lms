package com.qiraht.spring_lms.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "assignments")
public class Assignment {
    @Id
    private String id;

    private String title;

    private String content;

    @Column(nullable = false)
    private String attachment;

    @Column(name = "due_date")
    private LocalDateTime dueDate;

    @Column(name = "class_id")
    private String classId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}
