import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Scanner;

public class MotorPHv3 {
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

    // Arlove code/code fix: preserved the original broken payroll block as a comment based on the old line 87 to 374 range.
    

    //=======================================
    // DEDUCTION METHODS
    //=======================================
    static double computeSSS(double grossSalary) {
        if (grossSalary < 3250) return 135.00;
        if (grossSalary >= 24750) return 1125.00;

        double base = 3250;
        double contribution = 157.50;

        while (grossSalary >= base + 500){
            base += 500;
            contribution += 22.50;
        }

        return contribution;
    }

    static double computePhilHealth(double grossSalary) {
        if (grossSalary <= 10000) {
            return 300 / 2.0;
        } else if (grossSalary < 60000) {
            return (grossSalary * 0.03) / 2;
        } else {
            return 1800 / 2.0;
        }
    }

    static double computePagIbig(double grossSalary) {
        double pagIbig;
        if (grossSalary >= 1000 && grossSalary <= 1500) {
            pagIbig = grossSalary * 0.01;
        } else if (grossSalary > 1500) {
            pagIbig = grossSalary * 0.02;
        } else {
            pagIbig = 0;
        }
        if (pagIbig > 100) pagIbig = 100;
        return pagIbig;
    }

    static double computeWithholdingTax(double taxableIncome) {
        if      (taxableIncome <= 20832)  return 0;
        else if (taxableIncome <= 33333)  return (taxableIncome - 20833) * 0.20;
        else if (taxableIncome <= 66667)  return 2500 + (taxableIncome - 33333) * 0.25;
        else if (taxableIncome <= 166667) return 10833 + (taxableIncome - 66667) * 0.30;
        else if (taxableIncome <= 666667) return 40833.33 + (taxableIncome - 166667) * 0.32;
        else                              return 200833.33 + (taxableIncome - 666667) * 0.35;
    }

    //=======================================
    // COMPUTE GROSS PAY, returns double[]
    // [0] hoursFirstCutoff
    // [1] hoursSecondCutoff
    // [2] grossFirst
    // [3] grossSecond
    // [4] sss
    // [5] philHealth
    // [6] pagIbig
    // [7] tax
    // [8] totalDeductions
    // [9] netPaySecond
    // [10] daysInMonth
    //=======================================
    //Sir AJ's logic
    static double[] computeGrossPay(String employeeNumber, double hourlyRate, int month) {
        double hoursFirstCutoff  = 0;
        double hoursSecondCutoff = 0;
        int daysInMonth = YearMonth.of(2024, month).lengthOfMonth();

        try (BufferedReader br = new BufferedReader(new FileReader(ATTENDANCE_FILE))) {
            br.readLine();
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;

                String[] data = line.split(",(?=(?:[^\"]\"[^\"]\")[^\"]$)");//regex
                for (int i = 0; i < data.length; i++) {
                    data[i] = data[i].replace("\"", "").trim();
                }

                if (!data[0].equals(employeeNumber)) continue;

                String[] dateParts = data[3].split("/");
                int recordMonth = Integer.parseInt(dateParts[0]);
                int day         = Integer.parseInt(dateParts[1]);
                int year        = Integer.parseInt(dateParts[2]);

                if (year != 2024 || recordMonth != month) continue;

                double hours = computeWorkedHours(data[4], data[5]);

                if (day <= 15) hoursFirstCutoff  += hours;
                else           hoursSecondCutoff += hours;
            }
        } catch (Exception e) {
            System.out.println("Error reading attendance for month " + month);
        }

        double grossFirst    = hoursFirstCutoff  * hourlyRate;
        double grossSecond   = hoursSecondCutoff * hourlyRate;
        double combinedGross = grossFirst + grossSecond;

        double sss        = computeSSS(combinedGross);
        double philHealth = computePhilHealth(combinedGross);
        double pagIbig    = computePagIbig(combinedGross);

        double taxableIncome   = combinedGross - sss - philHealth - pagIbig;
        double tax             = computeWithholdingTax(taxableIncome);

        double totalDeductions = sss + philHealth + pagIbig + tax;
        double netPaySecond    = grossSecond - totalDeductions;

