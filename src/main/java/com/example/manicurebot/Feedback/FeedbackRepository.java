package com.example.manicurebot.Feedback;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {

    List<Feedback> findByChatId(Long chatId);
}
