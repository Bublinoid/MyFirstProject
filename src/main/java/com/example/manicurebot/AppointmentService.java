package com.example.manicurebot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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
        return appointmentRepository.getAvailableTimes(selectedDate);
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
        logger.info("Доступные времена для даты {}: {}", selectedDate, availableTimes);
        return availableTimes;
    }

    public List<LocalTime> getOccupiedTimesForDate(LocalDate selectedDate) {
        // Получите из репозитория список записей на приемы на выбранную дату
        List<Appointment> appointments = appointmentRepository.getAppointmentsByDate(selectedDate);

        List<LocalTime> occupiedTimes = appointments.stream()
                .map(Appointment::getTime)
                .collect(Collectors.toList());

        logger.info("Занятые времена для даты {}: {}", selectedDate, occupiedTimes);
        return occupiedTimes;
    }

    private boolean isTimeSlotAvailable(String selectedDateString, String selectedTimeString) {
        LocalDate selectedDate = LocalDate.parse(selectedDateString);
        LocalTime selectedTime = LocalTime.parse(selectedTimeString);

        // Получаем список доступных времен для выбранной даты
        List<LocalTime> availableTimes = getAvailableTimesForDate(selectedDate);

        // Получаем список занятых времен для выбранной даты
        List<LocalTime> occupiedTimes = getOccupiedTimesForDate(selectedDate);

        // Проверяем, что выбранное время входит в список доступных и не входит в список занятых
        if (availableTimes.contains(selectedTime) && !occupiedTimes.contains(selectedTime)) {
            return true;
        } else {
            // Выводим сообщение, если выбранное время занято
            logger.info("Время {} на дату {} уже занято или недоступно.", selectedTimeString, selectedDateString);
            return false;
        }
    }


    @Transactional
    public boolean makeAppointment(String username, LocalDateTime selectedDateTime, UserService userService) {
        // Пользователь будет создан или найден
        User user = userService.createUserIfNotExist("FirstName", "LastName", username);

        LocalDate selectedDate = selectedDateTime.toLocalDate();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        String selectedTimeString = selectedDateTime.toLocalTime().format(formatter);

        // Проверяем, свободно ли выбранное время
        if (isTimeSlotAvailable(selectedDate.toString(), selectedTimeString)) {
            // Получаем список занятых времен для выбранной даты
            List<LocalTime> occupiedTimes = getOccupiedTimesForDate(selectedDate);

            // Проверяем, что выбранное время не входит в список занятых
            if (!occupiedTimes.contains(selectedDateTime.toLocalTime())) {
                // Если время свободно, резервируем его
                appointmentRepository.reserveTimeSlot(selectedDate, Long.valueOf(String.valueOf(user.getId())), LocalTime.parse(selectedTimeString));

                // Теперь создадим новую запись в базе данных
                Appointment newAppointment = new Appointment();
                newAppointment.setDate(selectedDate);
                newAppointment.setTime(LocalTime.parse(selectedTimeString));
                newAppointment.setUserId(user.getId());
                appointmentRepository.save(newAppointment);

                return true;
            } else {
                // Выводим сообщение, если выбранное время занято
                System.out.println("Извините, выбранное время уже занято. Пожалуйста, выберите другое время.");
                return false;
            }
        } else {
            return false;
        }
    }


    public Optional<Appointment> findAvailableAppointment(LocalDate selectedDate, LocalTime selectedTime) {
        Appointment appointment = appointmentRepository.findAvailableAppointment(selectedDate, selectedTime);
        return Optional.ofNullable(appointment);
    }



//    public List<LocalDate> getAvailableDates() {
//        List<LocalDate> availableDates = appointmentRepository.getAvailableDates();
//        logger.info("Available dates from repository: {}", availableDates);
//        return availableDates != null ? availableDates : Collections.emptyList();
//    }

}
