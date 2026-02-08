package webuntisjava;

import org.json.JSONException;


import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import de.keule.webuntis.*;
import de.keule.webuntis.response.*;

public class UntisClient {
    private WebUntis untis;

    public UntisClient(String username, String password, String school, String server) {
        this.untis = new WebUntis(username, password, school, server);
    }

    public boolean login() throws IOException, JSONException {
        return untis.login();
    }

    public boolean logout() throws IOException, JSONException {
        return untis.logout();
    }

    private int toDateInt(LocalDate d) {
        return d.getYear() * 10000 + d.getMonthValue() * 100 + d.getDayOfMonth();
    }


    public int getFirstLessonForDate(LocalDate date, Boolean skipCancelled) throws IOException, JSONException {
        int[] starttime = getFirstHourForDate(date, skipCancelled);
        int start = starttime[0] * 100 + starttime[1];
        int lessonHour = getLessonHourFromStartTime(date, start);
        return lessonHour;
    }

    public int[] getFirstHourForDate(LocalDate date,Boolean skipCancelled) throws IOException, JSONException {
        if (!untis.sessionIsValid()) {
            untis.login();
        }

        int dateInt = toDateInt(date);
        Timetable tt = untis.getTimetableFor(dateInt);

        if (tt == null || tt.getPeriods().isEmpty()) {
            return new int[]{-1, -1};
        }
        Period first = null;
        for (Period p : tt.getPeriods()) {
            if (skipCancelled && p.getCode() != null && p.getCode().equalsIgnoreCase("cancelled")) {
                continue;
            }

            if (first == null || p.getStartTime() < first.getStartTime()) {
                first = p;
            }
        }

        if (first == null) return new int[]{-1, -1};

        int start = first.getStartTime();

        return new int[] {start/100, start%100};
    }

    private int getLessonHourFromStartTime(LocalDate date, int start) throws JSONException, IOException {
        Timegrid grid = untis.getTimegrid(date);
        DayOfWeek wochentagEnum = date.getDayOfWeek();
        int wochentagZahl = wochentagEnum.getValue() + 1;
        if (wochentagZahl > 7) {
            wochentagZahl = 1;
        }

        TimegridDay timegridOfDate = grid.getTimegridDay(wochentagZahl);
        if (timegridOfDate != null) {
            // Alle TimeUnits (=Schulstunden) durch iterieren
            for (TimeUnit unit : timegridOfDate.getTimeUnits()) {
                if (unit.getStartTime() == start) {
                    // Stunde hat selbe Startzeit wie die gesuchte, also gefunden! Rückgabe des Stundennamens (z.B. "1" oder "2"...)
                    return Integer.parseInt(unit.getName());
                }
            }
        }

        // wenn nicht gefunden
        return -1;

    }
    public Map<String, Integer> getWeekLessons() throws IOException, JSONException {
        Map<String, Integer> result = new LinkedHashMap<>();
        LocalDate today = LocalDate.now();
        int weekday = today.getDayOfWeek().getValue();
        LocalDate currentMonday = today.minusDays(weekday - 1);

        String[] tage = {"montag", "dienstag", "mittwoch", "donnerstag", "freitag"};

        for (int i = 0; i < tage.length; i++) {
            LocalDate check = currentMonday.plusDays(i);
            int lessonHour = -1;
            int counter = 0;
            boolean found = false;

            while (!found && counter < 10) {
                try {
                    lessonHour = getFirstLessonForDate(check, false);

                    if (lessonHour > 0) {
                        found = true;
                    } else {
                        check = check.plusWeeks(1);
                        counter++;
                    }
                } catch (Exception e) {
                    check = check.plusWeeks(1);
                    counter++;
                }
            }

            result.put(tage[i], lessonHour);

        }
        return result;
    }

    public Map<String, String> getWeekTimes() throws IOException, JSONException {
        Map<String, String> result = new LinkedHashMap<>();
        LocalDate today = LocalDate.now();
        int weekday = today.getDayOfWeek().getValue();
        LocalDate currentMonday = today.minusDays(weekday - 1);

        String[] tage = {"montag", "dienstag", "mittwoch", "donnerstag", "freitag"};

        for (int i = 0; i < tage.length; i++) {
            LocalDate check = currentMonday.plusDays(i);
            int[] hm = new int[]{-1, -1};
            int counter = 0;
            boolean found = false;

            while (!found && counter < 10) {
                try {
                    hm = getFirstHourForDate(check,false); // Nutzt jetzt die neue Funktion
                    if (hm[0] != -1) {
                        found = true;
                    } else {
                        check = check.plusWeeks(1);
                        counter++;
                    }
                } catch (Exception e) {
                    check = check.plusWeeks(1);
                    counter++;
                }
            }
            result.put(tage[i], String.format("%02d:%02d", hm[0], hm[1]));
        }
        return result;
    }
}