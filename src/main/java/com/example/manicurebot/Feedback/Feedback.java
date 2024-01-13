package com.example.manicurebot.Feedback;

import lombok.Getter;

import javax.persistence.*;

@Getter
@Entity
@Table(name = "feedbacks")

public class Feedback {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "feedback_sequence")
    @SequenceGenerator(name = "feedback_sequence", sequenceName = "feedback_sequence", allocationSize = 1)
    private Long id;

    private String username;
    private String message;
    private Long chatId;


    public void setId(Long id) {
        this.id = id;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setChatId(Long chatId) {
        this.chatId = chatId;
    }


}

