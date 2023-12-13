package com.example.manicurebot;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final UserStatusService userStatusService;


    @Autowired
    public AppointmentService(AppointmentRepository appointmentRepository, UserStatusService userStatusService) {
        this.appointmentRepository = appointmentRepository;
        this.userStatusService = userStatusService;

    }

    public List<String> getAvailableTimes(LocalDate selectedDate) {
        String selectedDateString = selectedDate.toString();
        return appointmentRepository.getAvailableTimes(selectedDateString);
    }

    public boolean makeAppointment(User user, LocalDate selectedDate, LocalTime selectedTime) {
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
        LocalDate currentDate = LocalDate.now();
        LocalDate endDate = currentDate.plusMonths(2);

        List<LocalDate> availableDates = new ArrayList<>();

        while (currentDate.isBefore(endDate)) {
            if (currentDate.getDayOfWeek() == DayOfWeek.SATURDAY || currentDate.getDayOfWeek() == DayOfWeek.SUNDAY) {
                availableDates.add(currentDate);
            }
            currentDate = currentDate.plusDays(1);
        }

        System.out.println("Available dates: " + availableDates);
        return availableDates;
    }

    public List<LocalTime> getAvailableTimesForDate(LocalDate selectedDate) {
        // Логика для получения доступных времен для конкретной даты
        // Зависит от ваших требований
        // Пример: возвращаем список времен с 10:00 до 17:00 с шагом 2 часа 30 минут
        List<LocalTime> availableTimes = new ArrayList<>();
        LocalTime startTime = LocalTime.of(10, 0);
        LocalTime endTime = LocalTime.of(17, 0);

        while (startTime.plusHours(2).plusMinutes(30).isBefore(endTime)) {
            availableTimes.add(startTime);
            startTime = startTime.plusHours(2).plusMinutes(30);
        }

        return availableTimes;
    }

    private boolean isTimeSlotAvailable(String selectedDateString, String selectedTimeString) {
        List<String> availableTimes = getAvailableTimes(LocalDate.parse(selectedDateString));
        return availableTimes.contains(selectedTimeString);
    }

    public boolean makeAppointment(User user, LocalDateTime selectedDateTime) {
        return false;
    }
}
