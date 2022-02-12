package com.osiris.vismemory;

import org.jutils.jprocesses.JProcesses;
import org.jutils.jprocesses.model.ProcessInfo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class Main {
    public static PrintStream out = System.out;
    public static List<ProcessInfo> processes;
    public static List<List<ProcessInfo>> processGroups;
    public static List<ProcessInfo> processRest;

    private static final DecimalFormat df = new DecimalFormat("0.00");

    public static void main(String[] args) throws IOException {
        out.println("Initialised VisMemory. Enter 'help' for a list of all commands.");
        out.println("Scanning memory... This may take a bit.");
        scan();
        out.println("Done! Scanned "+processes.size()+" processes.");
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        String command = null;
        while (true){
            command = reader.readLine();
            switch (command){
                case "help":
                    out.println("help | Prints all available commands to the console.");
                    out.println("exit | Exits the program.");
                    out.println("scan | Scans the RAM and creates a simplified report.");
                    out.println("details | Shows details about the groups and their ram usage.");
                    out.println("details <group-index> | Shows details about the processes inside a group.");
                    break;
                case "exit":
                    out.println("See you soon!");
                    System.exit(0);
                    out.println("Done! Enter 'details' to show the report.");
                    break;
                case "scan":
                    out.println("Scanning processes and gathering RAM information...");
                    out.println("This may take a bit.");
                    scan();
                    out.println("Done! Scanned "+processes.size()+" processes.");
                    break;
                case "details":
                    if (processes ==null){
                        out.println("Failed! Make sure to call 'scan' at least once before!");
                        break;
                    }
                    out.println("Amount of processes: "+ processes.size());
                    out.println("Amount of groups: "+ processGroups.size());
                    out.println("Total used RAM: "+ calcRamInGBPretty(processes)+"GB");
                    for (int i = 0; i < processGroups.size(); i++) {
                        List<ProcessInfo> list = processGroups.get(i);
                        out.println("Group "+i+": "+ calcRamInGBPretty(list)+"GB with "+list.size()+" processes names similar to '"+list.get(0).getName()+"'.");
                    }
                    out.println("Rest: "+ calcRamInGBPretty(processRest)+"GB with "+processRest.size()+" processes.");
                    break;
                default:
                    if (command.startsWith("details")){
                        int i = Integer.parseInt(command.replace("details", "").trim());
                        if (i >= processGroups.size()){
                            out.println("Failed, since index is out of bounds.");
                            break;
                        }
                        printDetails(processGroups.get(i));
                    } else{
                        out.println("Unknown command!");
                    }
                    break;
            }
        }
    }

    private static void printDetails(List<ProcessInfo> processInfos) {
        for (ProcessInfo proc :
                processInfos) {
            out.println("PID:"+proc.getPid()+" NAME:"+proc.getName()+" RAM:"+ calcRamInGBPretty(Collections.singletonList(proc))+"GB COMMAND:"+proc.getCommand());
        }
    }

    private static String calcRamInGBPretty(List<ProcessInfo> processes){
        return df.format(calcRamInGB(processes));
    }

    private static Double calcRamInGB(List<ProcessInfo> processes){
        long kb = 0;
        for (ProcessInfo proc :
                processes) {
            kb += Long.parseLong(proc.getPhysicalMemory());
        }
        return kb * 0.00000095367432;
    }


    private static void scan() {
        processes = new CopyOnWriteArrayList<>(JProcesses.getProcessList());
        processGroups = new ArrayList<>();
        processRest = new ArrayList<>();
        for (final ProcessInfo proc : processes) {
            // Check if this process already was added to the summary list
            boolean skip = false;
            for (List<ProcessInfo> list:
                    processGroups) {
                for (ProcessInfo proc1 :
                        list) {
                    if (proc1.equals(proc)) {
                        skip = true;
                        break;
                    }
                }
                if (skip) break;
            }
            if (skip) continue;

            List<ProcessInfo> similarProcesses = new ArrayList<>();
            for (ProcessInfo proc2 :
                    processes) {
                if (!proc2.getPid().equals(proc.getPid()) && StringComparator.similarity(proc2.getName(), proc.getName()) > 0.5)
                    similarProcesses.add(proc2);
            }
            if (similarProcesses.size() > 5)
                processGroups.add(similarProcesses);
            else
                processRest.addAll(similarProcesses);
        }
    }

}
