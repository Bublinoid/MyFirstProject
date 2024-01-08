package com.example.manicurebot;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Column;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "date")
    private LocalDate date;

    @Column(name = "time")
    private LocalTime time;

    @Column(name = "chat_id")
    private Long chatId;

    @Column(name = "procedure_type")
    private String procedureType;

    @Column(name = "nail_count")
    private Integer nailCount;


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public LocalTime getTime() {
        return time;
    }

    public void setTime(LocalTime time) {
        this.time = time;
    }

    public Long getChatId() {
        return chatId;
    }

    public void setChatId(Long chatId) {
        this.chatId = chatId;
    }

    public void setNailCount(Integer nailCount) {
        this.nailCount = nailCount;
    }

    public void setProcedureType(String procedureType) {
        this.procedureType = procedureType;
    }

    public Integer getNailCount() {
        return nailCount;
    }

    public String getProcedureType() {
        return procedureType;
    }
}
