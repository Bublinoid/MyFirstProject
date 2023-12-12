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
        if (isValidFeedback(feedback)) {
            feedbackRepository.save(feedback);
        } else {
            throw new IllegalArgumentException("Invalid feedback");

        }
    }
    private boolean isValidFeedback(Feedback feedback) {
        return feedback != null && feedback.getUsername() != null && !feedback.getUsername().isEmpty()
                && feedback.getMessage() != null && !feedback.getMessage().isEmpty();
    }



    public List<Feedback> getFeedbackByChatId(Long chatId) {
        return feedbackRepository.findByChatId(chatId);
    }
}

