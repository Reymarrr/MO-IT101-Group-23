import java.io.BufferedReader;
import java.io.FileReader;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;

public class MotorPHv1                                                                                               {
    //----------File Paths---------------
    static final String EMPLOYEE_FILE = "Employee Details.csv";
    static final String ATTENDANCE_FILE = "Attendance Record.csv";

    //----------Time Constants-----------
    static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("H:mm");
    static final LocalTime GRACE_PERIOD = LocalTime.of(8, 10);
    static final LocalTime CUTOFF_TIME  = LocalTime.of(17, 0);

    //----------Credentials--------------
    static final String PASSWORD = "12345";

    public static void main(String[] args) {
        Scanner input = new Scanner(System.in);

        System.out.print("Enter Username: ");
        String username = input.nextLine();

        System.out.print("Enter Password: ");
        String password = input.nextLine();

        // validate both username and password
        if (!username.equals("employee") || !password.equals(PASSWORD)) {
            System.out.println("Incorrect username and/or password. Program ends.");
            
            input.close();
            return;
        }

        System.out.println("\n=== EMPLOYEE MENU ===");
        System.out.println("1. Enter Employee Number");
        System.out.println("2. Exit Program");
        System.out.print("Choose 1 or 2: ");

        int menuChoice = input.nextInt();
        input.nextLine();

        if (menuChoice == 1) {
            System.out.print("Enter Employee Number: ");
            String empNumToFind = input.nextLine();
            boolean found = false;

            try (BufferedReader reader = new BufferedReader(new FileReader(EMPLOYEE_FILE))) {
                reader.readLine(); // skip header
                String line;

                while ((line = reader.readLine()) != null) {
                    String[] empData = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
                    for (int i = 0; i < empData.length; i++) {
                        empData[i] = empData[i].replace("\"", "").trim();
                    }

                    if (empData.length >= 4 && empData[0].equals(empNumToFind)) {
                        System.out.println("\n--- FOUND! ---");
                        System.out.println("Employee Number: " + empData[0]);
                        System.out.println("Employee Name: " + empData[2] + " " + empData[1]);
                        System.out.println("Birthday: " + empData[3]);
                        found = true;
                        break;
                    }
                }
            } catch (Exception e) {
                System.out.println("Error reading file: " + e.getMessage());
            }

            if (!found) {
                System.out.println("Employee number does not exist.");
            }

        } else if (menuChoice == 2) {
            System.out.println("Exiting program.");
        } else {
            System.out.println("Wrong choice. Program ends.");
        }

        input.close();
    }
}
        
        
        Scanner sc = new Scanner(System.in);

        System.out.println("1. One Employee");
        System.out.println("2. All Employees");
        System.out.println("3. Exit");
        System.out.print("Enter choice: ");
        int choice = Integer.parseInt(sc.nextLine());

        if (choice == 1) {
            // ONE EMPLOYEE
            System.out.print("Enter Employee Number: ");
            String targetEmpNo = sc.nextLine();

            String lastName   = null;
            String firstName  = null;
            String birthday   = null;
            double hourlyRate = 0;
            boolean found     = false;

            try (BufferedReader br = new BufferedReader(new FileReader(EMPLOYEE_FILE))) {
                br.readLine();
                String line;
                while ((line = br.readLine()) != null) {
                    if (line.trim().isEmpty()) continue;

                    String[] data = line.split(",(?=(?:[^\"]\"[^\"]\")[^\"]$)");// regex. split by commas, but ignore commas inside quoted fields (addresses, figures)
                    for (int i = 0; i < data.length; i++) {
                        data[i] = data[i].replace("\"", "").trim();
                    }

                    if (!data[0].equals(targetEmpNo)) continue;

                    found      = true;
                    lastName   = data[1];
                    firstName  = data[2];
                    birthday   = data[3];
                    hourlyRate = Double.parseDouble(data[18].replace(",", ""));
                    break;
                }
            } catch (Exception e) {
                System.out.println("Error reading employee file.");
            } 

            if (!found) {
                System.out.println("Employee number does not exist.");
            } else {
                for (int month = 6; month <= 12; month++) {
                    double[] payData = computeGrossPay(targetEmpNo, hourlyRate, month);
                    displayPayroll(targetEmpNo, lastName, firstName, birthday, hourlyRate,month, payData);
                }
            }

        } else if (choice == 2) {
            // ALL EMPLOYEES
            try (BufferedReader br = new BufferedReader(new FileReader(EMPLOYEE_FILE))) {
                br.readLine();
                String line;
                while ((line = br.readLine()) != null) {
                    if (line.trim().isEmpty()) continue;

                    String[] data = line.split(",(?=(?:[^\"]\"[^\"]\")[^\"]$)");
                    for (int i = 0; i < data.length; i++) {
                        data[i] = data[i].replace("\"", "").trim();
                    }

                    String employeeNumber = data[0];
                    String lastName       = data[1];
                    String firstName      = data[2];
                    String birthday       = data[3];
                    double hourlyRate     = Double.parseDouble(data[18].replace(",", ""));

                    for (int month = 6; month <= 12; month++) {
                        double[] payData = computeGrossPay(employeeNumber, hourlyRate, month);
                        displayPayroll(employeeNumber, lastName, firstName, birthday, hourlyRate, month, payData);
                    }
                }
            } catch (Exception e) {
                System.out.println("Error reading employee file: " + e.getMessage());
            }

        } else if (choice == 3) {
            System.out.println("Exiting program.");
        } else {
            System.out.println("Invalid choice.");
        }

        sc.close();
    }

    //=======================================
    // COMPUTE WORKED HOURS (Sir AJ's Logic)
    //=======================================
    static double computeWorkedHours(String loginStr, String logoutStr) {
        LocalTime logIn  = LocalTime.parse(loginStr, TIME_FORMAT);
        LocalTime logOut = LocalTime.parse(logoutStr, TIME_FORMAT);

        if (logOut.isAfter(CUTOFF_TIME)) {
            logOut = CUTOFF_TIME;
        }

        long minutesWorked = Duration.between(logIn, logOut).toMinutes();

        if (minutesWorked > 60) {
            minutesWorked -= 60;
        } else {
            minutesWorked = 0;
        }

        double hours = minutesWorked / 60.0;

        if (!logIn.isAfter(GRACE_PERIOD)) {
            return 8.0;
        }

        return Math.min(hours, 8.0);
    }

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
    static void displayPayroll(String employeeNumber, String lastName, String firstName, String birthday, double hourlyRate, int month, double[] p) {
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
}