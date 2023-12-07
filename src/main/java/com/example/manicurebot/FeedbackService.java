package com.example.manicurebot;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FeedbackService {

    private final FeedbackRepository feedbackRepository;

    @Autowired
    public FeedbackService(FeedbackRepository feedbackRepository) {
        this.feedbackRepository = feedbackRepository;
    }

    public void saveFeedback(Feedback feedback) {
        feedbackRepository.save(feedback);
    }

    public List<Feedback> getFeedbackByChatId(Long chatId) {
        return feedbackRepository.findByChatId(chatId);
    }
}
