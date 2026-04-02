import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class MotorPHv4 {
    // Arlove code/code fix: changed the file paths to relative CSV names so the program works in the project folder.
    static final String EMPLOYEE_FILE = "Employee Details.csv";
    static final String ATTENDANCE_FILE = "Attendance Record.csv";

    // Arlove code/code fix: updated the time rules to follow the documented work window and grace period.
    static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("MM/dd/yyyy");
    static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("H:mm");
    static final LocalTime WORK_START = LocalTime.of(8, 0);
    static final LocalTime GRACE_LIMIT = LocalTime.of(8, 5);
    static final LocalTime WORK_END = LocalTime.of(17, 0);

    // Arlove code/code fix: kept the required password in one constant so it is easier to maintain.
    static final String PASSWORD = "12345";
    
    // Arlove latest code/code fix (2026-03-22): added a shared attendance cache so payroll processing does not reread the CSV for every employee and month.
    static Map<String, List<AttendanceRecord>> attendanceByEmployee;

    // Arlove latest code/code fix (2026-03-22): grouped parsed attendance fields into one record so the cached data is easier to manage.
    static class AttendanceRecord {
        final LocalDate date;
        final String logInText;
        final String logOutText;

        AttendanceRecord(LocalDate date, String logInText, String logOutText) {
            this.date = date;
            this.logInText = logInText;
            this.logOutText = logOutText;
        }
    }

    // Arlove latest code/code fix (2026-03-22): replaced the index-based payroll array with named fields to make the payroll output easier to read.
    static class PayrollResult {
        final double firstCutoffHours;
        final double secondCutoffHours;
        final double grossFirst;
        final double grossSecond;
        final double sss;
        final double philHealth;
        final double pagIbig;
        final double tax;
        final double totalDeductions;
        final double netFirst;
        final double netSecond;

        PayrollResult(double firstCutoffHours, double secondCutoffHours, double grossFirst,
                double grossSecond, double sss, double philHealth, double pagIbig, double tax,
                double totalDeductions, double netFirst, double netSecond) {
            this.firstCutoffHours = firstCutoffHours;
            this.secondCutoffHours = secondCutoffHours;
            this.grossFirst = grossFirst;
            this.grossSecond = grossSecond;
            this.sss = sss;
            this.philHealth = philHealth;
            this.pagIbig = pagIbig;
            this.tax = tax;
            this.totalDeductions = totalDeductions;
            this.netFirst = netFirst;
            this.netSecond = netSecond;
        }
    }

    // Arlove code/code fix: changed the main flow so both valid usernames are checked before continuing.
    public static void main(String[] args) {
        Scanner input = new Scanner(System.in);

        printSeparator("=");
        System.out.println("                MOTORPH PAYROLL");
        printSeparator("=");
        System.out.print("Username: ");
        String username = input.nextLine().trim();
        System.out.print("Password: ");
        String password = input.nextLine().trim();

        if (!isValidLogin(username, password)) {
            System.out.println("Incorrect username and/or password.");
            input.close();
            return;
        }

        if (username.equals("employee")) {
            showEmployeeMenu(input);
        } else {
            showPayrollStaffMenu(input);
        }

        input.close();
    }

    // Arlove code/code fix: added one login checker so the username and password rules stay consistent.
    static boolean isValidLogin(String username, String password) {
        return (username.equals("employee") || username.equals("payroll_staff"))
            && password.equals(PASSWORD);
    }

    // Arlove code/code fix: rebuilt the employee menu to match the documentation scope.
    static void showEmployeeMenu(Scanner input) {
        while (true) {
            System.out.println();
            printSeparator("-");
            System.out.println("EMPLOYEE MENU");
            printSeparator("-");
            System.out.println("[1] Enter your employee number");
            System.out.println("[2] Exit the program");
            System.out.print("Choose an option: ");
            String choice = input.nextLine().trim();

            if (choice.equals("1")) {
                System.out.print("Enter your employee number: ");
                String employeeNumber = input.nextLine().trim();
                String[] employeeData = findEmployee(employeeNumber);

                if (employeeData == null) {
                    System.out.println("Employee number does not exist.");
                } else {
                    showEmployeeDetails(employeeData);
                }
            } else if (choice.equals("2")) {
                System.out.println("Program terminated.");
                return;
            } else {
                System.out.println("Invalid option.");
            }
        }
    }

    // Arlove code/code fix: rebuilt the payroll staff menu with process payroll and exit options only.
    static void showPayrollStaffMenu(Scanner input) {
        while (true) {
            System.out.println();
            printSeparator("-");
            System.out.println("PAYROLL STAFF MENU");
            printSeparator("-");
            System.out.println("[1] Process Payroll");
            System.out.println("[2] Exit the program");
            System.out.print("Choose an option: ");
            String choice = input.nextLine().trim();

            if (choice.equals("1")) {
                showProcessPayrollMenu(input);
            } else if (choice.equals("2")) {
                System.out.println("Program terminated.");
                return;
            } else {
                System.out.println("Invalid option.");
            }
        }
    }

    // Arlove code/code fix: added the required one employee, all employees, and exit sub-options.
    static void showProcessPayrollMenu(Scanner input) {
        while (true) {
            System.out.println();
            printSeparator("-");
            System.out.println("PROCESS PAYROLL");
            printSeparator("-");
            System.out.println("[1] One employee");
            System.out.println("[2] All employees");
            System.out.println("[3] Exit the program");
            System.out.print("Choose an option: ");
            String choice = input.nextLine().trim();

            if (choice.equals("1")) {
                System.out.print("Enter the employee number: ");
                String employeeNumber = input.nextLine().trim();
                String[] employeeData = findEmployee(employeeNumber);

                if (employeeData == null) {
                    System.out.println("Employee number does not exist.");
                } else {
                    showPayrollPerEmployee(employeeData);
                }
            } else if (choice.equals("2")) {
                showPayrollForAllEmployees();
            } else if (choice.equals("3")) {
                return;
            } else {
                System.out.println("Invalid option.");
            }
        }
    }

    // Arlove code/code fix: added proper CSV parsing so quoted values are handled correctly.
    static String[] splitCsvLine(String line) {
        String[] data = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
        for (int i = 0; i < data.length; i++) {
            data[i] = data[i].replace("\"", "").trim();
        }
        return data;
    }

    // Arlove code/code fix: added one reusable employee search instead of repeating the same logic.
    static String[] findEmployee(String employeeNumber) {
        try (BufferedReader reader = new BufferedReader(new FileReader(EMPLOYEE_FILE))) {
            reader.readLine();
            String line;

            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }

                String[] data = splitCsvLine(line);
                if (data.length > 18 && data[0].equals(employeeNumber)) {
                    return data;
                }
            }
        } catch (IOException e) {
            System.out.println("Error reading employee file: " + e.getMessage());
        }

        return null;
    }

    // Arlove code/code fix: formatted the employee details output to match the required fields only.
    static void showEmployeeDetails(String[] employeeData) {
        System.out.println();
        printSeparator("=");
        System.out.println("EMPLOYEE DETAILS");
        printSeparator("=");
        System.out.println("Employee Number : " + employeeData[0]);
        System.out.println("Employee Name   : " + employeeData[2] + " " + employeeData[1]);
        System.out.println("Birthday        : " + employeeData[3]);
        printSeparator("=");
    }

    // Arlove code/code fix: added the all employees payroll flow using the same display format as one employee.
    static void showPayrollForAllEmployees() {
        getAttendanceByEmployee();

        try (BufferedReader reader = new BufferedReader(new FileReader(EMPLOYEE_FILE))) {
            reader.readLine();
            String line;

            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }

                String[] employeeData = splitCsvLine(line);
                if (employeeData.length > 18) {
                    showPayrollPerEmployee(employeeData);
                }
            }
        } catch (IOException e) {
            System.out.println("Error reading employee file: " + e.getMessage());
        }
    }

    // Arlove code/code fix: grouped the payroll display per employee from June to December only.
    static void showPayrollPerEmployee(String[] employeeData) {
        String employeeNumber = employeeData[0];
        String employeeName = employeeData[2] + " " + employeeData[1];
        String birthday = employeeData[3];
        double hourlyRate = Double.parseDouble(employeeData[18].replace(",", ""));

        System.out.println();
        printSeparator("=");
        System.out.println("PAYROLL REPORT");
        printSeparator("=");
        System.out.println("Employee #    : " + employeeNumber);
        System.out.println("Employee Name : " + employeeName);
        System.out.println("Birthday      : " + birthday);
        System.out.println("Hourly Rate   : PHP " + (hourlyRate);
        printSeparator("=");

        // Arlove latest code/code fix (2026-03-22): switched the payroll display to use named PayrollResult fields instead of number indexes.
        for (int month = 6; month <= 12; month++) {
            PayrollResult payrollData = computeMonthlyPayroll(employeeNumber, hourlyRate, month);
            int lastDay = YearMonth.of(2024, month).lengthOfMonth();

            System.out.println();
            printSeparator("-");
            System.out.println("Cutoff Date       : " + getMonthName(month) + " 1 to " + getMonthName(month) + " 15");
            System.out.println("Total Hours Worked: " + (payrollData.firstCutoffHours);
            System.out.println("Gross Salary      : PHP " + (payrollData.grossFirst);
            System.out.println("Net Salary        : PHP " + (payrollData.netFirst);

            System.out.println();
            printSeparator("-");
            System.out.println("Cutoff Date       : " + getMonthName(month) + " 16 to " + getMonthName(month) + " " + lastDay);
            System.out.println("Second payout includes all deductions.");
            System.out.println("Total Hours Worked: " + (payrollData.secondCutoffHours);
            System.out.println("Gross Salary      : PHP " + (payrollData.grossSecond);
            printSeparator(".");
            System.out.println("Each Deduction:");
            System.out.println("SSS             : PHP " + (payrollData.sss);
            System.out.println("PhilHealth      : PHP " + (payrollData.philHealth);
            System.out.println("Pag-IBIG        : PHP " + (payrollData.pagIbig);
            System.out.println("Tax             : PHP " + (payrollData.tax);
            printSeparator(".");
            System.out.println("Total Deductions: PHP " + (payrollData.totalDeductions);
            System.out.println("Net Salary      : PHP " + (payrollData.netSecond);
        }
        printSeparator("=");
    }

    // Arlove code/code fix: changed the time computation to cap work from 8:00 AM to 5:00 PM and subtract lunch only once.
    static double computeWorkedHours(String logInText, String logOutText) {
        LocalTime logIn = parseTime(logInText);
        LocalTime logOut = parseTime(logOutText);

        if (logIn == null || logOut == null) {
            return 0;
        }

        LocalTime effectiveIn = logIn;
        if (logIn.isBefore(WORK_START) || !logIn.isAfter(GRACE_LIMIT)) {
            effectiveIn = WORK_START;
        }

        LocalTime effectiveOut = logOut.isAfter(WORK_END) ? WORK_END : logOut;
        if (!effectiveOut.isAfter(effectiveIn)) {
            return 0;
        }

        long totalMinutes = Duration.between(effectiveIn, effectiveOut).toMinutes();
        if (totalMinutes > 60) {
            totalMinutes -= 60;
        } else {
            totalMinutes = 0;
        }

        if (totalMinutes > 480) {
            totalMinutes = 480;
        }

        return totalMinutes / 60.0;
    }

    // Arlove code/code fix: deductions are now computed after combining the first and second cutoff gross salary.
    // Arlove latest code/code fix (2026-03-22): reused the cached attendance records here so monthly payroll no longer reopens the attendance file.
    static PayrollResult computeMonthlyPayroll(String employeeNumber, double hourlyRate, int month) {
        double firstCutoffHours = 0;
        double secondCutoffHours = 0;

        List<AttendanceRecord> attendanceRecords = getAttendanceByEmployee().get(employeeNumber);
        if (attendanceRecords != null) {
            for (AttendanceRecord record : attendanceRecords) {
                if (record.date.getYear() != 2024 || record.date.getMonthValue() != month) {
                    continue;
                }

                double workedHours = computeWorkedHours(record.logInText, record.logOutText);
                if (record.date.getDayOfMonth() <= 15) {
                    firstCutoffHours += workedHours;
                } else {
                    secondCutoffHours += workedHours;
                }
            }
        }

        double grossFirst = firstCutoffHours * hourlyRate;
        double grossSecond = secondCutoffHours * hourlyRate;
        double combinedGross = grossFirst + grossSecond;

        double sss = computeSSS(combinedGross);
        double philHealth = computePhilHealth(combinedGross);
        double pagIbig = computePagIbig(combinedGross);
        double taxableIncome = combinedGross - sss - philHealth - pagIbig;
        if (taxableIncome < 0) {
            taxableIncome = 0;
        }

        double tax = computeTax(taxableIncome);
        double totalDeductions = sss + philHealth + pagIbig + tax;
        double netFirst = grossFirst;
        double netSecond = grossSecond - totalDeductions;

        return new PayrollResult(firstCutoffHours, secondCutoffHours, grossFirst, grossSecond,
                sss, philHealth, pagIbig, tax, totalDeductions, netFirst, netSecond);
    }

    // Arlove latest code/code fix (2026-03-22): cached the attendance file once so all payroll reports reuse the same parsed records.
    static Map<String, List<AttendanceRecord>> getAttendanceByEmployee() {
        if (attendanceByEmployee != null) {
            return attendanceByEmployee;
        }

        Map<String, List<AttendanceRecord>> loadedAttendance = new HashMap<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(ATTENDANCE_FILE))) {
            reader.readLine();
            String line;

            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }

                String[] data = splitCsvLine(line);
                if (data.length < 6) {
                    continue;
                }

                LocalDate recordDate = parseDate(data[3]);
                if (recordDate == null) {
                    continue;
                }

                loadedAttendance
                    .computeIfAbsent(data[0], key -> new ArrayList<>())
                    .add(new AttendanceRecord(recordDate, data[4], data[5]));
            }
        } catch (IOException e) {
            System.out.println("Error reading attendance file: " + e.getMessage());
        }

        attendanceByEmployee = loadedAttendance;
        return attendanceByEmployee;
    }

    // Arlove code/code fix: kept the deduction methods procedural and separated for easier checking.
    static double computeSSS(double salary) {
        if (salary <= 0) {
            return 0;
        }
        if (salary < 3250) {
            return 135.00;
        }
        if (salary >= 24750) {
            return 1125.00;
        }

        double base = 3250;
        double contribution = 157.50;

        while (salary >= base + 500) {
            base += 500;
            contribution += 22.50;
        }

        return contribution;
    }

    // Arlove code/code fix: kept PhilHealth based on the total combined gross for the month.
    static double computePhilHealth(double salary) {
        if (salary <= 0) {
            return 0;
        }
        if (salary <= 10000) {
            return 150.00;
        }
        if (salary < 60000) {
            return (salary * 0.03) / 2.0;
        }
        return 900.00;
    }

    // Arlove code/code fix: kept the Pag-IBIG cap at 100 and based it on the combined gross salary.
    static double computePagIbig(double salary) {
        if (salary <= 0) {
            return 0;
        }

        double contribution;
        if (salary >= 1000 && salary <= 1500) {
            contribution = salary * 0.01;
        } else {
            contribution = salary * 0.02;
        }

        if (contribution > 100) {
            contribution = 100;
        }

        return contribution;
    }

    // Arlove code/code fix: renamed the tax method so the output label can stay as Tax in the payroll section.
    static double computeTax(double taxableIncome) {
        if (taxableIncome <= 20832) {
            return 0;
        } else if (taxableIncome <= 33333) {
            return (taxableIncome - 20833) * 0.20;
        } else if (taxableIncome <= 66667) {
            return 2500 + (taxableIncome - 33333) * 0.25;
        } else if (taxableIncome <= 166667) {
            return 10833 + (taxableIncome - 66667) * 0.30;
        } else if (taxableIncome <= 666667) {
            return 40833.33 + (taxableIncome - 166667) * 0.32;
        } else {
            return 200833.33 + (taxableIncome - 666667) * 0.35;
        }
    }

    // Arlove code/code fix: added a date parser so the attendance records can be filtered from June to December correctly.
    static LocalDate parseDate(String dateText) {
        try {
            return LocalDate.parse(dateText.trim(), DATE_FORMAT);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    // Arlove code/code fix: added a time parser so invalid attendance time values do not crash the program.
    static LocalTime parseTime(String timeText) {
        try {
            return LocalTime.parse(timeText.trim(), TIME_FORMAT);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    // Arlove code/code fix: added one month-name helper so the payroll output is consistent from June to December.
    static String getMonthName(int month) {
        switch (month) {
            case 6:
                return "June";
            case 7:
                return "July";
            case 8:
                return "August";
            case 9:
                return "September";
            case 10:
                return "October";
            case 11:
                return "November";
            case 12:
                return "December";
            default:
                return "Month";
        }
    }
    // Arlove code/code fix: added a reusable separator printer for cleaner console spacing and sections.
    static void printSeparator(String symbol) {
        System.out.println(symbol.repeat(48));
    }
}
