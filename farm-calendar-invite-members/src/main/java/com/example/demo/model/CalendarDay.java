package com.example.demo.model;

import java.time.LocalDate;
import java.util.List;

import com.example.demo.entity.Schedule;

public class CalendarDay {

    private int day;

    private String date;

    // 当月か
    private boolean currentMonth;

    // 今日か
    private boolean today;

    private List<Schedule> scheduleList;

    public CalendarDay(
            int day,
            String date,
            boolean currentMonth,
            List<Schedule> scheduleList){

        this.day = day;
        this.date = date;
        this.currentMonth = currentMonth;
        this.scheduleList = scheduleList;

        this.today =
                LocalDate.now().toString().equals(date);
    }

    public int getDay() {
        return day;
    }

    public void setDay(int day) {
        this.day = day;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public boolean isCurrentMonth() {
        return currentMonth;
    }

    public void setCurrentMonth(boolean currentMonth) {
        this.currentMonth = currentMonth;
    }

    public boolean isToday() {
        return today;
    }

    public int getScheduleCount() {

        if(scheduleList == null){

            return 0;

        }

        return scheduleList.size();

    }

    public List<Schedule> getScheduleList() {
        return scheduleList;
    }

    public void setScheduleList(List<Schedule> scheduleList) {
        this.scheduleList = scheduleList;
    }

}