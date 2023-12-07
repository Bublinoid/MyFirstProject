package com.example.manicurebot;

import com.example.manicurebot.AppointmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;


import java.time.LocalDate;
import java.time.LocalTime;


@Service
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;

    @Autowired
    public AppointmentService(AppointmentRepository appointmentRepository) {
        this.appointmentRepository = appointmentRepository;
    }

    public List<String> getAvailableTimes(LocalDate selectedDate) {
        String selectedDateString = selectedDate.toString();
        return appointmentRepository.getAvailableTimes(selectedDateString);
    }

    public boolean makeAppointment(org.telegram.telegrambots.meta.api.objects.User user, LocalDate selectedDate, LocalTime selectedTime) {
        String selectedDateString = selectedDate.toString();
        String selectedTimeString = selectedTime.toString();

        if (isTimeSlotAvailable(selectedDateString, selectedTimeString)) {
            appointmentRepository.reserveTimeSlot(selectedDateString, selectedTimeString, String.valueOf(user.getId()));
            return true;
        } else {
            return false;
        }
    }

    public List<LocalDate> getAvailableDates() {
        return Arrays.asList(
                LocalDate.now().plusDays(1),
                LocalDate.now().plusDays(2),
                LocalDate.now().plusDays(3),
                LocalDate.now().plusDays(4),
                LocalDate.now().plusDays(5),
                LocalDate.now().plusDays(6),
                LocalDate.now().plusDays(7)
        );
    }

    private boolean isTimeSlotAvailable(String selectedDateString, String selectedTimeString) {
        List<String> availableTimes = getAvailableTimes(LocalDate.parse(selectedDateString));
        return availableTimes.contains(selectedTimeString);
    }
}