        return new double[]{
            hoursFirstCutoff,   // [0]
            hoursSecondCutoff,  // [1]
            grossFirst,         // [2]
            grossSecond,        // [3]
            sss,                // [4]
            philHealth,         // [5]
            pagIbig,            // [6]
            tax,                // [7]
            totalDeductions,    // [8]
            netPaySecond,       // [9]
            daysInMonth         // [10]
        };
    }

    //=======================================
    // DISPLAY PAYROLL
    //=======================================
    static void displayPayroll(String employeeNumber, String lastName, String firstName,
                                String birthday, double hourlyRate, int month, double[] p) {
        String monthName;
        switch (month) {
            case 6:  monthName = "June";      break;
            case 7:  monthName = "July";      break;
            case 8:  monthName = "August";    break;
            case 9:  monthName = "September"; break;
            case 10: monthName = "October";   break;
            case 11: monthName = "November";  break;
            case 12: monthName = "December";  break;
            default: monthName = "Month " + month;
        }

        System.out.println("\n========================================");
        System.out.println("Employee Number   : " + employeeNumber);
        System.out.println("Employee Name     : " + firstName + " " + lastName);
        System.out.println("Birthday          : " + birthday);
        //System.out.println("Hourly Rate     : " + hourlyRate);
        System.out.println("========================================");

        // 1st cutoff
        System.out.println("\nCutoff Date         : " + monthName + " 1 to 15");
        System.out.println("Total Hours Worked  : " + p[0]);
        System.out.println("Gross Salary        : " + p[2]);
        System.out.println("Net Salary          : " + p[2]);

        // 2nd cutoff
        System.out.println("\nCutoff Date         : " + monthName + " 16 to " + (int)p[10]);
        System.out.println("Total Hours Worked  : " + p[1]);
        System.out.println("Gross Salary        : " + p[3]);
        System.out.println("Deductions:");
        System.out.println("  SSS               : " + p[4]);
        System.out.println("  PhilHealth        : " + p[5]);
        System.out.println("  Pag-IBIG          : " + p[6]);
        System.out.println("  Withholding Tax   : " + p[7]);
        System.out.println("Total Deductions    : " + p[8]);
        System.out.println("Net Salary          : " + p[9]);
    }
    */

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
        System.out.println("Hourly Rate   : PHP " + formatValue(hourlyRate));
        printSeparator("=");

        for (int month = 6; month <= 12; month++) {
            double[] payrollData = computeMonthlyPayroll(employeeNumber, hourlyRate, month);
            int lastDay = YearMonth.of(2024, month).lengthOfMonth();

            System.out.println();
            printSeparator("-");
            System.out.println("Cutoff Date       : " + getMonthName(month) + " 1 to " + getMonthName(month) + " 15");
            System.out.println("Total Hours Worked: " + formatValue(payrollData[0]));
            System.out.println("Gross Salary      : PHP " + formatValue(payrollData[2]));
            System.out.println("Net Salary        : PHP " + formatValue(payrollData[9]));

            System.out.println();
            printSeparator("-");
            System.out.println("Cutoff Date       : " + getMonthName(month) + " 16 to " + getMonthName(month) + " " + lastDay);
            System.out.println("Second payout includes all deductions.");
            System.out.println("Total Hours Worked: " + formatValue(payrollData[1]));
            System.out.println("Gross Salary      : PHP " + formatValue(payrollData[3]));
            printSeparator(".");
            System.out.println("Each Deduction:");
            System.out.println("SSS             : PHP " + formatValue(payrollData[4]));
            System.out.println("PhilHealth      : PHP " + formatValue(payrollData[5]));
            System.out.println("Pag-IBIG        : PHP " + formatValue(payrollData[6]));
            System.out.println("Tax             : PHP " + formatValue(payrollData[7]));
            printSeparator(".");
            System.out.println("Total Deductions: PHP " + formatValue(payrollData[8]));
            System.out.println("Net Salary      : PHP " + formatValue(payrollData[10]));
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
    static double[] computeMonthlyPayroll(String employeeNumber, double hourlyRate, int month) {
        double firstCutoffHours = 0;
        double secondCutoffHours = 0;

        try (BufferedReader reader = new BufferedReader(new FileReader(ATTENDANCE_FILE))) {
            reader.readLine();
            String line;

            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }

                String[] data = splitCsvLine(line);
                if (data.length < 6 || !data[0].equals(employeeNumber)) {
                    continue;
                }

                LocalDate recordDate = parseDate(data[3]);
                if (recordDate == null || recordDate.getYear() != 2024 || recordDate.getMonthValue() != month) {
                    continue;
                }

                double workedHours = computeWorkedHours(data[4], data[5]);
                if (recordDate.getDayOfMonth() <= 15) {
                    firstCutoffHours += workedHours;
                } else {
                    secondCutoffHours += workedHours;
                }
            }
        } catch (IOException e) {
            System.out.println("Error reading attendance file: " + e.getMessage());
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

        return new double[] {
            firstCutoffHours,
            secondCutoffHours,
            grossFirst,
            grossSecond,
            sss,
            philHealth,
            pagIbig,
            tax,
            totalDeductions,
            netFirst,
            netSecond
        };
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

    // Arlove code/code fix: added two-decimal display formatting to make the output easier to read.
    static String formatValue(double value) {
        return String.format("%.2f", value);
    }

    // Arlove code/code fix: added a reusable separator printer for cleaner console spacing and sections.
    static void printSeparator(String symbol) {
        System.out.println(symbol.repeat(48));
    }
}
