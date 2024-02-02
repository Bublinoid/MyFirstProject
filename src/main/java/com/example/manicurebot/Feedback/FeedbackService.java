package com.example.manicurebot.Feedback;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 *  logger, "@NotBlank", "@Transactional", exceptions
 */

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

    public List<Feedback> getAllFeedback() {
        return feedbackRepository.findAll();
    }

}

