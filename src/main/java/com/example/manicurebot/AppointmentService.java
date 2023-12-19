package com.example.manicurebot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final UserStatusService userStatusService;
    private final Logger logger = LoggerFactory.getLogger(AppointmentService.class);


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
        logger.info("Вызван метод getAvailableDates()");

        LocalDate currentDate = LocalDate.now();
        LocalDate endDate = currentDate.plusMonths(1);

        List<LocalDate> availableDates = new ArrayList<>();

        while (currentDate.isBefore(endDate)) {
            if (currentDate.getDayOfWeek() == DayOfWeek.SATURDAY || currentDate.getDayOfWeek() == DayOfWeek.SUNDAY) {
                availableDates.add(currentDate);
            }
            currentDate = currentDate.plusDays(1);
        }

        logger.info("Доступные даты: {}", availableDates);
        return availableDates;
    }





    public List<LocalTime> getAvailableTimesForDate(LocalDate selectedDate) {
        // Логика для получения доступных времен для конкретной даты
        // Зависит от ваших требований
        // Пример: возвращаем список времен с 10:00 до 17:00 с интервалом в 1 час

        List<LocalTime> availableTimes = new ArrayList<>();
        LocalTime startTime = LocalTime.of(10, 0);
        LocalTime endTime = LocalTime.of(18, 0);

        while (startTime.isBefore(endTime)) {
            availableTimes.add(startTime);
            startTime = startTime.plusHours(1);
        }

        return availableTimes;
    }

    public List<LocalTime> getOccupiedTimesForDate(LocalDate selectedDate) {
        // Получите из репозитория список записей на приемы на выбранную дату
        List<Appointment> appointments = appointmentRepository.getAppointmentsByDate(selectedDate.toString());

        List<LocalTime> occupiedTimes = appointments.stream()
                .map(appointment -> LocalTime.parse(appointment.getTime()))
                .collect(Collectors.toList());

        return occupiedTimes;
    }




    private boolean isTimeSlotAvailable(String selectedDateString, String selectedTimeString) {
        List<String> availableTimes = getAvailableTimes(LocalDate.parse(selectedDateString));
        return availableTimes.contains(selectedTimeString);
    }

    public boolean makeAppointment(User user, LocalDateTime selectedDateTime) {
        String selectedDateString = selectedDateTime.toLocalDate().toString();
        String selectedTimeString = selectedDateTime.toLocalTime().toString();

        if (isTimeSlotAvailable(selectedDateString, selectedTimeString)) {
            appointmentRepository.reserveTimeSlot(selectedDateString, selectedTimeString, String.valueOf(user.getId()));
            return true;
        } else {
            return false;
        }
    }


//    public List<LocalDate> getAvailableDates() {
//        List<LocalDate> availableDates = appointmentRepository.getAvailableDates();
//        logger.info("Available dates from repository: {}", availableDates);
//        return availableDates != null ? availableDates : Collections.emptyList();
//    }

}
