package org.example;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

public class VixSampler {

    public static void main(String[] args) {
        // --- CONFIGURATION: EXACT ABSOLUTE PATHS TO YOUR FILES ---
        String basePath = "C:\\Users\\BC-Tech\\IdeaProjects\\Statsproject\\";

        String datePoolFile = basePath + "MarchJuneSeptemberDecember.csv";
        String vixDataFile = basePath + "VIX_History.csv";
        String outputFile = basePath + "quiet_group_sample.csv";

        // Target months: Earnings (1, 4, 7, 10).
        // For the quiet months run, change this to: Arrays.asList(3, 6, 9, 12);
        // And change datePoolFile to basePath + "MarchJuneSeptemberDecember.csv" and outputFile to basePath + "quiet_group_sample.csv"
        List<Integer> targetMonths = Arrays.asList(3, 6, 9, 12);
        int yearsStart = 2021;
        int yearsEnd = 2025;
        // -------------------------------------------------------------

        Map<String, String> dayOfWeekMap = new HashMap<>();
        // Group days strictly by Month ID (e.g., Key "1" holds ALL January days from 2021-2025)
        Map<Integer, List<String>> monthsMasterPool = new HashMap<>();

        System.out.println("Step 1: Reading and pooling available dates by month...");

        try (BufferedReader br = new BufferedReader(new FileReader(datePoolFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();

                if (line.isEmpty() || line.toUpperCase().contains("TRADING DAY") || line.toUpperCase().contains("DATE")) {
                    continue;
                }

                String[] tokens = line.split(",");
                if (tokens.length >= 5) {
                    String dateStr = tokens[0].trim();
                    String dayOfWeek = tokens[4].trim();

                    dayOfWeekMap.put(dateStr, dayOfWeek);

                    String[] dateParts = dateStr.split("-");
                    if (dateParts.length == 3) {
                        String year = dateParts[0];
                        String month = dateParts[1];

                        int monthInt = Integer.parseInt(month);
                        int yearInt = Integer.parseInt(year);

                        // Pool days together across all 5 years into their respective month bucket
                        if (yearInt >= yearsStart && yearInt <= yearsEnd && targetMonths.contains(monthInt)) {
                            monthsMasterPool.putIfAbsent(monthInt, new ArrayList<>());
                            monthsMasterPool.get(monthInt).add(dateStr);
                        }
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading date pool file (" + datePoolFile + "): " + e.getMessage());
            return;
        }

        System.out.println("Step 2: Indexing historical VIX data prices...");
        Map<String, Double[]> vixPriceMap = new HashMap<>();

        try (BufferedReader br = new BufferedReader(new FileReader(vixDataFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.toUpperCase().contains("DATE") || line.toUpperCase().contains("HIGH")) continue;

                String[] tokens = line.split(",");
                if (tokens.length >= 4) {
                    String rawDate = tokens[0].trim();
                    String standardDate = formatVixDate(rawDate);

                    try {
                        double high = Double.parseDouble(tokens[2].trim());
                        double low = Double.parseDouble(tokens[3].trim());
                        vixPriceMap.put(standardDate, new Double[]{high, low});
                    } catch (NumberFormatException e) {
                        // Skip headers quietly
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading VIX data file (" + vixDataFile + "): " + e.getMessage());
            return;
        }

        System.out.println("Step 3: Executing stratified sampling (10 days from each combined month pool)...");
        List<String> finalizedSampleDates = new ArrayList<>();
        Random rand = new Random();

        // Loop through each of the 4 combined month pools
        for (int month : targetMonths) {
            List<String> masterMonthPool = monthsMasterPool.get(month);

            if (masterMonthPool != null && !masterMonthPool.isEmpty()) {
                // Scramble the master pool containing all 5 years of this month combined
                Collections.shuffle(masterMonthPool, rand);

                int successfulPicks = 0;
                int poolIndex = 0;

                // Select exactly 10 valid market days from this month's pool
                while (successfulPicks < 10 && poolIndex < masterMonthPool.size()) {
                    String candidateDate = masterMonthPool.get(poolIndex);

                    if (vixPriceMap.containsKey(candidateDate)) {
                        finalizedSampleDates.add(candidateDate);
                        successfulPicks++;
                    } else {
                        System.out.println("♻️ Holiday found (" + candidateDate + "). Searching grand pool for another day instead.");
                    }
                    poolIndex++;
                }

                if (successfulPicks < 10) {
                    System.out.println("⚠️ Warning: Could only find " + successfulPicks + " valid days total for Month ID: " + month);
                }
            }
        }

        System.out.println("Step 4: Computing High-Low swings and exporting finalized data...");

        try (PrintWriter pw = new PrintWriter(new FileWriter(outputFile))) {
            pw.println("Date,Day_of_Week,Daily_Volatility_Swing");

            int successfullyMatched = 0;
            for (String sampleDate : finalizedSampleDates) {
                if (vixPriceMap.containsKey(sampleDate)) {
                    Double[] prices = vixPriceMap.get(sampleDate);
                    double high = prices[0];
                    double low = prices[1];
                    double swing = high - low;
                    String dayName = dayOfWeekMap.get(sampleDate);

                    pw.printf("%s,%s,%.2f\n", sampleDate, dayName, swing);
                    successfullyMatched++;
                }
            }
            System.out.println("🎉 Done! Successfully generated '" + outputFile + "' with exactly " + successfullyMatched + " records.");
        } catch (IOException e) {
            System.err.println("Error writing output file: " + e.getMessage());
        }
    }

    private static String formatVixDate(String rawDate) {
        if (rawDate.contains("/")) {
            String[] parts = rawDate.split("/");
            if (parts.length == 3) {
                String month = String.format("%02d", Integer.parseInt(parts[0]));
                String day = String.format("%02d", Integer.parseInt(parts[1]));
                String year = parts[2];
                return String.format("%s-%s-%s", year, month, day);
            }
        }
        return rawDate;
    }
}