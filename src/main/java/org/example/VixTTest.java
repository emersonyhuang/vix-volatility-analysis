package org.example;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class VixTTest {

    public static void main(String[] args) {
        String dataDir = "data/";
        String earningsFile = dataDir + "earnings_group_sample.csv";
        String quietFile = dataDir + "quiet_group_sample.csv";

        List<Double> earningsSwings = readSwings(earningsFile);
        List<Double> quietSwings = readSwings(quietFile);

        if (earningsSwings.isEmpty() || quietSwings.isEmpty()) {
            System.err.println("Error: One or both data files are empty!");
            return;
        }

        // Calculate Sample Statistics
        double mean1 = getMean(earningsSwings);
        double mean2 = getMean(quietSwings);
        double s1 = getStdDev(earningsSwings, mean1);
        double s2 = getStdDev(quietSwings, mean2);
        int n1 = earningsSwings.size();
        int n2 = quietSwings.size();

        // Two-Sample t-test Math (Conservative Degrees of Freedom)
        double pooledSE = Math.sqrt((s1 * s1 / n1) + (s2 * s2 / n2));
        double tScore = (mean1 - mean2) / pooledSE;
        int df = Math.min(n1 - 1, n2 - 1); // AP Stats conservative method for DF

        System.out.println("=== AP STATISTICS INFERENCE RESULTS ===");
        System.out.printf("Earnings Group: n = %d, Mean = %.4f, StdDev = %.4f\n", n1, mean1, s1);
        System.out.printf("Quiet Group:    n = %d, Mean = %.4f, StdDev = %.4f\n", n2, mean2, s2);
        System.out.println("---------------------------------------");
        System.out.printf("Calculated t-score = %.4f\n", tScore);
        System.out.printf("Degrees of Freedom (df) = %d\n", df);

        // Approximate p-value using a basic T-distribution tail approximation
        double pValue = approximateUpperTailPValue(tScore, df);
        System.out.printf("Approximate p-value (Ha: mu_Earnings > mu_Quiet) = %.4f\n", pValue);
        System.out.println("---------------------------------------");

        if (pValue < 0.05) {
            System.out.println("Conclusion: Reject H0 at alpha = 0.05. There is significant evidence that");
            System.out.println("the market swings more during earnings months than quiet months!");
        } else {
            System.out.println("Conclusion: Fail to Reject H0 at alpha = 0.05. There is not enough evidence");
            System.out.println("to conclude that the market swings more during earnings months.");
        }
    }

    private static List<Double> readSwings(String filePath) {
        List<Double> swings = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.toUpperCase().contains("SWING") || line.trim().isEmpty()) continue;
                String[] tokens = line.split(",");
                if (tokens.length >= 3) {
                    swings.add(Double.parseDouble(tokens[2].trim()));
                }
            }
        } catch (IOException | NumberFormatException e) {
            System.err.println("Error parsing file: " + filePath + " - " + e.getMessage());
        }
        return swings;
    }

    private static double getMean(List<Double> list) {
        double sum = 0;
        for (double d : list) sum += d;
        return sum / list.size();
    }

    private static double getStdDev(List<Double> list, double mean) {
        double sumSqDiff = 0;
        for (double d : list) {
            sumSqDiff += Math.pow(d - mean, 2);
        }
        return Math.sqrt(sumSqDiff / (list.size() - 1)); // Sample standard dev (n-1)
    }

    // Rough polynomial calculation to give you a quick directional p-value check
    private static double approximateUpperTailPValue(double t, int df) {
        if (t <= 0) return 1.0 - 0.5 * Math.exp(t);
        return 0.5 * Math.exp(-t);
    }
}