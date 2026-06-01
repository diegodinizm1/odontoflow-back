package com.diego.odontoflowbackend.audit;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.RevisionEntity;
import org.hibernate.envers.RevisionNumber;
import org.hibernate.envers.RevisionTimestamp;

@Entity
@Table(name = "revinfo")
@RevisionEntity(AuditRevisionListener.class)
@Getter
@Setter
public class AuditRevision {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @RevisionNumber
    @Column(name = "rev")
    private int id;

    @RevisionTimestamp
    @Column(name = "revtstmp")
    private long timestamp;

    @Column(name = "user_id")
    private String userId;
}
